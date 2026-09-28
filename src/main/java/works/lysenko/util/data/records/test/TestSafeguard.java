package works.lysenko.util.data.records.test;

import static works.lysenko.util.data.strs.Swap.inn;
import static works.lysenko.util.func.data.Regexes.l0;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Represents a TestSafeguard object which contains the number of tests and milliseconds.
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
public record TestSafeguard(int tests, int milliseconds) {

    /**
     * Constructs a new TestSafeguard object with the given string.
     *
     * @param str the string representing the tests and milliseconds separated by a colon
     */
    public TestSafeguard(final String str) {

        this(inn(str.split(l0)[ZERO]), inn(str.split(l0)[ONE]));
    }
}
