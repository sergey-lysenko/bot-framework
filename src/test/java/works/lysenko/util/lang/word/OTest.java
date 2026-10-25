package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OTest {

    @Test
    void testInstantiation() {
        O instance = new O();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(O.OCCURED);
        assertFalse(O.OCCURED.isEmpty());
        assertNotNull(O.OCCURRENCE);
        assertFalse(O.OCCURRENCE.isEmpty());
        assertNotNull(O.OFFSET);
        assertFalse(O.OFFSET.isEmpty());
        assertNotNull(O.OPENING);
        assertFalse(O.OPENING.isEmpty());
        assertNotNull(O.OPENQA);
        assertFalse(O.OPENQA.isEmpty());
        assertNotNull(O.OPERATION);
        assertFalse(O.OPERATION.isEmpty());
        assertNotNull(O.OPTIONAL);
        assertFalse(O.OPTIONAL.isEmpty());
        assertNotNull(O.ORDER);
        assertFalse(O.ORDER.isEmpty());
        assertNotNull(O.ORIGINAL);
        assertFalse(O.ORIGINAL.isEmpty());
        assertNotNull(O.ORPHANED);
        assertFalse(O.ORPHANED.isEmpty());
        assertNotNull(O.OTHER);
        assertFalse(O.OTHER.isEmpty());
        assertNotNull(O.OUTPUT);
        assertFalse(O.OUTPUT.isEmpty());
        assertNotNull(O.OUTSIDE);
        assertFalse(O.OUTSIDE.isEmpty());
        assertNotNull(O.OVERSTRETCHED);
        assertFalse(O.OVERSTRETCHED.isEmpty());
    }
}
