#!groovy

import hudson.model.Result
import jenkins.model.CauseOfInterruption.UserInterruption
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper


def abortPreviousBuilds(RunWrapper promotedBuild) {
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
