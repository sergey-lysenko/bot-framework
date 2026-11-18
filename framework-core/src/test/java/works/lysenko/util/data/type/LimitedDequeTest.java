package works.lysenko.util.data.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LimitedDequeTest {

    @Test
    void testLimitEnforced() {
        final LimitedDeque<String> deque = new LimitedDeque<>(2);
        deque.add("a");
        deque.add("b");
        assertEquals(2, deque.size());

        deque.add("c");
        assertEquals(2, deque.size());
        assertNotNull(deque.descendingIterator());
        assertNotNull(deque.toString());
    }
}
