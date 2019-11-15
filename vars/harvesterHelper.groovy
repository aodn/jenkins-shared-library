#!groovy

import org.jenkinsci.plugins.workflow.cps.EnvActionImpl

/**
 * Extract the project name component from a job name string
 *
 * @param jobName string
 * @return the project name or null
 */
String getProjectNameFromJobName(String jobName) {
    String projectName = (jobName =~ /harvester_(.+)_build/).with { it.matches() ? it[0][1] : null }
    return projectName
}


/**
 * Search a given directory for a Talend harvester
 *
 * @param processDirectory directory in which to discover a harvester '.item' file
 * @return the harvester name
 */
String getHarvesterNameFromProjectName(String projectName, String processDirectory) {
    FileNameByRegexFinder finder = new FileNameByRegexFinder()
    String[] allItems = finder.getFileNames(processDirectory, '.*_[0-9]+\\.[0-9]+\\.item').collect {
        new File(it).getName().replaceAll('_[0-9]+\\.[0-9]+\\.item$', '')
    }
    String harvesterName = allItems.find { it.matches('.*_harvester') || it.matches("^${projectName}.*") }
    return harvesterName
}


/**
 * Add a file containing Jenkins build variables into a harvester ZIP file
 *
 * @param zipFilePath path to the ZIP file onto which the build properties will be appended
 * @param env environment variables passed from runtime Pipeline context
 * @param propertiesFilePath local path to write the build properties. Note: the file will *always* be written to the
 *          root of the zip file, using the basename of the file
 */
Map<String, String> addBuildProperties(String zipFilePath, EnvActionImpl env, String propertiesFileName = 'build.properties') {
    Map<String, String> buildVars = env.getEnvironment().findAll {
        it.key == 'GIT_COMMIT' || it.key.matches('.*BUILD_.*')
    }

    File.createTempDir().with { tempDir ->
        File propertiesFile = new File(tempDir.getAbsolutePath(), propertiesFileName)
        propertiesFile.withWriter { writer ->
            buildVars.each {
                writer.write("${it.key}=${it.value}\n")
            }
        }

        // add build.properties to the harvester ZIP file
        String baseName = propertiesFile.getName()
        Process process = "zip ${zipFilePath} ${baseName}".execute(null, propertiesFile.getParentFile())
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        ByteArrayOutputStream err = new ByteArrayOutputStream()
        process.consumeProcessOutput(out, err)
        process.waitFor()
        if (process.exitValue() != 0) {
            throw new Exception("failed to update harvester ZIP file with build.properties.\nstdOut:\n${out.toString()}\nstdErr:\n${err.toString()}")
        }
    }
    return buildVars
}
