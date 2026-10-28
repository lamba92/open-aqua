using System.Text.Json;
using OpenAqua.Engine.Host.Windows.Hardware;
using OpenAqua.Engine.Host.Windows.Model;

namespace OpenAqua.Engine.Host.Windows.Commands;

public static class StreamTelemetryCommand
{
    public static async Task<int> ExecuteAsync(
        IHostHardwareController controller,
        TimeSpan pollingInterval,
        CancellationToken cancellationToken)
    {
        await foreach (var telemetry in controller.GetTelemetryStream(pollingInterval, cancellationToken))
        {
            var response = new EngineSuccessResponse<HostTelemetry>(telemetry);
            var json = JsonSerializer.Serialize(response, HostJsonContext.Default.EngineSuccessResponseHostTelemetry);
            Console.WriteLine(json);
        }
        return 0;
    }
}
