#!groovy

import org.jenkinsci.plugins.workflow.cps.EnvActionImpl

def getHarvesterJobName(String processDirectory) {
    def finder = new FileNameByRegexFinder()
    processFile = finder.getFileNames(processDirectory, '.*_harvester_[0-9]+\\.[0-9]+\\.item')[0]

    if (!processFile) {
        return null
    }

    return processFile.split('/')[-1].replaceAll('_[0-9]+\\.[0-9]+\\.item$','')
}

def addBuildProperties(String zipFilePath, EnvActionImpl env, String propertiesFilePath='build.properties') {
    // generate build.properties file from environment variables
    propertiesFile = new File(propertiesFilePath)
    def buildVars = env.getEnvironment().findAll { it.key == 'GIT_COMMIT' || it.key.matches('.*BUILD_.*') }
    println "matching buildVars: ${buildVars}"
    propertiesFile.write('')
    buildVars.each {
        propertiesFile << "${it.key}=${it.value}\n"
    }

    // add build.properties to the harvester ZIP file
    def baseName = propertiesFile.getName()
    def workingDirectory = propertiesFile.getParentFile()
    def process = "zip ${zipFilePath} ${baseName}".execute(null, workingDirectory)
    def out = new ByteArrayOutputStream()
    def err = new ByteArrayOutputStream()
    process.consumeProcessOutput(out, err)
    process.waitFor()
    if (process.exitValue() != 0) {
        throw new Exception("failed to update harvester ZIP file with build.properties.\nstdOut:\n${out.toString()}\nstdErr:\n${err.toString()}")
    }
}
