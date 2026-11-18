package works.lysenko.util.data.type.logr.type;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.EventType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTest {
    @Test
    void testEvent() {
        Event event = new Event(1L, EventType.UNDEFINED, 2, "Test Message", "callstack");
        assertEquals("callstack", event.callstack());
        assertEquals(null, event.severity());
    }
}
