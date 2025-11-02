package hudson.plugins.nunit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.Publisher;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResultProjectAction;
import hudson.util.DescribableList;
import java.io.IOException;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NUnitPublisherTest {

    @Test
    @WithoutJenkins
    void testGetTestResultsPattern() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setDebug(true);
        assertEquals("**/*.xml", publisher.getTestResultsPattern(), "The test results pattern is incorrect");
    }

    @Test
    @WithoutJenkins
    void testGetDebug() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setDebug(true);
        assertTrue(publisher.getDebug(), "Debug is incorrect");
        publisher = new NUnitPublisher("**/*.xml");
        assertFalse(publisher.getDebug(), "Debug is incorrect");
    }

    @Test
    @WithoutJenkins
    void testDisabledDebug() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setSkipJUnitArchiver(true);
        assertFalse(publisher.getDebug(), "Debug is incorrect");
        assertFalse(publisher.getKeepJUnitReports(), "KeepJunitReports() is incorrect");
        assertFalse(publisher.getSkipJUnitArchiver(), "SkipJunitArchiver() is incorrect");
    }

    @Test
    @WithoutJenkins
    void testGetKeepJunitReports() {
        NUnitPublisher publisher = new NUnitPublisher("**/.xml");
        publisher.setDebug(true);
        publisher.setKeepJUnitReports(true);
        assertTrue(publisher.getKeepJUnitReports(), "KeepJunitReports() is incorrect");
        publisher = new NUnitPublisher("**/*.xml");
        publisher.setDebug(true);
        assertFalse(publisher.getKeepJUnitReports(), "KeepJunitReports() is incorrect");
    }

    @Test
    @WithoutJenkins
    void testGetSkipJunitArchiver() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setDebug(true);
        publisher.setSkipJUnitArchiver(true);
        assertTrue(publisher.getSkipJUnitArchiver(), "SkipJunitArchiver() is incorrect");
        publisher = new NUnitPublisher("**/*.xml");
        publisher.setDebug(true);
        assertFalse(publisher.getSkipJUnitArchiver(), "SkipJunitArchiver() is incorrect");
    }

    @Test
    @WithoutJenkins
    void testGetProjectActionProjectReusing() {
        Project project = mock(Project.class);
        when(project.getAction(TestResultProjectAction.class)).thenReturn(new TestResultProjectAction(project));

        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setSkipJUnitArchiver(true);

        @SuppressWarnings("rawtypes")
        Action projectAction = publisher.getProjectAction((AbstractProject) project);
        assertNotNull(projectAction, "The action was null");
    }

    @Test
    @WithoutJenkins
    void testGetProjectActionProject() {
        Project project = mock(Project.class);
        when(project.getAction(TestResultProjectAction.class)).thenReturn(null);

        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setSkipJUnitArchiver(true);
        @SuppressWarnings("rawtypes")
        Action projectAction = publisher.getProjectAction((AbstractProject) project);
        assertNotNull(projectAction, "The action was null");
        assertEquals(TestResultProjectAction.class, projectAction.getClass(), "The action type is incorrect");
    }

    @Test
    @WithoutJenkins
    void testGetFailBuildIfNoResults() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setSkipJUnitArchiver(true);
        assertTrue(publisher.getFailIfNoResults(), "Fail if no results is incorrect");
        publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setSkipJUnitArchiver(true);
        publisher.setFailIfNoResults(false);
        assertFalse(publisher.getFailIfNoResults(), "Fail if no results is incorrect");
    }

    @Test
    @Issue("JENKINS-42967")
    void testFailBuildIfNoResults(JenkinsRule j) throws Exception {
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

    @Test
    @Issue("JENKINS-34452")
    void testDoNotFailIfEmptyTests(JenkinsRule j) throws Exception {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setFailIfNoResults(false);
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("nunit.xml")
                        .copyFrom(this.getClass().getResourceAsStream("NUnit-issue34452.xml"));
                return true;
            }
        });

        prj.getPublishersList().add(publisher);
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, b);
    }

    @Test
    void testAgent(JenkinsRule j) throws Exception {
        Slave agent = j.createOnlineSlave();
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.setAssignedNode(agent);

        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("nunit.xml")
                        .copyFrom(this.getClass().getResourceAsStream("NUnit-issue44315-3.xml"));
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
    void testHealthScaleFactor(JenkinsRule j) throws Exception {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setHealthScaleFactor(5.0);
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("nunit.xml")
                        .copyFrom(this.getClass().getResourceAsStream("NUnit-healthReport.xml"));
                return true;
            }
        });

        prj.getPublishersList().add(publisher);
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.UNSTABLE, b);
        TestResultAction a = b.getAction(TestResultAction.class);
        assertNotNull(a);
        assertEquals(5.0, a.getHealthScaleFactor(), 0.01);
        HealthReport r = a.getBuildHealth();
        assertNotNull(r);
        assertEquals(50, r.getScore());
    }

    @Test
    void testFailIfTestsFail(JenkinsRule j) throws Exception {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setFailIfNoResults(false);
        publisher.setFailedTestsFailBuild(true);
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("nunit.xml")
                        .copyFrom(this.getClass().getResourceAsStream("NUnit-failure.xml"));
                return true;
            }
        });

        prj.getPublishersList().add(publisher);
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, b);
    }

    @Test
    void testUnstableIfTestsFail(JenkinsRule j) throws Exception {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml");
        publisher.setKeepJUnitReports(true);
        publisher.setFailIfNoResults(false);
        FreeStyleProject prj = j.createFreeStyleProject("foo");
        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("nunit.xml")
                        .copyFrom(this.getClass().getResourceAsStream("NUnit-failure.xml"));
                return true;
            }
        });

        prj.getPublishersList().add(publisher);
        FreeStyleBuild b = prj.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.UNSTABLE, b);
    }

    @Test
    void testDoNotOverwriteResultsIfThereAreNoFilesDuringNextPublishments(JenkinsRule j) throws Exception {
        NUnitPublisher publisherWithCorrectPattern = new NUnitPublisher("nunit.xml");
        NUnitPublisher publisherWithIncorrectPattern = new NUnitPublisher("nunit2.xml");
        publisherWithIncorrectPattern.setFailIfNoResults(false);
        FreeStyleProject freeStyleProject = j.createFreeStyleProject("foo");
        freeStyleProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("nunit.xml").copyFrom(this.getClass().getResourceAsStream("NUnit.xml"));
                return true;
            }
        });
        DescribableList<Publisher, Descriptor<Publisher>> publishersList = freeStyleProject.getPublishersList();
        publishersList.add(publisherWithCorrectPattern);
        publishersList.add(publisherWithIncorrectPattern);
        FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        TestResultAction existingAction = build.getAction(TestResultAction.class);
        assertEquals(4, existingAction.getTotalCount());
    }

    @Test
    void parallelPublishing(JenkinsRule j) throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "parallelInStage");
        FilePath ws = j.jenkins.getWorkspaceFor(job);

        FilePath testFile = ws.child("first-result.xml");
        testFile.copyFrom(this.getClass().getResourceAsStream("NUnit-correct.xml"));
        FilePath secondTestFile = ws.child("second-result.xml");
        secondTestFile.copyFrom(this.getClass().getResourceAsStream("NUnit-correct2.xml"));
        FilePath thirdTestFile = ws.child("third-result.xml");
        thirdTestFile.copyFrom(this.getClass().getResourceAsStream("NUnit-correct3.xml"));

        job.setDefinition(new CpsFlowDefinition("""
                        node {
                            parallel(a: { step([$class: 'NUnitPublisher', testResultsPattern: 'first-result.xml', debug: false, keepJUnitReports: true, skipJUnitArchiver:false]) },
                                     b: { step([$class: 'NUnitPublisher', testResultsPattern: 'second-result.xml', debug: false, keepJUnitReports: true, skipJUnitArchiver:false]) },
                                     c: { step([$class: 'NUnitPublisher', testResultsPattern: 'third-result.xml', debug: false, keepJUnitReports: true, skipJUnitArchiver:false]) })
                        }
                        """, true));
        WorkflowRun r = j.waitForCompletion(job.scheduleBuild2(0).waitForStart());
        TestResultAction action = r.getAction(TestResultAction.class);
        assertNotNull(action);
        assertEquals(28 + 218 + 22, action.getTotalCount());
    }
}
