using System.Text.Json;
using OpenAqua.Engine.Host.Windows.Hardware;
using OpenAqua.Engine.Host.Windows.Model;

namespace OpenAqua.Engine.Host.Windows.Commands;

public static class StreamFansCommand
{
    public static async Task<int> ExecuteAsync(
        IHostHardwareController controller,
        CancellationToken cancellationToken)
    {
        try
        {
            await controller.StreamFanControlAsync(ReadStdinAsync(cancellationToken), cancellationToken);
        }
        finally
        {
            controller.RestoreFanDefaults();
        }
        return 0;
    }

    private static async IAsyncEnumerable<HostFanControlRequest> ReadStdinAsync(
        [System.Runtime.CompilerServices.EnumeratorCancellation] CancellationToken cancellationToken)
    {
        using var reader = new StreamReader(Console.OpenStandardInput());
        while (!cancellationToken.IsCancellationRequested)
        {
            var line = await reader.ReadLineAsync(cancellationToken);
            if (line == null) break;
            var trimmed = line.Trim();
            if (trimmed.Length == 0) continue;
            var request = JsonSerializer.Deserialize(trimmed, HostJsonContext.Default.HostFanControlRequest);
            if (request != null)
            {
                yield return request;
            }
        }
    }
}
