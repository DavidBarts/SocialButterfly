namespace SocialButterfly.Tool.Commands;

public class Test(IServiceProvider serviceProvider, string[] args) : Command(serviceProvider, args)
{
    override public int Run()
    {
        var config = serviceProvider.GetRequiredService<IConfiguration>();
        Console.WriteLine("This is a test. Arguments follow:");
        Console.WriteLine(string.Join(' ', args));
        Console.WriteLine($"Allowed hosts: {config.GetValue<string>("AllowedHosts")}");
        return 0;
    }
}
