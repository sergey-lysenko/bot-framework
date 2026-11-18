package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkerTest {

    @Test
    void testGetters() {
        assertEquals("from", Marker.FROM_TO.getFirst());
        assertEquals("to", Marker.FROM_TO.getSecond());
    }
}
