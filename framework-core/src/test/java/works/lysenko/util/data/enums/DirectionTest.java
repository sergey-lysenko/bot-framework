package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectionTest {

    @Test
    void testValues() {
        assertEquals(-1, Direction.NEGATIVE.value());
        assertEquals(1, Direction.POSITIVE.value());
    }
}
