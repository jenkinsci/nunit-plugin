package hudson.plugins.nunit;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Publisher;
import hudson.tasks.test.AbstractTestResultAction;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Objects;

public class NUnitIntegrationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @Issue("JENKINS-5673")
    public void testIssue5673() throws Exception {
        FreeStyleBuild build = getBuildForIntegrationTest("5673");
        j.assertBuildStatus(Result.UNSTABLE, build);
        AbstractTestResultAction action = build.getAction(AbstractTestResultAction.class);
        Assert.assertEquals("The number of tests is not correct", 1355, action.getTotalCount());
    }

    @Test
    @Issue("JENKINS-9246")
    public void testIssue9246() throws Exception {
        FreeStyleBuild build = getBuildForIntegrationTest("9246");
        j.assertBuildStatus(Result.UNSTABLE, build);
        AbstractTestResultAction action = build.getAction(AbstractTestResultAction.class);
        Assert.assertEquals("The number of tests is not correct", 5454, action.getTotalCount());
    }

    @Test
    @Issue("JENKINS-7072")
    public void testIssue7072() throws Exception {
        FreeStyleBuild build = getBuildForIntegrationTest("7072");
        j.assertBuildStatus(Result.UNSTABLE, build);
        AbstractTestResultAction action = build.getAction(AbstractTestResultAction.class);
        Assert.assertEquals("The number of tests is not correct", 3003, action.getTotalCount());
        Assert.assertEquals("The number of failed tests is not correct", 96, action.getFailCount());
        Assert.assertEquals("The number of skipped tests is not correct", 34, action.getSkipCount());
    }

    private FreeStyleBuild getBuildForIntegrationTest(String issueNumber) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject(issueNumber);
        FilePath workspace = j.jenkins.getWorkspaceFor(project);
        Publisher publisher = new NUnitPublisher("**/*.xml");
        project.getPublishersList().add(publisher);
        String testResult = String.format("NUnit-issue%s.xml", issueNumber);
        Objects.requireNonNull(workspace)
                .child(testResult)
                .copyFrom(getClass().getResourceAsStream(testResult));
        return project.scheduleBuild2(0).get();
    }
}
