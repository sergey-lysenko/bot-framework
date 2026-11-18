package works.lysenko.util.func.data;

import java.text.DecimalFormat;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols._PRCNT_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * Additional Percents routines
 */
public record Percents() {

    /**
     * Retrieves an integer value representing a percentage from a given string.
     *
     * @param value The input string containing a numerical value followed by a '%' symbol
     * @return An Integer value representing the percentage extracted from the input string,
     * or null if the string format is invalid
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "StaticMethodOnlyUsedInOneClass"})
    public static Integer intPercentsFromString(final String value) {

        final String source = value.replace(s(_SPACE_), EMPTY);
        final int length = source.length();
        if (1 < length)
            if (_PRCNT_ == source.charAt(length - 1))
                return i(source.substring(0, length - 1));
        return null;
    }

    /**
     * Calculates percentage and passes its String representation
     *
     * @param part  fraction
     * @param total amount
     * @return xx.xx%
     */
    public static String percentString(final int part, final int total) {

        return percentString(part, (double) total);
    }

    /**
     * Calculates percentage and passes its String representation
     *
     * @param part  fraction
     * @param total amount
     * @return xx.xxxx%
     */
    public static String percentString(final double part, final double total) {

        // TODO: add variable precision
        return s(new DecimalFormat("#.#####").format(((part) / total) * 100), _PRCNT_);
    }
}
