package works.lysenko.util.data.comparator;

import works.lysenko.util.apis.scenario._Scenario;

import java.util.Comparator;

import static java.lang.System.identityHashCode;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols._AT_SGN;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ComparatorNotSerializable", "CallToSuspiciousStringMethod"})
public class IgnoreCaseScenario implements Comparator<_Scenario> {

    @SuppressWarnings("ObjectInstantiationInEqualsHashCode")
    public final int compare(final _Scenario o1, final _Scenario o2) {

        final String key1 = s(o1.getClass().getName(), _AT_SGN, identityHashCode(o1));
        final String key2 = s(o2.getClass().getName(), _AT_SGN, identityHashCode(o2));
        return key1.compareToIgnoreCase(key2);
    }
}
