package works.lysenko.util.spec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.TestProperties;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropEnumTest {

    private TestProperties previousProperties;

    @BeforeEach
    void setUp() {
        previousProperties = Base.properties;
    }

    @AfterEach
    void tearDown() {
        Base.properties = previousProperties;
    }

    @Test
    void everyEnumConstantHasValidMetadata() {
        for (final PropEnum prop : PropEnum.values()) {
            assertNotNull(prop.type(), prop.name() + " type must not be null");
            assertNotNull(prop.getPropertyName(), prop.name() + " propertyName must not be null");
            assertTrue(prop.getPropertyName().startsWith("."),
                    prop.name() + " propertyName should start with dot: " + prop.getPropertyName());
            assertEquals(prop.type(), prop.type());
        }
    }

    @Test
    void fallbackGetParsesDefaultValueWhenPropertiesNull() {
        Base.properties = null;
        for (final PropEnum prop : PropEnum.values()) {
            if (prop == PropEnum._TEST_BUNDLE_ID || prop == PropEnum._WEBD_FORCED_PLATFORM || prop == PropEnum._TEST_TESTS) {
                continue;
            }
            final Object value = prop.get();
            assertNotNull(value, prop.name() + " get() should parse default value into non-null object");
            assertTrue(prop.type().isInstance(value),
                    prop.name() + " parsed value should be instance of " + prop.type().getName());
        }
    }

    @Test
    void delegatesGetToPropertiesWhenPresent() throws Exception {
        final TestProperties testProperties = new TestProperties();
        final Field theField = TestProperties.class.getDeclaredField("the");
        theField.setAccessible(true);
        final Properties raw = new Properties();
        raw.setProperty(PropEnum._LOGS_DEBUG.getPropertyName(), "true");
        raw.setProperty(PropEnum._TEST_REPORT_PROGRESSION_TREE_GIF.getPropertyName(), "true");
        theField.set(testProperties, raw);
        Base.properties = testProperties;

        assertEquals(Boolean.TRUE, PropEnum._LOGS_DEBUG.get());
        assertEquals(Boolean.TRUE, PropEnum._TEST_REPORT_PROGRESSION_TREE_GIF.get());
    }

    @Test
    void progressionGifDefaultsAreFalse() {
        Base.properties = null;
        assertEquals(Boolean.FALSE, PropEnum._TEST_REPORT_PROGRESSION_SCENARIO_GIF.get());
        assertEquals(Boolean.FALSE, PropEnum._TEST_REPORT_PROGRESSION_TREE_GIF.get());
        assertEquals("false", PropEnum._TEST_REPORT_PROGRESSION_SCENARIO_GIF.defaultValue());
        assertEquals("false", PropEnum._TEST_REPORT_PROGRESSION_TREE_GIF.defaultValue());
    }

    @Test
    void swipeMarkerPropertyNamesAreExplicitlyMapped() {
        assertEquals(".swipes.marker.line.colour", PropEnum._SWIPE_LINE_MARKER_COLOUR.getPropertyName());
        assertEquals(".swipes.marker.start.colour", PropEnum._SWIPE_START_MARKER_COLOUR.getPropertyName());
        assertEquals(".swipes.marker.stop.colour", PropEnum._SWIPE_STOP_MARKER_COLOUR.getPropertyName());
    }

    @Test
    void checksSilentAndExecutionParameterFlags() {
        assertTrue(PropEnum._LOGS_DEBUG.silent());
        assertTrue(PropEnum._TEST_ALL_LEAFS.silent());
        assertTrue(PropEnum._TEST_ALL_LEAFS.executionParameter());
        assertTrue(PropEnum._TEST_ALL_LEAFS_COUNT.executionParameter());
        assertFalse(PropEnum._LOGS_DEBUG.executionParameter());
        assertFalse(PropEnum._TEST_REPORT_PROGRESSION_SCENARIO_GIF.silent());
        assertFalse(PropEnum._TEST_REPORT_PROGRESSION_SCENARIO_GIF.executionParameter());
    }
}
