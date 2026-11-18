package works.lysenko.util.apis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("_PropEnum Interface Tests")
class _PropEnumTest {

    private enum TestProperty implements _PropEnum {
        SAMPLE_PROPERTY,
        ANOTHER_TEST_PROP;

        @Override
        public String defaultValue() {
            return "default";
        }

        @Override
        public Class<?> type() {
            return String.class;
        }
    }

    @Test
    @DisplayName("Verify getPropertyName converts enum constant name to lowercase with dot separator")
    void testGetPropertyName() {
        assertEquals("sample.property", TestProperty.SAMPLE_PROPERTY.getPropertyName());
        assertEquals("another.test.prop", TestProperty.ANOTHER_TEST_PROP.getPropertyName());
    }

    @Test
    @DisplayName("Verify default values for silent and executionParameter")
    void testDefaultMethods() {
        assertFalse(TestProperty.SAMPLE_PROPERTY.silent());
        assertFalse(TestProperty.SAMPLE_PROPERTY.executionParameter());
    }
}
