package moe.yo3explorer.tarbuddy;

import moe.yo3explorer.tarbuddy.model.ArchiveEntry;
import moe.yo3explorer.tarbuddy.model.Tape;

import java.io.*;
import java.net.InetAddress;
import java.sql.*;
import java.util.HashSet;

public class DatabaseConnection implements Closeable, AutoCloseable
{
    private static boolean driverLoaded = false;

    public DatabaseConnection() throws ClassNotFoundException, SQLException, IOException {
        if (!driverLoaded)
        {
            Class.forName(DatabaseConfiguration.dbDriverClass);
            driverLoaded = true;
        }

        conn = DriverManager.getConnection(DatabaseConfiguration.dbUrl,DatabaseConfiguration.dbUser,DatabaseConfiguration.dbPassword);

        ResultSet resultSet = conn.prepareStatement("show variables like 'max_allowed_packet'").executeQuery();
        resultSet.next();
        long maxLen = resultSet.getLong(2);
        maxLen /= 1024; //kb
        maxLen /= 1024; //mb
        if (maxLen < 10)
        {
            throw new SQLException("Please set max_allowed_packet to at least 10MB and try again!\n Try: SET GLOBAL max_allowed_packet=20000000");
        }
        resultSet.close();

        if (!isMachineTableKnown()) {
            String sql = getSql("create.sql");
            sql = String.format(sql, getTableName());
            conn.prepareStatement(sql).execute();

            sql = String.format("CREATE INDEX %s_filename_index ON %s (filename)",getTableName(),getTableName());
            conn.prepareStatement(sql).execute();

            sql = String.format("CREATE INDEX %s_qhash_index ON %s (qhash)",getTableName(),getTableName());
            conn.prepareStatement(sql).execute();

            sql = String.format("INSERT INTO machineTables (tname) VALUES (?)");
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1,getTableName());
            preparedStatement.execute();
        }

        String sql = "CREATE TABLE IF NOT EXISTS tape(id int auto_increment primary key, insertedOn timestamp default CURRENT_TIMESTAMP not null," +
                "fname varchar(64) not null, content longblob not null)";
        conn.prepareStatement(sql).execute();

    }

    private boolean isMachineTableKnown() throws SQLException, IOException {
        String sql = "CREATE TABLE IF NOT EXISTS machineTables (tname varchar(64) not null primary key, insertedOn timestamp default CURRENT_TIMESTAMP not null)";
        conn.prepareStatement(sql).execute();

        sql = "SELECT * FROM machineTables WHERE tname=?";
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        preparedStatement.setString(1,getTableName());
        ResultSet resultSet = preparedStatement.executeQuery();
        boolean known = resultSet.next();
        resultSet.close();
        return known;
    }

    private String getSql(String resname) throws IOException {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(resname);
        InputStreamReader isr = new InputStreamReader(resourceAsStream);
        BufferedReader br = new BufferedReader(isr);
        String sql = br.readLine();
        br.close();
        isr.close();
        resourceAsStream.close();
        return sql;
    }

    Connection conn;

    public String getTableName() throws IOException {
        return "content_" + InetAddress.getLocalHost().getHostName();
    }

    public HashSet<ArchiveEntry> getArchiveContents() throws IOException, SQLException {
        System.out.println("Begin loading archive contents...");
        String sql = String.format("SELECT * FROM %s",getTableName());
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        HashSet<ArchiveEntry> result = new HashSet<>();

        long row = 0;

        while (rs.next())
        {
            result.add(ArchiveEntry.readFromResultSet(rs));
            if (++row % 1000 == 0)
                System.out.println(String.format("Loaded %d file entries.",row));
        }
        rs.close();
        ps.close();
        System.out.println(String.format("Done loading archive contents. Found %d strings.",row));
        return result;
    }

    public int getNextTapeId() throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT MAX(id) FROM tape");
        ResultSet rs = ps.executeQuery();
        int result = 1;
        if (rs.next())
            result =  rs.getInt(1) + 1;
        else
            result = 1;
        rs.close();
        ps.close();
        return result;
    }

    public void merge(Tape tape) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("INSERT INTO tape (fname,content) VALUES (?,?)");
        ps.setString(1,tape.getFilename());
        ps.setBlob(2,new ByteArrayInputStream(tape.getData()));
        ps.execute();
    }

    PreparedStatement insertStatement;
    PreparedStatement updateStatement;
    PreparedStatement checkByHash;
    PreparedStatement tryFetch;
    public void merge(ArchiveEntry ae) throws IOException, SQLException {
        if (ae.getId() == 0)
        {
            if (insertStatement == null)
                insertStatement = conn.prepareStatement("INSERT INTO " + getTableName() + " (tape,filename,size,ctime,mtime,qhash) VALUES (?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);

            insertStatement.setInt(1,ae.getTape());
            insertStatement.setString(2,ae.getName());
            insertStatement.setLong(3,ae.getSize());
            insertStatement.setTimestamp(4,ae.getCtime());
            insertStatement.setTimestamp(5,ae.getMtime());
            insertStatement.setString(6,ae.getHash());
            insertStatement.executeUpdate();

            ResultSet genKeys = insertStatement.getGeneratedKeys();
            if (genKeys.next())
            {
                ae.setId(genKeys.getInt(1));
            }
            else
            {
                throw new RuntimeException("Failed to re-read key from DB.");
            }
            genKeys.close();
        }
        else
        {
            if (updateStatement == null) {
                String sql = "UPDATE %s SET tape = ?, filename = ?, size = ?, ctime = ?, mtime = ? WHERE id = ?";
                sql = String.format(sql,getTableName());
                updateStatement = conn.prepareStatement(sql);
            }

            updateStatement.setInt(1,ae.getTape());
            updateStatement.setString(2,ae.getName());
            updateStatement.setLong(3,ae.getSize());
            updateStatement.setTimestamp(4,ae.getCtime());
            updateStatement.setTimestamp(5,ae.getMtime());
            updateStatement.setLong(6,ae.getId());
            int i = updateStatement.executeUpdate();
            if (i != 1)
                throw new RuntimeException("Could not execute update.");
        }
    }

    public ArchiveEntry tryFetchByHash(String hash) {
        try
        {
            if (checkByHash == null)
            {
                checkByHash = conn.prepareStatement(String.format("SELECT * FROM %s WHERE qhash = ?",getTableName()));
            }
            checkByHash.setString(1,hash);
            ResultSet resultSet = checkByHash.executeQuery();
            if (!resultSet.next())
            {
                resultSet.close();
                return null;
            }
            ArchiveEntry result = ArchiveEntry.readFromResultSet(resultSet);
            return result;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public ArchiveEntry tryFetchByName(String fname)
    {
        try
        {
            if (tryFetch == null)
            {
                tryFetch = conn.prepareStatement(String.format("SELECT * FROM %s WHERE filename=?",getTableName()));
            }

            tryFetch.setString(1,fname);
            ResultSet resultSet = tryFetch.executeQuery();
            if (!resultSet.next())
            {
                resultSet.close();
                return null;
            }
            ArchiveEntry result = ArchiveEntry.readFromResultSet(resultSet);
            return result;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            conn.close();
        } catch (SQLException e) {
        }
    }
}
