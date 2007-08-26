package hudson.plugins.nunit;

import hudson.Launcher;
import hudson.maven.agent.AbortException;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import hudson.tasks.test.TestResultProjectAction;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.TransformerConfigurationException;

import org.kohsuke.stapler.StaplerRequest;

/**
 * Class that records NUnit test reports into Hudson.
 * 
 * @author Erik Ramfelt
 */
public class NUnitPublisher extends hudson.tasks.Publisher {

	private static transient final String PLUGIN_NUNIT = "/plugin/nunit/";
	private static transient final String NUNIT_TO_JUNIT_XSL = "nunit-to-junit.xsl";

	public static final Descriptor<Publisher> DESCRIPTOR = new DescriptorImpl();
	
	private String testResultsPattern;

	public NUnitPublisher(String testResultsPattern) {
		this.testResultsPattern = testResultsPattern;
	}
	
	public String getTestResultsPattern() {
		return testResultsPattern;
	}

	@Override
	public Action getProjectAction(hudson.model.Project project) {
		return new TestResultProjectAction(project);
	}

	public boolean perform(final Build<?, ?> build, final Launcher launcher,
			final BuildListener listener) throws InterruptedException, IOException {
		Boolean result = Boolean.FALSE;
		
		try {
			InputStream xslFile = this.getClass().getResourceAsStream(NUNIT_TO_JUNIT_XSL);
			NUnitArchiver transformer = new NUnitArchiver(xslFile, build, launcher, listener, testResultsPattern);
			result = build.getProject().getWorkspace().act(transformer);
			
		} catch (TransformerConfigurationException tce) {
			throw new AbortException("There was a problem with the XSL transform file, please notify the plugin responsible", tce);
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
			return new NUnitPublisher(req.getParameter("nunit_reports.pattern"));
		}
	}
}
