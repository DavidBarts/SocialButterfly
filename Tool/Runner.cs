namespace SocialButterfly.Tool;

using System.Reflection;
using Microsoft.IdentityModel.Tokens;

public static class Runner
{
    private const string TOOL_ARGS = "--tool";
    private const string APP_ARGS = "--webapp";

    // Both splits arguments and detects tool invocation (the latter if
    // toolArgs is not null).
    public static (string[]? toolArgs, string[] appArgs) SplitArgs(string[] args)
    {
        if (args.Length == 0 || args[0] != TOOL_ARGS)
        {
            return (null, args);
        }
        if (args[0] == APP_ARGS)
        {
            return (null, args[1..]);
        }
        var toolEnd = 1;
        while (toolEnd < args.Length)
        {
            if (args[toolEnd] == APP_ARGS)
            {
                break;
            }
            toolEnd++;
        }
        return (args[1..toolEnd], args[Math.Min(args.Length, toolEnd+1)..]);
    }

    // Run a tool, if appropriate. If we run a tool, we exit (so the
    // webapp won't run). If we don't run a tool, we silently return.
    public static void Run(WebApplication app, string[]? args)
    {
        if (args == null)
        {
            // we shouldn't run a tool (see SplitArgs)
            return;
        }
        if (args.Length == 0)
        {
            throw new RunnerException("No command name specified.");
        }
        var commandName = args[0];
        var commandArgs = args[1..];
        var commandType = Type.GetType("SocialButterfly.Tool.Commands." + commandName)
            ?? throw new RunnerException($"Unknown command: {commandName}.");
        var ctorParams = new Type[] { typeof(IServiceProvider), typeof(string[]) };
        var commandCtor = commandType.GetConstructor(BindingFlags.Instance|BindingFlags.Public, null, ctorParams, null)
            ?? throw new RunnerException($"Standard constructor not found for ${commandName}");
        var serviceProvider = app.Services.CreateScope().ServiceProvider;
        var commandInstance = commandCtor.Invoke([serviceProvider, commandArgs]) as Command
            ?? throw new RunnerException("Cannot create command instance.");
        try {
            Environment.Exit(commandInstance.Run());
        } catch (CommandExit e) {
            if (!string.IsNullOrWhiteSpace(e.Message)) {
                Console.Error.WriteLine(e.Message);
            }
            Environment.Exit(e.Status);
        }
    }
}

public class RunnerException(string message) : Exception(message)
{
}
