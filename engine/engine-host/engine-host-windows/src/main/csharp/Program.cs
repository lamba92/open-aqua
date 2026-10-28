using System.Text.Json;
using OpenAqua.Engine.Host.Windows.Commands;
using OpenAqua.Engine.Host.Windows.Hardware;
using OpenAqua.Engine.Host.Windows.Model;

namespace OpenAqua.Engine.Host.Windows;

public static class Program
{
    public static async Task<int> Main(string[] args)
    {
        if (args.Length == 0 || args[0] is "-h" or "--help" or "help")
        {
            PrintHelp();
            return 0;
        }

        using var cts = new CancellationTokenSource();
        Console.CancelKeyPress += (_, e) =>
        {
            e.Cancel = true;
            cts.Cancel();
        };

        try
        {
            using var controller = new WindowsHostHardwareController();

            return args[0].ToLowerInvariant() switch
            {
                "info" => InfoCommand.Execute(controller, args.Contains("--pretty")),
                "stream-telemetry" => await StreamTelemetryCommand.ExecuteAsync(controller, TimeSpan.FromSeconds(1), cts.Token),
                _ => PrintUnknownCommand(args[0])
            };
        }
        catch (OperationCanceledException)
        {
            return 0;
        }
        catch (Exception ex)
        {
            var err = new EngineErrorResponse(ex.Message);
            Console.WriteLine(JsonSerializer.Serialize(err, HostJsonContext.Default.EngineErrorResponse));
            return 1;
        }
    }

    private static void PrintHelp()
    {
        Console.WriteLine("""
        Usage: open-aqua-host-engine <command> [options]

        Commands:
          info               Discover connected host devices and sensor descriptors
          stream-telemetry   Stream live host telemetry as NDJSON at 1 Hz

        Options:
          --pretty           Format JSON output with indentation (info command only)
          -h, --help         Show help information
        """);
    }

    private static int PrintUnknownCommand(string cmd)
    {
        var err = new EngineErrorResponse($"Unknown command '{cmd}'. Run with --help for usage.");
        Console.WriteLine(JsonSerializer.Serialize(err, HostJsonContext.Default.EngineErrorResponse));
        return 1;
    }
}
