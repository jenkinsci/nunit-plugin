package hudson.plugins.nunit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import jenkins.security.MasterToSlaveCallable;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.remoting.RoleChecker;
import org.jenkinsci.remoting.RoleSensitive;
import org.xml.sax.SAXException;

import hudson.FilePath;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.security.Roles;

/**
 * Class responsible for transforming NUnit to JUnit files and then run them all through the JUnit result archiver.
 * 
 * @author Erik Ramfelt
 */
public class NUnitArchiver extends MasterToSlaveCallable<Boolean, IOException> {

    private static final long serialVersionUID = 1L;

    public static final String JUNIT_REPORTS_PATH = "temporary-junit-reports";

    private final String root;
    private final TaskListener listener;
    private final String testResultsPattern;
    private final TestReportTransformer unitReportTransformer;
    private final boolean failIfNoResults;
    private final String warningNUnitTestResultHanlingBehavior;

    private int fileCount;

    public NUnitArchiver(String root, TaskListener listener, String testResultsPattern, TestReportTransformer unitReportTransformer, boolean failIfNoResults) {
        this.root = root;
        this.listener = listener;
        this.testResultsPattern = testResultsPattern;
        this.unitReportTransformer = unitReportTransformer;
        this.failIfNoResults = failIfNoResults;
        this.warningNUnitTestResultHanlingBehavior = "skipped";
    }

    public NUnitArchiver(String root, TaskListener listener, String testResultsPattern, TestReportTransformer unitReportTransformer, boolean failIfNoResults, String warningNUnitTestResultHanlingBehavior) {
        this.root = root;
        this.listener = listener;
        this.testResultsPattern = testResultsPattern;
        this.unitReportTransformer = unitReportTransformer;
        this.failIfNoResults = failIfNoResults;
        this.warningNUnitTestResultHanlingBehavior = warningNUnitTestResultHanlingBehavior;
    }

    /** {@inheritDoc} */
    public Boolean call() throws IOException {
        boolean retValue = true;
        String[] nunitFiles = findNUnitReports(new File(root));
        if (nunitFiles.length > 0) {
            File junitOutputPath = new File(root, JUNIT_REPORTS_PATH);
            junitOutputPath.mkdirs();
    
            for (String nunitFileName : nunitFiles) {
                try(FileInputStream fileStream = new FileInputStream(new File(root, nunitFileName))) {
                    boolean shouldTransformWarningsToFails = ! warningNUnitTestResultHanlingBehavior.equals("skipped");
                    listener.getLogger().println("Transforming :" + nunitFileName + "; shouldTransformWarningsToFails:" + Boolean.toString(shouldTransformWarningsToFails));
                    unitReportTransformer.transform(fileStream, junitOutputPath, shouldTransformWarningsToFails);
                    fileCount++;
                } catch (TransformerException te) {
                    throw new IOException(
                            "Could not transform the NUnit report. Please report this issue to the plugin author", te);
                } catch (SAXException se) {
                    throw new IOException(
                            "Could not transform the NUnit report. Please report this issue to the plugin author", se);
                } catch (ParserConfigurationException pce) {
                    throw new IOException(
                            "Could not initialize the XML parser. Please report this issue to the plugin author", pce);
                }
            }
        } else {
            retValue = false;
        }

        return retValue;
    }

    int getFileCount() {
        return fileCount;
    }

    /**
     * Return all NUnit report files
     * 
     * @param parentPath parent
     * @return an array of strings
     */
    private String[] findNUnitReports(File parentPath) {
        FileSet fs = Util.createFileSet(parentPath, testResultsPattern);
        DirectoryScanner ds = fs.getDirectoryScanner();

        String[] nunitFiles = ds.getIncludedFiles();
        if (nunitFiles.length == 0) {
        	if (this.failIfNoResults) {
	            // no test result. Most likely a configuration error or fatal problem
	            listener.fatalError("No NUnit test report files were found. Configuration error?");
        	} else {
        		listener.getLogger().println("No NUnit test report files were found.");
        	}
        }
        return nunitFiles;
    }
}
