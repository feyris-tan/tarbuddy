package moe.yo3explorer.tarbuddy;

import moe.yo3explorer.tarbuddy.model.Tape;

import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;

public class Main
{
    final static int tapeLength = 19;
    final static Unit tapeLengthUnit = Unit.GIGA;

    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        DatabaseConnection dbc = new DatabaseConnection();

        TarBuddyFileVisitor tbfv = new TarBuddyFileVisitor(dbc,tapeLength * tapeLengthUnit.getMul());

        FileSystem fs = FileSystems.getDefault();
        Iterable<Path> paths = fs.getRootDirectories();
        paths = parseArgs(args, paths);

        if (!paths.iterator().hasNext())
        {
            System.out.printf("Could not find any roots.");
            dbc.close();
            return;
        }

        for (Path path : paths) {
            String pathString = path.toString();
            if (pathString.length() == 3 && pathString.endsWith(":\\"))
            {
                callWindowsDiskCleanup(pathString);
            }
            System.out.println("Begin walking the file tree of: " + pathString);
            Files.walkFileTree(path,tbfv);
            if (tbfv.isConsideredFull()) {
                System.out.println("Tape is considered full.");
                break;
            }
        }

        Tape tape = new Tape();
        tape.setFilename(String.format("%d.txt",System.currentTimeMillis()));
        tape.setData(tbfv.getOutputFile().toByteArray());
        System.out.println("Saving file list...");
        tape.saveFile();
        System.out.println("Merging file list...");
        dbc.merge(tape);
        dbc.close();

        Hashtable<String, Long> hugeFiles = tbfv.getHugeFiles();
        Enumeration<String> keys = hugeFiles.keys();
        while (keys.hasMoreElements())
        {
            String s = keys.nextElement();
            long k = hugeFiles.get(s);
            System.out.printf("Huge file: %s (%d bytes)",s,k);
        }
    }

    private static Iterable<Path> parseArgs(String[] args, Iterable<Path> paths) {
        if (args.length != 0)
        {
            LinkedList<Path> argList = new LinkedList<>();
            for(String arg: args)
            {
                Path path = Paths.get(arg);
                if (Files.exists(path))
                    argList.add(path);
            }
            paths = argList;
        }
        return paths;
    }

    private static void callWindowsDiskCleanup(String disk)
    {
        try {
            System.out.println("Calling cleaning manager for: " + disk);
            ProcessBuilder pb = new ProcessBuilder("cleanmgr","/verylowdisk", "/d" + disk.substring(0,2).toUpperCase());
            Process process = pb.start();
            Thread.sleep(10 * 1000);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
