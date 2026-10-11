package works.lysenko.base.ui;

import org.junit.jupiter.api.Test;
import works.lysenko.base.parameters.PropertyValidator;

import static org.junit.jupiter.api.Assertions.*;

class InvalidPropertyHandlingTest {

    @Test
    void testUnknownPropertyKeyIsDetectedAndSuggestsCorrection() {
        assertFalse(PropertyValidator.isKnownConfigProperty(".test.test"));
        final String err = PropertyValidator.validateKey(".test.test", ConfigTestsEditorPanel.FileCategory.CONFIG_PART);
        assertNotNull(err);
        assertTrue(err.contains("Unknown configuration key '.test.test'"));
        assertTrue(err.contains("Did you mean '.test.tests'?"));
    }

    @Test
    void testValidConfigPropertyKeyReturnsNull() {
        assertTrue(PropertyValidator.isKnownConfigProperty(".test.tests"));
        assertNull(PropertyValidator.validateKey(".test.tests", ConfigTestsEditorPanel.FileCategory.CONFIG_PART));
        assertNull(PropertyValidator.validateKey(".include", ConfigTestsEditorPanel.FileCategory.GLOBAL_CONFIG));
    }

    @Test
    void testScenarioKeyInConfigFileIsDetectedAsScopeViolation() {
        final String err = PropertyValidator.validateKey("SomeScenario", ConfigTestsEditorPanel.FileCategory.CONFIG_PART);
        assertNotNull(err);
        assertTrue(err.contains("Scenario key 'SomeScenario' is not allowed in configuration files"));
    }

    @Test
    void testInvalidPropertyValueIsDetected() {
        final String err = PropertyValidator.validateProperty(".test.headless", "invalid_value", ConfigTestsEditorPanel.FileCategory.CONFIG_PART);
        assertNotNull(err);
        assertTrue(err.contains("Expected a boolean: true or false"));
    }

    @Test
    void testValidPropertyKeyAndValueReturnsNull() {
        assertNull(PropertyValidator.validateProperty(".test.tests", "999", ConfigTestsEditorPanel.FileCategory.CONFIG_PART));
        assertNull(PropertyValidator.validateProperty(".test.headless", "true", ConfigTestsEditorPanel.FileCategory.CONFIG_PART));
    }
}
