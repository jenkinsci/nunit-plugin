package hudson.plugins.nunit;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.test.AbstractTestResultAction;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.recipes.LocalData;

public class NUnitIntegrationTest extends HudsonTestCase {

    @Issue("JENKINS-5673")
    @LocalData
    public void testIssue5673() throws Exception {
        FreeStyleProject project = (FreeStyleProject) hudson.getItem("5673");
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.UNSTABLE, build);
        AbstractTestResultAction action = build.getAction(AbstractTestResultAction.class);
        assertEquals("The number of tests is not correct", 1355, action.getTotalCount());
    }

    @Issue("JENKINS-9246")
    @LocalData
    public void testIssue9246() throws Exception {
        FreeStyleProject project = (FreeStyleProject) hudson.getItem("9246");
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.UNSTABLE, build);
        AbstractTestResultAction action = build.getAction(AbstractTestResultAction.class);
        assertEquals("The number of tests is not correct", 5454, action.getTotalCount());
    }

    @Issue("JENKINS-7072")
    @LocalData
    public void testIssue7072() throws Exception {
        FreeStyleProject project = (FreeStyleProject) hudson.getItem("7072");
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertBuildStatus(Result.UNSTABLE, build);
        AbstractTestResultAction action = build.getAction(AbstractTestResultAction.class);
        assertEquals("The number of tests is not correct", 3003, action.getTotalCount());
        assertEquals("The number of failed tests is not correct", 96, action.getFailCount());
        assertEquals("The number of skipped tests is not correct", 34, action.getSkipCount());
    }
}
