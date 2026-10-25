package works.lysenko.util.data.comparator;

import works.lysenko.util.apis.scenario._Scenario;

import java.util.Comparator;

import static java.lang.System.identityHashCode;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols._AT_SGN;

/**
 * Comparator for comparing two {@link _Scenario} objects case-insensitively using their class name and identity hash code.
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ComparatorNotSerializable", "CallToSuspiciousStringMethod"})
public class IgnoreCaseScenario implements Comparator<_Scenario> {

    /**
     * Compares two {@link _Scenario} objects case-insensitively by constructing a unique string key
     * consisting of their class name, '@' symbol, and identity hash code.
     *
     * @param o1 the first scenario to be compared
     * @param o2 the second scenario to be compared
     * @return a negative integer, zero, or a positive integer as the first scenario's key is
     *         less than, equal to, or greater than the second scenario's key, ignoring case
     */
    @SuppressWarnings("ObjectInstantiationInEqualsHashCode")
    @Override
    public final int compare(final _Scenario o1, final _Scenario o2) {

        final String key1 = s(o1.getClass().getName(), _AT_SGN, identityHashCode(o1));
        final String key2 = s(o2.getClass().getName(), _AT_SGN, identityHashCode(o2));
        return key1.compareToIgnoreCase(key2);
    }
}
