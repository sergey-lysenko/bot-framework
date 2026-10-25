package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTypeTest {

    @Test
    void testGetString() {
        assertEquals("[FAILURE]", EventType.FAILURE.getString());
        assertEquals("[WARNING]", EventType.WARNING.getString());
    }
}
