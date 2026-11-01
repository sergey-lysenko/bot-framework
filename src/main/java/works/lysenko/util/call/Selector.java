package works.lysenko.util.call;

import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.base.output.SequenceTracker;
import works.lysenko.tree.Core;
import works.lysenko.tree.Ctrl;
import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.scenario._Node;
import java.util.HashSet;
import java.util.Set;
import static java.util.Objects.isNull;
import works.lysenko.util.call.selector.Utils;
import works.lysenko.util.data.enums.SequenceMode;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.core.*;
import works.lysenko.util.prop.tree.Include;
import works.lysenko.util.prop.tree.Notify;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.spec.Numbers;
import works.lysenko.util.prop.PropEnum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.core;
import static works.lysenko.Base.parameters;
import static works.lysenko.Base.isDebug;
import static works.lysenko.Base.isTrace;
import static works.lysenko.util.call.selector.Utils.*;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.enums.Severity.S3;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Weights.downstreamWeight;
import static works.lysenko.util.func.math.EnumeratedDistributions.enumDis;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.lang.U.UNABLE_TO_SELECT_A_SCENARIO___;
import static works.lysenko.util.lang.word.A.*;
import static works.lysenko.util.lang.word.C.CANDIDATE;
import static works.lysenko.util.lang.word.C.CUMULATIVE;
import static works.lysenko.util.lang.word.E.EXECUTABLE;
import static works.lysenko.util.lang.word.E.EXHAUSTED;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.I.IGNORE;
import static works.lysenko.util.lang.word.N.NEGATIVE;
import static works.lysenko.util.lang.word.P.PROBABILITY;
import static works.lysenko.util.lang.word.P.PROPOSED;
import static works.lysenko.util.lang.word.R.*;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.S.SCENARIOS;
import static works.lysenko.util.lang.word.S.SELECTION;
import static works.lysenko.util.lang.word.T.TESTS;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.U.UNMET;
import static works.lysenko.util.lang.word.W.WEIGHTS;
import static works.lysenko.util.prop.core.Mode.*;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Symbols._COLON_;

/**
 * Scenario selector for the control system.
 * <p>
 * Responsible for selecting and executing scenarios based on candidate weights,
 * dynamic weight adjustments (upstream, downstream, and completion deficits),
 * registered sequence scripts, and runtime prerequisite checks.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public final class Selector implements Callable<_Scenario> {

    /** The controller instance managing scenario candidate pools and execution state. */
    private final Ctrl ctrl; // TODO: use interface instead of concrete class

    /** The number of selection attempts remaining before giving up. */
    private int attemptsLeft;

    /**
     * Constructs a {@code Selector} bound to a specific controller and maximum retry limit.
     *
     * @param ctrl     the controller instance managing scenario selections and decisions
     * @param attempts the maximum number of retry attempts allowed if candidates do not fit
     */
    public Selector(final Ctrl ctrl, final int attempts) {

        this.ctrl = ctrl;
        attemptsLeft = attempts;
    }

    /**
     * Executes the scenario selection workflow.
     * <p>
     * Evaluates executable scenario candidates, applies dynamic weights, and selects a scenario
     * either via sequence script matching or weighted sampling. Each selected scenario is verified
     * against {@link Core#fits()}. Non-fitting candidates are removed, and selection retries until
     * a valid scenario is found or attempts are exhausted.
     *
     * @return the selected {@link _Scenario} that satisfies all fit requirements, or {@code null} if selection fails
     * @throws SafeguardException if an error occurs during selection verification
     */
    @Override
    public _Scenario call() throws SafeguardException {

        final List<KeyValue<_Scenario, Fraction>> candidates = getExecutionCandidates();
        Core scenario;
        boolean fits = false;
        do {
            scenario = selectAmongCandidates(candidates);
            if (isNotNull(scenario)) {
                fits = scenario.fits();
                if (!fits) {
                    if (Notify.doesNotFit) logEvent(S3, b(c(SCENARIO), q(scenario.getShortName()), DOES, NOT, FIT));
                    candidates.remove(Utils.getThePair(scenario, candidates));
                    if (Scenario.unreachableLeafPruning) {
                        scenario.markAsExhausted();
                    }
                    if (Scenario.branchStalePenalty) {
                        scenario.incrementStaleCount();
                    }
                }
            }
            logStateDebug(fits, candidates);
        } while (isStillSelectable(fits, attemptsLeft, candidates));
        ctrl.sleep(1L, true);
        if (fits) return verifyScenarioSelection(scenario);
        if (candidates.isEmpty()) {
            final String message = s(b(c(UNABLE), TO, FIT, ANY, SCENARIO, AMONG,
                    createReadableScenariosList(ctrl.getWeightedList(), false, isDebug()), s(_COLON_), ALL, SCENARIOS,
                    HAVE, UNMET, REQUIREMENTS, OR, ARE, NOT, EXECUTABLE));
            final boolean isAllLeafs = (isNotNull(works.lysenko.Base.parameters) && works.lysenko.Base.parameters.isAllLeafs()) 
                || Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get());
            if (isAllLeafs || resilient) {
                logAccounting();
                logDebug(a(kv(s(IGNORE, c(FAILED), c(SELECTION)), String.valueOf(true))));
                logEvent(S3, message);
                logEvent(S3, b(c(TESTS), WILL, GO, ON, AS, REQUESTED));
                if (Scenario.branchStalePenalty && isNotNull(ctrl.getParent())) {
                    ctrl.getParent().incrementStaleCount();
                }
            } else {
                if (isNotNull(ctrl.getParent())) {
                    ctrl.getParent().markAsExhausted();
                }
                selectionFailed(message);
            }
        }
        if (!(0 < attemptsLeft)) logEvent(S2, b(c(SCENARIO), SELECTION, EXHAUSTED, AFTER, s1(ctrl.getAttempts(), ATTEMPT)));
        return null;
    }

    /**
     * Builds the list of execution candidates from the controller's pool.
     * <p>
     * Filters out non-executable scenarios and calculates adjusted candidate weights by applying
     * upstream weights, downstream weights, and completion deficit bonuses based on uncompleted target executions.
     *
     * @return a list of candidate {@link KeyValue} pairs containing executable scenarios and their adjusted {@link Fraction} weights
     */
    @SuppressWarnings("ObjectAllocationInLoop")
    List<KeyValue<_Scenario, Fraction>> getExecutionCandidates() {

        final int target = (isNotNull(parameters))
                ? parameters.getAllLeafsExecutions()
                : (isNotNull(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) ? Math.max(1, PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) : 1);
        final List<KeyValue<_Scenario, Fraction>> candidates = new LinkedList<>();
        for (final KeyValue<_Scenario, Fraction> pair : ctrl.getWeightedList()) {
            final _Scenario scenario = pair.k();
            Fraction weight = pair.v();
            if (scenario.isExecutable()) {
                if (Include.upstream) weight = fr(weight.doubleValue() + scenario.weightUpstream().doubleValue());
                if (Include.downstream) weight = fr(weight.doubleValue() + downstreamWeight(scenario, ctrl.getParent()).doubleValue());
                if (isNotNull(weight) && 0.0 < weight.doubleValue()
                        && isNotNull(core) && isNotNull(core.getResults())
                        && isNotNull(Scenario.completionWeight)) {
                    final double uncompletedWeight = getUncompletedWeight(scenario, target);
                    if (0.0 < uncompletedWeight) {
                        weight = weight.add(fr(Scenario.completionWeight.doubleValue() * uncompletedWeight));
                    }
                }
                if (Scenario.branchStalePenalty && scenario.getStaleCount() > 0) {
                    final double penaltyDivisor = 1.0 + scenario.getStaleCount();
                    weight = fr(weight.doubleValue() / penaltyDivisor);
                }
                final KeyValue<_Scenario, Fraction> newPair = kv(scenario, weight);
                candidates.add(newPair);
            } else Utils.logScenarioNotExecutable(scenario);
        }
        return candidates;
    }

    /**
     * Sums the remaining execution deficits of a scenario's accessible leaf descendants.
     * <p>
     * For single scenarios, calculates the uncompleted execution ratio. For composite nodes ({@link _Node}),
     * recursively aggregates the deficits of all child leaves to attract proportionally more completion weight.
     *
     * @param scenario the scenario or node to evaluate
     * @param target   the target execution count
     * @return the total completion deficit across accessible leaf descendants
     */
    static double getUncompletedWeight(final _Scenario scenario, final int target) {

        if (isNull(core) || isNull(core.getResults()) || target <= 0) return 0.0;
        if (scenario instanceof _Node node) {
            return getDescendantUncompletedWeight(node, target, new HashSet<>());
        }
        return getLeafUncompletedRatio(scenario, target);
    }

    /**
     * Recursively computes the uncompleted weight for node descendants while tracking visited nodes to avoid cycles.
     * <p>
     * When {@link Scenario#branchStalePenalty} is enabled, discounts each child's contribution by
     * {@code 1 + child.getStaleCount()} so repeatedly failing subtrees lose completion-weight gravity
     * at ancestor selection points without penalising healthy sibling branches.
     *
     * @param node    the parent node whose descendants are being traversed
     * @param target  the target execution count for leaves
     * @param visited the set of already visited node objects to prevent infinite recursion
     * @return the aggregated uncompleted weight across all executable descendant leaves
     */
    private static double getDescendantUncompletedWeight(final _Node node, final int target, final Set<Object> visited) {

        if (isNull(node) || isNull(node.getPool()) || !visited.add(node)) return 0.0;
        final List<KeyValue<_Scenario, Fraction>> pairs = node.getPool().getPairList();
        if (isNull(pairs)) return 0.0;
        double total = 0.0;
        for (final KeyValue<_Scenario, Fraction> pair : pairs) {
            final _Scenario child = pair.k();
            if (isNotNull(child) && child.isExecutable() && child.calculateCombinations(true) > 0) {
                double childUncompleted = (child instanceof _Node childNode)
                        ? getDescendantUncompletedWeight(childNode, target, visited)
                        : getLeafUncompletedRatio(child, target);
                if (Scenario.branchStalePenalty && child.getStaleCount() > 0) {
                    childUncompleted /= (1.0 + child.getStaleCount());
                }
                total += childUncompleted;
            }
        }
        return total;
    }

    /**
     * Calculates the remaining uncompleted execution deficit ratio for a single leaf scenario.
     *
     * @param leaf   the leaf scenario to evaluate
     * @param target the target execution count
     * @return a ratio between {@code 0.0} (fully completed) and {@code 1.0} (unexecuted) representing remaining execution deficit
     */
    private static double getLeafUncompletedRatio(final _Scenario leaf, final int target) {

        final int leafTarget = (leaf instanceof Mono) ? 1 : target;
        final int executions = core.getResults().getExecutions(leaf);
        return (executions < leafTarget) ? (double) (leafTarget - executions) / leafTarget : 0.0;
    }

    /**
     * Checks if a scenario or any of its executable descendants have remaining unexecuted target state.
     * <p>
     * Retained for backward compatibility.
     *
     * @param scenario the scenario to check
     * @return {@code true} if the scenario or any descendant leaves have unfinished target executions; {@code false} otherwise
     */
    static boolean hasUnexecutedState(final _Scenario scenario) {

        return getUncompletedWeight(scenario, 1) > 0.0;
    }

    /**
     * Logs debug information detailing the current retry state and candidate pool size.
     *
     * @param fits       whether the currently sampled scenario satisfied runtime prerequisites
     * @param candidates the collection of remaining candidate scenario pairs
     */
    @SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "NestedConditionalExpression"})
    private void logStateDebug(final boolean fits, final Collection<KeyValue<_Scenario, Fraction>> candidates) {

        Utils.logDebug(b(false, L0, s(fits ? EMPTY : b(NO, FIT)), s(candidates.isEmpty() ? EMPTY : b(s1(candidates.size(),
                CANDIDATE))), s(0 < --attemptsLeft ? b(s(attemptsLeft), (Numbers.ONE == attemptsLeft) ? RETRY : RETRIES) :
                s(core.getStopFlag() ? STOP : EMPTY))));
    }

    /**
     * Selects a single scenario candidate from the candidate list.
     * <p>
     * First attempts to match against an active sequence script in {@link SequenceTracker}.
     * If no script match occurs, performs weighted random sampling using an enumerated probability distribution.
     *
     * @param candidates the collection of candidate scenario-weight pairs
     * @return the selected {@link Core} scenario
     */
    @SuppressWarnings({"ChainedMethodCall", "ObjectAllocationInLoop"})
    private Core selectAmongCandidates(final Collection<KeyValue<_Scenario, Fraction>> candidates) {

        if (SequenceTracker.hasActiveScript()) {
            final String expected = SequenceTracker.peekNextScenario();
            if (expected != null && !expected.isBlank()) {
                for (final KeyValue<_Scenario, Fraction> candidate : candidates) {
                    final _Scenario sc = candidate.k();
                    final String classKey = SequenceTracker.getScenarioClassKey(sc);
                    if (expected.equalsIgnoreCase(sc.getShortName()) || expected.equalsIgnoreCase(sc.getName()) || expected.equalsIgnoreCase(classKey)) {
                        SequenceTracker.advanceSequence();
                        Utils.logDebug(b(q(sc.getShortName()), "scripted"));
                        return (Core) sc;
                    }
                }
                final SequenceMode mode = PropEnum._TREE_SEQUENCE_MODE.get();
                if (SequenceMode.ORDER.equals(mode)) {
                    final Severity sev = PropEnum._TREE_SEQUENCE_FAILURE_SEVERITY.get();
                    logEvent(sev != null ? sev : Severity.S0, "Sequence script order failed: scenario '" + expected + "' is not available among candidates");
                } else {
                    Utils.logDebug("Sequence script suggested scenario '" + expected + "' is not available; falling back to weighted selection");
                }
            }
        }

        Core scenario = null;
        final Collection<KeyValue<_Scenario, Double>> doubleCandidates = new ArrayList<>(0);
        try {
            // Convert the List<Pair<_Scenario, Fraction>> to List<Pair<_Scenario, Double>>
            for (final KeyValue<_Scenario, Fraction> candidate : candidates)
                doubleCandidates.add(kv(candidate.k(), candidate.v().doubleValue()));
            scenario = (Core) enumDis(doubleCandidates).sample();
            Utils.logDebug(b(q(scenario.getShortName()), PROPOSED, FROM, s1(candidates.size(), CANDIDATE)));
        } catch (final MathArithmeticException ex) {
            selectionFailed(b(UNABLE_TO_SELECT_A_SCENARIO___, s(Utils.createReadableScenariosList(candidates, true,
                    isDebug()), _COLON_), CUMULATIVE, PROBABILITY, IS, works.lysenko.util.lang.word.Z.ZERO));
        } catch (final NotPositiveException ex) {
            selectionFailed(b(UNABLE_TO_SELECT_A_SCENARIO___, s(Utils.createReadableScenariosList(candidates, true,
                    isDebug()), _COLON_), NEGATIVE, WEIGHTS, ARE, NOT, ALLOWED));
        }
        return scenario;
    }

    /**
     * Handles selection failure processing, accounting logs, and stopping test execution if configured.
     *
     * @param message the failure error message to log
     */
    private void selectionFailed(final String message) {

        logAccounting();
        Utils.logDebug(a(kv(s(IGNORE, c(FAILED), c(SELECTION)), s(Scenario.ignoreFailedSelection))));

        if (Scenario.ignoreFailedSelection) {
            logEvent(S3, message);
            logEvent(S3, b(c(TESTS), WILL, GO, ON, AS, REQUESTED));
        } else {
            logEvent(S0, message);
            if (isNull(ctrl.getParent())) {
                ctrl.stopTests(message);
            }
        }
    }

}
