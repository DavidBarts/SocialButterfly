namespace SocialButterfly.Lib;

public static class Extensions
{
    extension(IConfiguration config)
    {
        public T GetRequiredValue<T>(string name)
        {
            return config.GetValue<T>(name)
                ?? throw new ArgumentException($"Parameter '{name}' not found.");
        }
    }
}
