package hudson.plugins.nunit;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Project;
import hudson.tasks.test.TestResultProjectAction;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class NUnitPublisherTest {

    private Mockery classContext;
    private Project project;

    @Before
    public void setUp() throws Exception {
        classContext = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        project = classContext.mock(Project.class);
    }

    @Test
    public void testGetTestResultsPattern() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml", true, false, false);
        assertEquals("The test results pattern is incorrect", publisher.getTestResultsPattern(), "**/*.xml");
    }

    @Test
    public void testGetDebug() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml", true, false, false);
        assertTrue("Debug is incorrect", publisher.getDebug());
        publisher = new NUnitPublisher("**/*.xml", false, false, false);
        assertFalse("Debug is incorrect", publisher.getDebug());
    }

    @Test
    public void testDisabledDebug() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml", false, true, true);
        assertFalse("Debug is incorrect", publisher.getDebug());
        assertFalse("KeepJunitReports() is incorrect", publisher.getKeepJunitReports());
        assertFalse("SkipJunitArchiver() is incorrect", publisher.getSkipJunitArchiver());
    }

    @Test
    public void testGetKeepJunitReports() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml", true, true, false);
        assertTrue("KeepJunitReports() is incorrect", publisher.getKeepJunitReports());
        publisher = new NUnitPublisher("**/*.xml", true, false, false);
        assertFalse("KeepJunitReports() is incorrect", publisher.getKeepJunitReports());
    }

    @Test
    public void testGetSkipJunitArchiver() {
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml", true, false, true);
        assertTrue("SkipJunitArchiver() is incorrect", publisher.getSkipJunitArchiver());
        publisher = new NUnitPublisher("**/*.xml", true, false, false);
        assertFalse("SkipJunitArchiver() is incorrect", publisher.getSkipJunitArchiver());
    }

    @Test
    public void testGetProjectActionProjectReusing() {
        classContext.checking(new Expectations() {
            {
                one(project).getAction(with(equal(TestResultProjectAction.class))); will(returnValue(new TestResultProjectAction(project)));
            }
        });
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml", false, false, true);
        Action projectAction = publisher.getProjectAction((AbstractProject)project);
        assertNull("The action was not null", projectAction);
    }

    @Test
    public void testGetProjectActionProject() {
        classContext.checking(new Expectations() {
            {
                one(project).getAction(with(equal(TestResultProjectAction.class))); will(returnValue(null));
            }
        });
        NUnitPublisher publisher = new NUnitPublisher("**/*.xml", false, false, true);
        Action projectAction = publisher.getProjectAction((AbstractProject)project);
        assertNotNull("The action was null", projectAction);
        assertEquals("The action type is incorrect", TestResultProjectAction.class, projectAction.getClass());
    }
}
