package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class LevelTest {

    @Test
    void testLevelConstants() {
        assertEquals(3, Level.debug);
        assertEquals(0, Level.none);
        assertEquals(1, Level.plain);
        assertEquals(2, Level.supplementary);
        assertNotNull(new Level());
    }
}

