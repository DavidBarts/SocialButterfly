namespace SocialButterfly.Tool.Commands;

using System.CommandLine;
using System.Text.Json;
using System.Text.RegularExpressions;
using Microsoft.AspNetCore.Identity;
using Microsoft.IdentityModel.Tokens;
using Microsoft.EntityFrameworkCore;
using SocialButterfly.Data;
using SocialButterfly.Lib;
using SocialButterfly.Tool.Lib;
using System.CommandLine.Help;

// this is mainly a confidence test that we can access the auth system
public partial class CreateUser(IServiceProvider serviceProvider, string[] args) : Tool.Command(serviceProvider, args)
{
    override public int Run()
    {
        /* get services first so we fail early if we can't */
        var userManager = serviceProvider.GetRequiredService<UserManager<IdentityUser>>();
        var userStore = serviceProvider.GetRequiredService<IUserStore<IdentityUser>>();
        var sbContext = serviceProvider.GetRequiredService<SbContext>();
        var kms = serviceProvider.GetRequiredService<Kms>();

        /* parse command-line args */
        var rootCommand = new Command("CreateUser", "Create a new user account.");
        Option userTypeOption = new Option<string>("--type") {
            DefaultValueFactory = _ => UserType.Normal.ToString(),
            Description = "User type (normal, super, or other user to be restricted to)."
        };
        Option generateOption = new Option<bool>("--generate") {
            Description = "Generate a random password instead of prompting for one."
        };
        rootCommand.Add(userTypeOption);
        rootCommand.Add(generateOption);
        var parseResult = rootCommand.Parse(args);
        if (parseResult.Errors.Count > 0)
        {
            foreach (var error in parseResult.Errors) {
                Console.Error.WriteLine($"CreateUser: {error.Message}");
            }
            return 2;
        }
        if (parseResult.Action is HelpAction)
        {
            parseResult.Invoke();
            return 0;
        }

        /* verify user type */
        var rawUserType = parseResult.GetRequiredValue<string>("--type");
        Enum.TryParse(rawUserType, true, out UserType userType);
        var userTypeCode = userType.ToInt();
        if (userType == UserType.Restricted)
        {
            if (!IsEmailAddress(rawUserType))
            {
                Console.Error.WriteLine($"CreateUser: --type '{rawUserType}' is invalid.");
                return 2;
            }
            var otherUsers = from u in sbContext.Users where u.Name == rawUserType select u;
            Data.SbSchema.User? found = null;
            foreach (var otherUser in otherUsers)
            {
                if (found != null)
                {
                    throw new CommandExit(1, $"CreateUser: '{rawUserType}' is ambiguous!");
                }
                found = otherUser;
            }
            if (found == null)
            {
                throw new CommandExit(1, $"CreateUser: '{rawUserType}' not found.");
            }
            if (found.Type?.ToUserType() == UserType.Restricted)
            {
                throw new CommandExit(1, $"CreateUser: '{rawUserType}' is itself restricted.");
            }
            userTypeCode = found.Id;
        }

        /* verify we are interactive */
        if (Console.IsInputRedirected)
        {
            Console.Error.WriteLine("CreateUser: input is not a terminal, goodbye!");
            return 1;
        }

        /* obtain data */
        string? username;
        while (true)
        {
            Console.Write("Email address: ");
            username = Console.ReadLine();
            var lcUsername = username?.ToLowerInvariant();
            if (string.IsNullOrEmpty(username)) {
                Console.WriteLine("Please enter an email address.");
            } else if (!IsEmailAddress(username)) {
                Console.WriteLine("Invalid email address, try again.");
            } else if (sbContext.Users.FromSql($"select * from Users where lower(Name) = {lcUsername}").Count() > 0) {
                Console.WriteLine("Duplicate email address, try again.");
            } else {
                break;
            }
        }
        var password = parseResult.GetValue<bool>("--generate") ? Password.Generate() : Password.Read();

        /* confirm operation */
        Console.WriteLine();
        Console.WriteLine($"Username: {username}");
        Console.WriteLine($"Type: {userType}");
        Console.WriteLine($"Password: {password}");
        Console.WriteLine();
        Console.Write($"Confirm? ");
        var answer = Console.ReadLine()?.Trim() ?? "";
        if (answer.IsNullOrEmpty() || (answer[0] != 'y' && answer[0] != 'Y' && answer[0] != 't' && answer[0] != 'T'))
        {
            return 0;
        }

        /* their stuff */
        var emailStore = (IUserEmailStore<IdentityUser>)userStore;
        var user = Activator.CreateInstance<IdentityUser>();
        userStore.SetUserNameAsync(user, username, CancellationToken.None).Wait();
        emailStore.SetEmailAsync(user, username, CancellationToken.None).Wait();
        var result = userManager.CreateAsync(user, password).Result;
        if (!result.Succeeded) {
            foreach (var error in result.Errors) {
                Console.Error.WriteLine($"CreateUser: [{error.Code}] {error.Description}");
            }
            return 1;
        }

        /* our stuff */
        var jsonObj = new { OtherId = user.Id };
        var keyId = kms.CreateKeyAsync().Result
            ?? throw new CommandExit(1, "CreateUser: Got null key ID!");
        var (keyToken, key) = kms.WrapKeyAsync(keyId).Result;
        if (keyToken == null || key == null) {
            throw new CommandExit(1, "CreateUser: Got null key and/or key token!");
        }
        var ourUser = new Data.SbSchema.User
        {
            Name = username,
            Json = Crypto.Encrypt(JsonSerializer.SerializeToUtf8Bytes(jsonObj), key),
            Type = userTypeCode,
            KeyId = keyId,
            KeyCreated = DateTime.UtcNow,
            KeyToken = keyToken
        };
        sbContext.Users.Add(ourUser);
        sbContext.SaveChanges();
        Console.WriteLine($"User IDs: {user.Id} (ASP.NET identity), {ourUser.Id} (ours).");
        return 0;
    }

    /* This is pretty rudimentary, basically a C# implementation of the
       email checking logic in Jquery validation. */
    private bool IsEmailAddress(string s)
    {
        return MyRegex().IsMatch(s);
    }

    [GeneratedRegex(@"^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$", RegexOptions.None)]
    private static partial Regex MyRegex();
}
