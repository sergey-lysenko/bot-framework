package works.lysenko.util.data.enums;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.records.RGB24;
import works.lysenko.util.grid.record.rgbc.HSB;

import static org.apache.commons.math3.fraction.Fraction.*;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.func.type.Fractions.ONE_TENTH;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.grid.record.rgbc.HSB.hsb;

/**
 * Represents ANSI escape sequences for different text colors and formatting options.
 */
@SuppressWarnings({"unused", "EnumClass", "MissingJavadoc"})
public enum Ansi {
    RESET("\033[0m"), // NON-NLS

    BLACK("\033[0;30m"), // NON-NLS
    RED("\033[0;31m"), // NON-NLS
    GREEN("\033[0;32m"), // NON-NLS
    YELLOW("\033[0;33m"), // NON-NLS
    BLUE("\033[0;34m"), // NON-NLS
    MAGENTA("\033[0;35m"), // NON-NLS
    CYAN("\033[0;36m"), // NON-NLS
    WHITE("\033[0;37m"), // NON-NLS

    BLACK_BOLD("\033[1;30m"), //NON-NLS
    RED_BOLD("\033[1;31m"), //NON-NLS
    RED_CROSSED("\033[9;31m"), //NON-NLS
    GREEN_BOLD("\033[1;32m"), //NON-NLS
    YELLOW_BOLD("\033[1;33m"), //NON-NLS
    BLUE_BOLD("\033[1;34m"), //NON-NLS
    MAGENTA_BOLD("\033[1;35m"), //NON-NLS
    CYAN_BOLD("\033[1;36m"), //NON-NLS
    WHITE_BOLD("\033[1;37m"), //NON-NLS

    BLACK_UNDERLINED("\033[4;30m"), //NON-NLS
    RED_UNDERLINED("\033[4;31m"), //NON-NLS
    GREEN_UNDERLINED("\033[4;32m"), //NON-NLS
    YELLOW_UNDERLINED("\033[4;33m"), //NON-NLS
    BLUE_UNDERLINED("\033[4;34m"), //NON-NLS
    MAGENTA_UNDERLINED("\033[4;35m"), //NON-NLS
    CYAN_UNDERLINED("\033[4;36m"), //NON-NLS
    WHITE_UNDERLINED("\033[4;37m"), //NON-NLS

    BLACK_BACKGROUND("\033[40m"), //NON-NLS
    RED_BACKGROUND("\033[41m"), //NON-NLS
    GREEN_BACKGROUND("\033[42m"), //NON-NLS
    YELLOW_BACKGROUND("\033[43m"), //NON-NLS
    BLUE_BACKGROUND("\033[44m"), //NON-NLS
    MAGENTA_BACKGROUND("\033[45m"), //NON-NLS
    CYAN_BACKGROUND("\033[46m"), //NON-NLS
    WHITE_BACKGROUND("\033[47m"), //NON-NLS

    BLACK_BRIGHT("\033[0;90m"), //NON-NLS
    RED_BRIGHT("\033[0;91m"), //NON-NLS
    GREEN_BRIGHT("\033[0;92m"), //NON-NLS
    YELLOW_BRIGHT("\033[0;93m"), //NON-NLS
    BLUE_BRIGHT("\033[0;94m"), //NON-NLS
    MAGENTA_BRIGHT("\033[0;95m"), //NON-NLS
    CYAN_BRIGHT("\033[0;96m"), //NON-NLS
    WHITE_BRIGHT("\033[0;97m"), //NON-NLS

    BLACK_BOLD_BRIGHT("\033[1;90m"), //NON-NLS
    RED_BOLD_BRIGHT("\033[1;91m"), //NON-NLS
    GREEN_BOLD_BRIGHT("\033[1;92m"), //NON-NLS
    YELLOW_BOLD_BRIGHT("\033[1;93m"), //NON-NLS
    BLUE_BOLD_BRIGHT("\033[1;94m"), //NON-NLS
    MAGENTA_BOLD_BRIGHT("\033[1;95m"),  //NON-NLS
    CYAN_BOLD_BRIGHT("\033[1;96m"), //NON-NLS
    WHITE_BOLD_BRIGHT("\033[1;97m"), //NON-NLS

    BLACK_BACKGROUND_BRIGHT("\033[0;100m"), //NON-NLS
    RED_BACKGROUND_BRIGHT("\033[0;101m"), //NON-NLS
    GREEN_BACKGROUND_BRIGHT("\033[0;102m"), //NON-NLS
    YELLOW_BACKGROUND_BRIGHT("\033[0;103m"), //NON-NLS
    BLUE_BACKGROUND_BRIGHT("\033[0;104m"), //NON-NLS
    MAGENTA_BACKGROUND_BRIGHT("\033[0;105m"), //NON-NLS
    CYAN_BACKGROUND_BRIGHT("\033[0;106m"), //NON-NLS
    WHITE_BACKGROUND_BRIGHT("\033[0;107m"); //NON-NLS

    private final String code;

    Ansi(final String code) {

        this.code = code;
    }

    /**
     * Execute {@link Ansi#ansi(String, Ansi)} based on content of the provided string.
     * Contents - to - color mapping defined in {@link Severity}
     *
     * @param s string to be colored
     * @return same string with added ANSI coloring escape sequences
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static String ansi(final String s) {

        for (final Severity e : Severity.values())
            if (s.contains(e.type().getString())) return ansi(s, e.color());
        return s;
    }

    /**
     * Wrap a sting into ANSI escape sequence of defined color
     *
     * @param s    string to be colored
     * @param ansi color escape sequence to be applied
     * @return string surrounded by defined color sequence and RESET
     */
    public static String ansi(final String s, final Ansi ansi) {

        return null == ansi ? s : s(ansi.code(), s, RESET.code());
    }

    /**
     * Applies blue bold color to the string representation of the given object.
     *
     * @param o the object to be colorized
     * @return the colorized string
     */
    @SuppressWarnings("QuestionableName")
    public static String bb(final Object o) {

        return ansi(sn(o), BLUE_BOLD);
    }

    /**
     * Applies white bold ANSI styling to the string representation of the given object.
     *
     * @param o the object to be converted to a string and styled
     * @return a string representing the object with white bold ANSI styling applied
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String wb(final Object o) {

        return ansi(sn(o), WHITE_BOLD);
    }

    /**
     * Applies colorization to the given object based on the provided condition.
     *
     * @param inCase boolean flag indicating whether colorization should be applied
     * @param o      the object to be colorized
     * @return the colorized object if inCase is true, otherwise the original object
     */
    @SuppressWarnings("QuestionableName")
    public static String bb(final boolean inCase, final Object o) {

        return colorize(inCase, BLUE_BOLD, o);
    }

    /**
     * Calculates the brightness of the specified object based on the given brightness fraction.
     *
     * @param o          the Object for which brightness is to be calculated
     * @param brightness the brightness fraction to be used for calculation
     * @return a String indicating the brightness of the specified object
     */
    public static String byBrightness(final Object o, final Fraction brightness) {

        return indicateBrightness(o, hsb(ZERO, ONE_HALF, brightness));
    }

    /**
     * Returns a string representation of an object with the specified hue value in HSB colour space.
     *
     * @param o   the object to be represented with the specified hue value
     * @param hue the hue value to set in the HSB colour space
     * @return a string representation of the object with the specified hue value in HSB colour space
     */
    public static String byHue(final Object o, final Fraction hue) {

        return indicateHue(o, hsb(hue, ONE_HALF, ONE_HALF));
    }

    /**
     * Converts the given RGB24 color to an ANSI color code string based on its hue, saturation, and brightness values.
     *
     * @param o   the object to apply the ANSI color code to
     * @param rgb the RGB24 color to convert
     * @return the ANSI color code string
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static String byRGB(final Object o, final RGB24 rgb) {

        final HSB hsb = HSB.convertRGBtoHSB(rgb);

        if (isNotNull(hsb)) {
            if (hsb.saturation().doubleValue() < ONE_TENTH.doubleValue())
                return indicateBrightness(o, hsb);
            else
                return indicateHue(o, hsb);
        }
        return null;
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private static String colorize(final boolean inCase, final Ansi ansi, final Object o) {

        if (inCase) return ansi(sn(o), ansi);
        return sn(o);
    }

    /**
     * Converts the given object to a string and applies green bold colour formatting.
     *
     * @param o the object to convert and colorize
     * @return the converted string with green bold colour formatting
     */
    public static String gb(final Object o) {

        return ansi(sn(o), GREEN_BOLD);
    }

    /**
     * Applies styling to the string representation of the given object based on the specified condition.
     *
     * @param inCase Specifies whether the formatting for the given object should be applied.
     * @param o      The object whose string representation is to be styled.
     * @return A1 styled string representation of the given object.
     */
    public static String gb(final boolean inCase, final Object o) {

        return colorize(inCase, GREEN_BOLD, sn(o));
    }

    /**
     * Converts the given object to a string and applies a gray color to it.
     *
     * @param o the object to be converted and colored gray
     * @return the string representation of the object with gray color applied
     */
    public static String gray(final Object o) {

        return ansi(sn(o), BLACK_BRIGHT);
    }

    public static String gray(final boolean inCase, final Object o) {

        return colorize(inCase, BLACK_BRIGHT, sn(o));
    }

    @SuppressWarnings({"IfStatementWithTooManyBranches", "MethodWithMultipleReturnPoints"})
    private static String indicateBrightness(final Object o, final HSB hsb) {

        if (THREE_QUARTERS.doubleValue() < hsb.brightness().doubleValue()) {
            return ansi(s(o), WHITE_BRIGHT);
        } else if (ONE_HALF.doubleValue() < hsb.brightness().doubleValue()) {
            return ansi(s(o), WHITE);
        } else if (ONE_QUARTER.doubleValue() < hsb.brightness().doubleValue()) {
            return ansi(s(o), BLACK_BRIGHT);
        } else {
            return ansi(s(o), BLACK);
        }
    }

    /**
     * Returns a colored version of the given object based on the provided HSB values.
     *
     * @param o   The object for which the color will be applied
     * @param hsb The HSB values used to determine the color
     * @return The colored string representation of the object
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "OverlyComplexMethod", "MagicNumber",
            "IfStatementWithTooManyBranches"})
    private static String indicateHue(final Object o, final HSB hsb) {

        if (1 / 6.0 > hsb.hue().doubleValue()) {
            return ansi(s(o), isBright(hsb) ? RED_BRIGHT : RED);
        } else if (2 / 6.0 > hsb.hue().doubleValue()) {
            return ansi(s(o), isBright(hsb) ? YELLOW_BRIGHT : YELLOW);
        } else if (3 / 6.0 > hsb.hue().doubleValue()) {
            return ansi(s(o), isBright(hsb) ? GREEN_BRIGHT : GREEN);
        } else if (4 / 6.0 > hsb.hue().doubleValue()) {
            return ansi(s(o), isBright(hsb) ? BLUE_BRIGHT : BLUE);
        } else if (5 / 6.0 > hsb.hue().doubleValue()) {
            return ansi(s(o), isBright(hsb) ? MAGENTA_BRIGHT : MAGENTA);
        } else {
            return ansi(s(o), isBright(hsb) ? CYAN_BRIGHT : CYAN);
        }
    }

    /**
     * Checks if the given HSB color is bright.
     *
     * @param hsb The HSB color to check.
     * @return True if the color is bright, false otherwise.
     */
    private static boolean isBright(final HSB hsb) {

        return ONE_HALF.doubleValue() < hsb.brightness().doubleValue();
    }

    /**
     * Converts the given object to a string and applies magenta bold colour formatting.
     *
     * @param o the object to convert and colorize
     * @return the converted string with magenta bold colour formatting
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String mb(final Object o) {

        return ansi(sn(o), MAGENTA_BOLD);
    }

    /**
     * Returns the given object as a red bold colorized string.
     *
     * @param o the object to be colorized
     * @return the colorized string
     */
    public static String rb(final Object o) {

        return ansi(sn(o), RED_BOLD);
    }

    /**
     * Applies a red bold style to the string representation of the provided object based on the given flag.
     *
     * @param inCase a boolean flag indicating whether the string should be in uppercase
     * @param o      the object to be converted to string and styled
     * @return the styled string
     */
    public static String rb(final boolean inCase, final Object o) {

        return colorize(inCase, RED_BOLD, sn(o));
    }

    /**
     * Returns a colorized string representation of the given object with a red crossed font.
     *
     * @param o the object to be converted to a string and colorized
     * @return the colorized string representation of the given object with a red crossed font
     */
    public static String rc(final Object o) {

        return ansi(sn(o), RED_CROSSED);
    }

    /**
     * Removes ANSI color codes from the given text.
     *
     * @param text the input string that may contain ANSI color codes
     * @return a new string with all ANSI color codes removed
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "HardCodedStringLiteral", "HardcodedFileSeparator",
            "DynamicRegexReplaceableByCompiledPattern"})
    public static String removeANSICodes(final String text) {

        final String ansiRegex = "\u001B\\[[;\\d]*m";
        return text.replaceAll(ansiRegex, "");
    }

    /**
     * Colorizes the given object in yellow bold using the provided colorize() method.
     *
     * @param o The object to be colorized.
     * @return The colorized object as a string.
     */
    public static String yb(final Object o) {

        return ansi(sn(o), YELLOW_BOLD);
    }

    /**
     * Adds color to a given object and returns the formatted string.
     *
     * @param inCase boolean value indicating whether the colorization should be applied
     * @param o      the object to be colorized
     * @return the colorized string representation of the object
     */
    public static String yb(final boolean inCase, final Object o) {

        return colorize(inCase, YELLOW_BOLD, sn(o));
    }

    /**
     * @return ANSI code for color
     */
    private String code() {

        return code;
    }
}
