using System.Text.Json.Serialization;

namespace OpenAqua.Engine.Host.Windows.Model;

public record EngineSuccessResponse<T>(
    [property: JsonPropertyName("value")] T Value,
    [property: JsonPropertyName("type")] string Type = "success"
);

public record EngineErrorResponse(
    [property: JsonPropertyName("message")] string Message,
    [property: JsonPropertyName("stacktrace")] IReadOnlyList<string>? Stacktrace = null,
    [property: JsonPropertyName("type")] string Type = "error"
);

[JsonConverter(typeof(JsonStringEnumConverter<HostDeviceType>))]
public enum HostDeviceType
{
    CPU,
    MOTHERBOARD,
    GPU,
    RAM,
    STORAGE,
    OTHER
}

public record HostSensorDescriptor(
    [property: JsonPropertyName("id")] string Id,
    [property: JsonPropertyName("name")] string Name
);

[JsonPolymorphic(TypeDiscriminatorPropertyName = "type")]
[JsonDerivedType(typeof(FanControllable), "controllable")]
[JsonDerivedType(typeof(FanReadOnly), "read-only")]
public abstract record HostFanDescriptor(
    [property: JsonPropertyName("id")] string Id,
    [property: JsonPropertyName("name")] string Name
);

public record FanControllable(
    string Id,
    string Name
) : HostFanDescriptor(Id, Name);

public record FanReadOnly(
    string Id,
    string Name
) : HostFanDescriptor(Id, Name);

public record HostDevice(
    [property: JsonPropertyName("id")] string Id,
    [property: JsonPropertyName("name")] string Name,
    [property: JsonPropertyName("type")] HostDeviceType Type,
    [property: JsonPropertyName("sensors")] IReadOnlyList<HostSensorDescriptor>? Sensors = null,
    [property: JsonPropertyName("fans")] IReadOnlyList<HostFanDescriptor>? Fans = null
);

public record HostInfo(
    [property: JsonPropertyName("devices")] IReadOnlyList<HostDevice> Devices
);

[JsonPolymorphic(TypeDiscriminatorPropertyName = "type")]
[JsonDerivedType(typeof(Connected), "connected")]
[JsonDerivedType(typeof(Disconnected), "disconnected")]
public abstract record HostSensorTelemetry(
    [property: JsonPropertyName("id")] string Id
);

public record Connected(
    string Id,
    [property: JsonPropertyName("tempCelsius")] double TempCelsius
) : HostSensorTelemetry(Id);

public record Disconnected(
    string Id
) : HostSensorTelemetry(Id);

[JsonPolymorphic(TypeDiscriminatorPropertyName = "type")]
[JsonDerivedType(typeof(FanControllableTelemetry), "controllable")]
[JsonDerivedType(typeof(FanReadOnlyTelemetry), "read-only")]
[JsonDerivedType(typeof(FanDisconnectedTelemetry), "disconnected")]
public abstract record HostFanTelemetry(
    [property: JsonPropertyName("id")] string Id
);

public record FanControllableTelemetry(
    string Id,
    [property: JsonPropertyName("rpm")] int Rpm,
    [property: JsonPropertyName("pwmPercent")] double PwmPercent,
    [property: JsonPropertyName("isFollowingBiosCurve")] bool IsFollowingBiosCurve
) : HostFanTelemetry(Id);

public record FanReadOnlyTelemetry(
    string Id,
    [property: JsonPropertyName("rpm")] int Rpm
) : HostFanTelemetry(Id);

public record FanDisconnectedTelemetry(
    string Id
) : HostFanTelemetry(Id);

public record HostTelemetry(
    [property: JsonPropertyName("sensors")] IReadOnlyList<HostSensorTelemetry>? Sensors = null,
    [property: JsonPropertyName("fans")] IReadOnlyList<HostFanTelemetry>? Fans = null
);

public record HostFanControlRequest(
    [property: JsonPropertyName("channel")] string Channel,
    [property: JsonPropertyName("pwmPercent")] double PwmPercent
);

[JsonSourceGenerationOptions(
    WriteIndented = false,
    GenerationMode = JsonSourceGenerationMode.Default)]
[JsonSerializable(typeof(EngineSuccessResponse<HostInfo>))]
[JsonSerializable(typeof(EngineSuccessResponse<HostTelemetry>))]
[JsonSerializable(typeof(EngineErrorResponse))]
[JsonSerializable(typeof(string[]))]
[JsonSerializable(typeof(IReadOnlyList<string>))]
[JsonSerializable(typeof(HostInfo))]
[JsonSerializable(typeof(HostTelemetry))]
[JsonSerializable(typeof(HostDevice))]
[JsonSerializable(typeof(HostSensorDescriptor))]
[JsonSerializable(typeof(HostFanDescriptor))]
[JsonSerializable(typeof(FanControllable))]
[JsonSerializable(typeof(FanReadOnly))]
[JsonSerializable(typeof(HostSensorTelemetry))]
[JsonSerializable(typeof(Connected))]
[JsonSerializable(typeof(Disconnected))]
[JsonSerializable(typeof(HostFanTelemetry))]
[JsonSerializable(typeof(FanControllableTelemetry))]
[JsonSerializable(typeof(FanReadOnlyTelemetry))]
[JsonSerializable(typeof(FanDisconnectedTelemetry))]
[JsonSerializable(typeof(HostFanControlRequest))]
public partial class HostJsonContext : JsonSerializerContext
{
}

[JsonSourceGenerationOptions(
    WriteIndented = true,
    GenerationMode = JsonSourceGenerationMode.Default)]
[JsonSerializable(typeof(EngineSuccessResponse<HostInfo>))]
[JsonSerializable(typeof(EngineSuccessResponse<HostTelemetry>))]
[JsonSerializable(typeof(EngineErrorResponse))]
public partial class PrettyHostJsonContext : JsonSerializerContext
{
}
