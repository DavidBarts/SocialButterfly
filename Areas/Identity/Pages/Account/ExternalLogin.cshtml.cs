// Licensed to the .NET Foundation under one or more agreements.
// The .NET Foundation licenses this file to you under the MIT license.
#nullable disable

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace SocialButterfly.Areas.Identity.Pages.Account
{
    [AllowAnonymous]
    public class ExternalLoginModel : PageModel
    {
        private readonly ILogger<ExternalLoginModel> _logger;

        public ExternalLoginModel(ILogger<ExternalLoginModel> logger)
        {
             _logger = logger;
        }
        
        public IActionResult OnGet() {
            _logger.LogError("Unsupported route {Route} accessed", Request.Path.Value);
            return BadRequest("Unsupported functionality");
        }

        public IActionResult OnPost(string provider, string returnUrl = null)
        {
            return OnGet();
        }
    }
}
