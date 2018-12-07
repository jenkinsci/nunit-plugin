package hudson.plugins.nunit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.input.BOMInputStream;
import hudson.Functions;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Transforms a NUnit report into seperate JUnit reports. The NUnit report can contain several test cases and the JUnit
 * report that is read by Jenkins should only contain one. This class will split up one NUnit report into several JUnit
 * files.
 * 
 */
public class NUnitReportTransformer implements TestReportTransformer, Serializable {

    private static final String ILLEGAL_FILE_CHARS_REGEX = "[\\*/:<>\\?\\|\\\\\";]+";

	private static final long serialVersionUID = 1L;
    
    public static final String JUNIT_FILE_POSTFIX = ".xml";
    public static final String JUNIT_FILE_PREFIX = "TEST-";

    private static final int MAX_PATH = 256;
    private static final String TEMP_JUNIT_FILE_STR = "temp-junit.xml";
    public static final String NUNIT_TO_JUNIT_XSLFILE_STR = "nunit-to-junit.xsl";

    private transient boolean xslIsInitialized;
    private transient Transformer nunitTransformer;
    private transient Transformer writerTransformer;
    private transient DocumentBuilder xmlDocumentBuilder;
    private transient int transformCount;

    /**
     * Transform the nunit file into several junit files in the output path
     * 
     * @param nunitFileStream the nunit file stream to transform
     * @param junitOutputPath the output path to put all junit files
     * @throws IOException thrown if there was any problem with the transform.
     * @throws TransformerException
     * @throws SAXException
     * @throws ParserConfigurationException 
     */
    public void transform(InputStream nunitFileStream, File junitOutputPath) throws IOException, TransformerException,
            SAXException, ParserConfigurationException {
        
        initialize();
        
        File junitTargetFile = new File(junitOutputPath, TEMP_JUNIT_FILE_STR);
        FileOutputStream fileOutputStream = new FileOutputStream(junitTargetFile);
        try {
            InputStream is = new InvalidXmlInputStream(new BOMInputStream(nunitFileStream), '?');
            nunitTransformer.transform(new StreamSource(is), new StreamResult(fileOutputStream));
        } finally {
            fileOutputStream.close();
        }
        splitJUnitFile(junitTargetFile, junitOutputPath);
        junitTargetFile.delete();
    }

    private void initialize() throws TransformerFactoryConfigurationError, TransformerConfigurationException,
            ParserConfigurationException {
        if (!xslIsInitialized) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            nunitTransformer = transformerFactory.newTransformer(new StreamSource(this.getClass().getResourceAsStream(NUNIT_TO_JUNIT_XSLFILE_STR)));
            writerTransformer = transformerFactory.newTransformer();
    
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            xmlDocumentBuilder = factory.newDocumentBuilder();
            
            xslIsInitialized = true;
        }
    }

    /**
     * Splits the junit file into several junit files in the output path
     * 
     * @param junitFile report containing one or more junit test suite tags
     * @param junitOutputPath the path to put all junit files
     * @throws IOException
     * @throws SAXException
     * @throws TransformerException
     */
    private void splitJUnitFile(File junitFile, File junitOutputPath) throws SAXException, IOException,
            TransformerException {
        transformCount++;
        try {
            Document document = xmlDocumentBuilder.parse(junitFile);

            NodeList elementsByTagName = ((Element) document.getElementsByTagName("testsuites").item(0)).getElementsByTagName("testsuite");
            for (int i = 0; i < elementsByTagName.getLength(); i++) {
                Element element = (Element) elementsByTagName.item(i);
                DOMSource source = new DOMSource(element);
                String fileNamePostfix = "_" + transformCount + "_" + i + JUNIT_FILE_POSTFIX;
                String filename = JUNIT_FILE_PREFIX + element.getAttribute("name").replaceAll(ILLEGAL_FILE_CHARS_REGEX, "_") + fileNamePostfix;
                File junitOutputFile = new File(junitOutputPath, filename);

                // check for really long file names
				if(junitOutputFile.toString().length() >= MAX_PATH) {
	                int maxMiddleLength = MAX_PATH - JUNIT_FILE_PREFIX.length() - fileNamePostfix.length() - junitOutputPath.toString().length();
	                filename = JUNIT_FILE_PREFIX + StringUtils.left(element.getAttribute("name").replaceAll(ILLEGAL_FILE_CHARS_REGEX, "_"), maxMiddleLength) + fileNamePostfix;
	                junitOutputFile = new File(junitOutputPath, filename);
	            }
                FileOutputStream fileOutputStream = new FileOutputStream(junitOutputFile);
                try {
                    StreamResult result = new StreamResult(fileOutputStream);
                    writerTransformer.transform(source, result);
                } finally {
                    fileOutputStream.close();
                }
            }
        } catch(SAXParseException e) {
            if(!e.getMessage().startsWith("Premature end of file")) {
                throw e;
            }
        }

    }
}
