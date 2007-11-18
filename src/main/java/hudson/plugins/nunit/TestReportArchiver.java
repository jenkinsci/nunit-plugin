package hudson.plugins.nunit;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;

/**
 * Interface for mocking out the JUnitArchiver from tests.
 */
public interface TestReportArchiver {
    /**
     * Performs the archiving of tests
     * 
     * @param build the current build
     * @param launcher the launcher
     * @param listener build listener
     * @return true, if it was successful; false otherwise
     */
    boolean archive(Build build, Launcher launcher, BuildListener listener) throws java.lang.InterruptedException,
            java.io.IOException;
}
