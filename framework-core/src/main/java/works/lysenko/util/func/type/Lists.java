package works.lysenko.util.func.type;

import java.util.List;

import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Provides utility methods for working with lists.
 */
@SuppressWarnings("unused")
public record Lists() {

    /**
     * Retrieves a random value from the provided list.
     *
     * @param <T>  the type of elements in the list
     * @param list the list from which a random value will be retrieved
     * @return a random value from the list
     * @throws IllegalArgumentException if the list is null or empty
     */
    public static <T> RandomListElement<T> getRandomListElement(final List<? extends T> list) {

        final T element = list.get(Numbers.random(ZERO, list.size() - 1));
        final int index = list.indexOf(element);
        return new RandomListElement<>(index, element);
    }
}