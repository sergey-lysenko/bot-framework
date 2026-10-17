package works.lysenko.base.logger.processor;

import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.log._Queue;
import works.lysenko.util.data.type.LogRecord;

import java.util.Comparator;
import java.util.PriorityQueue;

import static works.lysenko.util.func.type.Objects.isNotNull;

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
    public synchronized final void add(final LogRecord logRecord) {

        if (isNotNull(logRecord)) queue.add(logRecord);
    }

    @Override
    public synchronized final boolean isEmpty() {

        return queue.isEmpty();
    }

    @Override
    public synchronized final _LogRecord poll() {

        return queue.poll();
    }
}
