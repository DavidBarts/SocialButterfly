namespace SocialButterfly.Lib;

public class Secret
{
    private const string FILE_NAME = "secret.json.aes";
    public required IConfiguration Config { init; get; }

    public Secret(Passphrase passphrase)
    {
        using var inStream = new FileStream(FILE_NAME, FileMode.Open, FileAccess.Read);
        var salt = new byte[Crypto.SALT_LENGTH];
        inStream.ReadExactly(salt, 0, Crypto.SALT_LENGTH);
        var bytesToRead = (int) inStream.Length - Crypto.SALT_LENGTH;
        var bytesRead = 0;
        var encrypted = new byte[bytesToRead];
        while (bytesToRead > 0)
        {
            var nbytes = inStream.Read(encrypted, bytesRead, bytesToRead);
            if (nbytes == 0)
            {
                break;
            }
            bytesRead += nbytes;
            bytesToRead -= nbytes;
        }
        var decrypted = Crypto.Decrypt(encrypted, passphrase.Value, salt);
        using var memoryStream = new MemoryStream(decrypted);
        Config = new ConfigurationBuilder()
            .AddJsonStream(memoryStream)
            .Build();
    }
}
