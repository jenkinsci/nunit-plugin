package hudson.plugins.nunit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.test.AbstractTestResultAction;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

@WithJenkins
class NUnitIntegrationTest {

    @Issue("JENKINS-5673")
    @LocalData
    @Test
    void testIssue5673(JenkinsRule j) throws Exception {
        FreeStyleProject project = (FreeStyleProject) j.jenkins.getItem("5673");
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.UNSTABLE, build);
        AbstractTestResultAction action = build.getAction(AbstractTestResultAction.class);
        assertEquals(1355, action.getTotalCount(), "The number of tests is not correct");
    }

    @Issue("JENKINS-9246")
    @LocalData
    @Test
    void testIssue9246(JenkinsRule j) throws Exception {
        FreeStyleProject project = (FreeStyleProject) j.jenkins.getItem("9246");
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.UNSTABLE, build);
        AbstractTestResultAction action = build.getAction(AbstractTestResultAction.class);
        assertEquals(5454, action.getTotalCount(), "The number of tests is not correct");
    }

    @Issue("JENKINS-7072")
    @LocalData
    @Test
    void testIssue7072(JenkinsRule j) throws Exception {
        FreeStyleProject project = (FreeStyleProject) j.jenkins.getItem("7072");
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.UNSTABLE, build);
        AbstractTestResultAction action = build.getAction(AbstractTestResultAction.class);
        assertEquals(3003, action.getTotalCount(), "The number of tests is not correct");
        assertEquals(96, action.getFailCount(), "The number of failed tests is not correct");
        assertEquals(34, action.getSkipCount(), "The number of skipped tests is not correct");
    }
}
