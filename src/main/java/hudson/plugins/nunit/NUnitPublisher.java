package hudson.plugins.nunit;

import hudson.Launcher;
import hudson.maven.agent.AbortException;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.test.TestResultProjectAction;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.kohsuke.stapler.StaplerRequest;

/**
 * Class that records NUnit test reports into Hudson.
 * 
 * @author Erik Ramfelt
 */
public class NUnitPublisher extends hudson.tasks.Publisher {

    private static transient final String PLUGIN_NUNIT = "/plugin/nunit/";

    public static final Descriptor<Publisher> DESCRIPTOR = new DescriptorImpl();

    private String testResultsPattern;
    private boolean debug = false;
    private boolean keepJUnitReports = false;
    private boolean skipJUnitArchiver = false;

    public NUnitPublisher(String testResultsPattern, boolean debug, boolean keepJUnitReports, boolean skipJUnitArchiver) {
        this.testResultsPattern = testResultsPattern;
        this.debug = debug;
        if (this.debug) {
            this.keepJUnitReports = keepJUnitReports;
            this.skipJUnitArchiver = skipJUnitArchiver;
        }
    }

    public String getTestResultsPattern() {
        return testResultsPattern;
    }

    public boolean getDebug() {
        return debug;
    }

    public boolean getKeepJunitReports() {
        return keepJUnitReports;
    }

    public boolean getSkipJunitArchiver() {
        return skipJUnitArchiver;
    }

    @Override
    public Action getProjectAction(hudson.model.Project project) {
        return new TestResultProjectAction(project);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        if (debug) {
            listener.getLogger().println("NUnit publisher running in debug mode.");
        }
        Boolean result = Boolean.TRUE;
        try {
            NUnitArchiver transformer = new NUnitArchiver(listener, testResultsPattern, new NUnitReportTransformer());
            result = build.getProject().getWorkspace().act(transformer);

            if (result.booleanValue()) {
                if (skipJUnitArchiver) {
                    listener.getLogger().println("Skipping feeding JUnit reports to JUnitArchiver");
                } else {
                    // Run the JUnit test archiver
                    result = Boolean.valueOf(new JUnitResultArchiver(NUnitArchiver.JUNIT_REPORTS_PATH + "/TEST-*.xml").
                            perform(build, launcher, listener));
                }
                
                if (keepJUnitReports) {
                    listener.getLogger().println("Skipping deletion of temporary JUnit reports.");
                } else {
                    build.getProject().getWorkspace().child(NUnitArchiver.JUNIT_REPORTS_PATH).deleteRecursive();
                }
            }
            
        } catch (TransformerException te) {
            throw new AbortException("Could not read the XSL XML file. Please report this issue to the plugin author",
                    te);
        }

        return result.booleanValue();
    }

    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public static class DescriptorImpl extends Descriptor<Publisher> {

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
        public Publisher newInstance(StaplerRequest req) throws FormException {
            return new NUnitPublisher(req.getParameter("nunit_reports.pattern"), 
                    (req.hasParameter("nunit_reports.debug") ? Boolean.parseBoolean(req.getParameter("nunit_reports.debug")) : false), 
                    (req.hasParameter("nunit_reports.keepjunitreports") ? Boolean.parseBoolean(req.getParameter("nunit_reports.keepjunitreports")) : false), 
                    (req.hasParameter("nunit_reports.skipjunitarchiver") ? Boolean.parseBoolean(req.getParameter("nunit_reports.skipjunitarchiver")) : false));
        }
    }
}
