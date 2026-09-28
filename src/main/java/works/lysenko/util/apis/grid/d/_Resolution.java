package works.lysenko.util.apis.grid.d;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.enums.Shape;
import works.lysenko.util.func.logs.Trace;
import works.lysenko.util.grid.record.gsrc.Resolution;

import static java.lang.Math.max;
import static java.util.Objects.isNull;
import static org.apache.commons.math3.util.FastMath.ceil;
import static org.apache.commons.math3.util.FastMath.floor;
import static org.apache.commons.math3.util.FastMath.sqrt;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Shape.ELLIPCE;
import static works.lysenko.util.data.enums.Shape.RECTANGLE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.logs.trace.Data.traceable;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.lang.word.P.PARSE;
import static works.lysenko.util.lang.word.S.SIZE;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols.*;

/**
 * The _Resolution interface provides methods to calculate and handle resolution data. It includes computations
 * for determining dimensions, interpreting raw size strings, and logging the derived dimensions. It supports
 * different shapes and enables calculation of various resolution characteristics such as the filling fraction,
 * ratio, size, and textual representation of a resolution.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Resolution {

    /**
     * Constants representing the double precision floating-point value 2.0.
     * This value is used as a standard multiplier or baseline in calculations
     * within the _Resolution class where a factor of 2 is required.
     */
    double DOUBLE_TWO = 2.0;

    /**
     * Computes the dimension based on the given integer input.
     *
     * @param i An integer input used to calculate the dimension.
     * @return The calculated dimension, which is (i * 2) + 1.
     */
    static int getDimension(final int i) {

        return (i << 1) + 1;
    }

    /**
     * Calculates the resolution based on the provided raw size string, fill fraction, shape, and other parameters.
     *
     * @param raw       The raw text containing the size information.
     * @param fill      The fraction to fill in the resolution.
     * @param shape     The shape of the resolution, specified as an instance of the Shape enum.
     * @param silent    A1 boolean flag indicating whether to suppress logging output.
     * @param separator The character or string used to separate the components of the raw size string.
     * @return A1 new Resolution object representing the calculated resolution with specified parameters.
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    static Resolution getResolution(final String raw, final Fraction fill, final Shape shape, final boolean silent,
                                    final String separator) {

        final String[] sizes;
        int x;
        int y;
        sizes = StringUtils.split(raw, separator); // Not to bother with masking of *
        x = (int) (ceil((Integer.parseInt(sizes[ZERO]) - 1) / DOUBLE_TWO));
        y = (int) (ceil((Integer.parseInt(sizes[ONE]) - 1) / DOUBLE_TWO));
        x = max(1, x);
        y = max(1, y);
        if (!silent) logRawToSize(raw, x, y);
        return new Resolution(x, y, fill, shape);
    }

    /**
     * Calculates the resolution based on the provided raw size string, fill fraction, shape, and silent flag.
     *
     * @param raw    The raw text containing the size information that may include a fractional or double representation.
     * @param fill   The fraction used to determine the fill amount in the calculated resolution.
     * @param shape  The shape of the resolution, specified as an instance of the Shape enum.
     * @param silent A1 boolean flag indicating whether to suppress logging output during the calculation.
     * @return A1 new Resolution object representing the calculated resolution based on the specified parameters,
     * or null if the input cannot be parsed into a valid numeric value.
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "NumericCastThatLosesPrecision", "BadOddness"})
    static Resolution getResolution(final String raw, final Fraction fill, final Shape shape, final boolean silent) {

        int y;
        int x;
        final int amount;
        try {
            amount = (int) ceil(fr(raw).doubleValue());
        } catch (final NumberFormatException e) {
            logEvent(S0, b(c(UNABLE), TO, PARSE, VALUE, OF, q(SIZE)));
            return null;
        }
        final int draft = (int) ceil(sqrt(amount)); // draft value
        final int base = (1 == draft % 2) ? draft : draft + 1; // base size (must be odd number)
        final int extra = (base * base) - amount; // extra above requested amount
        final float rate = ((float) extra) / base; // float amount of extra rows
        final int reduction = ((int) floor(rate)) / 2; // real amount of steps which can be removed
        Trace.log(traceable(silent, draft, base, extra, rate, reduction));
        x = (int) (ceil((base - 1) / DOUBLE_TWO));
        y = x - reduction;
        x = max(1, x);
        y = max(1, y);
        if (!silent) logRawToSize(raw, x, y);
        return new Resolution(x, y, fill, shape);
    }

    /**
     * Logs the raw text, number of steps on the x-axis, and number of steps on the y-axis to the debug output.
     *
     * @param rawText The raw text to be logged.
     * @param x       The number of steps on the x-axis.
     * @param y       The number of steps on the y-axis.
     */
    static void logRawToSize(final String rawText, final int x, final int y) {

        final int x1 = getDimension(x);
        final int y1 = getDimension(y);
        Trace.log(traceable(rawText, x, y, x1, y1));
    }

    /**
     * Reads the size from the given raw text.
     *
     * @param raw The raw text containing the size information.
     * @return A1 java.awt.Point object representing the size, or null if the size cannot be determined.
     */
    static Resolution readResolution(final String raw) {

        return readResolution(raw, false);
    }

    /**
     * Reads the density from the given raw text.
     *
     * @param raw    The raw text containing the density information.
     * @param silent Flag indicating whether to log debug information or not.
     * @return The Density object representing the density, or null if the density cannot be determined.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    static Resolution readResolution(final String raw, final boolean silent) {

        if (isNull(raw)) return null;

        String source = raw;
        Fraction fill = Fraction.ONE;
        Shape shape = RECTANGLE;

        if (source.contains(s(_AT_SGN))) {
            final String[] parts = source.split(s(_AT_SGN));
            source = parts[ZERO];
            if (parts[ONE].startsWith(s(R))) {
                shape = ELLIPCE;
                fill = fr(parts[ONE].substring(1));
            } else fill = fr(parts[ONE]);
        }

        String separator = null;
        if (source.contains(s(_ASTRS_))) separator = s(_ASTRS_);
        if (source.contains(s(X))) separator = s(X);
        if (null != separator) return getResolution(source, fill, shape, silent, separator);
        else return getResolution(source, fill, shape, silent);
    }

    /**
     * Fills and retrieves a Fraction associated with the Resolution.
     *
     * @return A1 Fraction instance that represents the filled value.
     */
    Fraction fill();

    /**
     * Retrieves the ratio associated with the Resolution.
     *
     * @return The ratio as a float value.
     */
    float ratio();

    /**
     * Provides the shape of the current instance.
     *
     * @return The shape as a Shape enum, or null if the shape is not defined.
     */
    default Shape shape() {

        return null;
    }

    /**
     * Retrieves the size value associated with the Resolution.
     *
     * @return the size as an Integer, or null if the size is not defined
     */
    Integer size();

    /**
     * Retrieves the text associated with the Resolution.
     *
     * @return the text as a String
     */
    String text();

    /**
     * Retrieves the x-coordinate value associated with the Resolution.
     *
     * @return The x-coordinate value as an integer.
     */
    default int x() {

        return 0;
    }

    /**
     * Provides the y-coordinate value associated with the instance.
     *
     * @return The y-coordinate value as an integer.
     */
    default int y() {

        return 0;
    }
}
