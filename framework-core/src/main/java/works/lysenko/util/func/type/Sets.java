package works.lysenko.util.func.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;
import static works.lysenko.util.lang.U.UNABLE_TO_SELECT_A_RANDOM_ELEMENT_OF_AN_EMPTY_SET;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Provides utility methods for working with lists.
 */
@SuppressWarnings("unused")
public record Sets() {

    /**
     * Retrieves a random element from the provided set alongside its index
     * within a list representation of the set.
     *
     * @param <T> the type of the elements in the set
     * @param set the set from which to retrieve a random element
     * @return a {@code RandomListElement} containing the index and value of the selected element
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static <T> RandomListElement<T> getRandomSetElement(final Set<? extends T> set) {

        if (isNull(set) || set.isEmpty()) {throw new IllegalArgumentException(UNABLE_TO_SELECT_A_RANDOM_ELEMENT_OF_AN_EMPTY_SET);}
        final List<T> list = new ArrayList<>(set);
        final T element = list.get(Numbers.random(ZERO, list.size() - 1));
        final int index = list.indexOf(element);
        return new RandomListElement<>(index, element);
    }
}