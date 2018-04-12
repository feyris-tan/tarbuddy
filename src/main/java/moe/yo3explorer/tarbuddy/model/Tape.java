package moe.yo3explorer.tarbuddy.model;

import java.io.FileOutputStream;
import java.io.IOException;

public class Tape
{
    private String filename;
    private byte[] data;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void saveFile() throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(data);
        fos.flush();
        fos.close();
    }
}
