package works.lysenko.util.func.type;

import org.apache.commons.math3.fraction.Fraction;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * The Doubles class provides utility methods for converting Fraction objects to
 * their String representations with a customisable number of decimal places.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public record Doubles() {

    /**
     * Converts a Fraction object to its String representation with a specific number of decimal places.
     *
     * @param value    the Fraction object to be converted
     * @param decimals the maximum number of decimal places to include in the String representation
     * @return a String representation of the Fraction object with the specified number of decimal places
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String doubleToString(final Fraction value, final int decimals) {

        final DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(decimals);
        return df.format(value.doubleValue());
    }

    /**
     * Parses a string input and converts it to a double value using NumberFormat with the Locale.FRANCE setting.
     *
     * @param input the string input to be parsed into a double
     * @return the parsed double value from the input string
     * @throws ParseException if the input string cannot be parsed into a valid number
     */
    public static double parseDouble(final String input) throws ParseException {

        final NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        return format.parse(input).doubleValue();
    }
}
