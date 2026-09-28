package works.lysenko.util.data.strs;

import works.lysenko.util.data.records.TimeParts;

import java.util.ArrayList;
import java.util.List;

import static works.lysenko.util.chrs.__.MS;
import static works.lysenko.util.chrs.____.LESS;
import static works.lysenko.util.chrs.____.THAN;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.M.MILLISECOND;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Represents a TimeParts class that provides utility methods for converting
 * time in milliseconds to a string representation.
 */
@SuppressWarnings({"ConstantDeclaredInInterface", "InterfaceNeverImplemented"})
public interface Time {

    private static void addTimePartIfNonZero(final long timeValue, final String unit, final List<? super String> items) {

        if (ZERO < timeValue) items.add(b(s(timeValue), s(unit)));
    }

    private static void calculate(final long milliseconds, final List<? super String> items) {

        final TimeParts timeParts = TimeParts.get(milliseconds);
        addTimePartIfNonZero(timeParts.days(), s(D), items);
        addTimePartIfNonZero(timeParts.hours(), s(H), items);
        addTimePartIfNonZero(timeParts.minutes(), s(M), items);
        addTimePartIfNonZero(timeParts.seconds(), s(S), items);
        addTimePartIfNonZero(timeParts.ms(), s(MS), items);
    }

    /**
     * Converts the given time in milliseconds to a human-readable string representation.
     *
     * @param milliseconds the time in milliseconds to convert
     * @return the human-readable string representation of the time
     */
    @SuppressWarnings({"ZeroLengthArrayAllocation", "MethodWithMultipleReturnPoints"})
    static String t(final long milliseconds) {

        final List<String> items = new ArrayList<>(0);

        if (0 > milliseconds) return null;
        if (0 == milliseconds) items.add(b(LESS, THAN, MILLISECOND));
        else calculate(milliseconds, items);
        return b(items.toArray(new String[0]));
    }
}
