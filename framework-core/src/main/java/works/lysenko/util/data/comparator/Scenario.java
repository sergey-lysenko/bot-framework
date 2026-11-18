package works.lysenko.util.data.comparator;

import works.lysenko.util.apis.scenario._Scenario;

import java.util.Comparator;

/**
 * Comparator for comparing two {@link _Scenario} objects based on their names.
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ComparatorNotSerializable", "unused", "CallToSuspiciousStringMethod"})
public class Scenario implements Comparator<_Scenario> {

    /**
     * Compares two {@link _Scenario} objects lexicographically by their names using {@link String#compareTo(String)}.
     *
     * @param o1 the first scenario to be compared
     * @param o2 the second scenario to be compared
     * @return a negative integer, zero, or a positive integer as the first scenario's name is
     *         less than, equal to, or greater than the second scenario's name
     */
    @Override
    public final int compare(final _Scenario o1, final _Scenario o2) {

        return o1.getName().compareTo(o2.getName());
    }
}
