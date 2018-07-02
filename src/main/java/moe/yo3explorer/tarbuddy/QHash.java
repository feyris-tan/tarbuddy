package moe.yo3explorer.tarbuddy;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;

class QHash
{
    private static final long _128kb = 128 * 1000;

    public static StringBuffer QHash(BasicFileAttributes basicFileAttributes, Path fullname) throws QHashException {
        ByteBuffer buffer = ByteBuffer.allocate((int)_128kb);
        FileChannel fc = null;
        try {
            fc = FileChannel.open(fullname);
            MessageDigest md = MessageDigest.getInstance("SHA1");

            fc = fc.position(0);
            fc.read(buffer);
            md.update(buffer.array());

            fc = fc.position((basicFileAttributes.size() / 2) - (_128kb / 2));
            fc.read(buffer);
            md.update(buffer.array());

            fc = fc.position(basicFileAttributes.size() - _128kb);
            fc.read(buffer);
            md.update(buffer.array());

            byte[] hashBuffer = md.digest();

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < hashBuffer.length; i++) {
                sb.append(Integer.toString((hashBuffer[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb;
        } catch (Exception e) {
            throw new QHashException(e);
        }
    }
}
