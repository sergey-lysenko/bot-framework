package works.lysenko.base.parameters.renderer;

import works.lysenko.base.Parameters;
import works.lysenko.util.data.records.ParametersMeta;
import works.lysenko.util.func.logs.Columns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.lang.word.A.APPLIED;
import static works.lysenko.util.lang.word.D.DEFAULT;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.N.NONE;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.U.UNCHANGED;
import static works.lysenko.util.lang.word.V.VALUES;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._NUMBR_;

/**
 * Renders the applied and unchanged default execution parameter sections in multi-column format.
 */
public record Sections() {

    @SuppressWarnings({"MethodWithMultipleLoops", "UseOfSystemOutOrSystemErr"})
    private static List<String> appliedParameters(final Parameters parameters,
                                                  final Map<String, String> sorted,
                                                  final ParametersMeta meta,
                                                  final int length) {

        final List<String> list = new ArrayList<>(sorted.size());
        for (final Map.Entry<String, String> entry : sorted.entrySet()) {
            list.add(Keys.renderKeyValue(parameters, entry, meta));
        }
        if (list.isEmpty()) {
            System.out.println(gray(e(ROUND, NONE)));
            return list;
        }
        final List<String> minimalRows = Columns.minimalRows(list, length, 5);
        for (final String row : minimalRows) System.out.println(row);
        return list;
    }

    /**
     * Outputs the applied execution parameters section header and multi-column rows.
     *
     * @param parameters the Parameters instance
     * @param sorted     sorted map of applied/redefined parameter key-value pairs
     * @param meta       metadata tracking defaults and unknown keys
     * @param length     maximum row width
     * @return list of rendered applied parameter strings
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static List<String> renderAppliedParameters(final Parameters parameters,
                                                       final Map<String, String> sorted,
                                                       final ParametersMeta meta,
                                                       final int length) {

        System.out.println(b(gray(_NUMBR_), c(APPLIED), EXECUTION, PARAMETERS));
        return appliedParameters(parameters, sorted, meta, length);
    }

    /**
     * Outputs the separator, unchanged default execution parameters header, and multi-column rows.
     *
     * @param defaults        map of remaining unchanged default parameter key-value pairs
     * @param maxRecordLength width of the dashed divider line
     * @param length          maximum row width
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void renderDefaultValues(final Map<String, String> defaults,
                                           final int maxRecordLength,
                                           final int length) {

        System.out.println(s(_DASH_).repeat(maxRecordLength));
        System.out.println(b(gray(_NUMBR_), c(UNCHANGED), EXECUTION, PARAMETERS, WITH, DEFAULT, VALUES));
        unchangedDefaults(defaults, length);
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "MethodWithMultipleLoops"})
    private static void unchangedDefaults(final Map<String, String> defaults, final int length) {

        final List<String> list = new ArrayList<>(defaults.size());
        for (final Map.Entry<String, String> entry : defaults.entrySet()) {
            list.add(Keys.renderKeyValue(entry));
        }
        if (list.isEmpty()) {
            System.out.println(gray(e(ROUND, NONE)));
            return;
        }
        final List<String> minimalRows = Columns.minimalRows(list, length, 5);
        for (final String row : minimalRows) System.out.println(row);
    }
}
