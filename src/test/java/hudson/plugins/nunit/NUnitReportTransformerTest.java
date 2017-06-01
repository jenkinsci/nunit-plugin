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
import org.jvnet.hudson.test.Issue;

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

    @Issue("JENKINS-5673")
    @Test
    public void testFilenameDoesNotContainInvalidChars() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("issue-5673.xml"), tempFilePath);
        assertJunitFiles(3);
    }

    @Issue("JENKINS-44315")
    @Test
    public void testIssue44315() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue44315.xml"), tempFilePath);
        assertJunitFiles(195);
    }

    @Issue("JENKINS-44527")
    @Test
    public void testIssue44527() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue44527.xml"), tempFilePath);
        assertJunitFiles(144);
    }

    private void assertJunitFiles(int expectedJunitFilesCount) throws DocumentException {
        File[] listFiles = tempFilePath.listFiles(this);
        Assert.assertEquals("The number of junit files are incorrect.", expectedJunitFilesCount, listFiles.length);
        for (File file : listFiles) {
            Document result = new SAXReader().read(file);
            Assert.assertNotNull("The XML wasn't parsed", result);
            org.dom4j.Element root = result.getRootElement();
            Assert.assertNotNull("There is no root in the XML", root);
            Assert.assertEquals("The name is not correct", "testsuite", root.getName());
        }
    }

    @Issue("JENKINS-33493")
    @Test
    public void testXmlWithBOM() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue33493.xml"), tempFilePath);
        assertJunitFiles(2);
    }

    @Issue("JENKINS-17521")
    @Test
    public void testInvalidXmlCharacters() throws Exception {
        transformer.transform(getClass().getResourceAsStream("NUnit-issue17521.xml"), tempFilePath);
        assertJunitFiles(2);
    }

    public boolean accept(File dir, String name) {
        return name.startsWith(NUnitReportTransformer.JUNIT_FILE_PREFIX);
    }
}
