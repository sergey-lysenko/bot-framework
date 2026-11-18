package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PolychromyMethodTest {

    @Test
    void testGetByIndex() {
        assertEquals(PolychromyMethod.EUCLIDEAN_DISTANCE, PolychromyMethod.getByIndex(0));
        assertNull(PolychromyMethod.getByIndex(-1));
        assertNull(PolychromyMethod.getByIndex(1));
    }
}
