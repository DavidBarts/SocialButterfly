namespace SocialButterfly.Tool.Commands;

using Microsoft.AspNetCore.Identity;
using SocialButterfly.Data;
using SocialButterfly.Lib;

public class ListUser(IServiceProvider serviceProvider, string[] args) : Command(serviceProvider, args)
{
    override public int Run()
    {
        if (args.Length < 1)
        {
            throw new CommandExit(2, "ListUser: expecting username (email address)");
        }
        if (args.Length > 1)
        {
            throw new CommandExit(2, "ListUser: unexpected extra argument(s)");
        }
        var userManager = serviceProvider.GetRequiredService<UserManager<IdentityUser>>();
        var sbContext = serviceProvider.GetRequiredService<SbContext>();
        var kms = serviceProvider.GetRequiredService<Kms>();

        /* their stuff */
        var user = userManager.FindByEmailAsync(args[0]).Result
            ?? throw new CommandExit(1, "ListUser: no such user");
        Console.WriteLine("---------- ASP.NET Identity User ----------");
        DumpProperties(user);

        /* our stuff */
        var ourUser = (from u in sbContext.Users where u.Name == args[0] select u).First();
        Console.WriteLine("---------------- Our User ----------------");
        DumpProperties(ourUser);

        if (ourUser.KeyId == null || ourUser.KeyToken == null || ourUser.Json == null)
        {
            Console.WriteLine("JSON dump suppressed due to null values.");
            return 0;
        }
        Console.WriteLine("---------------- JSON Dump ----------------");
        var key = kms.UnwrapKeyAsync(ourUser.KeyId, ourUser.KeyToken).Result;
        Console.WriteLine(Crypto.DecryptToString(ourUser.Json, key));
        return 0;
    }

    private void DumpProperties(object o)
    {
        var properties = o.GetType().GetProperties();
        properties.Sort((a, b) => a.Name.CompareTo(b.Name));
        foreach (var property in properties)
        {
            Console.WriteLine($"{property.Name}: {property.GetValue(o)?.ToString() ?? "(null)"}");
        }
    }
}
