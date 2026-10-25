package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColoursIgnoreTest {

    @Test
    void testMarkerAndSet() {
        assertEquals("order", ColoursIgnore.ORDER.marker());

        final Set<ColoursIgnore> result = ColoursIgnore.set("Contains order and shrink");
        assertTrue(result.contains(ColoursIgnore.ORDER));
        assertTrue(result.contains(ColoursIgnore.SHRINK));

        assertTrue(ColoursIgnore.set(null).isEmpty());
    }
}
