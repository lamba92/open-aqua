using LibreHardwareMonitor.Hardware;
using OpenAqua.Engine.Host.Windows.Model;

namespace OpenAqua.Engine.Host.Windows.Hardware;

public sealed class WindowsHostHardwareController : IHostHardwareController
{
    private readonly Computer _computer;
    private readonly UpdateVisitor _updateVisitor = new();
    private bool _disposed;

    public WindowsHostHardwareController()
    {
        _computer = new Computer
        {
            IsCpuEnabled = true,
            IsGpuEnabled = true,
            IsMotherboardEnabled = true,
            IsStorageEnabled = true,
            IsControllerEnabled = true,
            IsMemoryEnabled = false,
            IsNetworkEnabled = false,
            IsBatteryEnabled = false
        };
        _computer.Open();
    }

    public HostInfo GetHostInfo()
    {
        _computer.Accept(_updateVisitor);

        var devices = new List<HostDevice>();

        foreach (var hardware in _computer.Hardware)
        {
            if (IsAquaComputerDevice(hardware)) continue;
            AddHardwareAndSubHardware(hardware, devices);
        }

        return new HostInfo(devices);
    }

    public async IAsyncEnumerable<HostTelemetry> GetTelemetryStream(
        TimeSpan pollingInterval,
        [System.Runtime.CompilerServices.EnumeratorCancellation] CancellationToken cancellationToken = default)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            _computer.Accept(_updateVisitor);

            var sensorTelemetryList = new List<HostSensorTelemetry>();
            var fanTelemetryList = new List<HostFanTelemetry>();

            foreach (var hardware in _computer.Hardware)
            {
                if (IsAquaComputerDevice(hardware)) continue;
                CollectTelemetry(hardware, sensorTelemetryList, fanTelemetryList);
            }

            yield return new HostTelemetry(sensorTelemetryList, fanTelemetryList);

            await Task.Delay(pollingInterval, cancellationToken);
        }
    }

    public async Task StreamFanControlAsync(
        IAsyncEnumerable<HostFanControlRequest> requests,
        CancellationToken cancellationToken = default)
    {
        EventHandler processExitHandler = (_, _) => RestoreFanDefaults();
        ConsoleCancelEventHandler cancelHandler = (_, _) => RestoreFanDefaults();

        AppDomain.CurrentDomain.ProcessExit += processExitHandler;
        Console.CancelKeyPress += cancelHandler;

        try
        {
            var controls = BuildControlLookup();

            await foreach (var request in requests.WithCancellation(cancellationToken))
            {
                SetFanSpeed(controls, request.Channel, request.PwmPercent);
            }
        }
        finally
        {
            RestoreFanDefaults();
            AppDomain.CurrentDomain.ProcessExit -= processExitHandler;
            Console.CancelKeyPress -= cancelHandler;
        }
    }

    public void RestoreFanDefaults()
    {
        foreach (var hardware in _computer.Hardware)
        {
            if (IsAquaComputerDevice(hardware)) continue;
            RestoreHardwareFanDefaults(hardware);
        }
    }

    private static void RestoreHardwareFanDefaults(IHardware hardware)
    {
        foreach (var sensor in hardware.Sensors)
        {
            if (sensor.SensorType == SensorType.Control && sensor.Control != null)
            {
                try
                {
                    sensor.Control.SetDefault();
                }
                catch
                {
                    // Ignore restore errors
                }
            }
        }

        foreach (var sub in hardware.SubHardware)
        {
            if (!IsAquaComputerDevice(sub))
            {
                RestoreHardwareFanDefaults(sub);
            }
        }
    }

    private Dictionary<string, IControl> BuildControlLookup()
    {
        var map = new Dictionary<string, IControl>(StringComparer.OrdinalIgnoreCase);

        void ScanHardware(IHardware hardware)
        {
            var fanSensors = hardware.Sensors
                .Where(s => s.SensorType == SensorType.Fan)
                .ToList();
            var controlSensors = hardware.Sensors
                .Where(s => s.SensorType == SensorType.Control && s.Control != null)
                .ToList();

            foreach (var controlSensor in controlSensors)
            {
                var ctrl = controlSensor.Control;
                if (ctrl == null) continue;

                var ctrlId = controlSensor.Identifier.ToString();
                map[ctrlId] = ctrl;

                if (ctrlId.Contains("/control/"))
                {
                    map[ctrlId.Replace("/control/", "/fan/")] = ctrl;
                }

                var pairedFan = fanSensors.FirstOrDefault(f => f.Index == controlSensor.Index);
                if (pairedFan != null)
                {
                    var fanId = pairedFan.Identifier.ToString();
                    map[fanId] = ctrl;
                    if (fanId.Contains("/fan/"))
                    {
                        map[fanId.Replace("/fan/", "/control/")] = ctrl;
                    }
                }
            }

            foreach (var sub in hardware.SubHardware)
            {
                if (!IsAquaComputerDevice(sub))
                {
                    ScanHardware(sub);
                }
            }
        }

        foreach (var hardware in _computer.Hardware)
        {
            if (!IsAquaComputerDevice(hardware))
            {
                ScanHardware(hardware);
            }
        }

        return map;
    }

    private static void SetFanSpeed(
        Dictionary<string, IControl> controls,
        string channel,
        double pwmPercent)
    {
        if (string.IsNullOrWhiteSpace(channel)) return;

        if (controls.TryGetValue(channel.Trim(), out var control))
        {
            var min = control.MinSoftwareValue;
            var max = control.MaxSoftwareValue;
            var target = max > min ? Math.Clamp((float)pwmPercent, min, max) : (float)pwmPercent;
            control.SetSoftware(target);
        }
    }

    private void AddHardwareAndSubHardware(IHardware hardware, List<HostDevice> devices)
    {
        var tempSensors = hardware.Sensors
            .Where(s => s.SensorType == SensorType.Temperature)
            .Select(s => new HostSensorDescriptor(s.Identifier.ToString(), s.Name))
            .ToList();

        var fanSensors = hardware.Sensors
            .Where(s => s.SensorType == SensorType.Fan)
            .ToList();
        var controlSensors = hardware.Sensors
            .Where(s => s.SensorType == SensorType.Control && s.Control != null)
            .ToList();

        var fanDescriptors = new List<HostFanDescriptor>();
        var allIndices = fanSensors.Select(f => f.Index)
            .Union(controlSensors.Select(c => c.Index))
            .OrderBy(i => i)
            .ToList();

        foreach (var index in allIndices)
        {
            var fanSensor = fanSensors.FirstOrDefault(f => f.Index == index);
            var controlSensor = controlSensors.FirstOrDefault(c => c.Index == index);

            if (controlSensor != null)
            {
                var id = fanSensor?.Identifier.ToString() ?? controlSensor.Identifier.ToString();
                var name = !string.IsNullOrWhiteSpace(fanSensor?.Name) ? fanSensor.Name : controlSensor.Name;
                fanDescriptors.Add(new FanControllable(id, name));
            }
            else if (fanSensor != null)
            {
                var id = fanSensor.Identifier.ToString();
                var name = fanSensor.Name;
                fanDescriptors.Add(new FanReadOnly(id, name));
            }
        }

        if (tempSensors.Count > 0 || fanDescriptors.Count > 0)
        {
            devices.Add(new HostDevice(
                hardware.Identifier.ToString(),
                hardware.Name,
                MapDeviceType(hardware.HardwareType),
                tempSensors,
                fanDescriptors
            ));
        }

        foreach (var sub in hardware.SubHardware)
        {
            if (!IsAquaComputerDevice(sub))
            {
                AddHardwareAndSubHardware(sub, devices);
            }
        }
    }

    private void CollectTelemetry(
        IHardware hardware,
        List<HostSensorTelemetry> sensorTelemetryList,
        List<HostFanTelemetry> fanTelemetryList)
    {
        foreach (var sensor in hardware.Sensors)
        {
            if (sensor.SensorType != SensorType.Temperature) continue;

            var id = sensor.Identifier.ToString();
            var val = sensor.Value;

            if (val.HasValue && val.Value is >= -40f and <= 150f)
            {
                sensorTelemetryList.Add(new Connected(id, Math.Round((double)val.Value, 2)));
            }
            else
            {
                sensorTelemetryList.Add(new Disconnected(id));
            }
        }

        var fanSensors = hardware.Sensors
            .Where(s => s.SensorType == SensorType.Fan)
            .ToList();
        var controlSensors = hardware.Sensors
            .Where(s => s.SensorType == SensorType.Control && s.Control != null)
            .ToList();

        var allIndices = fanSensors.Select(f => f.Index)
            .Union(controlSensors.Select(c => c.Index))
            .OrderBy(i => i)
            .ToList();

        foreach (var index in allIndices)
        {
            var fanSensor = fanSensors.FirstOrDefault(f => f.Index == index);
            var controlSensor = controlSensors.FirstOrDefault(c => c.Index == index);

            int? rpm = fanSensor?.Value.HasValue == true
                ? (int)Math.Round(fanSensor.Value.Value)
                : null;

            if (controlSensor != null)
            {
                var id = fanSensor?.Identifier.ToString() ?? controlSensor.Identifier.ToString();
                var control = controlSensor.Control;

                double pwmPercent = 0.0;
                if (controlSensor.Value.HasValue)
                {
                    pwmPercent = Math.Round((double)controlSensor.Value.Value, 1);
                }
                else if (control != null && control.ControlMode == ControlMode.Software)
                {
                    pwmPercent = Math.Round((double)control.SoftwareValue, 1);
                }

                pwmPercent = Math.Clamp(pwmPercent, 0.0, 100.0);
                bool isFollowingBiosCurve = control == null || control.ControlMode != ControlMode.Software;

                bool isDisconnected = rpm == null || (rpm == 0 && pwmPercent > 0.0);
                if (isDisconnected)
                {
                    fanTelemetryList.Add(new FanDisconnectedTelemetry(id));
                }
                else
                {
                    fanTelemetryList.Add(new FanControllableTelemetry(
                        id,
                        rpm!.Value,
                        pwmPercent,
                        isFollowingBiosCurve
                    ));
                }
            }
            else if (fanSensor != null)
            {
                var id = fanSensor.Identifier.ToString();
                if (rpm == null || rpm == 0)
                {
                    fanTelemetryList.Add(new FanDisconnectedTelemetry(id));
                }
                else
                {
                    fanTelemetryList.Add(new FanReadOnlyTelemetry(id, rpm!.Value));
                }
            }
        }

        foreach (var sub in hardware.SubHardware)
        {
            if (!IsAquaComputerDevice(sub))
            {
                CollectTelemetry(sub, sensorTelemetryList, fanTelemetryList);
            }
        }
    }

    private static HostDeviceType MapDeviceType(HardwareType type) => type switch
    {
        HardwareType.Cpu => HostDeviceType.CPU,
        HardwareType.GpuNvidia or HardwareType.GpuAmd or HardwareType.GpuIntel => HostDeviceType.GPU,
        HardwareType.Motherboard or HardwareType.SuperIO or HardwareType.EmbeddedController => HostDeviceType.MOTHERBOARD,
        HardwareType.Memory => HostDeviceType.RAM,
        HardwareType.Storage => HostDeviceType.STORAGE,
        _ => HostDeviceType.OTHER
    };

    private static bool IsAquaComputerDevice(IHardware hardware)
    {
        var name = hardware.Name?.ToLowerInvariant() ?? "";
        var id = hardware.Identifier.ToString().ToLowerInvariant();
        return name.Contains("aquacomputer") ||
               name.Contains("aqua computer") ||
               name.Contains("octo") ||
               name.Contains("quadro") ||
               name.Contains("d5next") ||
               name.Contains("d5 next") ||
               name.Contains("farbwerk") ||
               name.Contains("highflownext") ||
               id.Contains("0c70");
    }

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        try
        {
            RestoreFanDefaults();
        }
        catch
        {
            // Ignore errors during dispose
        }
        _computer.Close();
    }

    private sealed class UpdateVisitor : IVisitor
    {
        public void VisitComputer(IComputer computer) => computer.Traverse(this);

        public void VisitHardware(IHardware hardware)
        {
            hardware.Update();
            foreach (var subHardware in hardware.SubHardware)
                subHardware.Accept(this);
        }

        public void VisitSensor(ISensor sensor) { }
        public void VisitParameter(IParameter parameter) { }
    }
}
