package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.data.type.maps.SortedScenarioName;
import works.lysenko.util.spec.Level;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static works.lysenko.Base.*;
import static works.lysenko.base.output.File.fileScenariosStatistics;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.HAD;
import static works.lysenko.util.chrs.___.SET;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.enums.Severity.S3;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.assertEqualsSilent;
import static works.lysenko.util.func.core.Assertions.assertEqualsSilentEvent;
import static works.lysenko.util.lang.T.TEST_EXECUTION;
import static works.lysenko.util.lang.T.TO_BE;
import static works.lysenko.util.lang.word.A.AMONG;
import static works.lysenko.util.lang.word.A.AVAILABLE;
import static works.lysenko.util.lang.word.C.*;
import static works.lysenko.util.lang.word.D.DOWNSTREAM;
import static works.lysenko.util.lang.word.E.EXECUTED;
import static works.lysenko.util.lang.word.P.POSSIBLE;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.S.SCENARIOS;
import static works.lysenko.util.lang.word.S.STATISTICS;
import static works.lysenko.util.lang.word.T.THESE;
import static works.lysenko.util.lang.word.U.UNEXPECTED;
import static works.lysenko.util.lang.word.U.UPSTREAM;
import static works.lysenko.util.lang.word.W.WEIGHT;
import static works.lysenko.util.spec.Symbols.A;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * The Scenarios class represents a collection of scenarios and provides methods for aggregating and displaying statistics
 * about the scenarios.
 */
@SuppressWarnings({"FeatureEnvy", "ChainedMethodCall", "NestedMethodCall", "AutoBoxing", "ForeachStatement",
        "MethodWithMultipleLoops", "AccessToNonThreadSafeStaticField", "ImplicitNumericConversion", "LawOfDemeter"})
record Scenarios() {

    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("####0.0");

    private static Map<String, _Result> aggregateResult(final Map<? extends _Scenario, ? extends _Result> results) {

        final Map<String, _Result> aggResults = new SortedScenarioName();
        results.forEach((thisScenario, thisResult) ->
                updateAggregatedResult(aggResults, thisScenario, thisResult)
        );
        return aggResults;
    }

    /**
     * This method performs a series of assertions to compare the values of various properties of the provided scenarios.
     *
     * @param thisScenario     The current scenario being evaluated.
     * @param thisResult       The result of the current scenario.
     * @param aggregatedResult The aggregated result of all instances of this scenario.
     */
    private static void asserts(final _Scenario thisScenario, final _Result thisResult, final _Result aggregatedResult) {

        final String name = thisScenario.getName();
        final ScenarioType thisScenarioType = thisScenario.type();
        assertEqualsSilent(
                aggregatedResult.getScenarioType().name(),
                thisScenarioType.name(),
                b(c(UNEXPECTED), CHANGE, OF, SCENARIO, q(name), TYPE));
        final Fraction thisConfiguredWeight = thisResult.getConfiguredWeight();
        assertEqualsSilentEvent(
                aggregatedResult.getConfiguredWeight().doubleValue(),
                thisConfiguredWeight.doubleValue(),
                S3, b(c(CHANGE), OF, SCENARIO, q(name), CONFIGURED, WEIGHT));
        final Fraction thisUpstreamWeight = thisResult.getUpstreamWeight();
        assertEqualsSilentEvent(
                aggregatedResult.getUpstreamWeight().doubleValue(),
                thisUpstreamWeight.doubleValue(),
                S3, b(c(CHANGE), OF, SCENARIO, q(name), UPSTREAM, WEIGHT));
        final Fraction thisDownstreamWeight = thisResult.getDownstreamWeight();
        assertEqualsSilentEvent(
                aggregatedResult.getDownstreamWeight().doubleValue(),
                thisDownstreamWeight.doubleValue(),
                S3, b(c(CHANGE), OF, SCENARIO, q(name), DOWNSTREAM, WEIGHT));
    }

    /**
     * Calculate the percentage of active scenarios.
     *
     * @param total  Total number of scenario paths.
     * @param active Number of active scenario paths.
     * @return Percentage of active scenarios in string format.
     */
    private static String calculatePercentage(final int total, final int active) {

        final double percentageValue = (double) active / total * 100.0;
        return s(PERCENTAGE_FORMAT.format(percentageValue), "%");
    }

    /**
     * Displays statistics about the scenarios on the console.
     *
     * @param total      The total number of scenario paths.
     * @param active     The number of active scenario paths.
     * @param percentage The percentage of active scenarios.
     * @param sorted     A1 map containing the scenarios and their results.
     */
    private static void consoleScenariosStatistics(final int total, final int active, final String percentage,
                                                   final Map<String, _Result> sorted) {

        section(b(s1(sorted.size(), c(SCENARIO)), STATISTICS));
        log(Level.none, b(yb(total), s(PATH, s1(total)), WERE, POSSIBLE, WITH, CURRENT, SET, OF, c(SCENARIOS)), false);
        log(Level.none, b(yb(active), e(ROUND, percentage), AMONG, THESE, HAD, s(A), CHANCE, TO_BE, EXECUTED), false);
        if (sorted.isEmpty()) logEvent(S2, b(c(NO), TEST_EXECUTION, DATA, AVAILABLE));
        else {
            int longestNameLength = 0;
            for (final Map.Entry<String, _Result> e : sorted.entrySet())
                if (e.getKey().length() > longestNameLength) longestNameLength = e.getKey().length();
            for (final Map.Entry<String, _Result> scenarioResultEntry : sorted.entrySet()) {
                final String name = scenarioResultEntry.getKey();
                final int dots = longestNameLength - name.length();
                log(Level.none, b(scenarioResultEntry.getValue().getScenarioType().tag(),
                        s(name, s(_DOT_).repeat(dots)), scenarioResultEntry.getValue().toString()), false);
            }
        }
    }

    /**
     * Returns a new _Result object by aggregating the provided thisResult and aggregatedResult,
     * and combining their executions and instances.
     *
     * @param thisResult       The result of the current scenario.
     * @param aggregatedResult The aggregated result of all instances of this scenario.
     * @param theseEvents      The list of log records associated with this scenario.
     * @return The new aggregated _Result object.
     */
    private static _Result getNewResult(final _Result thisResult, final _Result aggregatedResult,
                                        final List<_LogRecord> theseEvents) {

        final int theseExecutions = thisResult.getExecutions() + aggregatedResult.getExecutions();

        // Instances
        final int theseInstances = 1 + aggregatedResult.getInstances();

        // New Result object
        return new Result(
                thisResult.getScenarioType(), thisResult.getConfiguredWeight(),
                thisResult.getUpstreamWeight(), thisResult.getDownstreamWeight(),
                theseEvents, theseExecutions, theseInstances);
    }

    /**
     * Runs the scenarios statistics.
     * <p>
     * This method retrieves the total number of scenario paths and the number of active scenario paths from the underlying
     * core object.
     * It then calculates the percentage of active scenarios. Next, it retrieves the scenario results and aggregates them.
     * Finally, it displays the statistics on the console and writes them to a file.
     */
    static void run() {

        final int total = core.getTotalScenarioPaths();
        final int active = core.getActiveScenarioPaths();
        final String percentage = calculatePercentage(total, active);
        final Map<_Scenario, _Result> scenarioResults = core.getResults().getSorted();
        final Map<String, _Result> aggregatedResults = aggregateResult(scenarioResults);
        consoleScenariosStatistics(total, active, percentage, aggregatedResults);
        fileScenariosStatistics(total, active, percentage, aggregatedResults);
    }

    private static void updateAggregatedResult(final Map<? super String, _Result> aggResults, final _Scenario thisScenario,
                                               final _Result thisResult) {

        final String thisScenarioName = thisScenario.getShortName();
        final _Result aggregatedResult = aggResults.get(thisScenarioName);

        if (isNull(aggregatedResult)) // Adding new result
            aggResults.put(thisScenarioName, thisResult);
        else { // Aggregating
            asserts(thisScenario, thisResult, aggregatedResult);
            // Events
            final List<_LogRecord> theseEvents = new ArrayList<>(thisResult.getEvents());
            theseEvents.addAll(aggregatedResult.getEvents());
            // Executions
            final _Result newResult = getNewResult(thisResult, aggregatedResult, theseEvents);
            aggResults.put(thisScenarioName, newResult);
        }
    }
}
