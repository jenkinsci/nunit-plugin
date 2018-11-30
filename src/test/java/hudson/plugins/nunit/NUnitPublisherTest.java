package hudson.plugins.nunit;

import hudson.Launcher;
import hudson.model.*;
import hudson.slaves.DumbSlave;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResultProjectAction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.WithoutJenkins;

import java.io.IOException;

public class NUnitPublisherTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test @WithoutJenkins
    public void testGetTestResultsPattern() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setDebug(true);
        assertEquals("The test results pattern is incorrect", publisher.getTestResultsPattern(), "**/*.xml");
    }

    @Test @WithoutJenkins
    public void testGetDebug() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setDebug(true);
        assertTrue("Debug is incorrect", publisher.getDebug());
        publisher = new NUnitPublisher("**/*.xml");
        assertFalse("Debug is incorrect", publisher.getDebug());
    }

    @Test @WithoutJenkins
    public void testDisabledDebug() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setSkipJUnitArchiver(true);
        assertFalse("Debug is incorrect", publisher.getDebug());
        assertFalse("KeepJunitReports() is incorrect", publisher.getKeepJUnitReports());
        assertFalse("SkipJunitArchiver() is incorrect", publisher.getSkipJUnitArchiver());
    }

    @Test @WithoutJenkins
    public void testGetKeepJunitReports() {
        NUnitPublisher publisher = new NUnitPublisher("**/.xml");
        publisher.setDebug(true);
        publisher.setKeepJUnitReports(true);
        assertTrue("KeepJunitReports() is incorrect", publisher.getKeepJUnitReports());
        publisher = new NUnitPublisher("**/*.xml");
        publisher.setDebug(true);
        assertFalse("KeepJunitReports() is incorrect", publisher.getKeepJUnitReports());
    }

    @Test @WithoutJenkins
    public void testGetSkipJunitArchiver() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setDebug(true);
        publisher.setSkipJUnitArchiver(true);
        assertTrue("SkipJunitArchiver() is incorrect", publisher.getSkipJUnitArchiver());
        publisher = new NUnitPublisher("**/*.xml");
        publisher.setDebug(true);
        assertFalse("SkipJunitArchiver() is incorrect", publisher.getSkipJUnitArchiver());
    }

    @Test @WithoutJenkins
    public void testGetProjectActionProjectReusing() {
        Project project = mock(Project.class);
        when(project.getAction(TestResultProjectAction.class)).thenReturn(new TestResultProjectAction(project));

        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setSkipJUnitArchiver(true);

        @SuppressWarnings("rawtypes")
		Action projectAction = publisher.getProjectAction((AbstractProject)project);
        assertNotNull("The action was null", projectAction);
    }

    @Test @WithoutJenkins
    public void testGetProjectActionProject() {
        Project project = mock(Project.class);
        when(project.getAction(TestResultProjectAction.class)).thenReturn(null);

        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setSkipJUnitArchiver(true);
        @SuppressWarnings("rawtypes")
		Action projectAction = publisher.getProjectAction((AbstractProject)project);
        assertNotNull("The action was null", projectAction);
        assertEquals("The action type is incorrect", TestResultProjectAction.class, projectAction.getClass());
    }

    @Test @WithoutJenkins
    public void testGetFailBuildIfNoResults() {
    	NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
    	publisher.setKeepJUnitReports(true);
    	publisher.setSkipJUnitArchiver(true);
        assertTrue("Fail if no results is incorrect", publisher.getFailIfNoResults());
        publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setSkipJUnitArchiver(true);
        publisher.setFailIfNoResults(false);
        assertFalse("Fail if no results is incorrect", publisher.getFailIfNoResults());
    }

    @Test @Issue("JENKINS-42967")
    public void testFailBuildIfNoResults() throws Exception {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setSkipJUnitArchiver(true);

        FreeStyleProject prj = j.createFreeStyleProject("foo");
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, b);
        prj.getPublishersList().add(publisher);
        b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, b);
    }

    @Test @Issue("JENKINS-34452")
    public void testDoNotFailIfEmptyTests() throws Exception {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setFailIfNoResults(false);
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("nunit.xml").copyFrom(this.getClass().getResourceAsStream("NUnit-issue34452.xml"));
                return true;
            }
        });

        prj.getPublishersList().add(publisher);
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, b);
    }

    @Test
    public void testAgent() throws Exception {
        Slave agent = j.createOnlineSlave();
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.setAssignedNode(agent);

        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("nunit.xml").copyFrom(this.getClass().getResourceAsStream("NUnit-issue44315-3.xml"));
                return true;
            }
        });

        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);

        prj.getPublishersList().add(publisher);
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, b);
    }

    @Test
    public void testHealthScaleFactor() throws Exception {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setHealthScaleFactor(5.0);
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("nunit.xml").copyFrom(this.getClass().getResourceAsStream("NUnit-healthReport.xml"));
                return true;
            }
        });

        prj.getPublishersList().add(publisher);
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.UNSTABLE, b);
        TestResultAction a = b.getAction(TestResultAction.class);
        assertNotNull(a);
        assertEquals( 5.0, a.getHealthScaleFactor(), 0.01);
        HealthReport r = a.getBuildHealth();
        assertNotNull(r);
        assertEquals(50, r.getScore());
    }

    @Test
    public void testFailIfTestsFail() throws Exception {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setFailIfNoResults(false);
        publisher.setFailedTestsFailBuild(true);
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("nunit.xml").copyFrom(this.getClass().getResourceAsStream("NUnit-failure.xml"));
                return true;
            }
        });

        prj.getPublishersList().add(publisher);
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, b);
    }

    @Test
    public void testUnstableIfTestsFail() throws Exception {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setFailIfNoResults(false);
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("nunit.xml").copyFrom(this.getClass().getResourceAsStream("NUnit-failure.xml"));
                return true;
            }
        });

        prj.getPublishersList().add(publisher);
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.UNSTABLE, b);
    }
}
