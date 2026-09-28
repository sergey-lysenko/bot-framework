package works.lysenko.util.func.type;

/**
 * Represents an element selected randomly from a list, including its value and its index within the list.
 *
 * @param <T> the type of the value this record holds
 * @param index the index of the element within the list
 * @param value the value of the selected element
 */
public record RandomListElement<T>(int index, T value) {
}
