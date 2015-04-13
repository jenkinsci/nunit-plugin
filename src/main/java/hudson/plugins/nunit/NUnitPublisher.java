package hudson.plugins.nunit;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResultProjectAction;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Class that records NUnit test reports into Jenkins.
 * 
 * @author Erik Ramfelt
 */
public class NUnitPublisher extends Recorder implements Serializable {

    private static final long serialVersionUID = 1L;

    private static transient final String PLUGIN_NUNIT = "/plugin/nunit/";

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private String testResultsPattern;
    private boolean debug = false;
    private boolean keepJUnitReports = false;
    private boolean skipJUnitArchiver = false;
    
    /**
     * <p>Flag that when set, <strong>marks the build as failed if there are
     * no test results</strong>.</p>
     * 
     * <p>Defaults to <code>true</code>.</p>
     */
    private final boolean failIfNoResults;

    @Deprecated
    public NUnitPublisher(String testResultsPattern, boolean debug, boolean keepJUnitReports, boolean skipJUnitArchiver) {
    	this(testResultsPattern, debug, keepJUnitReports, skipJUnitArchiver, Boolean.TRUE);
    }
    
    @DataBoundConstructor
    public NUnitPublisher(String testResultsPattern, boolean debug, boolean keepJUnitReports, boolean skipJUnitArchiver, Boolean failIfNoResults) {
        this.testResultsPattern = testResultsPattern;
        this.debug = debug;
        if (this.debug) {
            this.keepJUnitReports = keepJUnitReports;
            this.skipJUnitArchiver = skipJUnitArchiver;
        }
        this.failIfNoResults = BooleanUtils.toBooleanDefaultIfNull(failIfNoResults, Boolean.TRUE);
    }
    
    public Object readResolve() {
    	return new NUnitPublisher(
			testResultsPattern, 
			debug, 
			keepJUnitReports, 
			skipJUnitArchiver, 
			BooleanUtils.toBooleanDefaultIfNull(failIfNoResults, Boolean.TRUE));
    }

    public String getTestResultsPattern() {
        return testResultsPattern;
    }
    public void setTestResultsPattern(String pattern){
        this.testResultsPattern = pattern;
    }
    
    public void setDebug(boolean b){
        this.debug = b;
    }
    public boolean getDebug() {
        return debug;
    }

    public void setKeepJunitReports(boolean b){
        this.keepJUnitReports = b;
    }
    public boolean getKeepJunitReports() {
        return keepJUnitReports;
    }

    public void setSkipJunitArchiver(boolean b){
        this.skipJUnitArchiver = b;
    }
    public boolean getSkipJunitArchiver() {
        return skipJUnitArchiver;
    }
    
    public boolean getFailIfNoResults() {
		return failIfNoResults;
	}

    @Override
    public Action getProjectAction(AbstractProject<?,?> project) {
        TestResultProjectAction action = project.getAction(TestResultProjectAction.class);
        if (action == null) {
            return new TestResultProjectAction(project);
        } else {
            return action;
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        if (debug) {
            listener.getLogger().println("NUnit publisher running in debug mode.");
        }
        boolean result = true;
        try {
            EnvVars env = build.getEnvironment(listener);
            String resolvedTestResultsPattern = env.expand(testResultsPattern);

            listener.getLogger().println("Recording NUnit tests results");
            NUnitArchiver transformer = new NUnitArchiver(listener, resolvedTestResultsPattern, new NUnitReportTransformer(), failIfNoResults);
            result = build.getWorkspace().act(transformer);

            if (result) {
                if (skipJUnitArchiver) {
                    listener.getLogger().println("Skipping feeding JUnit reports to JUnitArchiver");
                } else {
                    // Run the JUnit test archiver
                    result = recordTestResult(NUnitArchiver.JUNIT_REPORTS_PATH + "/TEST-*.xml", build, listener);
                }
                
                if (keepJUnitReports) {
                    listener.getLogger().println("Skipping deletion of temporary JUnit reports.");
                } else {
                    build.getWorkspace().child(NUnitArchiver.JUNIT_REPORTS_PATH).deleteRecursive();
                }
            }
            
        } catch (TransformerException te) {
            throw new AbortException("Could not read the XSL XML file. Please report this issue to the plugin author");
        }

        return result;
    }

    /**
     * Record the test results into the current build.
     * @param junitFilePattern
     * @param build
     * @param listener
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private boolean recordTestResult(String junitFilePattern, AbstractBuild<?, ?> build, BuildListener listener)
            throws InterruptedException, IOException {
        TestResultAction existingAction = build.getAction(TestResultAction.class);
        TestResultAction action;

        final long buildTime = build.getTimestamp().getTimeInMillis();

        TestResult existingTestResults = null;
        if (existingAction != null) {
            existingTestResults = existingAction.getResult();
        }
        TestResult result = getTestResult(junitFilePattern, build, existingTestResults, buildTime);

        if (existingAction == null) {
            action = new TestResultAction(build, result, listener);
        } else {
            action = existingAction;
            action.setResult(result, listener);
        }

        if (this.failIfNoResults && result.getPassCount() == 0 && result.getFailCount() == 0 && result.getSkipCount() == 0) {
            listener.getLogger().println("None of the test reports contained any result");
            build.setResult(Result.FAILURE);
            return true;
        }

        if (existingAction == null) {
            build.getActions().add(action);
        }

        if (action.getResult().getFailCount() > 0)
            build.setResult(Result.UNSTABLE);

        return true;
    }

    /**
     * Collect the test results from the files
     * @param junitFilePattern
     * @param build
     * @param existingTestResults existing test results to add results to
     * @param buildTime
     * @return a test result
     * @throws IOException
     * @throws InterruptedException
     */
    private TestResult getTestResult(final String junitFilePattern, AbstractBuild<?, ?> build,
            final TestResult existingTestResults, final long buildTime) throws IOException, InterruptedException {
        TestResult result = build.getWorkspace().act(new FileCallable<TestResult>() {
			private static final long serialVersionUID = -8917897415838795523L;

			public TestResult invoke(File ws, VirtualChannel channel) throws IOException {
                FileSet fs = Util.createFileSet(ws,junitFilePattern);
                DirectoryScanner ds = fs.getDirectoryScanner();

                String[] files = ds.getIncludedFiles();
                if(files.length==0) {
                	if (NUnitPublisher.this.failIfNoResults) {
	                    // no test result. Most likely a configuration error or fatal problem
	                    throw new AbortException("No test report files were found. Configuration error?");
                	} else {
                		return new TestResult();
                	}
                }
                if (existingTestResults == null) {
                    return new TestResult(buildTime, ds, true);
                } else {
                    existingTestResults.parse(buildTime, ds);
                    return existingTestResults;
                }
            }
        });
        return result;
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        protected DescriptorImpl() {
            super(NUnitPublisher.class);
        }

        @Override
        public String getDisplayName() {
            return "Publish NUnit test result report";
        }

        @Override
        public String getHelpFile() {
            return PLUGIN_NUNIT + "help.html";
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
