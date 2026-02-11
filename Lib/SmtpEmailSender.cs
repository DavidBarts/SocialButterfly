using System.Net;
using System.Net.Mail;
using Microsoft.AspNetCore.Identity.UI.Services;

namespace SocialButterfly.Lib;

public class SmtpEmailSender(IConfiguration config, Secret secret, ILogger<SmtpEmailSender> logger) : IEmailSender
{
    private readonly IConfiguration config = config.GetRequiredSection("SMTP");
    private readonly IConfiguration secret = secret.Config.GetRequiredSection("SMTP");
    private readonly ILogger<SmtpEmailSender> logger = logger;

    public async Task SendEmailAsync(string recipient, string subject, string message)
    {
        var mailMessage = new MailMessage(
            config.GetRequiredValue<string>("Sender"),
            recipient)
        {
            Subject = subject,
            Body = message,
            IsBodyHtml = true
        };
        using var smtpClient = new SmtpClient(
            config.GetRequiredValue<string>("Host"),
            config.GetRequiredValue<int>("Port"));
        smtpClient.EnableSsl = true;
        smtpClient.Credentials = new NetworkCredential(
            secret.GetRequiredValue<string>("Username"),
            secret.GetRequiredValue<string>("Password"));
        using var semaphore = new SemaphoreSlim(0);
        smtpClient.SendCompleted += new SendCompletedEventHandler((_, e) => {
            semaphore.Release();
            if (e.Error != null) {
                logger.LogError("Sending mail to {recipient} failed: {error}", recipient, e.Error.ToString());
            }
        });
        smtpClient.SendAsync(mailMessage, null);
        await semaphore.WaitAsync();
    }
}
