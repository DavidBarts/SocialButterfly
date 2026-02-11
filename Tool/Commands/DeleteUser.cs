namespace SocialButterfly.Tool.Commands;

using Microsoft.AspNetCore.Identity;
using SocialButterfly.Data;
using SocialButterfly.Lib;

public class DeleteUser(IServiceProvider serviceProvider, string[] args) : Command(serviceProvider, args)
{
    override public int Run()
    {
        if (args.Length < 1)
        {
            throw new CommandExit(2, "DeleteUser: expecting username (email address)");
        }
        if (args.Length > 1)
        {
            throw new CommandExit(2, "DeleteUser: unexpected extra argument(s)");
        }
        var userManager = serviceProvider.GetRequiredService<UserManager<IdentityUser>>();
        var sbContext = serviceProvider.GetRequiredService<SbContext>();
        var kms = serviceProvider.GetRequiredService<Kms>();

        var user = userManager.FindByEmailAsync(args[0]).Result
            ?? throw new CommandExit(1, "DeleteUser: no such user");
        var result = userManager.DeleteAsync(user).Result;
        if (!result.Succeeded)
        {
            foreach (var error in result.Errors)
            {
                Console.Error.WriteLine($"DeleteUser: [{error.Code}] {error.Description}");
            }
            return 1;
        }
        var ourUser = (from u in sbContext.Users where u.Name == args[0] select u).First();
        if (ourUser.KeyId != null) {
            kms.DeleteKeyAsync(ourUser.KeyId).Wait();
        }
        sbContext.Users.Remove(ourUser);
        sbContext.SaveChanges();
        return 0;
    }
}
