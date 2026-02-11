namespace SocialButterfly.Tool.Commands;

using SocialButterfly.Lib;

public class AesCat(IServiceProvider serviceProvider, string[] args) : Command(serviceProvider, args)
{
    override public int Run()
    {
        if (args.Length < 1)
        {
            throw new CommandExit(2, "AesCat: expecting file name");
        }
        if (args.Length > 1)
        {
            throw new CommandExit(2, "AesCat: unexpected extra argument(s)");
        }
        var password = serviceProvider.GetRequiredService<Passphrase>().Value;
        var encrypted = File.ReadAllBytes(args[0]);
        var salt = encrypted[..16];
        var remainder = encrypted[16..];
        var decrypted = Crypto.DecryptToString(remainder, password, salt);
        Console.Write(decrypted);
        return 0;
    }
}
