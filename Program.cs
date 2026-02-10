using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using SocialButterfly.Data;
using SocialButterfly.Lib;
using SocialButterfly.Tool.Lib;

(var toolArgs, var appArgs) = SocialButterfly.Tool.Runner.SplitArgs(args);

var builder = WebApplication.CreateBuilder(appArgs);

// Add services to the container.
var authConnectionString = builder.Configuration.GetConnectionString("AuthConnection") ?? throw new InvalidOperationException("Connection string 'AuthConnection' not found.");
builder.Services.AddDbContext<ApplicationDbContext>(options =>
    options.UseSqlite(authConnectionString));
var sbConnectionString = builder.Configuration.GetConnectionString("SbConnection") ?? throw new InvalidOperationException("Connection string 'SbConnection' not found.");
builder.Services.AddDbContext<SbContext>(options =>
    options.UseSqlite(sbConnectionString));
builder.Services.AddDatabaseDeveloperPageExceptionFilter();

builder.Services.AddDefaultIdentity<IdentityUser>(options => options.SignIn.RequireConfirmedAccount = true)
    .AddEntityFrameworkStores<ApplicationDbContext>();
builder.Services.AddRazorPages(options =>
{
    options.Conventions.AuthorizeFolder("/Secure");
});

builder.Services.Configure<IdentityOptions>(options =>
{
    // Default Password settings.
    options.Password.RequireDigit = SbPasswordOptions.REQUIRE_DIGIT;
    options.Password.RequireLowercase = SbPasswordOptions.REQUIRE_LOWERCASE;
    options.Password.RequireNonAlphanumeric = SbPasswordOptions.REQUIRE_NON_ALPHANUMERIC;
    options.Password.RequireUppercase = SbPasswordOptions.REQUIRE_UPPERCASE;
    options.Password.RequiredLength = SbPasswordOptions.REQUIRED_LENGTH;
});

builder.Services.AddDistributedMemoryCache();

builder.Services.AddSession(options =>
{
    options.IdleTimeout = TimeSpan.FromMinutes(30);
    options.Cookie.HttpOnly = true;
    options.Cookie.IsEssential = true;
    options.Cookie.MaxAge = null;
});

builder.Services.AddSingleton(Passphrase.Instance);

builder.Services.AddSingleton<Kms>();
builder.Services.AddSingleton<Secret>();

var app = builder.Build();
// if we need to run a command-line app instead, this will do that (& exit)
SocialButterfly.Tool.Runner.Run(app, toolArgs);

// If we are still running here, we need to run the webapp (see above).
// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseMigrationsEndPoint();
}
else
{
    app.UseExceptionHandler("/Error");
    // The default HSTS value is 30 days. You may want to change this for production scenarios, see https://aka.ms/aspnetcore-hsts.
    app.UseHsts();
}

app.UseHttpsRedirection();

app.UseRouting();

app.UseAuthorization();

app.UseSession();

app.MapStaticAssets();
app.MapRazorPages()
   .WithStaticAssets();

app.Run();
