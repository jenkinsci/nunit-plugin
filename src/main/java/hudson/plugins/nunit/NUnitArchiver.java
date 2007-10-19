package hudson.plugins.nunit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.xml.sax.SAXException;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.util.IOException2;

/**
 * Class responsible for transforming NUnit to JUnit files and then run
 * them all through the JUnit result archiver.
 * 
 * @author Erik Ramfelt
 */
public class NUnitArchiver implements FilePath.FileCallable<Boolean> {

	private static final long serialVersionUID = 1L;

	public static final String JUNIT_REPORTS_PATH = "temporary-junit-reports";

	private transient final String testResultsPattern;
	private transient boolean keepJUnitReports = false;
	private transient boolean skipJUnitArchiver = false;
	
	// Build related objects
	private transient final BuildListener listener;
	private transient final Build<?, ?> build;
	private transient final Launcher launcher;

	private transient TestReportTransformer unitReportTransformer;
	private transient TestReportArchiver unitResultArchiver;;

	public NUnitArchiver(
			Build<?, ?> build, Launcher launcher, BuildListener listener,
			String testResults, TestReportArchiver unitResultArchiver, TestReportTransformer transformer,
			boolean keepJUnitReports, boolean skipJUnitArchiver) 
				throws TransformerException, ParserConfigurationException  {

		this.unitResultArchiver = unitResultArchiver;
		this.launcher = launcher;
		this.build = build;
		this.listener = listener;
		this.testResultsPattern = testResults;
		this.unitReportTransformer = transformer;
		this.keepJUnitReports = keepJUnitReports;
		this.skipJUnitArchiver = skipJUnitArchiver;
	}
	
	/** {@inheritDoc} */
	public Boolean invoke(File ws, VirtualChannel channel) throws IOException {
		Boolean retValue = Boolean.FALSE;
		listener.getLogger().println("Transforming NUnit tests results");
		String[] nunitFiles = findNUnitReports(ws);
        File junitOutputPath = new File(ws, JUNIT_REPORTS_PATH);
        junitOutputPath.mkdirs();

        for (String nunitFileName : nunitFiles) {
	        FileInputStream fileStream = new FileInputStream(new File(ws, nunitFileName));
	    	try {		        
		        // Transform all NUnit files
	        	//listener.getLogger().println("Transforming " + nunitFileName);
				unitReportTransformer.transform(fileStream, junitOutputPath);
			} catch (TransformerException te) {
				throw new IOException2("Could not transform the NUnit report. Please report this issue to the plugin author", te);
			} catch (SAXException se) {
				throw new IOException2("Could not transform the NUnit report. Please report this issue to the plugin author", se);
			} finally {
				fileStream.close();
			}
        }
    	
        if (skipJUnitArchiver) {
        	listener.getLogger().println("Skipping feeding JUnit reports to JUnitArchiver");
        } else {
	        // Run the JUnit test archiver
	        retValue = performJUnitArchiver();
        }

		if (keepJUnitReports) {
			listener.getLogger().println("Skipping deletion of temporary JUnit reports.");
		} else {
			// Delete JUnit report files and temp folder        
	        // listener.getLogger().println("Deleting transformed JUnit results");
	        for (File file : junitOutputPath.listFiles()) {
	        	file.delete();
	        }
	        junitOutputPath.delete();
		}
        
    	return retValue;
	}

	/**
	 * Return all NUnit report files
	 * @param parentPath parent
	 * @return an array of strings
	 */
	private String[] findNUnitReports(File parentPath) throws AbortException {
		FileSet fs = new FileSet();
        Project p = new Project();
        fs.setProject(p);
        fs.setDir(parentPath);
        fs.setIncludes(testResultsPattern);
        DirectoryScanner ds = fs.getDirectoryScanner(p);

        String[] nunitFiles = ds.getIncludedFiles();
        if(nunitFiles.length==0) {
            // no test result. Most likely a configuration error or fatal problem
        	listener.fatalError("No NUnit test report files were found. Configuration error?");
            throw new AbortException();
        }
		return nunitFiles;
	}
	
	/**
	 * Run all JUnit reports through the JUnit archiver
	 * @return true if the JUnitResultArchiver was successful; false otherwise
	 * @throws IOException thrown if the JUnitResultArchiver.perform() methods throws it
	 */
	private Boolean performJUnitArchiver() throws IOException {
		Boolean retValue = Boolean.TRUE;
		try {
			if (! unitResultArchiver.archive(build, launcher, listener)) {
				retValue = Boolean.FALSE;
			}
		} catch (InterruptedException ie) {
			throw new IOException2(ie);
		}
		return retValue;
	}
}
