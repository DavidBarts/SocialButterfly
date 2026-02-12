namespace SocialButterfly.Lib;

public enum UserType
{
    Restricted = 0,
    Normal = -1,
    Super = -2
}

public static class UserTypeExtensions
{
    extension(int intValue)
    {
        public UserType ToUserType()
        {
            return intValue switch
            {
                (int)UserType.Normal => UserType.Normal,
                (int)UserType.Super => UserType.Super,
                < 0 => throw new InvalidOperationException($"{intValue} is not a valid user type code."),
                _ => UserType.Restricted,
            };
        }
    }

    extension(UserType userType)
    {
        public int ToInt()
        {
            return (int) userType;
        }
    }
}
