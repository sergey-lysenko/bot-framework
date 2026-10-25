package works.lysenko.util.data.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaledPropertiesTest {

    @Test
    void testIsDefault() {
        assertTrue(ScaledProperties.isDefault("abc", "abc"));
        assertFalse(ScaledProperties.isDefault("abc", "def"));
        assertTrue(ScaledProperties.isDefault((Object) 123, (Object) 123));
    }
}
