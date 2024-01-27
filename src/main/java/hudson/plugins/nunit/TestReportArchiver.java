package hudson.plugins.nunit;

/**
 * Interface for mocking out the JUnitArchiver from tests.
 */
public interface TestReportArchiver {
    /**
     * Performs the archiving of test
     * @return true, if it was successful; false otherwise
     */
    boolean archive() throws java.lang.InterruptedException, java.io.IOException;
}
