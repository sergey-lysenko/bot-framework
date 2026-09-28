package works.lysenko.util.data.comparator;

import java.util.Comparator;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ComparatorNotSerializable", "CallToSuspiciousStringMethod"})
public class IgnoreCaseString implements Comparator<String> {

    @Override
    public final int compare(final String o1, final String o2) {

        return o1.compareToIgnoreCase(o2);
    }
}
