package moe.yo3explorer.tarbuddy;

class DatabaseConfiguration
{
    private DatabaseConfiguration() {}

    final static String dbDriverClass = "org.mariadb.jdbc.MySQLConnection";
    final static String dbUrl = "jdbc:mysql://192.168.0.3:3306/tarbuddy";
    final static String dbUser = "root";
    final static String dbPassword = "<YOUR_PASSWORD_HERE>";
}
