package works.lysenko.util.func.imgs;

import works.lysenko.util.apis.grid.t._BitPlane;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.records.Size;

import java.awt.Point;
import java.util.BitSet;

import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.func.type.Objects.isAnyNull;

/**
 * Represents a buffer of bits in a two-dimensional space.
 * This class provides methods to set and retrieve individual bits at specific coordinates.
 * This class implements the _BitBuffer interface.
 */
@SuppressWarnings({"MethodWithMultipleLoops", "BooleanParameter", "DataFlowIssue", "MethodWithMultipleReturnPoints"})
public class BitPlane implements _BitPlane {

    private final BitSet[] buffer;
    private final IntegerRange rX;
    private final IntegerRange rY;

    /**
     * Constructs a new BitBuffer with the specified size.
     *
     * @param size The Size object representing the width and height dimensions of the buffer
     */
    @SuppressWarnings("ObjectAllocationInLoop")
    BitPlane(final Size size) {

        buffer = new BitSet[size.height()];
        for (int i = 0; i < size.height(); i++) buffer[i] = new BitSet(size.width());
        rX = new IntegerRange(0, size.width() - 1);
        rY = new IntegerRange(0, size.height() - 1);
    }

    public final Point farBound() {

        if (isAnyNull(getMinSetX(), getMinSetY())) return null;
        return new Point(getMaxSetX(), getMaxSetY());
    }

    @Override
    public final Boolean get(final int x, final int y) {

        if (rX.includes(x) && rY.includes(y)) return buffer[y].get(x);
        return null;
    }

    @Override
    public final Integer getMaxSetX() {

        Integer result = null;
        for (int y = rY.min(); y <= rY.max(); y++)
            for (int x = rX.min(); x <= rX.max(); x++)
                if (get(x, y) && (x > n(0, result))) result = x;
        return result;
    }

    @Override
    public final Integer getMaxSetY() {

        Integer result = null;
        for (int y = rY.min(); y <= rY.max(); y++)
            for (int x = rX.min(); x <= rX.max(); x++)
                if (get(x, y) && (y > n(0, result))) result = y;
        return result;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public final Integer getMinSetX() {

        Integer result = null;
        for (int y = rY.max(); 0 <= y; y--)
            for (int x = rX.max(); 0 <= x; x--)
                if (get(x, y) && (x < n(rX.max(), result))) result = x;
        return result;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public final Integer getMinSetY() {

        Integer result = null;
        for (int y = rY.max(); 0 <= y; y--)
            for (int x = rX.max(); 0 <= x; x--)
                if (get(x, y) && (y < n(rY.max(), result))) result = y;
        return result;
    }

    public final Point nearBound() {

        if (isAnyNull(getMinSetX(), getMinSetY())) return null;
        return new Point(getMinSetX(), getMinSetY());
    }

    public final void set(final int x, final int y) {

        if (rX.includes(x) && rY.includes(y)) buffer[y].set(x);
    }

    public final void set(final int x, final int y, final boolean value) {

        if (rX.includes(x) && rY.includes(y)) buffer[y].set(x, value);
    }
}
