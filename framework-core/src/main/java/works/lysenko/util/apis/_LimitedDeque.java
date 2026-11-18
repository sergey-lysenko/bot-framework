package works.lysenko.util.apis;

import java.util.Iterator;

/**
 * An interface representing a limited deque, a constrained double-ended
 * queue that maintains a maximum size limit. When the maximum size is
 * exceeded, the oldest element is removed to make space for new elements.
 *
 * @param <T> the type of elements held in this deque
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _LimitedDeque<T> {

    /**
     * Adds the specified element to the limited deque. If the maximum size limit of the deque is exceeded,
     * the oldest element is removed to make room for the new element.
     *
     * @param element the element to be added to the deque
     */
    void add(T element);

    /**
     * Returns an iterator over the elements in this deque in reverse order.
     *
     * @return an iterator that traverses the elements of the deque from the tail to the head
     */
    Iterator<T> descendingIterator();

    /**
     * Returns the number of elements currently stored in the deque.
     *
     * @return the number of elements in the deque
     */
    int size();
}
