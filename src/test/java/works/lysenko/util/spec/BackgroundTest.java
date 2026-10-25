package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundTest {

    @Test
    void testBackgroundConstants() {
        assertTrue(Background.validation);
        assertEquals(39, Background.density);
        assertNotNull(new Background());
    }
}

