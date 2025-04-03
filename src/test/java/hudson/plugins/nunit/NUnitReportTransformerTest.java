package hudson.plugins.nunit;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.IOUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

class NUnitReportTransformerTest extends AbstractWorkspaceTest implements FilenameFilter {

    private NUnitReportTransformer transformer;
    private File tempFilePath;

    @BeforeEach
    void setUp() throws Exception {
        super.createWorkspace();
        transformer = new NUnitReportTransformer();
        tempFilePath = parentFile;
    }

    @AfterEach
    void tearDown() throws Exception {
        super.deleteWorkspace();
    }

    @Test
    void testUnicodeTransform() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("NUnitUnicode.xml"), tempFilePath);
        assertJunitFiles(1);
    }

    @Test
    void testDeleteOutputFiles() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("NUnit.xml"), tempFilePath);
        File[] listFiles = tempFilePath.listFiles(this);
        for (File file : listFiles) {
            assertTrue(file.delete(), "Could not delete the transformed files");
        }
    }

    @Test
    void testTransform() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("NUnit.xml"), tempFilePath);
        assertJunitFiles(2);
    }

    @Issue("JENKINS-5673")
    @Test
    void testFilenameDoesNotContainInvalidChars() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("issue-5673.xml"), tempFilePath);
        assertJunitFiles(3);
    }

    @Issue("JENKINS-44315")
    @Test
    void testIssue44315() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue44315.xml"), tempFilePath);
        assertJunitFiles(195);
    }

    @Test
    void testIssue44315_2() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue44315-2.xml"), tempFilePath);
        assertJunitFiles(1);
    }

    @Test
    void testIssue44315_3() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue44315-3.xml"), tempFilePath);
        assertJunitFiles(13);
    }

    @Issue("JENKINS-44527")
    @Test
    void testIssue44527() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue44527.xml"), tempFilePath);
        assertJunitFiles(144);
    }

    private void assertJunitFiles(int expectedJunitFilesCount) throws DocumentException {
        File[] listFiles = tempFilePath.listFiles(this);
        assertEquals(expectedJunitFilesCount, listFiles.length, "The number of junit files are incorrect.");
        for (File file : listFiles) {
            Document result = new SAXReader().read(file);
            assertNotNull(result, "The XML wasn't parsed");
            org.dom4j.Element root = result.getRootElement();
            assertNotNull(root, "There is no root in the XML");
            assertEquals("testsuite", root.getName(), "The name is not correct");
        }
    }

    @Issue("JENKINS-33493")
    @Test
    void testXmlWithBOM() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue33493.xml"), tempFilePath);
        assertJunitFiles(2);
    }

    @Issue("JENKINS-17521")
    @Test
    void testInvalidXmlCharacters() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue17521.xml"), tempFilePath);
        assertJunitFiles(2);
    }

    @Issue("JENKINS-50162")
    @Test
    void testNonAsciiCharacters() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue50162.xml"), tempFilePath);
        assertJunitFiles(2);
        boolean foundUmlaut = false;
        for (File file : tempFilePath.listFiles(this)) {
            Document result = new SAXReader().read(file);
            List<Node> nodes = result.selectNodes("//*/@*");
            for (Node node : nodes) {
                if (node instanceof Attribute && ((Attribute) node).getValue().contains("\u00c4")) {
                    foundUmlaut = true;
                    break;
                }
            }
        }
        assertTrue(foundUmlaut, "Non ASCII characters are not preserved.");
    }

    @Issue("SEC-1752")
    @Test
    void testPreventXXEWithHttps() throws Exception {
        assertThrows(
                TransformerException.class,
                () -> transformer.transform(getClass().getResourceAsStream("NUnit-sec1752-https.xml"), tempFilePath));
        assertJunitFiles(0);
    }

    @Issue("SEC-1752")
    @Test
    void testPreventXXEWithFile() throws Exception {
        File tempFile = new File(tempFilePath, "dummy.txt");
        try (FileWriter output = new FileWriter(tempFile)) {
            output.write("You should never see this");
        }
        InputStream input = getClass().getResourceAsStream("NUnit-sec1752-file.xml");
        String content =
                IOUtils.toString(input, StandardCharsets.UTF_8).replace("__FILEPATH__", tempFile.getAbsolutePath());
        try (InputStream transformStream = IOUtils.toInputStream(content, StandardCharsets.UTF_8)) {
            assertThrows(TransformerException.class, () -> transformer.transform(transformStream, tempFilePath));
        }
        assertJunitFiles(0);
    }

    public boolean accept(File dir, String name) {
        return name.startsWith(NUnitReportTransformer.JUNIT_FILE_PREFIX);
    }
}
