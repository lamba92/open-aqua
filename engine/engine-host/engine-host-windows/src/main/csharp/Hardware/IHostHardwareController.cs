using OpenAqua.Engine.Host.Windows.Model;

namespace OpenAqua.Engine.Host.Windows.Hardware;

public interface IHostHardwareController : IDisposable
{
    HostInfo GetHostInfo();
    IAsyncEnumerable<HostTelemetry> GetTelemetryStream(TimeSpan pollingInterval, CancellationToken cancellationToken = default);
}
