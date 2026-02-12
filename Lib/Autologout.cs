using System.Globalization;
using Microsoft.AspNetCore.Identity;
using SocialButterfly.Data.SbSchema;

namespace SocialButterfly.Lib;

// Strictly enforce a configurable maximum session lifetime, because the
// ASP.NET core auth system seems not to.
public class Autologout(RequestDelegate next, IConfiguration config)
{
    private readonly RequestDelegate _next = next;
    private const string SESSION_NAME = "LastRequestTime";
    private readonly TimeSpan maxIdleTime = config.GetRequiredValue<TimeSpan>("MaxIdleTime");

    public async Task InvokeAsync(HttpContext context, SignInManager<IdentityUser> signInManager, ILogger<Autologout> logger)
    {
        var lastTimeString = context.Session.GetString(SESSION_NAME);
        var lastTime = lastTimeString == null ?
            DateTime.MinValue :
            DateTime.Parse(lastTimeString, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind);
        var now = DateTime.UtcNow;
        var duration = now - lastTime;
        context.Session.SetString(SESSION_NAME, now.ToString("o", CultureInfo.InvariantCulture));
        if (duration > maxIdleTime && signInManager.IsSignedIn(context.User))
        {
            if (lastTimeString == null)
                logger.LogInformation("No existing session -- autologout.");
            else
                logger.LogInformation("Idle time of {duration} is too long -- autologout.", duration);
            await signInManager.SignOutAsync();
            context.Response.Redirect("/Identity/Account/Login");
            return;  /* terminate the middleware chain due to autologout */
        }
        await _next(context);
    }
}

public static class AutologoutExtensions
{
    extension(IApplicationBuilder builder)
    {
        public IApplicationBuilder UseAutologout()
        {
            return builder.UseMiddleware<Autologout>();
        }
    }
}
