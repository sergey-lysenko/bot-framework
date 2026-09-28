package works.lysenko.util.grid.expected;

import works.lysenko.util.apis.grid.r._Palette;
import works.lysenko.util.data.range.IntegerRange;

import static java.util.Objects.isNull;

/**
 * Represents a palette of colors.
 */
public class Palette implements _Palette {

    private final IntegerRange intMinMax;

    /**
     * Represents a palette of colors.
     *
     * @param source The source string used to initialize the palette.
     */
    public Palette(final String source) {

        if (isNull(source)) intMinMax = null;
        else intMinMax = new IntegerRange(source, 0, Integer.MAX_VALUE);
    }

    /**
     * Retrieves the IntMinMax object containing the minimum and maximum values.
     *
     * @return the IntMinMax object
     */
    public final IntegerRange get() {

        return intMinMax;
    }
}
