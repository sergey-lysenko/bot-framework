package works.lysenko.util.grid.record.rgbc;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.util.apis.grid.t._Colour;
import works.lysenko.util.data.records.RGB24;
import works.lysenko.util.func.logs.Columns;
import works.lysenko.util.prop.grid.Colours;
import works.lysenko.util.prop.logs.Table;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Objects.isNull;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.inn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.grid.record.rgbc.HSB.convertRGBtoHSB;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.T.THERE_ARE;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.D.DEFINED;
import static works.lysenko.util.lang.word.D.DESCRIBING;
import static works.lysenko.util.lang.word.D.DISTANCE;
import static works.lysenko.util.lang.word.E.EUCLIDEAN;
import static works.lysenko.util.lang.word.E.EXCEEDING;
import static works.lysenko.util.lang.word.F.FOUND;
import static works.lysenko.util.lang.word.N.NAMED;
import static works.lysenko.util.lang.word.P.PAIRS;
import static works.lysenko.util.lang.word.R.REQUESTED;
import static works.lysenko.util.lang.word.S.SIMILAR;
import static works.lysenko.util.lang.word.S.SIMILARITY;
import static works.lysenko.util.lang.word.T.THRESHOLD;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Layout.Files.knownColours;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * Colour represents a color object.
 * It encapsulates an RGB value and provides methods to perform operations on colors.
 */
@SuppressWarnings({"ObjectAllocationInLoop", "ArrayLengthInLoopCondition", "SuspiciousToArrayCall", "DataFlowIssue"})
public record Colour(int rgb) implements _Colour {

    private static final String B1 = b(c(COLOUR), DEFINED, BY, MORE, THAN, ONE, NAME);
    private static final String B2 = b(ARE, DESCRIBING, THE, SAME, RGB, VALUE);
    private static final String B3 = b(c(COLOUR), PAIRS, EXCEEDING, SIMILARITY, THRESHOLD);
    private static final String B4 = b(ARE, TOO, s(SIMILAR, _COMMA_), EUCLIDEAN, DISTANCE, IS);

    private static final String[] stringArray = new String[0];

    static {

        // Detect duplicates
        final Set<Duplicate> duplicates = new HashSet<>(0);
        for (final Object knownValue : knownColours.values()) {
            final List<String> sample = namesByValue(i(s(knownValue)));
            if (1 < sample.size())
                duplicates.add(new Duplicate(i(s(knownValue)), sample));
        }
        if (!duplicates.isEmpty()) logEvent(S2, b(s(duplicates.size()), B1));
        for (final Duplicate duplicate : duplicates) {

            final String coloursStr = StringUtils.join(duplicates, COMMA_SPACE);
            logEvent(S2, b(c(COLOURS), coloursStr, B2, s(duplicate.rgb())));
        }

        // Detect similar colours
        final var colours = knownColours.values().toArray(stringArray);
        final Set<Similar> similars = new HashSet<>(0);
        for (int i = 0; i < colours.length; i++) {
            for (int j = i + 1; j < colours.length; j++) {
                final int c1 = i(s(colours[i]));
                final int c2 = i(s(colours[j]));
                final RGB24 rgb1 = RGB24.rgb24(c1);
                final RGB24 rgb2 = RGB24.rgb24(c2);
                if (!isNull(rgb1) && !isNull(rgb2)) {
                    final double distance = distance(rgb1, rgb2);
                    if (Colours.threshold > distance) similars.add(new Similar(min(c1, c2), max(c1, c2), distance));
                }
            }
        }
        if (!similars.isEmpty()) logEvent(S2, b(s(similars.size()), B3, s(Colours.threshold), FOUND));
        for (final Similar similar : similars)
            logEvent(S2, b(c(COLOURS), shortOfValue(similar.c1()), AND,
                    shortOfValue(similar.c2()), B4, s(similar.distance())));

        final var colourNames = Collections.list(knownColours.keys()).toArray(stringArray);
        logDebug(b(THERE_ARE, yb(colourNames.length), NAMED, COLOURS), true);
        final List<String> list = Arrays.asList(colourNames);
        Collections.sort(list);
        for (final String row : Columns.minimalRows(list, Table.width, 9))
            logDebug(row);
    }

    /**
     * Retrieves the Colour enum constant that matches the provided name.
     *
     * @param name The name of the rgb.
     * @return The Colour enum constant that matches the name, or null if no match is found.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static Colour byName(final String name) {

        if (isNull(name)) return null;
        final String candidate = knownColours.getProperty(name);
        if (isNull(candidate)) {
            logEvent(S1, b(c(UNKNOWN), COLOUR, q(name), REQUESTED));
            return null;
        }
        return new Colour(i(candidate));
    }

    /**
     * Returns the Colour enum constant that matches the provided RGB value.
     *
     * @param rgb An RGB value.
     * @return The matching Colour constant, or null if no match is found.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private static Colour byValue(final int rgb) {

        for (final Object colour : knownColours.values())
            if (i(s(colour)) == rgb)
                return new Colour(i(s(colour)));
        return null;
    }

    /**
     * Calculates the distance between two RGB24 colours as points using the Euclidean distance formula.
     *
     * @param colour1 the first RGB24 point
     * @param colour2 the second RGB24 point
     * @return the distance between the two points
     */
    public static double distance(final RGB24 colour1, final RGB24 colour2) {

        final int redDiff = colour1.red() - colour2.red();
        final int greenDiff = colour1.green() - colour2.green();
        final int blueDiff = colour1.blue() - colour2.blue();

        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
    }

    /**
     * Returns a long representation of the given RGB value.
     * If a matching Colour enum constant is found for the RGB value, the long representation of the Colour object
     * is returned. Otherwise, the long representation of the RGB value calculated using RGB24 and HSB conversion is returned.
     *
     * @param rgb An RGB color value.
     * @return A1 string representing the long RGB value or the long representation of the matching Colour enum constant.
     * If no match is found and the RGB value is null, null is returned.
     */
    @SuppressWarnings({"DataFlowIssue", "unused", "MethodWithMultipleReturnPoints"})
    public static String longOfValue(final int rgb) {

        if (isNull(byValue(rgb))) return s(RGB24.longRgb(rgb), L0, convertRGBtoHSB(RGB24.rgb24(rgb)).longHSB());
        else return byValue(rgb).toLongString();
    }

    /**
     * Retrieves the name of a Colour object based on the provided integer value.
     *
     * @param i The integer value representing the Colour object.
     * @return The name of the Colour object, or null if no match is found.
     */
    public static String name(final int i) {

        return new Colour(i).name();
    }

    /**
     * Retrieves the names of Colour enum constants that have the specified RGB value.
     *
     * @param rgb The RGB value to search for.
     * @return A1 list containing the names of Colour enum constants with the specified RGB value.
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    private static List<String> namesByValue(final int rgb) {

        final List<String> names = new LinkedList<>();

        for (final String name : knownColours.stringPropertyNames())
            if (knownColours.getProperty(name).equals(s(rgb)))
                names.add(s(name));

        return names;
    }

    /**
     * Retrieves the RGB value of a Colour based on its name. If the Colour cannot
     * be found by name and fallback is allowed, attempts to interpret the provided
     * name as an RGB value.
     *
     * @param name            The name of the Colour or an interpretable RGB value.
     *                        If null, the method returns null.
     * @param fallbackAllowed A boolean indicating whether fallback to interpreting the
     *                        name as an RGB value is allowed when no Colour matches by name.
     * @return The RGB value of the matching Colour, or null if no match is found and
     * fallback is not allowed or unsuccessful.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static Integer rgbByName(final String name, final boolean fallbackAllowed) {

        if (isNull(name)) return null;
        Colour byName = byName(name);
        if (fallbackAllowed && isNull(byName)) byName = byValue(inn(name));
        if (isNull(byName)) return null;
        logDebug(byName.toLongString(), true);
        return byName.rgb();
    }

    /**
     * Retrieves the RGB value of a Colour based on its name.
     * This method calls the overloaded version of rgbByName with fallbackAllowed set to true.
     *
     * @param name The name of the Colour or an interpretable RGB value.
     *             If null, the method returns null.
     * @return The RGB value of the matching Colour, or null if no match is found.
     */
    public static Integer rgbByName(final String name) {

        return rgbByName(name, true);
    }

    /**
     * Returns a short representation of the given RGB value. If a matching Colour enum constant is found for the RGB value,
     * the short representation of the Colour object is returned. Otherwise, the short representation of the RGB value
     * calculated using RGB24 and HSB conversion is returned.
     *
     * @param rgb An RGB color value.
     * @return A1 string representing the short RGB value or the short representation of the matching Colour enum constant.
     * If no match is found and the RGB value is null, null is returned.
     */
    @SuppressWarnings({"DataFlowIssue", "MethodWithMultipleReturnPoints"})
    public static String shortOfValue(final int rgb) {

        if (isNull(byValue(rgb))) return s(RGB24.shortRgb(rgb), L0, convertRGBtoHSB(RGB24.rgb24(rgb)).shortHSB());
        else return byValue(rgb).toShortString();
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public String name() {

        for (final Map.Entry<Object, Object> colour : knownColours.entrySet())
            if (i(s(colour.getValue())) == rgb)
                return s(colour.getKey());
        return null;
    }

    public int rgb() {

        return rgb;
    }

    /**
     * Returns a long string representation of the Colour object.
     * If a matching Colour enum constant is found for the RGB value, the long representation of the Colour object
     * is returned. Otherwise, the long representation of the RGB value calculated using RGB24 and HSB conversion is returned.
     *
     * @return A1 string representing the long RGB value or the long representation of the matching Colour enum constant.
     * If no match is found and the RGB value is null, null is returned.
     */
    @SuppressWarnings("DataFlowIssue")
    public String toLongString() {

        return a(name(), s(RGB24.longRgb(rgb), L0, convertRGBtoHSB(RGB24.rgb24(rgb)).longHSB()));
    }

    /**
     * Returns a short string representation of the Colour object.
     * If a matching Colour enum constant is found for the RGB value, the short representation of the Colour object
     * is returned. Otherwise, the short representation of the RGB value calculated using RGB24 and HSB conversion is returned.
     *
     * @return A1 string representing the short RGB value or the short representation of the matching Colour enum constant.
     * If no match is found and the RGB value is null, null is returned.
     */
    @SuppressWarnings("DataFlowIssue")
    public String toShortString() {

        return a(name(), s(RGB24.shortRgb(rgb), L0, convertRGBtoHSB(RGB24.rgb24(rgb)).shortHSB()));
    }
}
