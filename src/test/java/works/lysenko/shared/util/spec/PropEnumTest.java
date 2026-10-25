package works.lysenko.shared.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PropEnumTest {

    @Test
    void testDummyProperty() {
        assertNotNull(PropEnum._DUMMY);
        assertEquals(Boolean.class, PropEnum._DUMMY.type());
        assertEquals("false", PropEnum._DUMMY.defaultValue());
    }
}
