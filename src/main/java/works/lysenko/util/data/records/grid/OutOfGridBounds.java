package works.lysenko.util.data.records.grid;

import works.lysenko.util.data.records.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs._____.THERE;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.lang.word.B.BOUNDS;
import static works.lysenko.util.lang.word.E.ELEMENT;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Represents a collection of OutOfMatrixBoundsRecord objects.
 * Maintains a list of OutOfGridBoundsRecords and provides methods to manipulate and summarize the data.
 */
@SuppressWarnings("PublicMethodNotExposedInInterface")
public class OutOfGridBounds {

    private final Collection<OutOfMatrixBoundsRecord> list = new ArrayList<>(0);

    /**
     * Adds a new OutOfMatrixBoundsRecord to the list with the specified location and integer coordinates.
     *
     * @param location the Location representing the point
     * @param iX       the integer x-coordinate value
     * @param iY       the integer y-coordinate value
     */
    public final void add(final Location location, final int iX, final int iY) {

        list.add(new OutOfMatrixBoundsRecord(location, iX, iY));
    }

    /**
     * Calculates and logs the summary of the OutOfMatrixBoundsRecord list.
     * Determines the minimum and maximum values for the x and y coordinates both in integer and float separately.
     * Logs the summary information including the size of the list, min and max x and y values for both integer and float.
     * Throws NoSuchElementException if any required element is not present.
     */
    @SuppressWarnings("NewExceptionWithoutArguments")
    public final void summary() {

        final int iXmin = list.stream().mapToInt(OutOfMatrixBoundsRecord::iX).min().orElseThrow(NoSuchElementException::new);
        final int iXmax = list.stream().mapToInt(OutOfMatrixBoundsRecord::iX).max().orElseThrow(NoSuchElementException::new);
        final int iYmin = list.stream().mapToInt(OutOfMatrixBoundsRecord::iY).min().orElseThrow(NoSuchElementException::new);
        final int iYmax = list.stream().mapToInt(OutOfMatrixBoundsRecord::iY).max().orElseThrow(NoSuchElementException::new);

        // Min and Max for Location's x and y
        final float xMin =
                list.stream().map(record -> record.location().x()).min(Float::compare).orElseThrow(NoSuchElementException::new);
        final float xMax =
                list.stream().map(record -> record.location().x()).max(Float::compare).orElseThrow(NoSuchElementException::new);
        final float yMin =
                list.stream().map(record -> record.location().y()).min(Float::compare).orElseThrow(NoSuchElementException::new);
        final float yMax =
                list.stream().map(record -> record.location().y()).max(Float::compare).orElseThrow(NoSuchElementException::new);
        logEvent(S0,
                b(
                        c(THERE),
                        (list.size() == ONE) ? IS : ARE,
                        s(s1(list.size(), b(s(OUT, c(OF), c(BOUNDS)), ELEMENT)), _COLON_),
                        s(kv(s(X, c(MIN)), xMin)),
                        s(kv(s(Y, c(MIN)), yMin)),
                        s(kv(s(I, c(X), MIN), iXmin)),
                        s(kv(s(I, c(Y), MIN), iYmin)),
                        s(kv(s(X, c(MAX)), xMax)),
                        s(kv(s(Y, c(MAX)), yMax)),
                        s(kv(s(I, c(X), MAX), iXmax)),
                        s(kv(s(I, c(Y), MAX), iYmax))
                )
        );
    }

    /**
     * Checks if the list is not empty.
     *
     * @return true if the list is not empty, false otherwise
     */
    public final boolean isNotEmpty() {

        return !list.isEmpty();
    }
}
