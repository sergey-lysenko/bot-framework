package works.lysenko.util.data.comparator;

import works.lysenko.util.apis.scenario._Scenario;

import java.util.Comparator;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ComparatorNotSerializable", "unused", "CallToSuspiciousStringMethod"})
public class Scenario implements Comparator<_Scenario> {

    @Override
    public final int compare(final _Scenario o1, final _Scenario o2) {

        return o1.getName().compareTo(o2.getName());
    }
}
