package works.lysenko.util.func.type;

import java.util.Arrays;

/**
 * Additional Chars routines
 */
@SuppressWarnings({"ArrayLengthInLoopCondition", "ImplicitNumericConversion", "lossy-conversions", "ClassIndependentOfModule"})
public record Chars() {

    /**
     * @param bs array of booleans
     * @return character
     */
    public static char booleansToChar(final boolean... bs) {

        char c = 0;
        for (int i = 0; i < bs.length; i++) if (bs[i]) c |= 1 << i;
        return c;
    }

    /**
     * @param c character
     * @return array of booleans
     */

    @SuppressWarnings({"CharUsedInArithmeticContext", "ImplicitNumericConversion", "unused"})
    public static boolean[] charToBooleans(final char c) {

        final boolean[] bs = new boolean[Character.SIZE];
        for (int i = 0; Character.SIZE > i; i++) bs[i] = 1 == ((c >> i) & 1);
        return bs;
    }

    /**
     * Converts a character to a String representation of its corresponding boolean array.
     *
     * @param c the character to convert
     * @return a String containing the boolean array representation of the character
     */
    @SuppressWarnings("unused")
    public static String charToBooleansString(final char c) {

        return Arrays.toString(charToBooleans(c));
    }
}
