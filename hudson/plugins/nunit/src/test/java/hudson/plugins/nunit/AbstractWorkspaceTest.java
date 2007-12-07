package hudson.plugins.nunit;

import hudson.FilePath;

import java.io.File;

public abstract class AbstractWorkspaceTest {

    protected static final File PARENT_FILE = new File(System.getProperty("java.io.tmpdir"), "nunit-files");
    protected FilePath workspace;

    public void createWorkspace() throws Exception {
        workspace = new FilePath(PARENT_FILE);
        if (workspace.exists()) {
            workspace.deleteRecursive();
        }
        workspace.mkdirs();
    }

    public void deleteWorkspace() throws Exception {
        workspace.deleteRecursive();
    }
}
