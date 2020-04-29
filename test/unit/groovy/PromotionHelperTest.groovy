package unit.groovy

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.model.Hudson
import hudson.model.Item
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import static org.junit.Assert.assertEquals

@RunWith(PowerMockRunner.class)
@PrepareForTest([Jenkins.class, RunWrapper.class, WorkflowJob.class, WorkflowRun.class])
class PromotionHelperTest extends BasePipelineTest {

    private Script promotionHelper
    private RunWrapper currentBuild
    private Hudson instance
    private ArrayList<RunWrapper> upstreamBuilds

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp();
        promotionHelper = loadScript('vars/promotionHelper.groovy')

        currentBuild = PowerMockito.mock(RunWrapper.class)
        instance = PowerMockito.mock(Hudson.class)

        PowerMockito.mockStatic(Jenkins.class);

        upstreamBuilds = new ArrayList<>()
    }

    @Test
    void getUpstreamTriggerOrLatestBuildNumber_ReturnsUpstreamBuildNumber_WithAutomaticTrigger() throws Exception {
        // when triggered automatically, currentBuild contains an array of upstream builds, which can be inspected to
        // retrieve the build number from the *first* upstream build
        PowerMockito.when(Jenkins.get()).thenReturn(instance)

        RunWrapper upstreamBuild1 = PowerMockito.mock(RunWrapper.class)
        PowerMockito.when(upstreamBuild1.number).thenReturn(666)
        PowerMockito.when(upstreamBuild1.getFullDisplayName()).thenReturn('upstream_job #666')

        RunWrapper upstreamBuild2 = PowerMockito.mock(RunWrapper.class)
        PowerMockito.when(upstreamBuild2.number).thenReturn(777)
        PowerMockito.when(upstreamBuild2.getFullDisplayName()).thenReturn('upstream_job #777')

        upstreamBuilds.add(upstreamBuild1)
        upstreamBuilds.add(upstreamBuild2)
        PowerMockito.when(currentBuild.getUpstreamBuilds()).thenReturn(upstreamBuilds)

        int buildNumber = promotionHelper.getUpstreamTriggerOrLatestBuildNumber(currentBuild, 'upstream_job')
        assertEquals(666, buildNumber)
    }

    @Test
    void getUpstreamTriggerOrLatestBuildNumber_ReturnsLatestBuildNumber_WithManualTrigger() throws Exception {
        // when triggered manually, there are no upstream builds, so the given job name is queried for the latest build
        PowerMockito.when(Jenkins.get()).thenReturn(instance)

        WorkflowRun run = PowerMockito.mock(WorkflowRun.class)
        PowerMockito.when(run.number).thenReturn(777)
        PowerMockito.when(run.getFullDisplayName()).thenReturn('upstream_job #777')

        WorkflowJob job = PowerMockito.mock(WorkflowJob.class)
        PowerMockito.when(job.lastSuccessfulBuild).thenReturn(run)

        PowerMockito.when(instance.getItemByFullName('upstream_job')).thenReturn((Item) job)
        PowerMockito.when(currentBuild.getUpstreamBuilds()).thenReturn(upstreamBuilds)

        int buildNumber = promotionHelper.getUpstreamTriggerOrLatestBuildNumber(currentBuild, 'upstream_job')
        assertEquals(777, buildNumber)
    }

    @Test(expected = RuntimeException.class)
    void getUpstreamTriggerOrLatestBuildNumber_ThrowsException_WithNullInstance() throws Exception {
        // test the scenario where Jenkins.get() throws an exception
        PowerMockito.when(Jenkins.get()).thenThrow(new IllegalStateException('unit test caused exception'))

        promotionHelper.getUpstreamTriggerOrLatestBuildNumber(currentBuild, 'upstream_job')
    }
}
