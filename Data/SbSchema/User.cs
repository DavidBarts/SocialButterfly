using System;
using System.Collections.Generic;

namespace SocialButterfly.Data.SbSchema;

public partial class User
{
    public int Id { get; set; }

    public string? Name { get; set; }

    public byte[]? Salt { get; set; }

    public byte[]? Json { get; set; }
}
