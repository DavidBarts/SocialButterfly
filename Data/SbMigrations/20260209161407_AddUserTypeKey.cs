using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SocialButterfly.Data.SbMigrations
{
    /// <inheritdoc />
    public partial class AddUserTypeKey : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "users",
                columns: table => new
                {
                    id = table.Column<int>(type: "INTEGER", nullable: false),
                    name = table.Column<string>(type: "TEXT", nullable: true),
                    salt = table.Column<byte[]>(type: "BLOB", nullable: true),
                    json = table.Column<byte[]>(type: "BLOB", nullable: true),
                    Type = table.Column<int>(type: "INTEGER", nullable: true),
                    KeyId = table.Column<string>(type: "TEXT", nullable: true),
                    KeyCreated = table.Column<DateTime>(type: "TEXT", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_users", x => x.id);
                });

            migrationBuilder.CreateIndex(
                name: "users_name_ndx",
                table: "users",
                column: "name",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "users");
        }
    }
}
