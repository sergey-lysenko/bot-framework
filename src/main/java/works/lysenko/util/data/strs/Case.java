package works.lysenko.util.data.strs;

import works.lysenko.util.func.core.Assertions;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.C.CASING;
import static works.lysenko.util.lang.word.C.CHANGE;
import static works.lysenko.util.lang.word.D.DURING;
import static works.lysenko.util.lang.word.L.LOWER;
import static works.lysenko.util.lang.word.U.UPPER;

/**
 * The Case interface provides methods for various types of string casing.
 */
@SuppressWarnings("InterfaceNeverImplemented")
public interface Case {

    /**
     * Capitalization of string representation of given object
     *
     * @param s string to capitalize
     * @return capitalized string
     */
    @SuppressWarnings("StandardVariableNames")
    static String c(final Object s) {

        final String original = s(s);
        final String capitalized = capitalize(original);
        Assertions.assertNotEqualsSilent(original, capitalized, "No change during Capitalization"); //NON-NLS
        return capitalized;
    }

    /**
     * Lower case of String representation of an Object with optional change assertion
     *
     * @param o to lowerCase
     * @return lowerCased string
     */
    static String l(final Object o) {

        return l(o, false);
    }

    /**
     * Lower case of String representation of an Object with optional change assertion
     *
     * @param o               to lowerCase
     * @param noChangeAllowed if true, unchanged string will fail assertion
     * @return lowerCased string
     */
    static String l(final Object o, final boolean noChangeAllowed) {

        final String original = s(o);
        final String lowerCased = lowerCase(original);
        if (!noChangeAllowed)
            Assertions.assertNotEqualsSilent(original, lowerCased, b(c(NO), CHANGE, DURING, s(LOWER, c(CASING))));
        return lowerCased;
    }

    /**
     * Upper case of String representation of an Object
     *
     * @param o to upperCase
     * @return upperCased string
     */
    static String u(final Object o) {

        final String original = s(o);
        final String upperCased = upperCase(original);
        Assertions.assertNotEqualsSilent(original, upperCased, b(c(NO), CHANGE, DURING, s(UPPER, c(CASING))));
        return upperCased;
    }
}
