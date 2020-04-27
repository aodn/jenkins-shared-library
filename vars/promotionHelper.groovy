#!groovy

import jenkins.model.Jenkins
import hudson.model.Result
import jenkins.model.CauseOfInterruption.UserInterruption
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper


void abortPreviousBuilds(RunWrapper promotedBuild) {
    def previousBuild = promotedBuild.rawBuild.getPreviousBuild()

    while (previousBuild != null) {
        if (previousBuild.isInProgress()) {
            def executor = previousBuild.getExecutor()
            if (executor != null) {
                println(">> Aborting older build #${previousBuild.number}")
                executor.interrupt(Result.ABORTED, new UserInterruption(
                        "Aborted by newer build #${promotedBuild.number}"
                ))
            }
        }
        previousBuild = previousBuild.getPreviousBuild()
    }
}

/**
 * Get the build number of the build which triggered a given build. If no upstreams are detected, retrieve the latest
 * successful build instead
 *
 * @param promotedBuild the downstream build instance to be queried
 * @param upstreamJobName the upstream build name
 * @return the build number from an upstream job
 */
static int getUpstreamTriggerOrLatestBuildNumber(RunWrapper promotedBuild, String upstreamJobName) {
    int buildNum

    // if build is trigger from upstream, retrieve the build number of the build that triggered this build
    promotedBuild.getUpstreamBuilds().each { b ->
        buildNum = b.number
        println(">> Automatic trigger detected, using build ${b.getFullDisplayName()}")
        return buildNum
    }

    // if build is trigger manually by a user, there is no upstream, so get the latest build number
    def job = Jenkins.instance.getJob(upstreamJobName)
    def build = job?.lastSuccessfulBuild
    if (build == null) {
        throw new RuntimeException("No builds detected for job ${upstreamJobName}")
    }
    buildNum = build.number
    println(">> Manual trigger detected, using latest build ${build.getFullDisplayName()}")
    return buildNum
}
