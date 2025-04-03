package hudson.plugins.nunit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.remoting.VirtualChannel;
import hudson.util.StreamTaskListener;
import java.io.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NUnitArchiverTest {

    private JenkinsRule j;

    private StreamTaskListener buildListener;
    private TestReportTransformer transformer;
    private NUnitArchiver nunitArchiver;
    private VirtualChannel virtualChannel;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
        buildListener = StreamTaskListener.fromStdout();
        transformer = mock(TestReportTransformer.class);
        virtualChannel = mock(VirtualChannel.class);
    }

    /*@Test
    public void testRemovalOfJunitFiles() throws Exception {
        nunitArchiver = new NUnitArchiver(buildListener, "*.xml", archiver, transformer, false, false);
        workspace.createTextTempFile("nunit-report", ".xml", "content");
        workspace.child(NUnitArchiver.JUNIT_REPORTS_PATH).mkdirs();
        workspace.child(NUnitArchiver.JUNIT_REPORTS_PATH).createTextTempFile("TEST-", ".xml", "<tests>");

        context.checking(new Expectations() {
            {
                one(transformer).transform(with(any(InputStream.class)), with(any(File.class)));
                one(archiver).archive();
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(buildListener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));
            }
        });

        nunitArchiver.invoke(PARENT_FILE, virtualChannel);

        assertFalse("The temp folder still exists", workspace.child(NUnitArchiver.JUNIT_REPORTS_PATH).exists());
        context.assertIsSatisfied();
    }*/

    @Test
    void testTransformOfTwoReports() throws Exception {
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().createTextTempFile("nunit-report", ".xml", "content");
                build.getWorkspace().createTextTempFile("nunit-report", ".xml", "content");
                return true;
            }
        });
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        nunitArchiver = new NUnitArchiver(
                b.getWorkspace().getRemote(), "tempJunitReports", buildListener, "*.xml", transformer, true);
        assertTrue(nunitArchiver.call(), "Error during archiver call");
        assertEquals(2, nunitArchiver.getFileCount(), "Should have processed two files");
    }

    /*
    @Test
    public void testKeepJUnitReportFiles() throws Exception {
        nunitArchiver = new NUnitArchiver(buildListener, "*.xml", archiver, transformer, true, false);
        workspace.createTextTempFile("nunit-report", ".xml", "content");

        context.checking(new Expectations() {
            {
                one(transformer).transform(with(any(InputStream.class)), with(any(File.class)));
                one(archiver).archive();
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(buildListener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));
            }
        });

        nunitArchiver.invoke(PARENT_FILE, virtualChannel);

        assertTrue("The temp folder still exists", workspace.child(NUnitArchiver.JUNIT_REPORTS_PATH).exists());
        context.assertIsSatisfied();
    }

    @Test
    public void testSkipJUnitArchiver() throws Exception {
        nunitArchiver = new NUnitArchiver(buildListener, "*.xml", archiver, transformer, true, true);
        workspace.createTextTempFile("nunit-report", ".xml", "content");

        context.checking(new Expectations() {
            {
                one(transformer).transform(with(any(InputStream.class)), with(any(File.class)));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(buildListener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));
            }
        });

        nunitArchiver.invoke(PARENT_FILE, virtualChannel);
        context.assertIsSatisfied();
    }*/

    @Test
    void testNoNUnitReports() throws Exception {
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        FreeStyleBuild b = prj.scheduleBuild2(0).get();

        nunitArchiver = new NUnitArchiver(
                b.getWorkspace().getRemote(),
                "tempJunitReports",
                StreamTaskListener.fromStdout(),
                "*.xml",
                transformer,
                true);
        assertFalse(nunitArchiver.call(), "The archiver did not return false when it could not find any files");
    }
}
