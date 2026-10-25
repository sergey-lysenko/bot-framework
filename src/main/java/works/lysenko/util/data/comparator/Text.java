package works.lysenko.util.data.comparator;

import java.util.Comparator;

/**
 * Comparator for comparing two {@link String} objects using natural ordering.
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ComparatorNotSerializable", "CallToSuspiciousStringMethod"})
public class Text implements Comparator<String> {

    /**
     * Compares two strings lexicographically using {@link String#compareTo(String)}.
     *
     * @param o1 the first string to be compared
     * @param o2 the second string to be compared
     * @return the value 0 if the argument string is equal to this string; a value less
     *         than 0 if this string is lexicographically less than the string argument;
     *         and a value greater than 0 if this string is lexicographically greater
     *         than the string argument
     */
    @Override
    public final int compare(final String o1, final String o2) {

        return o1.compareTo(o2);
    }
}
