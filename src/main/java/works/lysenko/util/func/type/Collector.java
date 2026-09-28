package works.lysenko.util.func.type;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.util.data.enums.Brackets;
import works.lysenko.util.data.type.maps.SortedString;

import java.lang.reflect.Array;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.spec.Symbols._EQUAL_;

/**
 * Additional Collection routines
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "unused", "CallToSuspiciousStringMethod", "unchecked",
        "MethodWithMultipleLoops",
        "ForeachStatement", "OptionalGetWithoutIsPresent"})
public record Collector() {

    private static List<Map.Entry<String, Integer>> entriesSortedByValues(final Map<String, Integer> map) {

        return map.entrySet().stream()
                .sorted(Comparator
                        .comparing(Map.Entry<String, Integer>::getValue)
                        .thenComparing(Map.Entry::getKey))
                .collect(Collectors.toList());
    }

    /**
     * @param map to invert by values
     * @return map sorted by inverted values
     */
    public static LinkedHashMap<String, Integer> getInvertedMap(final Map<String, Integer> map) {

        final List<Map.Entry<String, Integer>> sortedEntries = entriesSortedByValues(map);
        final Map<String, Integer> sorted = new LinkedHashMap<>(0);
        for (final Map.Entry<String, Integer> entry : sortedEntries)
            sorted.put(entry.getKey(), entry.getValue());
        final Object[] array = sorted.entrySet().toArray();
        final LinkedHashMap<String, Integer> inverted = new LinkedHashMap<>(0);
        for (int i = array.length - 1; 0 < i; i--) {
            final Map.Entry<String, Integer> e = (Map.Entry<String, Integer>) array[i];
            inverted.put(e.getKey(), e.getValue());
        }
        return inverted;
    }

    /**
     * Retrieves a sorted map based on the given map.
     *
     * @param map The map to sort.
     * @return A1 new map containing the sorted entries.
     */
    @SuppressWarnings("KeySetIterationMayUseEntrySet")
    public static Map<String, String> getSorted(final Map<Object, Object> map) {

        final Map<String, String> sorted = new SortedString();
        for (final Object name : map.keySet())
            sorted.put((String) name, (String) map.get(name));
        return sorted;
    }

    /**
     * @param map   to operate on
     * @param value to search for
     * @return first key of an element by its value
     */
    public static String keyForValue(final Map<String, String> map, final String value) {

        return map.entrySet().stream().filter(entry ->
                value.equals(entry.getValue())).map(Map.Entry::getKey).findFirst().get();
    }

    /**
     * @param <K>   key type
     * @param <V>   value type
     * @param map   to operate on
     * @param value to search for
     * @return keys of an element by its value
     */
    public static <K, V> Stream<K> keys(final Map<? extends K, V> map, final V value) {

        return map.entrySet().stream().filter(entry -> value.equals(entry.getValue())).map(Map.Entry::getKey);
    }

    /**
     * @param map   to operate on
     * @param value to search for
     * @return Stream of all keys of an element by their value
     */
    public static Stream<String> keysForValue(final Map<String, String> map, final String value) {

        return map.entrySet().stream().filter(entry -> value.equals(entry.getValue())).map(Map.Entry::getKey);
    }

    /**
     * Converts a given map to a string representation.
     *
     * @param <K>         the type of the map keys
     * @param <V>         the type of the map values
     * @param map         the map to be converted
     * @param keyMapper   the function to map keys to string representations
     * @param valueMapper the function to map values to string representations
     * @return a string representation of the map
     */
    public static <K, V> String mapToString(final Map<K, V> map, final Function<K, String> keyMapper, final Function<V,
            String> valueMapper) {

        return mapToString(map, keyMapper, _EQUAL_, valueMapper);
    }

    /**
     * Converts a given map to a string representation.
     *
     * @param <K>         the type of the map keys
     * @param <V>         the type of the map values
     * @param map         the map to be converted
     * @param keyMapper   the function to map keys to string representations
     * @param separator   the separator character between key-value pairs
     * @param valueMapper the function to map values to string representations
     * @return a string representation of the map
     */
    @SuppressWarnings({"BoundedWildcard", "MethodWithMultipleReturnPoints"})
    public static <K, V> String mapToString(final Map<K, V> map, final Function<K, String> keyMapper, final char separator,
                                            final Function<V, String> valueMapper) {

        if (isNull(map)) return null;
        final Collection<String> list = new ArrayList<>(0);
        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (isAnyNull(entry, keyMapper)) list.add(null);
            else {
                final String key = (null == entry.getKey()) ? null : keyMapper.apply(entry.getKey());
                final String value = (null == entry.getValue()) ? null : valueMapper.apply(entry.getValue());
                list.add(s(key, separator, value));
            }
        }
        return e(Brackets.CURLY, StringUtils.join(list, COMMA_SPACE));
    }

    /**
     * Get a random object from a list of objects
     *
     * @param <T>  objects type
     * @param list list of objects
     * @return a random object from a list of objects
     */
    public static <T> T selectOneOf(final List<T> list) {

        return list.get(new SecureRandom().nextInt(list.size()));
    }

    /**
     * Get a random object from an array of objects
     *
     * @param a array of objects
     * @return a random element from an array of objects
     */
    public static Object selectOneOf(final Object[] a) {

        return a[new SecureRandom().nextInt(Array.getLength(a))];
    }

    /**
     * Get a random object from a set of objects
     *
     * @param <T>        objects type
     * @param collection of objects
     * @return a random object from a set of objects
     */
    public static <T> Object selectOneOf(final Collection<T> collection) {

        return collection.toArray()[new SecureRandom().nextInt(collection.size())];
    }

    /**
     * Get an element from a list of strings different from defined one.
     * Presence of a string in a list not verified, only the exclusion
     * of defined value performed. Possible to define null for no exclusion.
     *
     * @param exclude    string to exclude
     * @param collection list of strings to exclude from
     * @param <T>        type
     * @return an element from a list of strings, which differs from specified one.
     */
    public static <T> T selectOther(final T exclude, final Collection<? extends T> collection) {

        final List<T> copy = new ArrayList<>(collection);
        if (isNotNull(exclude)) copy.remove(exclude);
        return selectOneOf(copy);
    }

    /**
     * Sorts the map entries by their values in reverse order.
     *
     * @param valueMap the Map of string keys to integer values
     * @return New map sorted by reverse values
     */
    public static Map<String, Integer> sortMapByReverseValue(final Map<String, Integer> valueMap) {

        final Comparator<Map.Entry<String, Integer>> valueComparator
                = Map.Entry.comparingByValue(Comparator.reverseOrder());

        return valueMap.entrySet().stream()
                .sorted(valueComparator)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,  // in case of duplicate keys, old value is used
                        LinkedHashMap::new
                ));
    }
}
