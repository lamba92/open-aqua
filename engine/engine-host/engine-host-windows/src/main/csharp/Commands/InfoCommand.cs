using System.Text.Json;
using OpenAqua.Engine.Host.Windows.Hardware;
using OpenAqua.Engine.Host.Windows.Model;

namespace OpenAqua.Engine.Host.Windows.Commands;

public static class InfoCommand
{
    private static readonly JsonSerializerOptions PrettyOptions = new() { WriteIndented = true };
    private static readonly JsonSerializerOptions CompactOptions = new() { WriteIndented = false };

    public static int Execute(IHostHardwareController controller, bool pretty)
    {
        var info = controller.GetHostInfo();
        var response = new EngineSuccessResponse<HostInfo>(info);
        var json = JsonSerializer.Serialize(response, pretty ? PrettyOptions : CompactOptions);
        Console.WriteLine(json);
        return 0;
    }
}
