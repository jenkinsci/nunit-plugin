package hudson.plugins.nunit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.xml.sax.SAXException;

import hudson.FilePath;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.util.IOException2;

/**
 * Class responsible for transforming NUnit to JUnit files and then run them all through the JUnit result archiver.
 * 
 * @author Erik Ramfelt
 */
public class NUnitArchiver implements FilePath.FileCallable<Boolean>, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String JUNIT_REPORTS_PATH = "temporary-junit-reports";

    // Build related objects
    private final BuildListener listener;
    private final String testResultsPattern;

    private TestReportTransformer unitReportTransformer;

    public NUnitArchiver(BuildListener listener, String testResults, TestReportTransformer unitReportTransformer) throws TransformerException {
        this.listener = listener;
        this.testResultsPattern = testResults;
        this.unitReportTransformer = unitReportTransformer;
    }

    /** {@inheritDoc} */
    public Boolean invoke(File ws, VirtualChannel channel) throws IOException {
        Boolean retValue = Boolean.TRUE;
        String[] nunitFiles = findNUnitReports(ws);
        if (nunitFiles.length > 0) {
            File junitOutputPath = new File(ws, JUNIT_REPORTS_PATH);
            junitOutputPath.mkdirs();
    
            for (String nunitFileName : nunitFiles) {
                FileInputStream fileStream = new FileInputStream(new File(ws, nunitFileName));
                try {
                    unitReportTransformer.transform(fileStream, junitOutputPath);
                } catch (TransformerException te) {
                    throw new IOException2(
                            "Could not transform the NUnit report. Please report this issue to the plugin author", te);
                } catch (SAXException se) {
                    throw new IOException2(
                            "Could not transform the NUnit report. Please report this issue to the plugin author", se);
                } catch (ParserConfigurationException pce) {
                    throw new IOException2(
                            "Could not initalize the XML parser. Please report this issue to the plugin author", pce);
                } finally {
                    fileStream.close();
                }
            }
        } else {
            retValue = Boolean.FALSE;
        }

        return retValue;
    }

    /**
     * Return all NUnit report files
     * 
     * @param parentPath parent
     * @return an array of strings
     */
    private String[] findNUnitReports(File parentPath) {
        FileSet fs = Util.createFileSet(parentPath,testResultsPattern);
        DirectoryScanner ds = fs.getDirectoryScanner();

        String[] nunitFiles = ds.getIncludedFiles();
        if (nunitFiles.length == 0) {
            // no test result. Most likely a configuration error or fatal problem
            listener.fatalError("No NUnit test report files were found. Configuration error?");
        }
        return nunitFiles;
    }
}
