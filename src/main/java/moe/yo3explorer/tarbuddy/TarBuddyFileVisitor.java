package moe.yo3explorer.tarbuddy;

import moe.yo3explorer.tarbuddy.model.ArchiveEntry;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;

public class TarBuddyFileVisitor implements FileVisitor, Closeable, AutoCloseable
{
    public TarBuddyFileVisitor(DatabaseConnection dbc, long tapeSize) throws IOException, SQLException {
        filesOnTape = new HashSet<>();
        databaseConnection = dbc;
        tapeLength = tapeSize;
        tapeRemain = tapeSize - 1024;   //Start und End-Block abziehen.
        tapeId = dbc.getNextTapeId();
        scanned = 0;
        outputFile = new ByteArrayOutputStream();
        outputPrintStream = new PrintStream(outputFile);
    }

    private HashSet<ArchiveEntry> filesOnTape;
    private DatabaseConnection databaseConnection;
    private long tapeLength;
    private long tapeRemain;
    private int tapeId;
    private boolean isConsideredFull;
    private long scanned;
    ByteArrayOutputStream outputFile;
    PrintStream outputPrintStream;

    @Override
    public FileVisitResult preVisitDirectory(Object o, BasicFileAttributes basicFileAttributes) throws IOException {
        if (tapeRemain < 512) {
            isConsideredFull = true;
            return FileVisitResult.TERMINATE;
        }
        String filename = getFilename(o);
        String fullname = getRealPath(o);
        if (filename.equals("")) return FileVisitResult.CONTINUE;
        if (filename.equals("$Recycle.Bin")) return FileVisitResult.SKIP_SUBTREE;
        if (filename.equals(".Trash-1000")) return FileVisitResult.SKIP_SUBTREE;

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Object o, BasicFileAttributes basicFileAttributes) throws IOException {
        String filename = getFilename(o);
        String fullname = getRealPath(o);
        if (filename.toLowerCase().equals("hiberfil.sys"))
            return FileVisitResult.CONTINUE;
        if (filename.toLowerCase().equals("pagefile.sys"))
            return FileVisitResult.CONTINUE;
        if (filename.equals("")) return FileVisitResult.CONTINUE;
        if (tapeRemain < basicFileAttributes.size())
        {
            isConsideredFull = true;
            return FileVisitResult.TERMINATE;
        }
        if (++scanned % 1000 == 0)
        {
            System.out.println(String.format("Now scanning file #%d",scanned));
        }

        handleEntry(basicFileAttributes, fullname, (Path)o);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Object o, IOException e) throws IOException {
        System.out.println("Could not stat file: " + getRealPath(o));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Object o, IOException e) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public void close() throws IOException {

    }


    private boolean timestampEquality(Timestamp left, Timestamp right)
    {
        if ((left == null) && (right == null)) return true;
        if (left == null) return false;
        if (right == null) return false;
        long l = left.getTime();
        long r = right.getTime();
        l = l - r;

        return l < 1000;
    }

    private String getRealPath(Object o)
    {
        if (!(o instanceof Path))
            throw new RuntimeException();

        Path path = (Path)o;
        String result = path.toString();
        if (result.charAt(1) == ':')
        {
            result = "/cygdrive/" + result.substring(0,1).toLowerCase() + result.substring(2).replace("\\","/");
        }
        return result;
    }

    private String getFilename(Object o)
    {
        if (!(o instanceof Path))
            throw new RuntimeException();

        Path path = (Path)o;
        Path filenamepath = path.getFileName();
        if (filenamepath == null) return "";
        return filenamepath.toString();
    }

    private static final long _100mb = 100 * 1000 * 1000;


    private void handleEntry(BasicFileAttributes basicFileAttributes, String fullname,Path originalPath) {
        boolean merge = false;
        ArchiveEntry ae;
        ArchiveEntry dbEntry;
        if (basicFileAttributes.size() > _100mb)
        {
            StringBuffer sb = null;
            try {
                sb = QHash.QHash(basicFileAttributes, originalPath);
            } catch (QHashException e) {
                System.out.printf("Could not calculate hash of %s",fullname);
                return;
            }
            String result = sb.toString();
            ae = new ArchiveEntry("");
            ae.setHash(result);
            dbEntry = databaseConnection.tryFetchByHash(result);
            if (dbEntry != null)
                ae = dbEntry;
            else
                merge = true;

            ae.setSize(basicFileAttributes.size());
            if (ae.getCtime() != null) ae.setCtime(new Timestamp(basicFileAttributes.creationTime().toMillis()));
            if (ae.getMtime() != null) ae.setMtime(new Timestamp(basicFileAttributes.lastModifiedTime().toMillis()));
            if (!fullname.equals(ae.getName())) ae.setName(fullname);
        }
        else {
            ae = new ArchiveEntry(fullname);
            dbEntry = databaseConnection.tryFetchByName(fullname);
            if (dbEntry != null)
                ae = dbEntry;
            else
                merge = true;

            if (ae.getSize() != basicFileAttributes.size()) {
                ae.setSize(basicFileAttributes.size());
                merge = true;
            }

            Timestamp ctime = new Timestamp(basicFileAttributes.creationTime().toMillis());
            Timestamp mtime = new Timestamp(basicFileAttributes.lastModifiedTime().toMillis());
            if (!timestampEquality(ctime, ae.getCtime())) {
                ae.setCtime(ctime);
                merge = true;
            }
            if (!timestampEquality(mtime, ae.getMtime())) {
                ae.setMtime(mtime);
                merge = true;
            }
        }

        if (merge)
        {
            long size = ae.getSize();
            size /= 512;
            size += 2;      //Größe blockweise aufruden, und den tar Index Block nicht vergessen.
            size *= 512;
            tapeRemain -= size;
            ae.setTape(tapeId);
            filesOnTape.add(ae);

            try {
                System.out.println("Merging: " + ae.getName());
                databaseConnection.merge(ae);
                outputPrintStream.print(ae.getName());
                outputPrintStream.print('\n');
            } catch (Exception e)
            {
                throw new RuntimeException("Persistence error.",e);
            }
        }
    }



    public HashSet<ArchiveEntry> getFilesOnTape() {
        return filesOnTape;
    }

    public long getTapeRemain() {
        return tapeRemain;
    }

    public boolean isConsideredFull() {
        return isConsideredFull;
    }

    public ByteArrayOutputStream getOutputFile() {
        outputPrintStream.flush();
        return outputFile;
    }
}
