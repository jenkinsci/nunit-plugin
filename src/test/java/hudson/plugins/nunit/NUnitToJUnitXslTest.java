package hudson.plugins.nunit;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Transform;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.xml.sax.InputSource;

/**
 * Unit test for the XSL transformation
 * 
 * @author Erik Ramfelt
 */
public class NUnitToJUnitXslTest {
    @Before
    public void setUp() {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreComments(true);
    }

    @Test
    public void testTransformation() throws Exception {

        Transform myTransform = new Transform(new InputSource(this.getClass().getResourceAsStream("NUnit-simple.xml")),
                new InputSource(this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-simple.xml"), myTransform);
        assertTrue("XSL transformation did not work" + myDiff, myDiff.similar());
    }

    @Test
    public void testTransformationFailure() throws Exception {

        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-failure.xml")), new InputSource(this
                        .getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-failure.xml"), myTransform.getResultString());
        assertTrue("XSL transformation did not work" + myDiff, myDiff.similar());
    }

    @Test
    public void testTransformationMultiNamespace() throws Exception {

        XMLUnit.setNormalizeWhitespace(false);
        Transform myTransform = new Transform(new InputSource(this.getClass().getResourceAsStream(
                "NUnit-multinamespace.xml")), new InputSource(this.getClass().getResourceAsStream(
                NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-multinamespace.xml"), myTransform);
        assertTrue("XSL transformation did not work" + myDiff, myDiff.similar());
    }

    @Test
    public void testTransformedIgnored() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-ignored.xml")), new InputSource(this
                        .getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-ignored.xml"), myTransform);
        assertTrue("XSL transformation did not work" + myDiff, myDiff.similar());
    }

    @Test
    public void testTransformedIssue1077() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-issue1077.xml")), new InputSource(this
                        .getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-issue1077.xml"), myTransform);
        assertTrue("XSL transformation did not work. " + myDiff, myDiff.similar());
    }

    @Test
    @Bug(6353)
    public void testSkippedTests() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-issue6353.xml")), new InputSource(this
                        .getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-issue6353.xml"), myTransform);
        assertTrue("XSL transformation did not work. " + myDiff, myDiff.similar());
    }

    @Test
    @Bug(5674)
    public void testThatNameIsFilledOut() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-issue5674.xml")), new InputSource(this
                        .getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-issue5674.xml"), myTransform);
        assertTrue("XSL transformation did not work. " + myDiff, myDiff.similar());
    }

    private String readXmlAsString(String resourceName) throws IOException {
        String xmlString = "";

        BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(
                resourceName)));
        String line = reader.readLine();
        while (line != null) {
            xmlString += line + "\n";
            line = reader.readLine();
        }
        reader.close();

        return xmlString;
    }
}
