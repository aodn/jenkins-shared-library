#!groovy

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurper
import groovy.json.internal.LazyMap
import groovy.text.SimpleTemplateEngine

/**
 * Load a JSON file from a path
 *
 * @param jsonFilePath path to the input JSON file
 * @return LazyMap representation of the JSON file
 */
@NonCPS
static LazyMap loadJSONFile(String jsonFilePath) {
    File file

    if (jsonFilePath.startsWith('/')) {
        file = new File(jsonFilePath)
    } else {
        def defaultPathBase = new File( "." ).getCanonicalPath()
        file = new File(defaultPathBase, jsonFilePath)
    }

    LazyMap json = new JsonSlurper().parse(file)
    return json
}

/**
 * Strips the extension from a file name.
 *
 * @param fileName file name including extension
 * @return file name with extension stripped
 */
static String stripExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.')
    if (dotIndex < 0) {
        return fileName
    } else {
        return fileName.take(dotIndex)
    }
}

/**
 * Extract a list of Chef node names from a Chef 'nodes' directory for a given environment. Optionally ignore
 *
 * @param environment `chef_environment` attribute to be matched against
 * @param nodesDirectory Chef `nodes` directory to discover node files
 * @param ignoredNodes space separated list of node names to ignore
 * @return a list of node names matching the given environment, but *not* appearing in the ignoredNodes list
 * @throws RuntimeException
 */
static List<String> getNodesForEnvironment(String environment, String nodesDirectory, String ignoredNodes = null) throws RuntimeException {
    List<String> nodesToIgnore = ignoredNodes?.trim()?.split()
    List<String> environmentNodes = []
    FileNameByRegexFinder finder = new FileNameByRegexFinder()

    List<String> allNodes = finder.getFileNames(nodesDirectory, '^.*\\.json$')
    for (node in allNodes) {
        File nodeFile = new File(node)
        String nodeName = stripExtension(nodeFile.getName())

        if (nodesToIgnore?.contains(nodeName)) {
            println("Ignoring node ${nodeName}")
            continue
        }

        LazyMap nodeJson = loadJSONFile(nodeFile.getAbsolutePath())

        String nodeEnvironment = nodeJson.get('chef_environment', null)
        String nodeHostName = nodeJson.get('hostname', null)

        if (nodeEnvironment == null || nodeHostName == null ) {
            throw new RuntimeException("One or more required values not found in node file ${nodeFile.getAbsolutePath()}. Must contain 'chef_environment' and 'hostname' attributes.")
        }

        if (nodeEnvironment == environment) {
            environmentNodes.add(nodeHostName)
        }
    }
    return environmentNodes
}

/**
 * Get a Git tag corresponding to a given chef environment. If the enviroment is unknown, default to returning 'prod'
 *
 * @param environment chef_environment value for which to find the matching tag
 * @return tag name for the given environment name
 */
static String getTagForEnvironment(String environment) throws RuntimeException {
    def environmentToTagMap = [
            'development': 'master',
            'testing': 'systest',
            'production': 'prod'
    ]
    String tag = environmentToTagMap.get(environment, 'prod')
    return tag
}

/**
 * Get a Git tags for a given node file by
 *
 * @param nodeFilePath path to the input JSON node file
 * @return tag name for the given node name
 */
static String getTagForNode(String nodeFilePath) throws RuntimeException {
    File nodeFile = new File(nodeFilePath)
    LazyMap nodeJson = loadJSONFile(nodeFile.getAbsolutePath())
    String nodeEnvironment = nodeJson.get('chef_environment', null)
    String tag = getTagForEnvironment(nodeEnvironment)
    return tag
}

/**
 * Generates an SSH client config file and associated known_hosts file from a Chef node file
 *
 * @param nodeFilePath path to the JSON node file
 * @param sshConfigOutputPath path to which ssh_config file will be written
 * @param knownHostsOutputPath path to which known_hosts file will be written
 * @throws RuntimeException
 */
static void generateSshConfigForNode(String nodeFilePath, String sshConfigOutputPath, String knownHostsOutputPath) throws RuntimeException {
    File nodeFile = new File(nodeFilePath)
    String nodeName = stripExtension(nodeFile.getName())
    LazyMap nodeJson = loadJSONFile(nodeFile.getAbsolutePath())
    String ipAddress = nodeJson?.get('ipaddress')
    String key = nodeJson?.get('keys')?.get('ssh')?.get('host_rsa_public')

    Map bindings = [
            nodeName: nodeName,
            ipAddress: ipAddress,
            key: key
    ]

    // test for any null or empty values
    if (bindings.any { k,v -> !v?.trim()}) {
        throw new RuntimeException("One or more required values not found in node file ${nodeFilePath}: ${bindings.toString()}. Must contain 'ipaddress' and 'keys.ssh.host_rsa_public' attributes.")
    }

    def engine = new SimpleTemplateEngine()

    String knownHostsTemplate = """\
        $nodeName,$ipAddress ssh-rsa $key
    """.stripIndent()

    File knownHostsFile = new File(knownHostsOutputPath)
    String knownHostsContent = engine.createTemplate(knownHostsTemplate).make(bindings)
    knownHostsFile.text = knownHostsContent
    bindings['knownHostsFile'] = knownHostsFile.getAbsolutePath()

    String sshConfigTemplate = """\
        Host $nodeName
            HostName $ipAddress
            UserKnownHostsFile $knownHostsFile
            PasswordAuthentication no
    """.stripIndent()

    File sshConfigFile = new File(sshConfigOutputPath)
    String sshConfigContent = engine.createTemplate(sshConfigTemplate).make(bindings)
    sshConfigFile.text = sshConfigContent
}
