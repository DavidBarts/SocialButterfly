using System.Net;
using System.Net.Mail;
using Microsoft.AspNetCore.Identity.UI.Services;

namespace SocialButterfly.Lib;

public class SmtpEmailSender(IConfiguration config, Secret secret) : IEmailSender
{
    private IConfiguration config = config.GetRequiredSection("SMTP");
    private IConfiguration secret = secret.Config.GetRequiredSection("SMTP");
    
    public async Task SendEmailAsync(string recipient, string subject, string message)
    {
        var mailMessage = new MailMessage(
            config.GetRequiredValue<string>("Sender"),
            recipient);
        mailMessage.Subject = subject;
        mailMessage.Body = message;
        mailMessage.IsBodyHtml = true;
        using var smtpClient = new SmtpClient(
            config.GetRequiredValue<string>("Host"),
            config.GetRequiredValue<int>("Port"));
        smtpClient.EnableSsl = true;
        smtpClient.Credentials = new NetworkCredential(
            secret.GetRequiredValue<string>("Username"),
            secret.GetRequiredValue<string>("Password"));
        smtpClient.SendAsync(mailMessage, null);
    }
}
