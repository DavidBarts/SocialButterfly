namespace SocialButterfly.Lib;

using System.Text;

// see https://www.joshooaj.com/blog/2022/01/04/securely-reading-passwords-from-the-console/
// XXX - we return type string and not SecureString, because ASP.NET uses
// string for the form params it gets.
public static class Getpass
{
    public static string ReadLine()
    {
        const string ERASER = "\b \b";
        var password = new StringBuilder();
        ConsoleKeyInfo key;
        while ((key = Console.ReadKey(true)).Key != ConsoleKey.Enter)
        {
            if (key.Key == ConsoleKey.Backspace && password.Length > 0)
            {
                Console.Write(ERASER);
                password.Remove(password.Length - 1, 1);
            }
            else if(key.Key == ConsoleKey.Escape || (key.Key == ConsoleKey.U && key.Modifiers.HasFlag(ConsoleModifiers.Control)))
            {
                for (int i=0; i<password.Length; i++) {
                    Console.Write(ERASER);
                }
                password.Clear();
            }
            else if (!char.IsControl(key.KeyChar))
            {
                Console.Write("*");
                password.Append(key.KeyChar);
            }
            else
            {
                Console.Write("\a");
            }
        }
        Console.WriteLine();
        return password.ToString();
    }
}
