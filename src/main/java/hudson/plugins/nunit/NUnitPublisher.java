package hudson.plugins.nunit;

import hudson.Launcher;
import hudson.maven.agent.AbortException;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.test.TestResultProjectAction;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.kohsuke.stapler.StaplerRequest;

/**
 * Class that records NUnit test reports into Hudson.
 * 
 * @author Erik Ramfelt
 */
public class NUnitPublisher extends hudson.tasks.Publisher implements TestReportArchiver {

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

	public boolean perform(final Build<?, ?> build, final Launcher launcher,
			final BuildListener listener) throws InterruptedException, IOException {
		if (debug) {
			listener.getLogger().println("NUnit publisher running in debug mode.");
		}
		Boolean result = Boolean.FALSE;
		try {
			NUnitArchiver transformer = new NUnitArchiver(build, launcher, listener, testResultsPattern, 
					this, new NUnitReportTransformer(), keepJUnitReports, skipJUnitArchiver);
			result = build.getProject().getWorkspace().act(transformer);
		} catch (TransformerException te) {
			throw new AbortException("Could not read the XSL XML file. Please report this issue to the plugin author", te);
		} catch (ParserConfigurationException pce) {
			throw new AbortException("Could not initalize the XML parser. Please report this issue to the plugin author", pce);
		}
		
		return result.booleanValue();
	}
	
	public boolean archive(Build build, Launcher launcher, BuildListener listener) 
		throws java.lang.InterruptedException, java.io.IOException {
		return new JUnitResultArchiver(NUnitArchiver.JUNIT_REPORTS_PATH + "/TEST-*.xml").perform(build, launcher, listener);
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
