using System.Text.Json.Serialization;

namespace OpenAqua.Engine.Host.Windows.Model;

public record EngineSuccessResponse<T>(
    [property: JsonPropertyName("value")] T Value,
    [property: JsonPropertyName("type")] string Type = "success"
);

public record EngineErrorResponse(
    [property: JsonPropertyName("error")] string Error,
    [property: JsonPropertyName("type")] string Type = "error"
);

[JsonConverter(typeof(JsonStringEnumConverter<HostDeviceType>))]
public enum HostDeviceType
{
    CPU,
    MOTHERBOARD,
    GPU,
    STORAGE,
    OTHER
}

public record HostSensorDescriptor(
    [property: JsonPropertyName("id")] string Id,
    [property: JsonPropertyName("name")] string Name
);

public record HostDevice(
    [property: JsonPropertyName("id")] string Id,
    [property: JsonPropertyName("name")] string Name,
    [property: JsonPropertyName("type")] HostDeviceType Type,
    [property: JsonPropertyName("sensors")] IReadOnlyList<HostSensorDescriptor> Sensors
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

public record HostTelemetry(
    [property: JsonPropertyName("sensors")] IReadOnlyList<HostSensorTelemetry> Sensors
);

[JsonSourceGenerationOptions(
    WriteIndented = false,
    GenerationMode = JsonSourceGenerationMode.Default)]
[JsonSerializable(typeof(EngineSuccessResponse<HostInfo>))]
[JsonSerializable(typeof(EngineSuccessResponse<HostTelemetry>))]
[JsonSerializable(typeof(EngineErrorResponse))]
[JsonSerializable(typeof(HostInfo))]
[JsonSerializable(typeof(HostTelemetry))]
[JsonSerializable(typeof(HostDevice))]
[JsonSerializable(typeof(HostSensorDescriptor))]
[JsonSerializable(typeof(HostSensorTelemetry))]
[JsonSerializable(typeof(Connected))]
[JsonSerializable(typeof(Disconnected))]
public partial class HostJsonContext : JsonSerializerContext
{
}

[JsonSourceGenerationOptions(
    WriteIndented = true,
    GenerationMode = JsonSourceGenerationMode.Default)]
[JsonSerializable(typeof(EngineSuccessResponse<HostInfo>))]
public partial class PrettyHostJsonContext : JsonSerializerContext
{
}
