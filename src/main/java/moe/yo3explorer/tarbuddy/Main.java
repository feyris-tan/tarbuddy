package moe.yo3explorer.tarbuddy;

import moe.yo3explorer.tarbuddy.model.Tape;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

public class Main
{
    final static int tapeLength = 10;
    final static Unit tapeLengthUnit = Unit.GIGA;

    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        DatabaseConnection dbc = new DatabaseConnection();

        TarBuddyFileVisitor tbfv = new TarBuddyFileVisitor(dbc,tapeLength * tapeLengthUnit.getMul());

        FileSystem fs = FileSystems.getDefault();
        for (Path path : fs.getRootDirectories()) {
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
}
