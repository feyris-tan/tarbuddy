package moe.yo3explorer.tarbuddy;

import moe.yo3explorer.tarbuddy.model.Tape;

import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;

public class Main
{
    final static int tapeLength = 99;
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
            System.out.println("Begin walking the file tree of: " + path.toString());
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
}
