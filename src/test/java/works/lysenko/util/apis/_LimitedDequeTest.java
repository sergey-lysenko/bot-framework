package works.lysenko.util.apis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("_LimitedDeque Interface Tests")
class _LimitedDequeTest {

    private static class DummyLimitedDeque implements _LimitedDeque<String> {
        private final List<String> list = new ArrayList<>();
        private final int maxSize;

        DummyLimitedDeque(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public void add(String element) {
            if (list.size() >= maxSize) {
                list.remove(0);
            }
            list.add(element);
        }

        @Override
        public Iterator<String> descendingIterator() {
            List<String> reversed = new ArrayList<>(list);
            java.util.Collections.reverse(reversed);
            return reversed.iterator();
        }

        @Override
        public int size() {
            return list.size();
        }
    }

    @Test
    @DisplayName("Verify _LimitedDeque interface contract")
    void testLimitedDequeContract() {
        _LimitedDeque<String> deque = new DummyLimitedDeque(2);
        deque.add("a");
        deque.add("b");
        assertEquals(2, deque.size());

        deque.add("c");
        assertEquals(2, deque.size());

        Iterator<String> desc = deque.descendingIterator();
        assertEquals("c", desc.next());
        assertEquals("b", desc.next());
    }
}
