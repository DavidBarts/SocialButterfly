using System.Buffers.Text;
using System.Security.Cryptography.X509Certificates;

namespace SocialButterfly.Lib;

public class Kms : IDisposable
{
    private const int KEY_SIZE = 256;
    private readonly string host;
    private readonly HttpClientHandler httpClientHandler;
    private readonly HttpClient httpClient;

    private class CreateKeyResponse
    {
        public DateTime? CreatedAt { get; set; }
        public required string Id { get; set; }
        public required string Name { get; set; }
        public required string[] Operations { get; set; }
        public required int Size { get; set; }
        public required string Type { get; set; }
    }

    private class WrapKeyResponse
    {
        public required string Key { get; set; }
        public required string Plaintext { get; set; }
    }

    private class UnwrapKeyResponse
    {
        public required string Plaintext { get; set; }
    }

    public Kms(string host, string certFile, string password)
    {
        this.host = host;
        var cert = X509CertificateLoader.LoadPkcs12FromFile(certFile, password, X509KeyStorageFlags.DefaultKeySet, null);
        httpClientHandler = new HttpClientHandler();
        httpClientHandler.ClientCertificates.Add(cert);
        httpClient = new HttpClient(httpClientHandler);
    }

    public async Task<string> CreateKeyAsync(string name, byte[]? value = null)
    {
        var keyOps = new string[] { "wrapKey", "unwrapKey" };
        object requestData;
        if (value == null) {
            requestData = new {
                Name = name,
                Context = "Social Butterfly",
                Type = "oct",
                Size = KEY_SIZE,
                Operations = keyOps
            };
        } else {
            requestData = new {
                Name = name,
                Context = "Social Butterfly",
                Keys = new object[] {
                     new { Use = "enc", Key_ops = keyOps, Kty = "oct", K = Base64Url.EncodeToString(value) }
                }
            };
        }
        var response = await httpClient.PostAsJsonAsync($"https://{host}/v1/servicekey", requestData);
        response.EnsureSuccessStatusCode();
        var deserialized = await response.Content.ReadFromJsonAsync<CreateKeyResponse>()
            ?? throw new HttpRequestException("Null deserialization result.");
        return deserialized.Id;
    }

    public async Task DeleteKeyAsync(string keyId)
    {
        var requestData = new { Reason = "unspecified" };
        var response = await httpClient.PostAsJsonAsync($"https://{host}/v1/servicekey/{keyId}/deactivate", requestData);
        response.EnsureSuccessStatusCode();
        response = await httpClient.DeleteAsync($"https://{host}/v1/servicekey/{keyId}");
        response.EnsureSuccessStatusCode();
    }

    /* to access things in appsettings.json: builder.Configuration.GetValue<string>("KmsHost") */
    public async Task<(string keyToken, byte[] key)> WrapKeyAsync(string keyId, string keyName)
    {
        var requestData = new { Name = keyName, Size = KEY_SIZE };
        var response = await httpClient.PostAsJsonAsync($"https://{host}/v1/servicekey/{keyId}/datakey", requestData);
        response.EnsureSuccessStatusCode();
        var deserialized = await response.Content.ReadFromJsonAsync<WrapKeyResponse>()
            ?? throw new HttpRequestException("Null deserialization result.");
        return (deserialized.Key, Convert.FromBase64String(deserialized.Plaintext));
    }

    public async Task<byte[]> UnwrapKeyAsync(string keyId, string keyToken)
    {
        var requestData = new { Key = keyToken };
        var response = await httpClient.PostAsJsonAsync($"https://{host}/v1/servicekey/{keyId}/datakey/decrypt", requestData);
        response.EnsureSuccessStatusCode();
        var deserialized = await response.Content.ReadFromJsonAsync<UnwrapKeyResponse>()
            ?? throw new HttpRequestException("Null deserialization result.");
        return Convert.FromBase64String(deserialized.Plaintext);
    }

    public void Dispose() {
        httpClient.Dispose();
        httpClientHandler.Dispose();
        GC.SuppressFinalize(this);
    }

}
