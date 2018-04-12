package moe.yo3explorer.tarbuddy;

public enum Unit
{
    BYTE(1),
    KILO(1000),
    MEGA(1000000),
    GIGA(1000000000),
    TERA(1000000000000L);

    private Unit(long multiplicator)
    {
        mul = multiplicator;
    }

    private long mul;

    public long getMul() {
        return mul;
    }
}
