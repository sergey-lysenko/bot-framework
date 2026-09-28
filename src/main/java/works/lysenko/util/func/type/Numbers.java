package works.lysenko.util.func.type;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.random.RandomDataGenerator;

import java.util.Arrays;

import static java.util.Objects.isNull;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Additional Integer routines
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public record Numbers() {

    private static final int NEGATIVE_BIT_SHIFT = 63;

    /**
     * Retrieves a Double value from a string representation or returns null if the string is not a valid Double.
     *
     * @param s the string to convert to a Double
     * @return the Double value of the string, or null if the string is not a valid Double
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MethodWithMultipleReturnPoints", "unused"})
    public static Double getDoubleOrNull(final String s) {

        if (isNull(s)) return null;
        try {
            return Double.parseDouble(s);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Double -0.0 and +0.0 are equal numbers, but it is still possible to get information about sign
     *
     * @param number to check
     * @return true if number is positive
     */
    @SuppressWarnings("unused")
    public static boolean isPositive(final double number) {

        final long bits = Double.doubleToLongBits(number);
        return 0L == (bits >> NEGATIVE_BIT_SHIFT);
    }

    /**
     * Generate a random integer. With the range and list of values to exclude.
     *
     * @param from minimum value
     * @param to   maximum value
     * @param x    list of values to exclude
     * @return random integer
     */
    @SuppressWarnings({"OverloadedVarargsMethod", "ObjectAllocationInLoop"})
    public static Integer random(final int from, final int to, final Integer... x) {

        Integer n; // Non-primitive for asList() compatibility
        do n = new RandomDataGenerator().nextInt(from, to); while (Arrays.asList(x).contains(n));
        return n;
    }

    /**
     * Generate a random integer in the defined range.
     *
     * @param from minimum value
     * @param to   maximum value
     * @return random integer
     */
    public static Integer random(final int from, final int to) {

        return new RandomDataGenerator().nextInt(from, to);
    }

    /**
     * Generate a random integer in the defined range.
     *
     * @param from minimum value
     * @param to   maximum value
     * @return random integer
     */
    public static long random(final long from, final long to) {

        return new RandomDataGenerator().nextLong(from, to);
    }

    /**
     * Generate a random integer in the defined range.
     *
     * @param from minimum value
     * @param to   maximum value
     * @return random integer
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    public static float random(final float from, final float to) {

        return (float) new RandomDataGenerator().nextUniform(from, to);
    }

    /**
     * Generates a random fractional number within a specified range [from, to) and with a given
     * discretization factor. The result is represented as a Fraction.
     *
     * @param from           the lower bound of the range (inclusive), must be between 0.0 and 1.0
     * @param to             the upper bound of the range (exclusive), must be between 0.0 and 1.0
     * @param discretization the number of discrete steps in the range
     * @return a random Fraction within the specified range and discretization, or null if the inputs are invalid
     */
    @SuppressWarnings({"OverlyComplexBooleanExpression", "unused", "NumericCastThatLosesPrecision"})
    public static Fraction randomNormal(final Double from, final Double to, final int discretization) {

        if (isNull(from) || isNull(to)) return null;
        if (from >= to) return null;
        if (discretization <= ZERO) return null;
        if ((from > ONE) || (to > ONE) || (from < ZERO) || (to < ZERO)) return null;
        final double range = to - from;
        final int scaledNumeratorRange = (int) (range * discretization);
        final int numerator = new RandomDataGenerator().nextInt(0, scaledNumeratorRange);
        final int adjustedNumerator = (int) (from * discretization) + numerator;
        return new Fraction(adjustedNumerator, discretization);
    }


    /**
     * Selects the index of a randomly chosen item based on the given probabilities.
     * The probabilities are provided as an array of integers, where each integer represents
     * the weight or likelihood of selecting its corresponding index.
     * A higher value increases the likelihood of the index being selected.
     *
     * @param probabilities an array of integers representing the selection probabilities
     *                      for each index. Must contain at least one element.
     * @return the index of the selected item based on the probabilities
     * @throws IllegalArgumentException if the probabilities array is empty
     * @throws IllegalStateException    if an error occurs during random index calculation
     */
    @SuppressWarnings({"ArrayLengthInLoopCondition", "MethodWithMultipleLoops", "StaticMethodOnlyUsedInOneClass", "unused"})
    public static int getRandomItemIndex(final int... probabilities) {

        if (probabilities.length == ZERO) {
            throw new IllegalArgumentException("At least one probability must be provided");
        }
        int totalSum = 0;
        for (final int probability : probabilities) {
            totalSum += probability;
        }
        final int random = random(0, totalSum);
        int cumulativeSum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            cumulativeSum += probabilities[i];
            if (random < cumulativeSum) {
                return i;
            }
        }
        throw new IllegalStateException("Random index calculation failed"); // Defensive, should not happen
    }
}