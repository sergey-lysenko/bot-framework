package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelTest {

    @Test
    void testLevelConstants() {
        assertEquals(3, Level.debug);
        assertEquals(0, Level.none);
        assertEquals(1, Level.plain);
        assertEquals(2, Level.supplementary);
    }
}
