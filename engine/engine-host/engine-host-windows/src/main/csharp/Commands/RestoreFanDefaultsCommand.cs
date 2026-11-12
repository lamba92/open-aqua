using OpenAqua.Engine.Host.Windows.Hardware;

namespace OpenAqua.Engine.Host.Windows.Commands;

public static class RestoreFanDefaultsCommand
{
    public static int Execute(IHostHardwareController controller, bool pretty)
    {
        controller.RestoreFanDefaults();
        return 0;
    }
}
