using System;
using System.Collections.Generic;
using Microsoft.EntityFrameworkCore;
using SocialButterfly.Data.SbSchema;

namespace SocialButterfly.Data;

public partial class SbContext : DbContext
{
    public SbContext()
    {
    }

    public SbContext(DbContextOptions<SbContext> options)
        : base(options)
    {
    }

    public virtual DbSet<User> Users { get; set; }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>(entity =>
        {
            entity.ToTable("users");

            entity.HasIndex(e => e.Name, "users_name_ndx").IsUnique();

            entity.Property(e => e.Id)
                .ValueGeneratedNever()
                .HasColumnName("id");
            entity.Property(e => e.Json).HasColumnName("json");
            entity.Property(e => e.Name).HasColumnName("name");
            entity.Property(e => e.Salt).HasColumnName("salt");
        });

        OnModelCreatingPartial(modelBuilder);
    }

    partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
}
