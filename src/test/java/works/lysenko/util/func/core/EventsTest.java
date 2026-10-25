package works.lysenko.util.func.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.util.data.enums.RangeResult;
import works.lysenko.util.data.enums.Severity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventsTest {

    @BeforeEach
    void setUp() {
        Base.core = null;
    }

    @AfterEach
    void tearDown() {
        Base.core = null;
    }

    @Test
    void testEventsInstantiation() {
        Events events = new Events();
        assertNotNull(events);
    }

    @Test
    void testLogGenericFailure() {
        assertFalse(Events.logGenericFailure(Severity.S1, "Test failure"));
    }

    @Test
    void testLogPolychromyUndefinedBoundsFailure() {
        assertEquals(RangeResult.NOT_OK, Events.logPolychromyUndefinedBoundsFailure("test"));
    }

    @Test
    void testLogPolychromyUndefinedMethodFailure() {
        assertNull(Events.logPolychromyUndefinedMethodFailure("test"));
    }
}
