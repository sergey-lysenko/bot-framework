package works.lysenko.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.util.prop.PropEnum;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TestProperties}.
 */
class TestPropertiesTest {

    private TestProperties properties;
    private TestProperties previousProperties;

    @BeforeEach
    void setUp() {
        previousProperties = Base.properties;
        properties = new TestProperties();
        Base.properties = properties;
    }

    @AfterEach
    void tearDown() {
        Base.properties = previousProperties;
    }

    @Test
    void testCompendiumInitialization() {
        assertNotNull(properties.getCompendium());
        assertTrue(properties.getCompendium().contains(PropEnum.class));
    }

    @Test
    void testUserOverrides() {
        assertTrue(properties.getUserOverrides().isEmpty());

        properties.setUserOverride("test.key", "test.value");
        assertEquals("test.value", properties.getUserOverrides().get("test.key"));

        properties.setUserOverride("test.key", null);
        assertNull(properties.getUserOverrides().get("test.key"));

        Map<String, String> overrides = new HashMap<>();
        overrides.put("key1", "val1");
        overrides.put("key2", "val2");

        properties.setUserOverrides(overrides);
        assertEquals(2, properties.getUserOverrides().size());
        assertEquals("val1", properties.getUserOverrides().get("key1"));

        properties.clearUserOverrides();
        assertTrue(properties.getUserOverrides().isEmpty());
    }

    @Test
    void testDefaultsSize() {
        assertTrue(properties.getDefaultsSize() > 0);
        assertNotNull(properties.getDefaults());
    }

    @Test
    void testResolveEffectiveProperties() {
        Map<String, String> effective = properties.resolveEffectiveProperties("DummyTest", true, true, 2);
        assertNotNull(effective);
        assertEquals("true", effective.get(PropEnum._TEST_HEADLESS.getPropertyName()));
        assertEquals("true", effective.get(PropEnum._TEST_ALL_LEAFS.getPropertyName()));
        assertEquals("2", effective.get(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.getPropertyName()));
    }
}
