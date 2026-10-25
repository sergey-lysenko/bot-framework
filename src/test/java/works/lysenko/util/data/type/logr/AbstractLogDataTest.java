package works.lysenko.util.data.type.logr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractLogDataTest {

    private static class ConcreteLogData extends AbstractLogData {
        protected ConcreteLogData(final long id, final int depth, final String message) {
            super(id, depth, message);
        }

        @Override
        public String render() {
            return message;
        }
    }

    @Test
    void testAbstractLogDataGetters() {
        final ConcreteLogData logData = new ConcreteLogData(101L, 2, "Test Message");
        assertEquals(101L, logData.id());
        assertEquals(2, logData.depth());
        assertEquals("Test Message", logData.message());
        assertEquals("Test Message", logData.render());
    }
}
