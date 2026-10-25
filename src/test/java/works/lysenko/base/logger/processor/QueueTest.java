package works.lysenko.base.logger.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Queue}.
 */
class QueueTest {

    @Test
    void testQueueOperations() {
        Queue queue = new Queue();
        assertTrue(queue.isEmpty());
        assertNull(queue.poll());
    }
}
