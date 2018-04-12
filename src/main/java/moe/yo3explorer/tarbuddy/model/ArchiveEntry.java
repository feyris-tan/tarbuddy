package moe.yo3explorer.tarbuddy.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

public class ArchiveEntry
{
    private long id;
    private int tape;
    private String name;
    private long size;
    private Timestamp ctime, mtime;
    private String hash;

    public static ArchiveEntry readFromResultSet(ResultSet rs) throws SQLException {
        ArchiveEntry child = new ArchiveEntry();
        child.id = rs.getLong(1);
        child.tape = rs.getInt(3);
        child.name = rs.getString(4);
        child.size = rs.getLong(5);
        child.ctime = rs.getTimestamp(6);
        child.mtime = rs.getTimestamp(7);
        child.hash = rs.getString(8);
        return child;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getTape() {
        return tape;
    }

    public void setTape(int tape) {
        this.tape = tape;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Timestamp getCtime() {
        return ctime;
    }

    public void setCtime(Timestamp ctime) {
        this.ctime = ctime;
    }

    public Timestamp getMtime() {
        return mtime;
    }

    public void setMtime(Timestamp mtime) {
        this.mtime = mtime;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "ArchiveEntry{" +
                "name='" + name + '\'' +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArchiveEntry that = (ArchiveEntry) o;
        if (this.hash != null && that.hash != null)
        {
            if (this.hash.equals(that.hash))
                return true;
        }
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        if (this.hash != null)
            return Objects.hash(hash);
        return Objects.hash(name);
    }

    public ArchiveEntry() {
    }

    public ArchiveEntry(String name) {
        this.name = name;
    }
}
