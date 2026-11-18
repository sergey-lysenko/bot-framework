package works.lysenko.base.parameters.renderer;

import works.lysenko.base.Parameters;
import works.lysenko.util.data.records.ParametersMeta;
import works.lysenko.util.spec.Numbers;

import java.util.Collection;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.ONE;
import static works.lysenko.util.chrs.____.HAVE;
import static works.lysenko.util.chrs.____.WERE;
import static works.lysenko.util.data.enums.Ansi.rb;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.lang.T.THERE_ARE;
import static works.lysenko.util.lang.word.A.ADDITIONAL;
import static works.lysenko.util.lang.word.D.DEFAULT;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.P.PARAMETER;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.R.REDEFINED;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.lang.word.V.VALUES;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * Utility routines for measuring key-value widths and validating rendered execution parameters.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Routines() {

    /**
     * Minimum row width for multi-column parameter tables.
     */
    public static final int MIN_LENGTH = 150;

    /**
     * Finds the length of the longest key-value combination across applied and default parameter maps.
     *
     * @param parameters the Parameters instance used to check source markers
     * @param applied    map of applied/redefined parameter key-value pairs
     * @param defaults   map of default parameter key-value pairs
     * @return length of the longest visible key-value combination
     */
    public static int findLongestKeyValue(final Parameters parameters,
                                          final Map<String, String> applied,
                                          final Map<String, String> defaults) {

        int longestKeyValue = 0;
        for (final Map.Entry<String, String> entry : applied.entrySet()) {
            final String key = entry.getKey();
            final String val = (null == entry.getValue()) ? EMPTY : entry.getValue();
            final String rawVal = parameters.getProperty(key, val);
            final boolean isEnv = parameters.isEnvValue(key, rawVal);
            final boolean isFile = parameters.isFileValue(key, rawVal);
            final int markerLen = isEnv ? Keys.ENV_MARKER.length() : (isFile ? Keys.FILE_MARKER.length() : 0);
            final int candidate = key.length() + val.length() + (markerLen > 0 ? markerLen + 1 : 0);
            if (candidate > longestKeyValue) {
                longestKeyValue = candidate;
            }
        }
        for (final Map.Entry<String, String> entry : defaults.entrySet()) {
            final String val = (null == entry.getValue()) ? EMPTY : entry.getValue();
            final int candidate = entry.getKey().length() + val.length();
            if (candidate > longestKeyValue) {
                longestKeyValue = candidate;
            }
        }
        return longestKeyValue;
    }

    /**
     * Outputs summary diagnostics and validates that the sum of applied and default parameters
     * matches the expected total count.
     *
     * @param meta    parameters metadata containing remaining defaults, unknown keys, and counts
     * @param changed collection of rendered applied parameter strings
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void validate(final ParametersMeta meta, final Collection<String> changed) {

        final int changedSize = changed.size();
        final int defaultsSize = meta.defaults().size();
        final int additionalCount = meta.additionalCount();
        final int unknownSize = meta.unknownKeys().size();

        final int actualTotalSize = changedSize + defaultsSize;
        final int expectedTotalSize = meta.expectedDefaultsSize() + unknownSize;

        System.out.println(b(c(VALUES), OF, yb(changedSize), EXECUTION, PARAMETERS, WERE, s(REDEFINED, _COMMA_),
                yb(defaultsSize), HAVE, DEFAULT, VALUES));

        if (additionalCount > ZERO) {
            if (additionalCount == Numbers.ONE) {
                System.out.println(b(yb(ONE), ADDITIONAL, EXECUTION, PARAMETER));
            } else {
                System.out.println(b(THERE_ARE, yb(additionalCount), ADDITIONAL, EXECUTION, PARAMETERS));
            }
        }

        if (unknownSize > ZERO) {
            if (unknownSize == Numbers.ONE) {
                System.out.println(b(rb(ONE), UNKNOWN, EXECUTION, PARAMETER));
            } else {
                System.out.println(b(THERE_ARE, rb(unknownSize), UNKNOWN, EXECUTION, PARAMETERS));
            }
        }

        if (actualTotalSize != expectedTotalSize) {
            throw new IllegalStateException(b(s(actualTotalSize), EXECUTION, PARAMETERS, IS,
                    percentString(actualTotalSize, expectedTotalSize), OF, EXPECTED, s(expectedTotalSize)));
        }
    }
}
