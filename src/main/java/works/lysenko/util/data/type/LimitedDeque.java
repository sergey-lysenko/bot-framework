package works.lysenko.util.data.type;

import works.lysenko.util.apis._LimitedDeque;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.spec.Symbols.L;
import static works.lysenko.util.spec.Symbols.Q;

/**
 * A bounded implementation of a double-ended queue that enforces a maximum size limit.
 * When the size exceeds the specified limit, the oldest element is automatically removed.
 *
 * @param <T> the type of elements stored in this deque
 */
public class LimitedDeque<T> implements _LimitedDeque<T> {

    private final int limit;
    private final ArrayDeque<T> queue;

    /**
     * Constructs a new LimitedDeque with the specified maximum size limit.
     *
     * @param limit the maximum number of elements that the deque can hold.
     *              If the deque's size exceeds this limit, the oldest elements will be removed.
     */
    public LimitedDeque(final int limit) {

        this.limit = limit;
        queue = new ArrayDeque<>(0);
    }

    @Override
    public final void add(final T element) {

        if (queue.size() >= limit) {
            queue.poll();
        }
        queue.add(element);
    }

    @Override
    public final Iterator<T> descendingIterator() {

        return queue.descendingIterator();
    }

    @Override
    public final int size() {

        return queue.size();
    }

    @Override
    public final String toString() {

        return a(List.of(kv(L, limit), kv(Q, queue.toString())), COMMA_SPACE);
    }
}