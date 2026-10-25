package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ETest {

    @Test
    void testInstantiation() {
        E instance = new E();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(E.EXPECT);
        assertFalse(E.EXPECT.isEmpty());
        assertNotNull(E.EDGE);
        assertFalse(E.EDGE.isEmpty());
        assertNotNull(E.EDGES);
        assertFalse(E.EDGES.isEmpty());
        assertNotNull(E.EFFECTIVE);
        assertFalse(E.EFFECTIVE.isEmpty());
        assertNotNull(E.ELEMENT);
        assertFalse(E.ELEMENT.isEmpty());
        assertNotNull(E.EMPTY);
        assertFalse(E.EMPTY.isEmpty());
        assertNotNull(E.ENABLED);
        assertFalse(E.ENABLED.isEmpty());
        assertNotNull(E.ENOUGH);
        assertFalse(E.ENOUGH.isEmpty());
        assertNotNull(E.ENTRY);
        assertFalse(E.ENTRY.isEmpty());
        assertNotNull(E.EQUAL);
        assertFalse(E.EQUAL.isEmpty());
        assertNotNull(E.EQUALS);
        assertFalse(E.EQUALS.isEmpty());
        assertNotNull(E.ERROR);
        assertFalse(E.ERROR.isEmpty());
        assertNotNull(E.EUCLIDEAN);
        assertFalse(E.EUCLIDEAN.isEmpty());
        assertNotNull(E.EVENT);
        assertFalse(E.EVENT.isEmpty());
        assertNotNull(E.EVENTS);
        assertFalse(E.EVENTS.isEmpty());
        assertNotNull(E.EXACT);
        assertFalse(E.EXACT.isEmpty());
        assertNotNull(E.EXCEEDING);
        assertFalse(E.EXCEEDING.isEmpty());
        assertNotNull(E.EXCEPTION);
        assertFalse(E.EXCEPTION.isEmpty());
        assertNotNull(E.EXCEPTIONAL);
        assertFalse(E.EXCEPTIONAL.isEmpty());
        assertNotNull(E.EXCEPTIONS);
        assertFalse(E.EXCEPTIONS.isEmpty());
        assertNotNull(E.EXECUTABLE);
        assertFalse(E.EXECUTABLE.isEmpty());
        assertNotNull(E.EXECUTED);
        assertFalse(E.EXECUTED.isEmpty());
        assertNotNull(E.EXECUTING);
        assertFalse(E.EXECUTING.isEmpty());
        assertNotNull(E.EXECUTION);
        assertFalse(E.EXECUTION.isEmpty());
        assertNotNull(E.EXHAUSTED);
        assertFalse(E.EXHAUSTED.isEmpty());
        assertNotNull(E.EXIST);
        assertFalse(E.EXIST.isEmpty());
        assertNotNull(E.EXISTENT);
        assertFalse(E.EXISTENT.isEmpty());
        assertNotNull(E.EXPECTATIONS);
        assertFalse(E.EXPECTATIONS.isEmpty());
        assertNotNull(E.EXPECTED);
        assertFalse(E.EXPECTED.isEmpty());
        assertNotNull(E.EXTENDED);
        assertFalse(E.EXTENDED.isEmpty());
        assertNotNull(E.EXTENSION);
        assertFalse(E.EXTENSION.isEmpty());
        assertNotNull(E.EXTERNAL);
        assertFalse(E.EXTERNAL.isEmpty());
        assertNotNull(E.EXTRA);
        assertFalse(E.EXTRA.isEmpty());
    }
}
