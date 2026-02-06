namespace SocialButterfly.Lib;

using System.IO;
using System.Security.Cryptography;
using System.Text;

public static class Crypto
{
    private static readonly Encoding ENCODING = Encoding.UTF8;
    private const int ITERATIONS = 1_200_000;
    // for testing: private const int ITERATIONS = 390_000;
    private static readonly int NONCE_LENGTH = AesGcm.NonceByteSizes.MaxSize;
    private static readonly int TAG_LENGTH = AesGcm.TagByteSizes.MaxSize;
    private const int KEY_LENGTH = 32;
    private const int SALT_LENGTH = 16;

    public static byte[] Encrypt(byte[] decrypted, byte[] key)
    {
        var nonce = MakeNonce();
        var ciphertext = new byte[decrypted.Length];
        var tag = new byte[TAG_LENGTH];
        using var aes = new AesGcm(key, TAG_LENGTH);
        aes.Encrypt(nonce, decrypted, ciphertext, tag);
        using var memoryStream = new MemoryStream();
        memoryStream.Write(tag);
        memoryStream.Write(nonce);
        memoryStream.Write(ciphertext);
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
        var tag = encrypted[..TAG_LENGTH];
        var nonce = encrypted[TAG_LENGTH..(TAG_LENGTH+NONCE_LENGTH)];
        var ciphertext = encrypted[(TAG_LENGTH+NONCE_LENGTH)..];
        var decrypted = new byte[ciphertext.Length];
        using var aes = new AesGcm(key, TAG_LENGTH);
        aes.Decrypt(nonce, ciphertext, tag, decrypted);
        return decrypted;
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

    public static byte[] MakeKey()
    {
        return RandomNumberGenerator.GetBytes(KEY_LENGTH);
    }

    public static byte[] MakeNonce()
    {
        return RandomNumberGenerator.GetBytes(NONCE_LENGTH);
    }

    public static byte[] MakeSalt()
    {
        return RandomNumberGenerator.GetBytes(SALT_LENGTH);
    }
}
