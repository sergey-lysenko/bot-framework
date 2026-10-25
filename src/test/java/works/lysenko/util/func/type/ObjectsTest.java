package works.lysenko.util.func.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectsTest {

    @Test
    void testObjectsInstantiation() {
        Objects objects = new Objects();
        assertNotNull(objects);
    }

    @Test
    void testIsNotNull() {
        assertTrue(Objects.isNotNull("test"));
        assertFalse(Objects.isNotNull(null));
    }
}
