namespace SocialButterfly.Lib;

using System.IO;
using System.Security.Cryptography;
using System.Text;

public static class Crypto
{
    private static readonly Encoding ENCODING = Encoding.UTF8;
    private const int ITERATIONS = 1_200_000;
    // for testing: private const int ITERATIONS = 390_000;
    private const int IV_LENGTH = 16;
    private const int KEY_LENGTH = 32;
    private const int SALT_LENGTH = 16;

    public static byte[] Encrypt(byte[] decrypted, byte[] key)
    {
        using var aes = Aes.Create();
        aes.Mode = CipherMode.CBC;
        aes.Padding = PaddingMode.PKCS7;
        aes.Key = key;
        aes.IV = MakeIv();
        var encryptor = aes.CreateEncryptor(aes.Key, aes.IV);
        using var memoryStream = new MemoryStream();
        memoryStream.Write(aes.IV);
        using var cryptoStream = new CryptoStream(memoryStream, encryptor, CryptoStreamMode.Write);
        cryptoStream.Write(decrypted);
        cryptoStream.Clear();
        return memoryStream.ToArray();
    }

    public static byte[] Encrypt(string decrypted, byte[] key)
    {
        return Encrypt(ENCODING.GetBytes(decrypted), key);
    }

    public static byte[] Encrypt(byte[] decrypted, string password, byte[] salt)
    {
        return Encrypt(decrypted, MakeKey(password, salt));
    }
    public static byte[] Encrypt(string decrypted, string password, byte[] salt)
    {
        return Encrypt(ENCODING.GetBytes(decrypted), MakeKey(password, salt));
    }

    public static byte[] Decrypt(byte[] encrypted, byte[] key)
    {
        using var aes = Aes.Create();
        aes.Mode = CipherMode.CBC;
        aes.Padding = PaddingMode.PKCS7;
        aes.Key = key;
        aes.IV = encrypted[0..IV_LENGTH];
        using var decryptor = aes.CreateDecryptor(aes.Key, aes.IV);
        using var memoryStream = new MemoryStream(encrypted[IV_LENGTH..]);
        using var cryptoStream = new CryptoStream(memoryStream, decryptor, CryptoStreamMode.Read);
        using var retDataStream = new MemoryStream();
        cryptoStream.CopyTo(retDataStream);
        return retDataStream.ToArray();
    }

    public static byte[] Decrypt(byte[] encrypted, string password, byte[] salt)
    {
        return Decrypt(encrypted, MakeKey(password, salt));
    }

    public static string DecryptToString(byte[] encrypted, byte[] key)
    {
        return ENCODING.GetString(Decrypt(encrypted, key));
    }

    public static string DecryptToString(byte[] encrypted, string password, byte[] salt)
    {
        return ENCODING.GetString(Decrypt(encrypted, MakeKey(password, salt)));
    }

    public static byte[] MakeKey(string password, byte[] salt)
    {
        return Rfc2898DeriveBytes.Pbkdf2(password, salt, ITERATIONS, HashAlgorithmName.SHA256, KEY_LENGTH);
    }

    public static byte[] MakeIv()
    {
        return RandomNumberGenerator.GetBytes(IV_LENGTH);
    }

    public static byte[] MakeSalt()
    {
        return RandomNumberGenerator.GetBytes(SALT_LENGTH);
    }
}
