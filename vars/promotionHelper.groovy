#!groovy
import hudson.model.Item
import hudson.model.Result
import jenkins.model.CauseOfInterruption.UserInterruption
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

import java.time.LocalDateTime

/**
 * Aborts all previous builds of a given build
 *
 * @param promotedBuild RunWrapper object, typically `currentBuild` when called from a Pipeline context
 */
void abortPreviousBuilds(RunWrapper promotedBuild) {
    def previousBuild = promotedBuild.rawBuild.getPreviousBuild()

    while (previousBuild != null) {
        if (previousBuild.isBuilding()) {
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
static int getUpstreamTriggerOrLatestBuildNumber(RunWrapper promotedBuild, String upstreamJobName) throws RuntimeException {
    int buildNum
    def build
    def instance

    // if build is trigger from upstream, retrieve the build number of the build that triggered this build
    def upstreamBuilds = promotedBuild.getUpstreamBuilds()
    if (!upstreamBuilds.isEmpty()) {
        build = upstreamBuilds.first()
        println(">> Automatic trigger detected, using build ${build.getFullDisplayName()}")
        buildNum = build.number
        return buildNum
    }

    // if build is trigger manually by a user, there is no upstream, so get the latest build number
    try {
        instance = Jenkins.get()
    } catch (IllegalStateException e) {
        throw new RuntimeException("Failed to get Jenkins singleton ${e.getStackTrace()}")
    }

    Item job = instance.getItemByFullName(upstreamJobName)
    build = job?.lastSuccessfulBuild
    if (build == null) {
        throw new RuntimeException("No builds detected for job ${upstreamJobName}")
    }
    buildNum = build.number
    println(">> Manual trigger detected, using latest build ${build.getFullDisplayName()}")
    return buildNum
}

/**
 * Print the time when a stage promotion occurred
 *
 * @param stage the stage being promoted to
 */
void printStagePromotionTime(String stage) {
    def date = LocalDateTime.now()
    println("Promoted to ${stage} at ${date}")
}
