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
            IsControllerEnabled = false,
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

            var telemetryList = new List<HostSensorTelemetry>();

            foreach (var hardware in _computer.Hardware)
            {
                if (IsAquaComputerDevice(hardware)) continue;
                CollectTelemetry(hardware, telemetryList);
            }

            yield return new HostTelemetry(telemetryList);

            await Task.Delay(pollingInterval, cancellationToken);
        }
    }

    public async Task StreamFanControlAsync(IAsyncEnumerable<HostFanControlRequest> requests, CancellationToken cancellationToken = default)
    {
        await foreach (var request in requests.WithCancellation(cancellationToken))
        {
            // Windows LHM fan control implementation in Task 4
        }
    }

    public void RestoreFanDefaults()
    {
        // Windows LHM fan restore in Task 4
    }

    private void AddHardwareAndSubHardware(IHardware hardware, List<HostDevice> devices)
    {
        var tempSensors = hardware.Sensors
            .Where(s => s.SensorType == SensorType.Temperature)
            .Select(s => new HostSensorDescriptor(s.Identifier.ToString(), s.Name))
            .ToList();

        if (tempSensors.Count > 0)
        {
            devices.Add(new HostDevice(
                hardware.Identifier.ToString(),
                hardware.Name,
                MapDeviceType(hardware.HardwareType),
                tempSensors
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

    private void CollectTelemetry(IHardware hardware, List<HostSensorTelemetry> telemetryList)
    {
        foreach (var sensor in hardware.Sensors)
        {
            if (sensor.SensorType != SensorType.Temperature) continue;

            var id = sensor.Identifier.ToString();
            var val = sensor.Value;

            if (val.HasValue && val.Value is >= -40f and <= 150f)
            {
                telemetryList.Add(new Connected(id, Math.Round((double)val.Value, 2)));
            }
            else
            {
                telemetryList.Add(new Disconnected(id));
            }
        }

        foreach (var sub in hardware.SubHardware)
        {
            if (!IsAquaComputerDevice(sub))
            {
                CollectTelemetry(sub, telemetryList);
            }
        }
    }

    private static HostDeviceType MapDeviceType(HardwareType type) => type switch
    {
        HardwareType.Cpu => HostDeviceType.CPU,
        HardwareType.GpuNvidia or HardwareType.GpuAmd or HardwareType.GpuIntel => HostDeviceType.GPU,
        HardwareType.Motherboard or HardwareType.SuperIO => HostDeviceType.MOTHERBOARD,
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
