package works.lysenko.util.func.imgs;

import org.apache.commons.math3.fraction.Fraction;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import works.lysenko.util.data.enums.PolychromyMethod;
import works.lysenko.util.data.records.RGB24;

import java.util.ArrayList;
import java.util.List;

import static works.lysenko.Base.exec;
import static works.lysenko.Base.getInteger;
import static works.lysenko.Base.log;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.chrs.____.TREE;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.PolychromyMethod.EUCLIDEAN_DISTANCE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.grid.record.rgbc.Colour.distance;
import static works.lysenko.util.lang.word.A.ALGORITHM;
import static works.lysenko.util.lang.word.B.BUILDING;
import static works.lysenko.util.lang.word.C.CALCULATING;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.D.DISPERSION;
import static works.lysenko.util.lang.word.D.DISTANCE;
import static works.lysenko.util.lang.word.D.DISTRIBUTED;
import static works.lysenko.util.lang.word.E.EDGES;
import static works.lysenko.util.lang.word.E.EUCLIDEAN;
import static works.lysenko.util.lang.word.E.EXECUTING;
import static works.lysenko.util.lang.word.G.GRAPH;
import static works.lysenko.util.lang.word.K.KRUSKAL;
import static works.lysenko.util.lang.word.M.METHOD;
import static works.lysenko.util.lang.word.M.MINIMUM;
import static works.lysenko.util.lang.word.P.PERSISTING;
import static works.lysenko.util.lang.word.P.POINTS;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.lang.word.S.SPANNING;
import static works.lysenko.util.lang.word.U.USING;
import static works.lysenko.util.lang.word.V.VERTICES;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * Utility class for calculating polychromy values and generating RGB24 points.
 */
public record Analyser() {

    /**
     * Calculates the Euclidean distance polychromy between a list of RGB24 points.
     *
     * @param points the list of RGB24 points
     * @return the Euclidean distance polychromy value
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static double calculateEuclideanDistance(final List<RGB24> points) {

        final double raw = getRawEuclideanDistance(points);
        final double max = getMaximumEuclideanDispersion(points.size());
        final double result = raw / max;
        log(b(c(RAW), IS, s(yb((int) raw), _COMMA_), MAX, IS, s(yb((int) max), _COMMA_), RESULT, IS, yb(ts(fr(result)))));
        return result;
    }

    /**
     * Calculates the polychromy value using the specified method.
     *
     * @param points the list of RGB24 points
     * @param method the method to use for polychromy calculation
     * @return the polychromy value
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static Fraction calculatePolychromy(final List<RGB24> points, final PolychromyMethod method) {

        if (EUCLIDEAN_DISTANCE == method) return fr(calculateEuclideanDistance(points));
        return null;
    }

    /**
     * Generates a list of RGB24 points that are maximally far away from each other.
     *
     * @param n the number of points to generate
     * @return a list of maximally far away RGB24 points
     */
    @SuppressWarnings({"ObjectAllocationInLoop", "MethodWithMultipleLoops", "MethodWithMultipleReturnPoints", "WeakerAccess"
            , "NumericCastThatLosesPrecision"})
    public static List<RGB24> getMaximallyFarAwayPoints(final int n) {

        log(b(c(CALCULATING), DISTRIBUTED, s(n), COLOURS, SET));
        final List<RGB24> points = new ArrayList<>(0);
        final int max = 255;
        final double step = StrictMath.pow(1.0 / n, 1.0 / 3); // n-th root of 1
        for (double x = 0; 1.0 > x; x += step) {
            for (double y = 0; 1.0 > y; y += step) {
                for (double z = 0; 1.0 > z; z += step) {

                    // Add a new point
                    points.add(new RGB24((int) (x * max), (int) (y * max), (int) (z * max)));

                    // Stop when we have enough points
                    if (points.size() == n) {
                        return points;
                    }
                }
            }
        }
        return points;
    }

    /**
     * Calculates the maximum Euclidean dispersion for a given size.
     *
     * @param size the size of the points to generate
     * @return the maximum Euclidean dispersion value
     */
    @SuppressWarnings("WeakerAccess")
    public static double getMaximumEuclideanDispersion(final Integer size) {

        return getMaximumEuclideanDispersion(size, false);
    }

    /**
     * Calculates the maximum Euclidean dispersion for a given size.
     *
     * @param size   the size of the points to generate
     * @param silent flag to indicate whether to log the calculation process (true = silent, false = log)
     * @return the maximum Euclidean dispersion value
     */
    @SuppressWarnings({"NumericCastThatLosesPrecision", "MethodWithMultipleReturnPoints"})
    public static double getMaximumEuclideanDispersion(final Integer size, final boolean silent) {

        final String tag = d(MAX, DISPERSION, s(size));
        if (exec.data.containsKey(tag, silent)) return getInteger(tag, silent);
        else {
            log(b(c(CALCULATING), MAX, DISPERSION));
            final List<RGB24> points = getMaximallyFarAwayPoints(size);
            final int calculated = (int) getRawEuclideanDistance(points);
            log(b(c(MAX), DISPERSION, IS, s(calculated)));
            log(b(c(PERSISTING), MAX, DISPERSION, DOTS));
            exec.data.put(tag, calculated);
            return calculated;
        }
    }

    /**
     * Calculates the raw Euclidean distance polychromy value for a given list of RGB24 points.
     *
     * @param points the list of RGB24 points
     * @return the raw Euclidean distance polychromy value
     */
    @SuppressWarnings("MethodWithMultipleLoops")
    private static double getRawEuclideanDistance(final List<RGB24> points) {

        log(b(c(USING), c(EUCLIDEAN), c(DISTANCE), METHOD, WITH, yb(points.size()), POINTS));
        final Graph<RGB24, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        for (final RGB24 point : points) {
            graph.addVertex(point);
        }
        log(b(c(BUILDING), c(GRAPH), FROM, yb(graph.vertexSet().size()), VERTICES));
        for (final RGB24 point1 : points) {
            for (final RGB24 point2 : points) {
                if (!point1.equals(point2)) {
                    // find the Euclidean distance between point1 and point2
                    final double distance = distance(point1, point2);
                    final DefaultWeightedEdge edge = graph.addEdge(point1, point2);
                    if (isNotNull(edge)) graph.setEdgeWeight(edge, distance);
                }
            }
        }
        log(b(c(EXECUTING), c(KRUSKAL), c(MINIMUM), c(SPANNING), c(TREE), ALGORITHM, WITH, yb(graph.edgeSet().size()), EDGES));
        final SpanningTreeAlgorithm.SpanningTree<DefaultWeightedEdge> tree =
                new KruskalMinimumSpanningTree<>(graph).getSpanningTree();
        return tree.getWeight();
    }
}