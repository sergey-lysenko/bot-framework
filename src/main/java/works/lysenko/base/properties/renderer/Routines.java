package works.lysenko.base.properties.renderer;

import works.lysenko.util.data.records.PropertiesMeta;
import works.lysenko.util.spec.Numbers;

import java.util.Collection;
import java.util.Map;

import static works.lysenko.Base.properties;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.ONE;
import static works.lysenko.util.chrs.____.HAVE;
import static works.lysenko.util.chrs.____.WERE;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.lang.T.THERE_ARE;
import static works.lysenko.util.lang.word.C.CLASS;
import static works.lysenko.util.lang.word.D.DEFAULT;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.P.PARAMETER;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.R.REDEFINED;
import static works.lysenko.util.lang.word.R.REFERENCE;
import static works.lysenko.util.lang.word.V.VALUES;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols.A;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * A1 utility class that provides methods for working with maps and validating properties metadata.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Routines() {

    /**
     * TODO: parametrize
     */
    public static final int MIN_LENGTH = 150;

    /**
     * Finds the length of the longest key-value combination in a given map.
     *
     * @param map the map to search for the longest key-value combination
     * @return the length of the longest key-value combination
     */
    public static int findLongestKeyValue(final Map<String, String> map) {

        int longestKeyValue = 0;
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey().length() + entry.getValue().length() > longestKeyValue) {
                longestKeyValue = entry.getKey().length() + entry.getValue().length() +
                        (properties.isCommonValue(entry.getKey(), entry.getValue()) ? Keys.COMMON_MARKER.length() + 1 : 0);
            }
        }
        return longestKeyValue;
    }

    /**
     * Validates the properties metadata by comparing the number of changed properties,
     * default properties, and valid class references against the expected sizes.
     * Throws an exception if the actual total size does not match the expected total size.
     * Outputs status messages for diagnostic purposes.
     *
     * @param meta    The properties metadata containing default values and keys with valid class references.
     * @param changed A1 collection of property keys that have been changed.
     * @throws IllegalStateException if the actual number of parameters (changed + default)
     *                               does not match the expected total number of parameters.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void validate(final PropertiesMeta meta, final Collection<String> changed) {

        final int changedSize = changed.size();
        final int defaultsSize = meta.defaults().size();
        final int classesSize = meta.keysWithValidClasses().size();

        final int actualTotalSize = changedSize + defaultsSize;
        final int expectedTotalSize = properties.getDefaultsSize() + classesSize;

        System.out.println(b(c(VALUES), OF, yb(changedSize), PARAMETERS, WERE, s(REDEFINED, _COMMA_), yb(defaultsSize), HAVE
                , DEFAULT, VALUES));

        if (classesSize > ZERO)
            if (classesSize == Numbers.ONE) System.out.println(b(yb(ONE), PARAMETER, WITH, s(A), CLASS, REFERENCE));
            else System.out.println(b(THERE_ARE, yb(classesSize), PARAMETERS, WITH, s(A), CLASS, REFERENCE));

        if (actualTotalSize != expectedTotalSize)
            throw new IllegalStateException(b(s(actualTotalSize), PARAMETERS, IS, percentString(actualTotalSize,
                    expectedTotalSize), OF, EXPECTED, s(expectedTotalSize)));
    }
}
