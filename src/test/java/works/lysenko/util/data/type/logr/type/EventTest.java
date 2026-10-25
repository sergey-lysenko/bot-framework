package works.lysenko.util.data.type.logr.type;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.enums.Severity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventTest {

    @Test
    void testEvent() {
        final Event event = new Event(1L, EventType.WARNING, 2, "Warning msg", "stacktrace");
        assertEquals(1L, event.id());
        assertEquals(2, event.depth());
        assertEquals(Severity.S2, event.severity());
        assertNotNull(event.message());
        assertNotNull(event.render());
        assertNotNull(event.tag());
    }
}
