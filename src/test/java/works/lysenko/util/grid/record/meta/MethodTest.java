package works.lysenko.util.grid.record.meta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MethodTest {

    @Test
    void testEnumValues() {
        Method compulsive = Method.valueOf("COMPULSIVE");
        Method optional = Method.valueOf("OPTIONAL");
        assertEquals(Method.COMPULSIVE, compulsive);
        assertEquals(Method.OPTIONAL, optional);
        assertEquals(2, Method.values().length);
    }
}
