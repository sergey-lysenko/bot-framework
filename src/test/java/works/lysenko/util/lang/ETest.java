package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ETest {

    @Test
    void testInstantiation() {
        E instance = new E();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(E.ELEMENT_IS_NULL);
        assertFalse(E.ELEMENT_IS_NULL.isEmpty());
        assertNotNull(E.EQUALS_TO_EXPECTED);
        assertFalse(E.EQUALS_TO_EXPECTED.isEmpty());
        assertNotNull(E.EXPECTATIONS_UPDATE_REQUESTED_BY___);
        assertFalse(E.EXPECTATIONS_UPDATE_REQUESTED_BY___.isEmpty());
        assertNotNull(E.EXPECTATIONS_UPDATE_WAS_NOT___);
        assertFalse(E.EXPECTATIONS_UPDATE_WAS_NOT___.isEmpty());
        assertNotNull(E.EXPECTED_CONDITION_FAILED___);
        assertFalse(E.EXPECTED_CONDITION_FAILED___.isEmpty());
        assertNotNull(E.EXCEPTIONAL_SCENARION);
        assertFalse(E.EXCEPTIONAL_SCENARION.isEmpty());
    }
}
