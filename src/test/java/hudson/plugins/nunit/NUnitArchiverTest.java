package hudson.plugins.nunit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.remoting.VirtualChannel;
import hudson.util.StreamTaskListener;
import java.io.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

public class NUnitArchiverTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private StreamTaskListener buildListener;
    private TestReportTransformer transformer;
    private NUnitArchiver nunitArchiver;
    private VirtualChannel virtualChannel;

    @Before
    public void setUp() throws Exception {
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
    public void testTransformOfTwoReports() throws Exception {
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
        assertTrue("Error during archiver call", nunitArchiver.call());
        assertEquals("Should have processed two files", 2, nunitArchiver.getFileCount());
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
    public void testNoNUnitReports() throws Exception {
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        FreeStyleBuild b = prj.scheduleBuild2(0).get();

        nunitArchiver = new NUnitArchiver(
                b.getWorkspace().getRemote(),
                "tempJunitReports",
                StreamTaskListener.fromStdout(),
                "*.xml",
                transformer,
                true);
        assertFalse("The archiver did not return false when it could not find any files", nunitArchiver.call());
    }
}
