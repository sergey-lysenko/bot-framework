package works.lysenko.util.apis.exception;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import works.lysenko.util.apis.exception.unchecked.ScenarioRuntimeException;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.data.records.test.Triplet;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static works.lysenko.util.chrs.____.NAME;
import static works.lysenko.util.data.enums.Ansi.bb;
import static works.lysenko.util.data.enums.Ansi.rb;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.A.ANTEPENULTIMATE;
import static works.lysenko.util.lang.word.C.CAUSE;
import static works.lysenko.util.lang.word.M.MESSAGE;
import static works.lysenko.util.lang.word.P.PENULTIMATE;
import static works.lysenko.util.lang.word.U.ULTIMATE;
import static works.lysenko.util.lang.word.U.UNDEFINED;
import static works.lysenko.util.spec.Numbers.THREE;
import static works.lysenko.util.spec.Symbols._LFD_;

/**
 * The Routines class provides static utility methods for common routines.
 */
public record Routines() {

    /**
     * Returns a formatted description of the given exception.
     *
     * @param throwable the exception to describe
     * @return a string containing the class name, message, and cause of the exception
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private static String describe(final Throwable throwable) {

        if (isNull(throwable)) return UNDEFINED;
        final List<KeyValue<?, ?>> list = new ArrayList<>(0);
        list.add(kv(bb(c(NAME)), throwable.getClass().getName()));
        list.add(kv(bb(c(MESSAGE)), throwable.getMessage()));
        if (isNotNull(throwable.getCause()))
            list.add(kv(bb(c(CAUSE)), s(throwable.getCause())));
        return wideMapPrint(list);
    }

    /**
     * Returns a formatted description of the given exception triplet.
     *
     * @param triplet the exception triplet to describe
     * @return a string containing the description of the exception triplet
     */
    public static String describeTriplet(final Triplet triplet) {

        final List<KeyValue<?, ?>> entries = new ArrayList<>(0);
        if (isNotNull(triplet.ultimate())) entries.add(kv(rb(c(ULTIMATE)), describe(triplet.ultimate())));
        if (isNotNull(triplet.penultimate())) entries.add(kv(rb(c(PENULTIMATE)), describe(triplet.penultimate())));
        if (isNotNull(triplet.antepenultimate())) entries.add(kv(rb(c(ANTEPENULTIMATE)), describe(triplet.antepenultimate())));
        return wideMapPrint(entries);
    }

    /**
     * Extracts a Triplet of Throwables from the given Throwable.
     *
     * @param throwable the Throwable from which to extract the Triplet
     * @return a Triplet containing the ultimate, penultimate, and antepenultimate Throwables
     */
    public static Triplet extractTriplet(final Throwable throwable) {

        final CircularFifoQueue<Throwable> queue = fillQueueWithThrowables(throwable);

        return getTriplet(queue, queue.size());
    }

    /**
     * Fills a CircularFifoQueue with Throwables by iterating through the cause chain until a Throwable of type
     * ScenarioRuntimeException is encountered.
     *
     * @param throwable the Throwable from which to start filling the queue
     * @return the CircularFifoQueue filled with Throwables
     */
    @SuppressWarnings("UseOfConcreteClass")
    private static CircularFifoQueue<Throwable> fillQueueWithThrowables(final Throwable throwable) {

        final CircularFifoQueue<Throwable> queue = new CircularFifoQueue<>(THREE);
        if (isNotNull(throwable)) {
            Throwable the = throwable;
            queue.add(the);
            while (the instanceof ScenarioRuntimeException) {
                the = the.getCause();
                queue.add(the);
            }
        }
        return queue;
    }

    /**
     * Retrieves the throwable at the specified index from a CircularFifoQueue of throwables,
     * or returns null if the index exceeds the queue's size.
     *
     * @param queue the CircularFifoQueue of throwables
     * @param index the index of the throwable to retrieve
     * @return the throwable at the specified index, or null if index is out of bounds
     */
    private static Throwable getThrowableIfExists(final CircularFifoQueue<? extends Throwable> queue, final int index) {

        return (queue.size() > index) ? queue.get(index) : null;
    }

    /**
     * Retrieves the ultimate, penultimate, and antepenultimate throwables based on the provided size.
     *
     * @param queue the CircularFifoQueue of throwables
     * @param size  the size of the queue
     * @return the Triplet of throwables
     */
    private static Triplet getTriplet(final CircularFifoQueue<? extends Throwable> queue, final int size) {

        final Throwable ultimate = getThrowableIfExists(queue, size - 1);
        final Throwable penultimate = 1 < size ? getThrowableIfExists(queue, size - 2) : null;
        final Throwable antepenultimate = 2 < size ? getThrowableIfExists(queue, size - 3) : null;

        return new Triplet(ultimate, penultimate, antepenultimate);
    }

    /**
     * Concatenates the key-value pairs from a list of map entries into a string representation.
     *
     * @param entries the list of map entries to concatenate
     * @return the concatenated string representation of the key-value pairs
     */
    private static String wideMapPrint(final List<KeyValue<?, ?>> entries) {

        return a(entries, s(_LFD_, _LFD_));
    }
}