package hudson.plugins.nunit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import hudson.model.TaskListener;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.remoting.RoleChecker;
import org.jenkinsci.remoting.RoleSensitive;
import org.xml.sax.SAXException;

import hudson.FilePath;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import jenkins.security.Roles;

/**
 * Class responsible for transforming NUnit to JUnit files and then run them all through the JUnit result archiver.
 * 
 * @author Erik Ramfelt
 */
public class NUnitArchiver implements FilePath.FileCallable<Boolean>, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String JUNIT_REPORTS_PATH = "temporary-junit-reports";

    // Build related objects
    private final TaskListener listener;
    private final String testResultsPattern;

    private TestReportTransformer unitReportTransformer;
    
    private final Boolean failIfNoResults;

    @Deprecated
    public NUnitArchiver(BuildListener listener, String testResults, TestReportTransformer unitReportTransformer) throws TransformerException {
    	this(listener, testResults, unitReportTransformer, true);
    }
    
    public NUnitArchiver(TaskListener listener, String testResults, TestReportTransformer unitReportTransformer, Boolean failIfNoResults) throws TransformerException {
        this.listener = listener;
        this.testResultsPattern = testResults;
        this.unitReportTransformer = unitReportTransformer;
        this.failIfNoResults = failIfNoResults;
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
                    throw new IOException(
                            "Could not transform the NUnit report. Please report this issue to the plugin author", te);
                } catch (SAXException se) {
                    throw new IOException(
                            "Could not transform the NUnit report. Please report this issue to the plugin author", se);
                } catch (ParserConfigurationException pce) {
                    throw new IOException(
                            "Could not initalize the XML parser. Please report this issue to the plugin author", pce);
                } finally {
                    fileStream.close();
                }
            }
        } else if(this.failIfNoResults) {
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
        	if (this.failIfNoResults) {
	            // no test result. Most likely a configuration error or fatal problem
	            listener.fatalError("No NUnit test report files were found. Configuration error?");
        	} else {
        		listener.getLogger().println("No NUnit test report files were found.");
        	}
        }
        return nunitFiles;
    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {
        roleChecker.check((RoleSensitive) this, Roles.MASTER);
    }
}
