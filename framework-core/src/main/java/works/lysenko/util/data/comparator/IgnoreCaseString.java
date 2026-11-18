package works.lysenko.util.data.comparator;

import java.util.Comparator;

/**
 * Comparator for comparing two {@link String} objects case-insensitively using natural ordering.
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ComparatorNotSerializable", "CallToSuspiciousStringMethod"})
public class IgnoreCaseString implements Comparator<String> {

    /**
     * Compares two strings lexicographically, ignoring case differences, using {@link String#compareToIgnoreCase(String)}.
     *
     * @param o1 the first string to be compared
     * @param o2 the second string to be compared
     * @return a negative integer, zero, or a positive integer as the specified string is greater
     *         than, equal to, or less than the first, ignoring case considerations
     */
    @Override
    public final int compare(final String o1, final String o2) {

        return o1.compareToIgnoreCase(o2);
    }
}
