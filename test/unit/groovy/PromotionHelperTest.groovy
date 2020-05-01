package unit.groovy

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.EnvVars
import hudson.model.Hudson
import hudson.model.Item
import hudson.model.Job
import hudson.model.Run
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

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp();
        promotionHelper = loadScript('vars/promotionHelper.groovy')

        currentBuild = PowerMockito.mock(RunWrapper.class)
        instance = PowerMockito.mock(Hudson.class)

        PowerMockito.mockStatic(Jenkins.class);
    }

    RunWrapper getAutomaticallyTriggeredBuild(RunWrapper build) {
        // when triggered automatically, currentBuild contains an array of upstream builds, which can be inspected to
        // retrieve the build number from the *first* upstream build
        EnvVars runVars = new EnvVars(new HashMap<String, String> ([
                BUILD_NUMBER : '666',
                BUILD_ID : '666',
                BUILD_TAG : 'jenkins-upstream_job-branch-666'
        ]))

        WorkflowRun run = PowerMockito.mock(WorkflowRun.class)
        PowerMockito.when(run.getNumber()).thenReturn(666)
        PowerMockito.when(run.getFullDisplayName()).thenReturn('upstream_job/branch #666')
        PowerMockito.when(run.getExternalizableId()).thenReturn('upstream_job/branch#666')
        PowerMockito.when(run.getCharacteristicEnvVars()).thenReturn(runVars)

        WorkflowJob job = PowerMockito.mock(WorkflowJob.class)
        PowerMockito.when(job.getName()).thenReturn('upstream_job')
        PowerMockito.when(job.getFullName()).thenReturn('upstream_job/branch')
        PowerMockito.when(job.getBuildByNumber(666)).thenReturn(run)
        PowerMockito.when(job.lastSuccessfulBuild).thenReturn(run)

        RunWrapper upstreamBuild1 = PowerMockito.mock(RunWrapper.class)
        PowerMockito.when(upstreamBuild1.number).thenReturn(666)
        PowerMockito.when(upstreamBuild1.getFullDisplayName()).thenReturn('upstream_job/branch #666')
        PowerMockito.when(upstreamBuild1.getRawBuild()).thenReturn((Run)run)

        RunWrapper upstreamBuild2 = PowerMockito.mock(RunWrapper.class)
        PowerMockito.when(upstreamBuild2.number).thenReturn(777)
        PowerMockito.when(upstreamBuild2.getFullDisplayName()).thenReturn('upstream_job/branch #777')

        ArrayList upstreamBuilds = new ArrayList<RunWrapper>()
        upstreamBuilds.add(upstreamBuild1)
        upstreamBuilds.add(upstreamBuild2)
        PowerMockito.when(build.getUpstreamBuilds()).thenReturn(upstreamBuilds)
        PowerMockito.when(Jenkins.get()).thenReturn(instance)
        PowerMockito.when(Jenkins.getInstanceOrNull()).thenReturn(instance)

        return build
    }

    RunWrapper getManuallyTriggeredBuild(RunWrapper build) {
        // when triggered manually, there are no upstream builds, so the given job name is queried for the latest build
        EnvVars runVars = new EnvVars(new HashMap<String, String> ([
                BUILD_NUMBER : '888',
                BUILD_ID : '888',
                BUILD_TAG : "jenkins-upstream_job-branch-888"
        ]))

        WorkflowRun run = PowerMockito.mock(WorkflowRun.class)
        PowerMockito.when(run.getNumber()).thenReturn(888)
        PowerMockito.when(run.getFullDisplayName()).thenReturn('upstream_job/branch #888')
        PowerMockito.when(run.getExternalizableId()).thenReturn('upstream_job/branch#888')
        PowerMockito.when(run.getCharacteristicEnvVars()).thenReturn(runVars)

        WorkflowJob job = PowerMockito.mock(WorkflowJob.class)
        PowerMockito.when(job.getName()).thenReturn('upstream_job')
        PowerMockito.when(job.getFullName()).thenReturn('upstream_job/branch')
        PowerMockito.when(job.getBuildByNumber(888)).thenReturn(run)
        PowerMockito.when(job.lastSuccessfulBuild).thenReturn(run)

        PowerMockito.when(instance.getItemByFullName('upstream_job/branch')).thenReturn((Item) job)
        PowerMockito.when(instance.getItemByFullName('upstream_job/branch', Job.class)).thenReturn((Item)job as Job)
        PowerMockito.when(build.getUpstreamBuilds()).thenReturn(new ArrayList<RunWrapper>())
        PowerMockito.when(Jenkins.get()).thenReturn(instance)
        PowerMockito.when(Jenkins.getInstanceOrNull()).thenReturn(instance)

        return build
    }

    @Test
    void getUpstreamTriggerOrLatestBuildTag_ReturnsBuildTag_WithAutomaticTrigger() throws Exception {
        RunWrapper build = getAutomaticallyTriggeredBuild(currentBuild)
        String buildTag = promotionHelper.getUpstreamTriggerOrLatestBuildTag(build, 'upstream_job/branch')
        assertEquals('jenkins-upstream_job-branch-666', buildTag)
    }

    @Test
    void getUpstreamTriggerOrLatestBuildTag_ReturnsBuildTag_WithManualTrigger() throws Exception {
        RunWrapper build = getManuallyTriggeredBuild(currentBuild)
        String buildTag = promotionHelper.getUpstreamTriggerOrLatestBuildTag(build, 'upstream_job/branch')
        assertEquals('jenkins-upstream_job-branch-888', buildTag)
    }

    @Test
    void getUpstreamTriggerOrLatestBuild_ReturnsUpstreamBuildNumber_WithAutomaticTrigger() throws Exception {
        RunWrapper build = getAutomaticallyTriggeredBuild(currentBuild)
        RunWrapper upstreamBuild = promotionHelper.getUpstreamTriggerOrLatestBuild(build, 'upstream_job/branch')
        int upstreamBuildNumber = upstreamBuild.getNumber()
        assertEquals(666, upstreamBuildNumber)
    }

    @Test
    void getUpstreamTriggerOrLatestBuild_ReturnsLatestBuild_WithManualTrigger() throws Exception {
        RunWrapper build = getManuallyTriggeredBuild(currentBuild)
        RunWrapper upstreamBuild = promotionHelper.getUpstreamTriggerOrLatestBuild(build, 'upstream_job/branch')
        int upstreamBuildNumber = upstreamBuild.getNumber()
        assertEquals(888, upstreamBuildNumber)
    }

    @Test(expected = RuntimeException.class)
    void getUpstreamTriggerOrLatestBuild_ThrowsException_WithNullInstance() throws Exception {
        // test the scenario where Jenkins.get() throws an exception
        PowerMockito.when(Jenkins.get()).thenThrow(new IllegalStateException('unit test caused exception'))

        promotionHelper.getUpstreamTriggerOrLatestBuild(currentBuild, 'upstream_job/branch')
    }

    @Test
    void getUpstreamTriggerOrLatestBuildNumber_ReturnsUpstreamBuildNumber_WithAutomaticTrigger() throws Exception {
        RunWrapper build = getAutomaticallyTriggeredBuild(currentBuild)
        int upstreamBuildNumber = promotionHelper.getUpstreamTriggerOrLatestBuildNumber(build, 'upstream_job/branch')
        assertEquals(666, upstreamBuildNumber)
    }

    @Test
    void getUpstreamTriggerOrLatestBuildNumber_ReturnsLatestBuildNumber_WithManualTrigger() throws Exception {
        RunWrapper build = getManuallyTriggeredBuild(currentBuild)

        int upStreamBuildNumber = promotionHelper.getUpstreamTriggerOrLatestBuildNumber(build, 'upstream_job/branch')
        assertEquals(888, upStreamBuildNumber)
    }
}
