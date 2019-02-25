package hudson.plugins.nunit;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.transform.TransformerException;

import jenkins.MasterToSlaveFileCallable;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.BooleanUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.jenkinsci.remoting.RoleSensitive;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResultProjectAction;
import jenkins.security.Roles;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Class that records NUnit test reports into Jenkins.
 * 
 * @author Erik Ramfelt
 */
public class NUnitPublisher extends Recorder implements Serializable, SimpleBuildStep {

    private static final long serialVersionUID = 1L;

    private static transient final String PLUGIN_NUNIT = "/plugin/nunit/";

    private String testResultsPattern;
    private boolean debug = false;
    private boolean keepJUnitReports = false;
    private boolean skipJUnitArchiver = false;
    private Double healthScaleFactor;
    
    /**
     * <p>Flag that when set, <strong>marks the build as failed if there are
     * no test results</strong>.</p>
     * 
     * <p>Defaults to <code>true</code>.</p>
     */
    private boolean failIfNoResults;

    /**
     * <p>Flag that when set, <strong>marks the build as failed if any tests
     * fail</strong>.</p>
     *
     * <p>Defaults to <code>false</code>.</p>
     */
    private boolean failedTestsFailBuild;

    /**
     * <p>Configuration value specifying how to handle NUnit warnings.  
     * Since there is no direct mapping from NUNit's 'Warning' result to a JUnit result, 
     * we need a way to handle the behavior</p>
     * <ul>
     *  <li> skipped - (Default) NUnit warnings are interpreted as "skipped."  
     *  This is the default behavior in plugin version 0.24 
     *  </li>
     *  <li> unstable - NUnuit warnings are counted as failures; however, 
     *  any count (even exceeding healthScaleFactor) will mark the build as UNSTABLE,
     *  and will not cause the build to fail even if failedTestsFailBuild is set to true
     *  </li>
     *  <li> softfail - NUnuit warnings are counted as failures. 
     *  A warning count below the healthScaleFactor will mark the build as UNSTABLE, 
     *  but a A warning above healthScaleFactor will mark the buld FAILURE.
     *  Warnings will not cause the build to fail if failedTestsFailBuild is set to true
     *  </li>
     *  <li> normalfail - NUnit warnings are counted as failures, and will be subject to 
     *  the configuration of failedTestsFailBuild and healthScaleFactor to determine whether 
     *  the build is UNSTABLE or FAILURE
     *  </li>
     * </ul>
     * <p>Defaults to <code>skip</code>.</p>
     */
    private String warningNUnitTestResultHanlingBehavior;

    @Deprecated
    public NUnitPublisher(String testResultsPattern, boolean debug, boolean keepJUnitReports, boolean skipJUnitArchiver) {
    	this(testResultsPattern, debug, keepJUnitReports, skipJUnitArchiver, Boolean.TRUE, Boolean.FALSE, "skipped");
    }
    
    @Deprecated
    public NUnitPublisher(String testResultsPattern, boolean debug, boolean keepJUnitReports, boolean skipJUnitArchiver,
                          Boolean failIfNoResults, Boolean failedTestsFailBuild, String warningNUnitTestResultHanlingBehavior) {
        this.testResultsPattern = testResultsPattern;
        this.debug = debug;
        if (this.debug) {
            this.keepJUnitReports = keepJUnitReports;
            this.skipJUnitArchiver = skipJUnitArchiver;
        }
        this.failIfNoResults = BooleanUtils.toBooleanDefaultIfNull(failIfNoResults, Boolean.TRUE);
        this.failedTestsFailBuild = BooleanUtils.toBooleanDefaultIfNull(failedTestsFailBuild, Boolean.FALSE);
        this.warningNUnitTestResultHanlingBehavior = warningNUnitTestResultHanlingBehavior;
    }

    @DataBoundConstructor
    public NUnitPublisher(String testResultsPattern) {
        this.testResultsPattern = testResultsPattern;
        this.failIfNoResults = true;
        this.warningNUnitTestResultHanlingBehavior = "skipped";
    }
    
    public Object readResolve() {
    	return new NUnitPublisher(
			testResultsPattern, 
			debug, 
			keepJUnitReports, 
			skipJUnitArchiver, 
			BooleanUtils.toBooleanDefaultIfNull(failIfNoResults, Boolean.TRUE),
            BooleanUtils.toBooleanDefaultIfNull(failedTestsFailBuild, Boolean.FALSE),
            "skipped"
            );
            
    }

    public String getTestResultsPattern() {
        return testResultsPattern;
    }
    @DataBoundSetter
    public void setTestResultsPattern(String testResultsPattern){
        this.testResultsPattern = testResultsPattern;
    }

    public boolean getDebug() {
        return debug;
    }
    @DataBoundSetter
    public void setDebug(boolean debug){
        this.debug = debug;
    }

    public boolean getKeepJUnitReports() {
        return keepJUnitReports;
    }
    @DataBoundSetter
    public void setKeepJUnitReports(boolean keepJUnitReports){
        if(debug) {
            this.keepJUnitReports = keepJUnitReports;
        }
    }

    public double getHealthScaleFactor() {
        return healthScaleFactor == null ? 1.0 : healthScaleFactor;
    }
    @DataBoundSetter
    public void setHealthScaleFactor(double healthScaleFactor) {
        this.healthScaleFactor = Math.max(0.0, healthScaleFactor);
    }

    public boolean getSkipJUnitArchiver() {
        return skipJUnitArchiver;
    }
    @DataBoundSetter
    public void setSkipJUnitArchiver(boolean skipJUnitArchiver){
        if(debug) {
            this.skipJUnitArchiver = skipJUnitArchiver;
        }
    }
    
    public boolean getFailIfNoResults() {
		return failIfNoResults;
	}
	@DataBoundSetter
    public void setFailIfNoResults(boolean failIfNoResults) {
        this.failIfNoResults = failIfNoResults;
    }

    public boolean getFailedTestsFailBuild() {
        return failedTestsFailBuild;
    }
    @DataBoundSetter
    public void setFailedTestsFailBuild(boolean failedTestsFailBuild) {
        this.failedTestsFailBuild = failedTestsFailBuild;
    }

    @DataBoundSetter
    public void setWarningNUnitTestResultHanlingBehavior(String warningNUnitTestResultHanlingBehavior) {
        this.warningNUnitTestResultHanlingBehavior = warningNUnitTestResultHanlingBehavior;
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
        return BuildStepMonitor.NONE;
    }

    private int getCountTestsConvertedFromWarnings(TestResultAction action) {
        List<CaseResult> failedTests = action.getResult().getFailedTests();
        int testsConvertedFromWarnings = 0;
        for(CaseResult test : failedTests) {
            String convertedFromWarningToken = "This test case was reported as a \"Warning\" in NUnit, but converted to \"Fail\" by Jenkins NUnuit Plugin";
            if (test.getErrorDetails().contains(convertedFromWarningToken))
            {
                testsConvertedFromWarnings++;
            }
        }
        return testsConvertedFromWarnings; 
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
    private boolean recordTestResult(String junitFilePattern, Run<?, ?> build, TaskListener listener, FilePath filePath)
            throws InterruptedException, IOException {
        TestResultAction existingAction = build.getAction(TestResultAction.class);
        TestResultAction action;

        final long buildTime = build.getTimestamp().getTimeInMillis();

        TestResult existingTestResults = null;
        if (existingAction != null) {
            existingTestResults = existingAction.getResult();
        }
        TestResult result = getTestResult(junitFilePattern, build, existingTestResults, buildTime, filePath);

        if (existingAction == null) {
            action = new TestResultAction(build, result, listener);
        } else {
            action = existingAction;
            action.setResult(result, listener);
        }

        action.setHealthScaleFactor(getHealthScaleFactor());

        if (this.failIfNoResults && result.getPassCount() == 0 && result.getFailCount() == 0 && result.getSkipCount() == 0) {
            listener.getLogger().println("None of the test reports contained any result");
            build.setResult(Result.FAILURE);
            return true;
        }

        if (existingAction == null) {
            build.addAction(action);
        }


        if (action.getResult().getFailCount() > 0) {

            int testsConvertedFromWarnings = getCountTestsConvertedFromWarnings(action);
            listener.getLogger().println("Fail count was " + Integer.toString(action.getResult().getFailCount()) + ".  Converted from warnings: " + Integer.toString(testsConvertedFromWarnings));
            
            if(action.getResult().getFailCount() - testsConvertedFromWarnings == 0 ) {
                // all failures were from warnings:
                if (warningNUnitTestResultHanlingBehavior.equals("normalfail") && failedTestsFailBuild ){
                    build.setResult(Result.FAILURE);
                }
                else if (warningNUnitTestResultHanlingBehavior.equals("softfail")) {
                     build.setResult(Result.UNSTABLE);
                }
            } 
            else {
                // at least one failure resulted from a non-warning case.
                if(failedTestsFailBuild ) {
                    build.setResult(Result.FAILURE);
                }
                else {
                    build.setResult(Result.UNSTABLE);
                }
            }
            
         
        }

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
    private TestResult getTestResult(final String junitFilePattern, Run<?, ?> build,
                                     final TestResult existingTestResults, final long buildTime,
                                     final FilePath filePath) throws IOException, InterruptedException {
        TestResult result = filePath.act(new MasterToSlaveCallable<TestResult, IOException>() {
			private static final long serialVersionUID = -8917897415838795523L;

			public TestResult call() throws IOException {
                FileSet fs = Util.createFileSet(new File(filePath.getRemote()),junitFilePattern);
                DirectoryScanner ds = fs.getDirectoryScanner();

                String[] files = ds.getIncludedFiles();
                if(files.length==0) {
                	if (failIfNoResults) {
	                    // no test result. Most likely a configuration error or fatal problem
	                    throw new AbortException("No test report files were found or the NUnit input XML file contained no tests.");
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

			/**
			 * {@inheritDoc}
			 */
            public void checkRoles(RoleChecker roleChecker) throws SecurityException {
                // It is all right to run nunit-plugin on master or slave.
                roleChecker.check(this, Roles.MASTER);
            }
        });
        return result;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath ws, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        if (debug) {
            listener.getLogger().println("NUnit publisher running in debug mode.");
        }
        boolean result;
        try {
            EnvVars env = run.getEnvironment(listener);
            String resolvedTestResultsPattern = env.expand(testResultsPattern);

            listener.getLogger().println("Recording NUnit tests results");
            NUnitArchiver transformer = new NUnitArchiver(ws.getRemote(), listener, resolvedTestResultsPattern, new NUnitReportTransformer(), failIfNoResults, warningNUnitTestResultHanlingBehavior);
            result = ws.act(transformer);

            if (result) {
                if (skipJUnitArchiver) {
                    listener.getLogger().println("Skipping feeding JUnit reports to JUnitArchiver");
                } else {
                    // Run the JUnit test archiver
                    recordTestResult(NUnitArchiver.JUNIT_REPORTS_PATH + "/TEST-*.xml", run, listener, ws);
                }

                if (keepJUnitReports) {
                    listener.getLogger().println("Skipping deletion of temporary JUnit reports.");
                } else {
                    ws.child(NUnitArchiver.JUNIT_REPORTS_PATH).deleteRecursive();
                }
            } else {
                if (this.getFailIfNoResults()) {
                    // this should only happen if failIfNoResults is true and there are no result files, see NUnitArchiver.
                    run.setResult(Result.FAILURE);
                }
            }
        } catch(AbortException e) {
            // this is used internally to signal issues, so we just rethrow instead of letting the IOException
            // catch it below.
            throw e;
        } catch (IOException e) {
            listener.getLogger().println("Error in NUnit processing: " + e.getMessage());
            throw new AbortException("Could not read the XSL XML file. Please report this issue to the plugin author");
        }
    }

    @Extension @Symbol("nunit")
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
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
