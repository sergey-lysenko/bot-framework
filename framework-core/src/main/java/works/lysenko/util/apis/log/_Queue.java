package works.lysenko.util.apis.log;

import works.lysenko.util.data.type.LogRecord;

/**
 * Interface for a queue data structure that holds LogRecord objects.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Queue {

    /**
     * Adds a LogRecord to the queue.
     *
     * @param logRecord the LogRecord to be added to the queue
     */
    void add(LogRecord logRecord);

    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Retrieves and removes the next LogRecord from the queue.
     *
     * @return the next LogRecord to be processed from the queue
     */
    _LogRecord poll();
}
