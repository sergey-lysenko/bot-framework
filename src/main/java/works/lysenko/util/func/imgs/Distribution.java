package works.lysenko.util.func.imgs;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.enums.DistributionType;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * The Distribution class provides methods to calculate the distribution of values in a given list or graph image.
 */
@SuppressWarnings({"ForeachStatement", "MethodWithMultipleLoops"})
public record Distribution() {

    /**
     * Calculates the distribution of values in a given list.
     *
     * @param list the list of values to calculate the distribution for
     * @return a map representing the distribution, where the keys are the distinct values in the list
     * and the values are the corresponding distribution percentages
     */
    @SuppressWarnings({"CollectionWithoutInitialCapacity", "ObjectAllocationInLoop"})
    private static Map<Integer, Fraction> calculateDistribution(final Collection<Integer> list) {

        final Map<Integer, Long> countMap = list.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        final Map<Integer, Fraction> distributionMap = new HashMap<>();
        for (final Map.Entry<Integer, Long> entry : countMap.entrySet()) {
            final Fraction fraction = fr(entry.getValue().intValue(), list.size());
            distributionMap.put(entry.getKey(), fraction);
        }
        final Fraction sumFraction = distributionMap.values().stream().reduce(fr(0, 1), Fraction::add);
        final double sum = sumFraction.doubleValue();
        final double tolerance = 0.00001;
        if (tolerance < abs(sum - 1)) {
            throw new ArithmeticException("The sum of distribution percentages is not approximately 1.");
        }
        return distributionMap;
    }

    /**
     * Returns the complete distribution of values in the given graph image.
     *
     * @param graph the graph image to analyze
     * @return a map representing the distribution, where the keys are the distinct values in the graph
     * and the values are the corresponding distribution percentages
     */
    private static Map<Integer, Fraction> getCompleteDistribution(final BufferedImage graph) {

        final Collection<Integer> list = new ArrayList<>(0);
        final int width = graph.getWidth();
        final int height = graph.getHeight();
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                list.add(graph.getRGB(x, y));
        return calculateDistribution(list);
    }

    /**
     * Returns the distribution based on the given type and graph.
     *
     * @param type  the type of distribution (VERTICAL_CENTER, HORIZONTAL_CENTER, COMPLETE)
     * @param graph the graph image to analyze
     * @return the distribution object based on the given type
     */
    public static Map<Integer, Fraction> getDistribution(final DistributionType type, final BufferedImage graph) {

        return switch (type) {
            case VERTICAL_CENTER -> getVerticalCenterDistribution(graph);
            case HORIZONTAL_CENTER -> getHorizontalCenterDistribution(graph);
            case COMPLETE -> getCompleteDistribution(graph);
        };
    }

    /**
     * Calculates the distribution of colors along the horizontal center line of a given BufferedImage.
     *
     * @param graph the BufferedImage representing the graph
     * @return an Object representing the calculated distribution
     */
    private static Map<Integer, Fraction> getHorizontalCenterDistribution(final BufferedImage graph) {

        final Collection<Integer> list = new ArrayList<>(0);
        final int width = graph.getWidth();
        final int height = graph.getHeight();
        for (int x = 0; x < width; x++)
            list.add(graph.getRGB(x, height / 2));
        return calculateDistribution(list);
    }

    /**
     * Calculates the vertical distribution of pixels in the center column of the given graph image.
     *
     * @param graph The graph image from which to calculate the vertical center distribution.
     * @return The calculated distribution of pixels as a List of Integers.
     */
    private static Map<Integer, Fraction> getVerticalCenterDistribution(final BufferedImage graph) {

        final Collection<Integer> list = new ArrayList<>(0);
        final int width = graph.getWidth();
        final int height = graph.getHeight();
        for (int y = 0; y < height; y++)
            list.add(graph.getRGB(width / 2, y));
        return calculateDistribution(list);
    }
}
