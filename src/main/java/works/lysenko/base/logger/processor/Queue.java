package works.lysenko.base.logger.processor;

import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.log._Queue;
import works.lysenko.util.data.type.LogRecord;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Queue class represents a priority queue for LogRecord objects.
 */
@SuppressWarnings({"UseOfConcreteClass", "NestedMethodCall"})
public class Queue implements _Queue {

    private final PriorityQueue<LogRecord> queue;

    /**
     * Queue class represents a priority queue for LogRecord objects.
     */
    public Queue() {

        queue = new PriorityQueue<>(Comparator.comparing(LogRecord::time));
    }

    @Override
    public final void add(final LogRecord logRecord) {

        queue.add(logRecord);
    }

    @Override
    public final boolean isEmpty() {

        return queue.isEmpty();
    }

    @Override
    public final _LogRecord poll() {

        return queue.poll();
    }
}