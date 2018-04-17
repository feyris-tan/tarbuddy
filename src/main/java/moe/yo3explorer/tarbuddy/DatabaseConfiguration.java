package moe.yo3explorer.tarbuddy;

class DatabaseConfiguration
{
    private DatabaseConfiguration() {}

    final static String dbDriverClass = "org.mariadb.jdbc.MySQLConnection";
    final static String dbUrl = "jdbc:mysql://192.168.0.3:3306/tapebuddy";
    final static String dbUser = "ft";
    final static String dbPassword = "welcometotheworld";
}
