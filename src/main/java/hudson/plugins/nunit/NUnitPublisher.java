package hudson.plugins.nunit;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
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
import java.util.UUID;
import javax.annotation.Nonnull;
import jenkins.security.MasterToSlaveCallable;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.BooleanUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Class that records NUnit test reports into Jenkins.
 *
 * @author Erik Ramfelt
 */
public class NUnitPublisher extends Recorder implements Serializable, SimpleBuildStep {

    private static final long serialVersionUID = 1L;

    private static final transient String PLUGIN_NUNIT = "/plugin/nunit/";

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

    @Deprecated
    public NUnitPublisher(
            String testResultsPattern, boolean debug, boolean keepJUnitReports, boolean skipJUnitArchiver) {
        this(testResultsPattern, debug, keepJUnitReports, skipJUnitArchiver, Boolean.TRUE, Boolean.FALSE);
    }

    @Deprecated
    public NUnitPublisher(
            String testResultsPattern,
            boolean debug,
            boolean keepJUnitReports,
            boolean skipJUnitArchiver,
            Boolean failIfNoResults,
            Boolean failedTestsFailBuild) {
        this.testResultsPattern = testResultsPattern;
        this.debug = debug;
        if (this.debug) {
            this.keepJUnitReports = keepJUnitReports;
            this.skipJUnitArchiver = skipJUnitArchiver;
        }
        this.failIfNoResults = BooleanUtils.toBooleanDefaultIfNull(failIfNoResults, Boolean.TRUE);
        this.failedTestsFailBuild = BooleanUtils.toBooleanDefaultIfNull(failedTestsFailBuild, Boolean.FALSE);
    }

    @DataBoundConstructor
    public NUnitPublisher(String testResultsPattern) {
        this.testResultsPattern = testResultsPattern;
        this.failIfNoResults = true;
    }

    public Object readResolve() {
        return new NUnitPublisher(
                testResultsPattern,
                debug,
                keepJUnitReports,
                skipJUnitArchiver,
                BooleanUtils.toBooleanDefaultIfNull(failIfNoResults, Boolean.TRUE),
                BooleanUtils.toBooleanDefaultIfNull(failedTestsFailBuild, Boolean.FALSE));
    }

    public String getTestResultsPattern() {
        return testResultsPattern;
    }

    @DataBoundSetter
    public void setTestResultsPattern(String testResultsPattern) {
        this.testResultsPattern = testResultsPattern;
    }

    public boolean getDebug() {
        return debug;
    }

    @DataBoundSetter
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean getKeepJUnitReports() {
        return keepJUnitReports;
    }

    @DataBoundSetter
    public void setKeepJUnitReports(boolean keepJUnitReports) {
        if (debug) {
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
    public void setSkipJUnitArchiver(boolean skipJUnitArchiver) {
        if (debug) {
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

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
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

    /**
     * Record the test results into the current build.
     * @param junitFilePattern JUnit file pattern
     * @param build The current build
     * @param listener Task listner
     * @return True or false
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    private boolean recordTestResult(String junitFilePattern, Run<?, ?> build, TaskListener listener, FilePath filePath)
            throws InterruptedException, IOException {
        synchronized (build) {
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

            if (this.failIfNoResults
                    && result.getPassCount() == 0
                    && result.getFailCount() == 0
                    && result.getSkipCount() == 0) {
                listener.getLogger().println("None of the test reports contained any result");
                build.setResult(Result.FAILURE);
                return true;
            }

            if (existingAction == null) {
                build.addAction(action);
            }

            if (action.getResult().getFailCount() > 0) {
                if (failedTestsFailBuild) {
                    build.setResult(Result.FAILURE);
                } else {
                    build.setResult(Result.UNSTABLE);
                }
            }

            return true;
        }
    }

    /**
     * Collect the test results from the files
     * @param junitFilePattern JUnit file pattern
     * @param build The current build
     * @param existingTestResults existing test results to add results to
     * @param buildTime
     * @return a test result
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     */
    private TestResult getTestResult(
            final String junitFilePattern,
            Run<?, ?> build,
            final TestResult existingTestResults,
            final long buildTime,
            final FilePath filePath)
            throws IOException, InterruptedException {
        TestResult result = filePath.act(new MasterToSlaveCallable<TestResult, IOException>() {
            private static final long serialVersionUID = -8917897415838795523L;

            public TestResult call() throws IOException {
                FileSet fs = Util.createFileSet(new File(filePath.getRemote()), junitFilePattern);
                DirectoryScanner ds = fs.getDirectoryScanner();

                String[] files = ds.getIncludedFiles();
                if (files.length == 0) {
                    if (failIfNoResults) {
                        // no test result. Most likely a configuration error or fatal problem
                        throw new AbortException(
                                "No test report files were found or the NUnit input XML file contained no tests.");
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
    public void perform(
            @Nonnull Run<?, ?> run, @Nonnull FilePath ws, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {
        if (debug) {
            listener.getLogger().println("NUnit publisher running in debug mode.");
        }
        boolean result;
        try {
            EnvVars env = run.getEnvironment(listener);
            String resolvedTestResultsPattern = env.expand(testResultsPattern);

            listener.getLogger().println("Recording NUnit tests results");
            String junitTempReportsDirectoryName =
                    "tempJunitReports" + UUID.randomUUID().toString();
            NUnitArchiver transformer = new NUnitArchiver(
                    ws.getRemote(),
                    junitTempReportsDirectoryName,
                    listener,
                    resolvedTestResultsPattern,
                    new NUnitReportTransformer(),
                    failIfNoResults);
            result = ws.act(transformer);

            if (result) {
                if (skipJUnitArchiver) {
                    listener.getLogger().println("Skipping feeding JUnit reports to JUnitArchiver");
                } else {
                    // Run the JUnit test archiver
                    recordTestResult(junitTempReportsDirectoryName + "/TEST-*.xml", run, listener, ws);
                }

                if (keepJUnitReports) {
                    listener.getLogger().println("Skipping deletion of temporary JUnit reports.");
                } else {
                    ws.child(junitTempReportsDirectoryName).deleteRecursive();
                }
            } else {
                if (this.getFailIfNoResults()) {
                    // this should only happen if failIfNoResults is true and there are no result files, see
                    // NUnitArchiver.
                    run.setResult(Result.FAILURE);
                }
            }
        } catch (AbortException e) {
            // this is used internally to signal issues, so we just rethrow instead of letting the IOException
            // catch it below.
            throw e;
        } catch (IOException e) {
            listener.getLogger().println("Error in NUnit processing: " + e.getMessage());
            throw new AbortException("Could not read the XSL XML file. Please report this issue to the plugin author");
        }
    }

    @Extension
    @Symbol("nunit")
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
