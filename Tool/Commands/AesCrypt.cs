namespace SocialButterfly.Tool.Commands;

using SocialButterfly.Lib;
using SocialButterfly.Tool.Lib;

public class AesCrypt(IServiceProvider serviceProvider, string[] args) : Command(serviceProvider, args)
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
        Console.Write("Encryption key: ");
        var password = Getpass.ReadLine();
        var plain = File.ReadAllBytes(args[0]);
        var salt = Crypto.MakeSalt();
        var encrypted = Crypto.Encrypt(plain, password, salt);
        using var outStream = new FileStream(args[0]+".aes", FileMode.Create);
        outStream.Write(salt, 0, salt.Length);
        outStream.Write(encrypted, 0, encrypted.Length);
        Console.WriteLine($"salt = {salt.Length}, encrypted = {encrypted.Length}, total = {salt.Length+encrypted.Length}");
        return 0;
    }
}
