package hudson.plugins.nunit;

import org.apache.commons.io.input.ProxyInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by acearl on 6/1/2017.
 */
public class InvalidXmlInputStream extends ProxyInputStream {
    private final byte replacement;

    /**
     * Constructs a new ProxyInputStream.
     *
     * @param delegate the InputStream to delegate to
     */
    public InvalidXmlInputStream(InputStream delegate, char replacement) {
        super(delegate);
        this.replacement = (byte)replacement;
    }

    private boolean isValid(int input) {
        return ((input == 0x9) || (input == 0xA) || (input == 0xD) ||
                ((input >= 0x20) && (input <= 0xD7FF)) ||
                ((input >= 0xE000) && (input <= 0xFFFD)) ||
                ((input >= 0x10000) && (input <= 0x10FFFF)));
    }

    /**
     * Invokes the delegate's <code>read()</code> method, detecting and skipping invalid xml characters
     *
     * @return the byte read (excluding invalid xml characters) or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        int read = super.read();
        if(read >= 0 && !isValid(read)) {
            return (int)replacement;
        }
        return read;
    }

    /**
     * Invokes the delegate's <code>read(byte[], int, int)</code> method, detecting and skipping invalid xml.
     *
     * @param cbuf the buffer to read the bytes into
     * @param off  The start offset
     * @param len  The number of bytes to read
     * @return the number of bytes read or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] cbuf, int off, int len) throws IOException {
        int read = super.read(cbuf, off, len);

        if (read == -1) {
            return -1;
        }

        int pos = off - 1;
        for (int readPos = off; readPos < off + read; readPos++) {
            if (!isValid(cbuf[readPos])) {
                cbuf[readPos] = replacement;
            }
            pos++;
        }
        return pos - off + 1;
    }
}
