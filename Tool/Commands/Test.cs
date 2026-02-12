namespace SocialButterfly.Tool.Commands;

public class Test(IServiceProvider serviceProvider, string[] args) : Command(serviceProvider, args)
{
    override public int Run()
    {
        var email = serviceProvider.GetRequiredService<Microsoft.AspNetCore.Identity.UI.Services.IEmailSender>();
        email.SendEmailAsync("david.w.barts@gmail.com", "Test", "This is a test.\n").Wait();
        Console.WriteLine("Email sent (I hope).");
        return 0;
    }
}
