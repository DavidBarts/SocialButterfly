namespace SocialButterfly.Tool.Commands;

using Microsoft.AspNetCore.Identity;
using SocialButterfly.Lib;

// this is mainly a confidence test that we can access the auth system
public class CheckPassword(IServiceProvider serviceProvider, string[] args) : Command(serviceProvider, args)
{
    override public int Run()
    {
        if (args.Length < 1)
        {
            throw new CommandExit(2, "CheckPassword: expecting username (email address)");
        }
        if (args.Length > 1)
        {
            throw new CommandExit(2, "CheckPassword: unexpected extra argument(s)");
        }
        var userManager = serviceProvider.GetRequiredService<UserManager<IdentityUser>>();
        var user = userManager.FindByNameAsync(args[0]).Result
            ?? throw new CommandExit(1, $"CheckPassword: no such user as {args[0]}");
        Console.Write("Password: ");
        var password = Getpass.ReadLine();
        Console.WriteLine(userManager.CheckPasswordAsync(user, password).Result ? "valid" : "invalid");
        return 0;
    }
}
