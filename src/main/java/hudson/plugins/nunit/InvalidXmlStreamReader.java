package hudson.plugins.nunit;

import java.io.BufferedInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.io.input.BOMInputStream;

/**
 * Provides similar functionality to org.apache.commons.io.input.XmlStreamReader plus replacing invalid xml characters.
 */
public class InvalidXmlStreamReader extends FilterReader {
    private static class Source {
        public final String version;
        public final Reader reader;

        public Source(InputStream in) throws IOException {
            InputStream is = new BufferedInputStream(new BOMInputStream(in));
            is.mark(1024);
            String encoding;
            try {
                XMLStreamReader xsr = XMLInputFactory.newFactory().createXMLStreamReader(is);
                try {
                    encoding = xsr.getCharacterEncodingScheme();
                    version = xsr.getVersion();
                } finally {
                    xsr.close();
                }
            } catch (XMLStreamException e) {
                throw new IOException(e);
            }
            is.reset();
            reader = new InputStreamReader(is, encoding == null ? "UTF-8" : encoding);
        }
    }

    private static final boolean[] validControls10 = {
        false, false, false, false, false, false, false, false,
        false, true, true, false, false, true, false, false,
        false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false
    };
    private static final boolean[] validControls11 = {
        false, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true
    };

    private final boolean[] validControls;
    private final char replacement;

    private InvalidXmlStreamReader(Source in, char replacement) {
        super(in.reader);
        this.validControls = "1.1".equals(in.version) ? validControls11 : validControls10;
        this.replacement = replacement;
    }
    /**
     * Constructs a new reader.
     *
     * @param in the underlying stream.
     * @param replacement the replacement character for invalid xml characters.
     */
    public InvalidXmlStreamReader(InputStream in, char replacement) throws IOException {
        this(new Source(in), replacement);
    }

    private boolean isValid(int input) {
        if (input < 0x20) return validControls[input];
        return input <= 0xD7FF || input >= 0xE000 && input <= 0xFFFD || input >= 0x10000 && input <= 0x10FFFF;
    }

    /**
     * Invokes the in's <code>read()</code> method, detecting and skipping invalid xml characters
     *
     * @return the character read (excluding invalid xml characters) or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read >= 0 && !isValid(read)) {
            return (int) replacement;
        }
        return read;
    }

    /**
     * Invokes the in's <code>read(byte[], int, int)</code> method, detecting and skipping invalid xml.
     *
     * @param cbuf the buffer to read the characters into
     * @param off  The start offset
     * @param len  The maximum number of characters to read
     * @return the number of characters read or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = super.read(cbuf, off, len);

        if (read == -1) {
            return -1;
        }

        for (int readPos = off; readPos < off + read; readPos++) {
            if (!isValid(cbuf[readPos])) {
                cbuf[readPos] = replacement;
            }
        }
        return read;
    }
}
