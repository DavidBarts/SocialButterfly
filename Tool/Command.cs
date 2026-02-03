namespace SocialButterfly.Tool;

public abstract class Command(IServiceProvider serviceProvider, string[] args)
{
    protected IServiceProvider serviceProvider = serviceProvider;
    protected string[] args = args;

    public abstract int Run();
}

// It is preferred to use this instead of Environment.Exit to abort a
// subcommand w/o a stack trace.
public class CommandExit : Exception
{
    public int Status {get; init;}
    public CommandExit(int status)
    {
        Status = status;
    }

    public CommandExit(int status, string message) : base(message)
    {
        Status = status;
    }
}
