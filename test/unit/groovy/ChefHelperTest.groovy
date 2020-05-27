package unit.groovy

import com.lesfurets.jenkins.unit.BasePipelineTest
import groovy.json.internal.LazyMap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ChefHelperTest extends BasePipelineTest {

    private Script chefHelper

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        chefHelper = loadScript('vars/chefHelper.groovy')
    }

    @Test
    void loadJSONFile_ReturnsEqualMap_WithAbsoluteNodeFilePath() throws Exception {
        def absoluteNodeFilePath = this.getClass().getResource('/nodes/example-node-1.example.com.json').getFile()
        LazyMap node = chefHelper.loadJSONFile(absoluteNodeFilePath)

        def expected = [
                "chef_environment": "production",
                "name": "example-node-1",
                "description": [
                        "This node is used for unit testing ONLY."
                ],
                "hostname": "example-node-1",
                "run_list": [
                ],
                "ipaddress": "192.168.1.1",
                "network": [
                    "public_ipv4": "192.168.2.1"
                ],
                "fqdn": "example-node-1.example.com",
                "aliases": [
                        "example-node-1-alias.example.com"
                ],
                "keys": [
                    "ssh": [
                        "host_rsa_public": "EXAMPLE_NODE_1_RSA_PUBLIC_KEY"
                    ]
                ]
        ] as LazyMap

        assertTrue(expected == node)
    }

    @Test
    void loadJSONFile_ReturnsEqualMap_WithRelativeNodeFilePath() throws Exception {
        def currentDirectory = new File(new File(".").getCanonicalPath())
        def nodeFilePath = new File(this.getClass().getResource('/nodes/example-node-1.example.com.json').getFile())
        def relativeNodePath = currentDirectory.toPath().relativize(nodeFilePath.toPath()).toFile().toString()

        LazyMap node = chefHelper.loadJSONFile(relativeNodePath)

        def expected = [
                "chef_environment": "production",
                "name": "example-node-1",
                "description": [
                        "This node is used for unit testing ONLY."
                ],
                "hostname": "example-node-1",
                "run_list": [
                ],
                "ipaddress": "192.168.1.1",
                "network": [
                        "public_ipv4": "192.168.2.1"
                ],
                "fqdn": "example-node-1.example.com",
                "aliases": [
                        "example-node-1-alias.example.com"
                ],
                "keys": [
                        "ssh": [
                                "host_rsa_public": "EXAMPLE_NODE_1_RSA_PUBLIC_KEY"
                        ]
                ]
        ] as LazyMap

        assertTrue(expected == node)

    }

    @Test
    void stripExtension_StripsExtension_WithJsonFile() throws Exception {
        assertEquals('example-node-1.example.com', chefHelper.stripExtension('example-node-1.example.com.json'))
    }

    @Test
    void stripExtension_ReturnsInput_WithNoExtension() throws Exception {
        assertEquals('example-node-1', chefHelper.stripExtension('example-node-1'))
    }

    @Test
    void stripExtension_StripsLastExtension_WithMultipleExtensions() throws Exception {
        assertEquals('multiple-extensions.tar', chefHelper.stripExtension('multiple-extensions.tar.gz'))
    }

    @Test
    void getNodesForEnvironment_ReturnsNonExcludedNodesForEnvironment_WithAbsoluteNodesDirectory() throws Exception {
        def nodesDirectory = new File(this.getClass()
                .getResource('/nodes/example-node-1.example.com.json')
                .getFile()
        ).getParentFile().toString()

        List<String> nodes = chefHelper.getNodesForEnvironment("production", nodesDirectory, "ignored-node-1.example.com")
        List<String> expected = ['example-node-1']

        // comparison must ignore ordering, due to undefined load order from filesystem
        assertTrue(expected.size() == nodes.size() &&
                expected.containsAll(nodes) && nodes.containsAll(expected))
    }

    @Test
    void getNodesForEnvironment_ReturnsNonExcludedNodesForEnvironment_WithRelativeNodesDirectory() throws Exception {
        def nodesDirectory = new File(this.getClass()
                .getResource('/nodes/example-node-1.example.com.json')
                .getFile()
        ).getParentFile()

        def currentDirectory = new File(new File(".").getCanonicalPath())

        def relativeNodesDirectory = currentDirectory.toPath().relativize(nodesDirectory.toPath()).toFile().toString()

        List<String> nodes = chefHelper.getNodesForEnvironment("production", relativeNodesDirectory, "ignored-node-1.example.com")
        List<String> expected = ['example-node-1']

        // comparison must ignore ordering, due to undefined load order from filesystem
        assertTrue(expected.size() == nodes.size() &&
                expected.containsAll(nodes) && nodes.containsAll(expected))
    }

    @Test
    void getTagForEnvironment_ReturnsMapValue_WithKnownEnvironments() throws Exception {
        assertEquals("prod", chefHelper.getTagForEnvironment("production"))
        assertEquals("systest", chefHelper.getTagForEnvironment("testing"))
        assertEquals("master", chefHelper.getTagForEnvironment("development"))
    }

    @Test
    void getTagForEnvironment_ReturnsDefault_WithUnknownEnvironments() throws Exception {
        assertEquals("prod", chefHelper.getTagForEnvironment("unknown"))
    }
    @Test
    void getTagForNode_ReturnsProdTag_WithKnownEnvironment() throws Exception {
        String nodeFilePath = this.getClass().getResource('/nodes/example-node-1.example.com.json').getFile()
        String tag = chefHelper.getTagForNode(nodeFilePath)
        assertEquals("prod", tag)
    }

    @Test
    void generateSshConfigForNode_GeneratesFiles_WithValidExampleNode() throws Exception {
        String nodeFilePath = this.getClass().getResource('/nodes/example-node-1.example.com.json').getFile()
        String sshConfigOutputPath = folder.newFile()
        String knownHostsOutputPath = folder.newFile()

        chefHelper.generateSshConfigForNode(nodeFilePath, sshConfigOutputPath, knownHostsOutputPath)

        assertTrue(new File(sshConfigOutputPath).exists())
        assertTrue(new File(knownHostsOutputPath).exists())
    }

    @Test(expected = RuntimeException.class)
    void generateSshConfigForNode_RaisesException_WithInvalidExampleNode() throws Exception {
        String nodeFilePath = this.getClass().getResource('/nodes/ignored-node-1.example.com.json').getFile()
        String sshConfigOutputPath = folder.newFile()
        String knownHostsOutputPath = folder.newFile()

        // node is missing keys.ssh.host_rsa_public attribute, so should fail
        chefHelper.generateSshConfigForNode(nodeFilePath, sshConfigOutputPath, knownHostsOutputPath)
    }
}
