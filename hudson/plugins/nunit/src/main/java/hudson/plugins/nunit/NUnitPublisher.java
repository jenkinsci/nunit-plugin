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

import javax.xml.parsers.ParserConfigurationException;
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
			NUnitArchiver transformer = new NUnitArchiver(build, launcher, listener, testResultsPattern);
			result = build.getProject().getWorkspace().act(transformer);
		} catch (TransformerException te) {
			throw new AbortException("Could not read the XSL XML file. Please report this issue to the plugin author", te);
		} catch (ParserConfigurationException pce) {
			throw new AbortException("Could not initalize the XML parser. Please report this issue to the plugin author", pce);
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
