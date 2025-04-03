package hudson.plugins.nunit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Transform;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.xml.sax.InputSource;

/**
 * Unit test for the XSL transformation
 *
 * @author Erik Ramfelt
 */
class NUnitToJUnitXslTest {

    @BeforeEach
    void setUp() {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
    }

    @Test
    void testTransformation() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-simple.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-simple.xml"), myTransform);
        assertTrue(myDiff.similar(), "XSL transformation did not work" + myDiff);
    }

    @Test
    void testTransformationFailure() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-failure.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-failure.xml"), myTransform.getResultString());
        assertTrue(myDiff.similar(), "XSL transformation did not work" + myDiff);
    }

    @Test
    void testTransformationMultiNamespace() throws Exception {
        XMLUnit.setNormalizeWhitespace(false);
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-multinamespace.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-multinamespace.xml"), myTransform);
        assertTrue(myDiff.similar(), "XSL transformation did not work" + myDiff);
    }

    @Test
    void testTransformedIgnored() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-ignored.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-ignored.xml"), myTransform);
        assertTrue(myDiff.similar(), "XSL transformation did not work" + myDiff);
    }

    @Test
    void testTransformedIssue1077() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-issue1077.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-issue1077.xml"), myTransform);
        assertTrue(myDiff.similar(), "XSL transformation did not work. " + myDiff);
    }

    @Test
    @Issue("JENKINS-48478")
    void testTransformedIssue48478() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-issue48478.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-issue48478.xml"), myTransform);
        assertTrue(myDiff.similar(), "XSL transformation did not work. " + myDiff);
    }

    @Test
    @Issue("JENKINS-6353")
    void testSkippedTests() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-issue6353.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-issue6353.xml"), myTransform);
        assertTrue(myDiff.similar(), "XSL transformation did not work. " + myDiff);
    }

    @Test
    @Issue("JENKINS-5674")
    void testThatNameIsFilledOut() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-issue5674.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-issue5674.xml"), myTransform);
        assertTrue(myDiff.similar(), "XSL transformation did not work. " + myDiff);
    }

    @Test
    @Issue("JENKINS-5674")
    void namedTestsAreProperlyParsed() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-issue5674-setname.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-issue5674-setname.xml"), myTransform);
        assertTrue(myDiff.similar(), "XSL transformation did not work. " + myDiff);
    }

    @Test
    void testTransformationHandlesSkippedTestsIssue110() throws Exception {
        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("NUnit-issue110.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(NUnitReportTransformer.NUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("JUnit-issue110.xml"), myTransform);
        assertTrue(myDiff.similar(), "XSL transformation did not correctly handle skipped tests. " + myDiff);
    }

    private String readXmlAsString(String resourceName) throws IOException {
        StringBuilder xmlString = new StringBuilder();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(resourceName)));
        String line = reader.readLine();
        while (line != null) {
            xmlString.append(line).append("\n");
            line = reader.readLine();
        }
        reader.close();

        return xmlString.toString();
    }
}
