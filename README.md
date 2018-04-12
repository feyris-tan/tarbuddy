# Tarbuddy

This is a small tool I wrote to aid me in my backup strategy. It walks through the local filesystem, compares the file sizes and timestamps to a database, and writes the names of files with differences to a text file.

Such text files might be useful for feeding into tar's -T  option, or similar. ( https://www.gnu.org/software/tar/manual/tar.html#SEC108 )

## How to use

1. Create a database schema. This program was designed with MariaDB in mind, but might work on other databases too.
2. Put your database credentials into DatabaseConfiguration.java
3. Configure media size in Main.java
4. Run program

## Important

This is not intended as a full fledged backup solution in production environments. This program is not intended to provide disaster recovery or such. I recommend keeping an image of your hard drive handy. ;)

## TODO:
- maybe use JPA instead of JDBC (?)