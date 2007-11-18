package hudson.plugins.nunit;

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
    boolean archive() throws java.lang.InterruptedException,
            java.io.IOException;
}
