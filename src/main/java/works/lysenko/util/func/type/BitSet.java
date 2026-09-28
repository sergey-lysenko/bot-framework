package works.lysenko.util.func.type;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import static works.lysenko.util.func.type.Booleans.isTrue;

/**
 * A wrapper record for a {@code java.util.BitSet}, providing utility methods.
 */
@SuppressWarnings({"PublicMethodNotExposedInInterface", "unused"})
public record BitSet(java.util.BitSet bitSet, int length) {

    /**
     * Generates a random {@code BitSet} of the specified length.
     *
     * @param length the length of the {@code BitSet} to generate
     * @return a new {@code BitSet} instance with random boolean values
     */
    @SuppressWarnings("UnsecureRandomNumberGeneration")
    public static BitSet random(final int length) {

        final RandomGenerator random = new Random();
        final java.util.BitSet bitSet = new java.util.BitSet(length);
        IntStream.range(0, length).forEach(i -> bitSet.set(i, random.nextBoolean()));
        return new BitSet(bitSet, length);
    }


    /**
     * Generates a {@code BitSet} based on the given probabilities for each bit.
     * Each bit in the resulting {@code BitSet} is set to {@code true} with the probability
     * specified in the corresponding index of the input array.
     *
     * @param probabilities an array of float values representing the probability (0.0 to 1.0)
     *                       of each bit being {@code true}; each value must be within the valid range.
     * @return a new {@code BitSet} instance where each bit is set to {@code true} or {@code false}
     *         based on the probabilities.
     * @throws IllegalArgumentException if any probability value is outside the range of 0.0 to 1.0.
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public static BitSet random(final float... probabilities) {

        final java.util.BitSet bitSet = new java.util.BitSet(probabilities.length);
        IntStream.range(0, probabilities.length).forEach(i -> bitSet.set(i, isTrue(probabilities[i])));
        return new BitSet(bitSet, probabilities.length);
    }


    /**
     * Calculates the number of bits set to {@code true} in the underlying {@code BitSet}.
     *
     * @return the number of bits set to {@code true} in the {@code BitSet}.
     */
    public int cardinality() {

        return bitSet.cardinality();
    }

    /**
     * Retrieves a list of booleans representing the state of bits in the {@code BitSet}.
     *
     * @return a list of booleans where each boolean corresponds to the value of a bit
     * in the {@code BitSet}, with the size determined by the {@code length} field.
     */
    public List<Switch> switches() {

        return IntStream.range(0, length)
                .mapToObj(i -> new Switch(i, bitSet.get(i)))
                .toList();
    }

    /**
     * Represents a switch with a specific index and its corresponding boolean value.
     * This is used to encapsulate the state of a bit in a bitset-like structure.
     *
     * @param index the position of this switch in the structure, typically represented as an integer index
     * @param value the boolean value of the switch, indicating its state (true for "on", false for "off")
     */
    public record Switch(int index, boolean value) {}
}
