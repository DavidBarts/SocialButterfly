namespace SocialButterfly.Tool.Commands;

using System.CommandLine;
using System.CommandLine.Help;
using Microsoft.AspNetCore.Identity;
using SocialButterfly.Tool.Lib;

public class SetPassword(IServiceProvider serviceProvider, string[] args) : Tool.Command(serviceProvider, args)
{
    override public int Run()
    {
        /* parse command-line args */
        var rootCommand = new Command("SetPassword", "Create a new user account.");
        var generateOption = new Option<bool>("--generate") {
            Description = "Generate a random password instead of prompting for one."
        };
        var userName = new Argument<string>("username") {
            Description = "Username (e-mail address)."
        };
        rootCommand.Add(generateOption);
        rootCommand.Add(userName);
        var parseResult = rootCommand.Parse(args);
        if (parseResult.Errors.Count > 0)
        {
            foreach (var error in parseResult.Errors) {
                Console.Error.WriteLine($"SetPassword: {error.Message}");
            }
            return 2;
        }
        if (parseResult.Action is HelpAction)
        {
            parseResult.Invoke();
            return 0;
        }

        var userManager = serviceProvider.GetRequiredService<UserManager<IdentityUser>>();
        var username = parseResult.GetRequiredValue<string>("username");
        var user = userManager.FindByNameAsync(username).Result
            ?? throw new CommandExit(1, $"SetPassword: no such user as {username}");

        string newPassword;
        if (parseResult.GetValue<bool>("--generate")) {
            newPassword = Password.Generate();
            Console.WriteLine($"Generated password: {newPassword}");
        } else {
            newPassword = Password.Read();
        }

        var token = userManager.GeneratePasswordResetTokenAsync(user).Result;
        var result = userManager.ResetPasswordAsync(user, token, newPassword).Result;
        if (!result.Succeeded)
        {
            foreach (var error in result.Errors)
            {
                Console.Error.WriteLine($"SetPassword: [{error.Code}] {error.Description}");
            }
            return 1;
        }
        Console.WriteLine("Password changed.");
        return 0;
    }
}
