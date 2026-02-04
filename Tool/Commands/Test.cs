namespace SocialButterfly.Tool.Commands;

public class Test(IServiceProvider serviceProvider, string[] args) : Command(serviceProvider, args)
{
    override public int Run()
    {
        Console.WriteLine("This is a test. Arguments follow:");
        Console.WriteLine(string.Join(' ', args));
        return 0;
    }
}
