namespace SocialButterfly.Tool.Commands;

public class Test(IServiceProvider serviceProvider, string[] args) : SocialButterfly.Tool.Command(serviceProvider, args)
{
    override public int Run()
    {
        Console.WriteLine("This is a test. Arguments follow:");
        Console.WriteLine(string.Join(' ', args));
        return 0;
    }
}
