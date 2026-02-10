using SocialButterfly.Tool.Lib;

namespace SocialButterfly.Lib;

public class Passphrase
{
    public required string Value { init; get; }
    private static readonly Passphrase _instance = new(){ Value = GetValue() };

    public static Passphrase Instance {
        get { return _instance; }
    }

    static Passphrase()
    {
    }

    private Passphrase()
    {
    }

    /* TODO: modify the following to use Console.IsInputRedirected or
       exception from Console.KeyAvailable to determine if tty exists,
       read from HTTPS network socket if not */
    private static string GetValue()
    {
        Console.Write("Application passphase: ");
        return Getpass.ReadLine();
    }
}