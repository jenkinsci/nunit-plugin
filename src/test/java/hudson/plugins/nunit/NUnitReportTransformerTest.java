package hudson.plugins.nunit;

import java.io.File;
import java.io.FilenameFilter;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;

public class NUnitReportTransformerTest extends AbstractWorkspaceTest implements FilenameFilter {

    private NUnitReportTransformer transformer;
    private File tempFilePath;

    @Before
    public void setup() throws Exception {
        super.createWorkspace();
        transformer = new NUnitReportTransformer();
        tempFilePath = parentFile;
    }

    @After
    public void teardown() throws Exception {
        super.deleteWorkspace();
    }

    @Test
    public void testUnicodeTransform() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("NUnitUnicode.xml"), tempFilePath);
        assertJunitFiles(1);
    }

    @Test
    public void testDeleteOutputFiles() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("NUnit.xml"), tempFilePath);
        File[] listFiles = tempFilePath.listFiles(this);
        for (File file : listFiles) {
            Assert.assertTrue("Could not delete the transformed files", file.delete());
        }
    }

    @Test
    public void testTransform() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("NUnit.xml"), tempFilePath);
        assertJunitFiles(2);
    }

    @Bug(5673)
    @Test
    public void testFilenameDoesNotContainInvalidChars() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("issue-5673.xml"), tempFilePath);
        assertJunitFiles(3);
    }

    private void assertJunitFiles(int expectedJunitFilesCount) throws DocumentException {
        File[] listFiles = tempFilePath.listFiles(this);
        Assert.assertEquals("The number of junit files are incorrect.", expectedJunitFilesCount, listFiles.length);
        for (File file : listFiles) {
            Document result = new SAXReader().read(file);
            Assert.assertNotNull("The XML wasnt parsed", result);
            org.dom4j.Element root = result.getRootElement();
            Assert.assertNotNull("There is no root in the XML", root);
            Assert.assertEquals("The name is not correct", "testsuite", root.getName());
        }
    }

    public boolean accept(File dir, String name) {
        return name.startsWith(NUnitReportTransformer.JUNIT_FILE_PREFIX);
    }
}
