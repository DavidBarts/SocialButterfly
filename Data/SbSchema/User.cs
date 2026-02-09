using System;
using System.Collections.Generic;

namespace SocialButterfly.Data.SbSchema;

public partial class User
{
    public int Id { get; set; }

    public string? Name { get; set; }

    public byte[]? Json { get; set; }

    public int? Type { get; set; }

    public string? KeyId { get; set; }

    public DateTime? KeyCreated { get; set; }

    public string? KeyToken { get; set; }
}
