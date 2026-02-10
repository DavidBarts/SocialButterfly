namespace SocialButterfly.Tool.Commands;

using SocialButterfly.Lib;

public class AesCrypt(IServiceProvider serviceProvider, string[] args) : Command(serviceProvider, args)
{
    override public int Run()
    {
        if (args.Length < 1)
        {
            throw new CommandExit(2, "AesCrypt: expecting file name");
        }
        if (args.Length > 1)
        {
            throw new CommandExit(2, "AesCrypt: unexpected extra argument(s)");
        }
        var password = serviceProvider.GetRequiredService<Passphrase>().Value;
        var plain = File.ReadAllBytes(args[0]);
        var salt = Crypto.MakeSalt();
        var encrypted = Crypto.Encrypt(plain, password, salt);
        using var outStream = new FileStream(args[0]+".aes", FileMode.Create);
        outStream.Write(salt, 0, salt.Length);
        outStream.Write(encrypted, 0, encrypted.Length);
        return 0;
    }
}
