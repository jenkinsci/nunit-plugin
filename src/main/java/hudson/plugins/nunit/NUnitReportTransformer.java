package hudson.plugins.nunit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
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

    private static final Logger LOGGER = Logger.getLogger(NUnitReportTransformer.class.getName());

    private static final String ILLEGAL_FILE_CHARS_REGEX = "[\\*/:<>\\?\\|\\\\\";]+";

    private static final long serialVersionUID = 1L;

    public static final String JUNIT_FILE_POSTFIX = ".xml";
    public static final String JUNIT_FILE_PREFIX = "TEST-";

    private static final int MAX_PATH = 255;
    private static final String TEMP_JUNIT_FILE_STR = "temp-junit.xml";
    public static final String NUNIT_TO_JUNIT_XSLFILE_STR = "nunit-to-junit.xsl";

    private static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

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
     * @throws TransformerException TransformerException
     * @throws SAXException SAXException
     * @throws ParserConfigurationException ParserConfigurationException
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public void transform(InputStream nunitFileStream, File junitOutputPath)
            throws IOException, TransformerException, SAXException, ParserConfigurationException {

        initialize();

        File junitTargetFile = new File(junitOutputPath, TEMP_JUNIT_FILE_STR);
        FileOutputStream fileOutputStream = new FileOutputStream(junitTargetFile);
        try {
            Reader reader = new BufferedReader(new InvalidXmlStreamReader(nunitFileStream, '?'));
            nunitTransformer.transform(new StreamSource(reader), new StreamResult(fileOutputStream));
        } finally {
            fileOutputStream.close();
        }
        splitJUnitFile(junitTargetFile, junitOutputPath);
        junitTargetFile.delete();
    }

    private void initialize()
            throws TransformerFactoryConfigurationError, TransformerConfigurationException,
                    ParserConfigurationException {
        if (!xslIsInitialized) {
            TransformerFactory transformerFactory = createTransformer();

            nunitTransformer = transformerFactory.newTransformer(
                    new StreamSource(this.getClass().getResourceAsStream(NUNIT_TO_JUNIT_XSLFILE_STR)));
            writerTransformer = transformerFactory.newTransformer();

            DocumentBuilderFactory factory = createDocumentBuilderFactory();
            xmlDocumentBuilder = factory.newDocumentBuilder();

            xslIsInitialized = true;
        }
    }

    private TransformerFactory createTransformer() throws TransformerConfigurationException {
        // the default class does not support the options needed for secure processing
        TransformerFactory transformerFactory = TransformerFactory.newInstance(
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null);
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        return transformerFactory;
    }

    @SuppressFBWarnings("WMI_WRONG_MAP_ITERATOR")
    private DocumentBuilderFactory createDocumentBuilderFactory() {
        // the default class does not support the options needed for secure processing
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance(
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl", null);
        HashMap<String, Boolean> features = new HashMap<>();

        dFactory.setExpandEntityReferences(false);

        features.put(DISALLOW_DOCTYPE_DECL, true);
        features.put(EXTERNAL_GENERAL_ENTITIES, false);
        features.put(EXTERNAL_PARAMETER_ENTITIES, false);
        features.put(LOAD_EXTERNAL_DTD, false);
        features.put(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        for (String feature : features.keySet()) {
            try {
                dFactory.setFeature(feature, features.get(feature));
            } catch (ParserConfigurationException e) {
                LOGGER.log(Level.INFO, "Could not enable/disable feature: " + feature);
            }
        }

        HashMap<String, String> attributes = new HashMap<>();
        attributes.put(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        attributes.put(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        for (String attribute : attributes.keySet()) {
            try {
                dFactory.setAttribute(attribute, attributes.get(attribute));
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.INFO, "Could not set attribute: " + attribute);
            }
        }

        dFactory.setXIncludeAware(false);
        dFactory.setExpandEntityReferences(false);
        return dFactory;
    }
    /**
     * Splits the junit file into several junit files in the output path
     *
     * @param junitFile report containing one or more junit test suite tags
     * @param junitOutputPath the path to put all junit files
     * @throws IOException IOException
     * @throws SAXException SAXException
     * @throws TransformerException TransformerException
     */
    private void splitJUnitFile(File junitFile, File junitOutputPath)
            throws SAXException, IOException, TransformerException {
        transformCount++;
        try {
            Document document = xmlDocumentBuilder.parse(junitFile);

            NodeList elementsByTagName =
                    ((Element) document.getElementsByTagName("testsuites").item(0)).getElementsByTagName("testsuite");
            for (int i = 0; i < elementsByTagName.getLength(); i++) {
                Element element = (Element) elementsByTagName.item(i);
                DOMSource source = new DOMSource(element);
                String fileNamePostfix = "_" + transformCount + "_" + i + JUNIT_FILE_POSTFIX;
                String filename = JUNIT_FILE_PREFIX
                        + element.getAttribute("name").replaceAll(ILLEGAL_FILE_CHARS_REGEX, "_")
                        + fileNamePostfix;
                File junitOutputFile = new File(junitOutputPath, filename);

                // check for really long file names
                if (junitOutputFile.toString().length() >= MAX_PATH) {
                    int maxMiddleLength = MAX_PATH
                            - JUNIT_FILE_PREFIX.length()
                            - fileNamePostfix.length()
                            - junitOutputPath.toString().length();
                    filename = JUNIT_FILE_PREFIX
                            + StringUtils.left(
                                    element.getAttribute("name").replaceAll(ILLEGAL_FILE_CHARS_REGEX, "_"),
                                    maxMiddleLength)
                            + fileNamePostfix;
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
        } catch (SAXParseException e) {
            if (!e.getMessage().startsWith("Premature end of file")) {
                throw e;
            }
        }
    }
}
