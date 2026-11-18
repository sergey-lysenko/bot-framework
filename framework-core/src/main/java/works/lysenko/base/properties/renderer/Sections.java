package works.lysenko.base.properties.renderer;

import works.lysenko.util.data.records.PropertiesMeta;
import works.lysenko.util.func.logs.Columns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.APPLIED;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.D.DEFAULT;
import static works.lysenko.util.lang.word.P.PROPERTIES;
import static works.lysenko.util.lang.word.U.UNCHANGED;
import static works.lysenko.util.lang.word.V.VALUES;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._NUMBR_;

/**
 * The `Sections` record is responsible for processing and rendering configuration properties,
 * displaying applied configurations, and formatting default properties for output.
 * It supports operations to process properties, display applied configurations,
 * and render unchanged default values, along with their formatting details.
 */
public record Sections() {

    /**
     * Processes and renders configuration properties into a formatted output list.
     * <p>
     * This method takes a sorted map of property key-value pairs, applies rendering
     * transformations using metadata, and formats the list based on a specified
     * maximum line length. The generated minimal list of rows is printed during processing.
     *
     * @param sorted a sorted map containing property key-value pairs to be processed and rendered
     * @param meta   the metadata object containing default property mappings and valid class references
     * @param length the maximum allowed length of each output line
     * @return a list of formatted strings representing the rendered configuration
     */
    @SuppressWarnings({"MethodWithMultipleLoops", "UseOfSystemOutOrSystemErr"})
    private static List<String> appliedConfiguration(final Map<String, String> sorted, final PropertiesMeta meta,
                                                     final int length) {

        final List<String> minimalRows;
        final List<String> list = new ArrayList<>(sorted.size());
        for (final Map.Entry<String, String> entry : sorted.entrySet()) list.add(Keys.renderKeyValue(entry, meta));
        minimalRows = Columns.minimalRows(list, length, 5);
        for (final String row : minimalRows) System.out.println(row);
        return list;
    }

    /**
     * Renders the applied configuration by processing a sorted map of properties
     * and formatting the output based on metadata and display constraints.
     * <p>
     * This method outputs a header before processing and rendering the applied
     * configuration keys and their values. The rendered configuration is returned as
     * a list of formatted strings.
     *
     * @param sorted a map of properties sorted by their keys, containing the key-value pairs to be rendered
     * @param meta   the metadata object specifying default properties and valid class references
     * @param length the maximum allowed length for each line in the output
     * @return a list of strings representing the rendered configuration
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static List<String> renderAppliedConfiguration(final Map<String, String> sorted, final PropertiesMeta meta,
                                                          final int length) {

        System.out.println(b(gray(_NUMBR_), c(APPLIED), TEST, CONFIGURATION));
        return appliedConfiguration(sorted, meta, length);
    }

    /**
     * Renders and displays default values and their proper formatting for unchanged properties.
     * <p>
     * This method outputs a heading followed by a detailed list of default key-value pairs
     * that have not been modified. It ensures proper formatting for console output.
     *
     * @param defaults        the map containing default key-value pairs to be rendered
     * @param maxRecordLength the maximum length of a record, used for formatting the output
     * @param changed         the collection of keys whose values have been modified
     * @param length          the maximum allowed length for output rows
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void renderDefaultValues(final Map<String, String> defaults, final int maxRecordLength,
                                           final Collection<String> changed, final int length) {

        System.out.println(s(_DASH_).repeat(maxRecordLength));
        System.out.println(b(gray(_NUMBR_), c(UNCHANGED), TEST, PROPERTIES, WITH, DEFAULT, VALUES));
        unchangedDefaults(defaults, changed, length);
    }

    /**
     * Processes unchanged default key-value pairs from a map and outputs them in a formatted list.
     *
     * @param defaults the map containing default key-value pairs to be processed
     * @param changed  the collection of keys whose values have been modified
     * @param length   the maximum allowed length for output rows
     */
    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "MethodWithMultipleLoops"})
    private static void unchangedDefaults(final Map<String, String> defaults, final Collection<String> changed,
                                          final int length) {

        final List<String> minimalRows;
        final List<String> list = new ArrayList<>(changed.size());
        for (final Map.Entry<String, String> entry : defaults.entrySet()) list.add(Keys.renderKeyValue(entry));
        minimalRows = Columns.minimalRows(list, length, 5);
        for (final String row : minimalRows) System.out.println(row);
    }
}
