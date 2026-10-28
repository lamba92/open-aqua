using System.Text.Json;
using OpenAqua.Engine.Host.Windows.Hardware;
using OpenAqua.Engine.Host.Windows.Model;

namespace OpenAqua.Engine.Host.Windows.Commands;

public static class InfoCommand
{
    public static int Execute(IHostHardwareController controller, bool pretty)
    {
        var info = controller.GetHostInfo();
        var response = new EngineSuccessResponse<HostInfo>(info);
        var json = pretty
            ? JsonSerializer.Serialize(response, PrettyHostJsonContext.Default.EngineSuccessResponseHostInfo)
            : JsonSerializer.Serialize(response, HostJsonContext.Default.EngineSuccessResponseHostInfo);
        Console.WriteLine(json);
        return 0;
    }
}
