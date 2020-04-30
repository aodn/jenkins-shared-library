package unit.groovy

import com.lesfurets.jenkins.unit.BasePipelineTest
import hudson.EnvVars
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.modules.junit4.PowerMockRunner

import java.util.zip.ZipFile

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

@RunWith(PowerMockRunner.class)
class HarvesterHelperTest extends BasePipelineTest {

    private Script harvesterHelper
    private EnvActionImpl env

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp();
        harvesterHelper = loadScript('vars/harvesterHelper.groovy')

        HashMap<String, String> vars = [
                BUILD_NUMBER   : '123',
                GIT_COMMIT     : 'da39a3ee5e6b4b0d3255bfef95601890afd80709',
                GIT_SECRET     : 'SHOULD_IGNORE',
                RANDOM_VARIABLE: 'SHOULD_IGNORE'
        ]
        EnvVars envVars = new EnvVars(vars)

        env = PowerMockito.mock(EnvActionImpl.class)
        PowerMockito.when(env.getEnvironment()).thenReturn(envVars)
    }

    @Test
    void getProjectNameFromJobName_ReturnsProjectName_IfValidJobName() throws Exception {
        String projectName = harvesterHelper.getProjectNameFromJobName('harvester_TESTPROJECT_build')
        assertEquals('TESTPROJECT', projectName)
    }

    @Test
    void getProjectNameFromJobName_ReturnsProjectName_IfValidJobNameWithUnderscore() throws Exception {
        String projectName = harvesterHelper.getProjectNameFromJobName('harvester_TEST_PROJECT_build')
        assertEquals('TEST_PROJECT', projectName)
    }

    @Test
    void getProjectNameFromJobName_ReturnsNull_IfInvalidJobName() throws Exception {
        String projectName = harvesterHelper.getProjectNameFromJobName('qwerty')
        assertNull(projectName)
    }

    @Test
    void getHarvesterNameFromProjectName_ReturnsHarvesterName_WithoutSuffix() throws Exception {
        def projectDir = new File(this.getClass()
                .getResource('/TEST_PROJECT1/TEST_PROJECT1_0.1.item')
                .getFile()
        ).getParentFile().toString()
        String harvesterName = harvesterHelper.getHarvesterNameFromProjectName('TEST_PROJECT1', projectDir)
        assertEquals('TEST_PROJECT1', harvesterName)
    }

    @Test
    void getHarvesterNameFromProjectName_ReturnsHarvesterName_WithSuffix() throws Exception {
        def projectDir = new File(this.getClass()
                .getResource('/TEST_PROJECT2/TEST_PROJECT2_harvester_0.1.item')
                .getFile()
        ).getParentFile().toString()
        String harvesterName = harvesterHelper.getHarvesterNameFromProjectName('TEST_PROJECT2', projectDir)
        assertEquals('TEST_PROJECT2_harvester', harvesterName)
    }

    @Test
    void getHarvesterNameFromProjectName_ReturnsNull_WithInvalidProjectDir() throws Exception {
        def projectDir = new File(this.getClass()
                .getResource('/TEST_PROJECT3/dummy.item')
                .getFile()
        ).getParentFile().toString()
        String harvesterName = harvesterHelper.getHarvesterNameFromProjectName('TEST_PROJECT2', projectDir)
        assertNull(harvesterName)
    }

    @Test
    void addBuildProperties_AppendsPropertiesFile_WithValidZip() throws Exception {
        def zipFilePath = this.getClass().getResource('/valid-test.zip').getFile()
        Map<String, String> buildVars = harvesterHelper.addBuildProperties(zipFilePath, env)

        ZipFile zipFile = new ZipFile(new File(zipFilePath))
        Properties props = new Properties()
        props.load(zipFile.getInputStream(zipFile.getEntry('build.properties')))

        HashMap<String, String> expectedMap = [
                BUILD_NUMBER: '123',
                GIT_COMMIT  : 'da39a3ee5e6b4b0d3255bfef95601890afd80709'
        ]
        assertEquals(expectedMap, buildVars)
        assertEquals(expectedMap, props)
    }

    @Test(expected = RuntimeException.class)
    void addBuildProperties_ThrowsException_WithInvalidZip() throws Exception {
        def zipFilePath = this.getClass().getResource('/invalid-test.zip').getFile()
        Map<String, String> buildVars = harvesterHelper.addBuildProperties(zipFilePath, env)
    }

    @Test(expected = RuntimeException.class)
    void addBuildProperties_ThrowsException_WithMissingZip() throws Exception {
        def zipFilePath = '/test-zipfile-that-doesnt-exist.zip'
        Map<String, String> buildVars = harvesterHelper.addBuildProperties(zipFilePath, env)
    }
}
