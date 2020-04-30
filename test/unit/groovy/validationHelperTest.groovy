package unit.groovy

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ValidationTest extends BasePipelineTest {

    private Script validationHelper

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp();
        validationHelper = loadScript('vars/validationHelper.groovy')
    }

    @Test
    void validatesExactMatch() {
        assertTrue(validationHelper.validateString("THIS_IS_A_TEST", ~/THIS_IS_A_TEST/))
    }

    @Test
    void invalidatesExactMismatch() {
        assertFalse(validationHelper.validateString("THIS_IS_A_TEST", ~/THIS_IS_NOT_A_TEST/))
    }

    @Test
    void validatesMatchedRegex() {
        assertTrue(validationHelper.validateString("THIS_IS_A_TEST", ~/[A-Z]+_[A-Z]+/))
    }

    @Test
    void invalidatesMismatchedRegex() {
        assertFalse(validationHelper.validateString("THIS_IS_A_TEST", ~/[a-z]+_[a-z]+/))
    }

    @Test
    void validatesPartialMatch() {
        assertTrue(validationHelper.validateString("THIS_IS_A_TEST", ~/IS_A/))
    }
}
