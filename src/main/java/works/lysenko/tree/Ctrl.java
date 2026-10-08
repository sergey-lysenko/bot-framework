package works.lysenko.tree;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Ctrl;
import works.lysenko.util.apis.scenario._Pool;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.call.Selector;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.core.Time;
import works.lysenko.util.prop.tree.Include;
import works.lysenko.util.prop.tree.Scenario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.scenario._Node;

import static java.util.Objects.isNull;
import static works.lysenko.Base.core;
import static works.lysenko.Base.isDebug;
import static works.lysenko.util.call.selector.Utils.createReadableScenariosList;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.HAS;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.J.JUST;
import static works.lysenko.util.lang.word.M.MANUALLY;
import static works.lysenko.util.lang.word.N.NESTED;
import static works.lysenko.util.lang.word.P.PAUSED;
import static works.lysenko.util.lang.word.P.PERFORMED;
import static works.lysenko.util.lang.word.R.REQUESTED;
import static works.lysenko.util.lang.word.R.RESTART;
import static works.lysenko.util.lang.word.R.RESUMING;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.S.SCENARIOS;
import static works.lysenko.util.lang.word.S.SELECTING;
import static works.lysenko.util.lang.word.W.WAITING;
import static works.lysenko.util.lang.word.T.TRYING;
import static works.lysenko.util.lang.word.U.UNTIL;
import static works.lysenko.util.lang.word.W.WAITING;
import static works.lysenko.util.spec.Symbols.*;

/**
 * This class represents a set of scenarios and provides methods to manage and execute them.
 */
public class Ctrl extends Root implements _Ctrl {

    private final _Pool pool;
    private final _Scenario parent;
    private final int attempts;

    /**
     * Construct new instance
     *
     * @param parent Scenario
     */
    public Ctrl(final _Scenario parent) {

        this(n(0, Scenario.selectionRetries), parent);
    }

    /**
     * Construct new instance
     *
     * @param parent             Scenario
     * @param sufficiencyRetries number of retries while trying to select a scenario to be executed
     */
    private Ctrl(final int sufficiencyRetries, final _Scenario parent) {

        this.parent = parent;
        pool = new Pool(parent);
        attempts = sufficiencyRetries;
    }

    /**
     * This constructor creates a new instance of the Ctrl class and initializes its scenarios.
     * The scenarios are added to the parent scenario provided in the constructor, and the new instance
     * of Ctrl is assigned to the parent scenario's Ctrl field.
     *
     * @param parent The parent scenario
     * @param pool   The iterable collection of scenarios to be added
     */
    public Ctrl(final _Scenario parent, final Iterable<? extends _Scenario> pool) {

        this(parent);
        this.pool.add(pool);
    }

    @SuppressWarnings({"MethodCallInLoopCondition", "MethodWithMultipleReturnPoints"})
    public final boolean exec() throws SafeguardException {

        if (getWeightedList().isEmpty()) handleEmptyScenarios();
        else {
            handlePause();
            if (isHalt()) halt();
            else return isOk();
        }
        return false;
    }

    /**
     * Retrieves the number of retries for the current selection process.
     *
     * @return The number of retries remaining for the current selection process.
     */
    public final int getAttempts() {

        return attempts;
    }

    public final _Scenario getParent() {

        return parent;
    }

    /**
     * Calculates the number of possible combinations for underlying scenarios.
     *
     * @param onlyConfigured true if only the configured scenarios should be considered, false otherwise
     * @return the number of possible combinations for underlying scenarios
     */
    public final int getPathsCount(final boolean onlyConfigured) {

        int combinations = 0;
        for (final KeyValue<_Scenario, Fraction> scenario : getWeightedList())
            combinations = combinations + scenario.name().calculateCombinations(onlyConfigured);
        return combinations;
    }

    /**
     * Retrieves the weighted scenarios.
     *
     * @return The weighted scenarios.
     */
    public final _Pool getPool() {

        return pool;
    }

    /**
     * Retrieves the list of scenarios along with their weights.
     *
     * @return The list of scenarios along with their weights
     */
    public final List<KeyValue<_Scenario, Fraction>> getWeightedList() {

        return pool.getPairList();
    }

    @Override
    public final Set<_Scenario> getAccessibleLeafs() {

        final Set<_Scenario> leafs = new HashSet<>();
        collectAccessibleLeafs(getWeightedList(), leafs);
        return leafs;
    }

    private static void collectAccessibleLeafs(final Iterable<KeyValue<_Scenario, Fraction>> pairs, final Set<_Scenario> leafs) {

        if (isNull(pairs)) return;
        for (final KeyValue<_Scenario, Fraction> pair : pairs) {
            final _Scenario s = pair.k();
            if ((s.isExecutable() || isNotNull(s.weightConfigured())) && s.calculateCombinations(true) > 0) {
                if (s instanceof _Node node) {
                    collectAccessibleLeafs(node.getPool().getPairList(), leafs);
                } else {
                    leafs.add(s);
                }
            }
        }
    }

    @Override
    public final Set<_Scenario> getAccessibleNodes() {

        final Set<_Scenario> nodes = new HashSet<>();
        collectAccessibleNodes(getWeightedList(), nodes);
        return nodes;
    }

    private static void collectAccessibleNodes(final Iterable<KeyValue<_Scenario, Fraction>> pairs, final Set<_Scenario> nodes) {

        if (isNull(pairs)) return;
        for (final KeyValue<_Scenario, Fraction> pair : pairs) {
            final _Scenario s = pair.k();
            if ((s.isExecutable() || isNotNull(s.weightConfigured())) && s.calculateCombinations(true) > 0) {
                if (s instanceof _Node node) {
                    nodes.add(s);
                    collectAccessibleNodes(node.getPool().getPairList(), nodes);
                }
            }
        }
    }

    @Override
    public final Set<_Scenario> getAccessibleScenarios() {

        final Set<_Scenario> scenarios = new HashSet<>();
        collectAccessibleScenarios(getWeightedList(), scenarios);
        return scenarios;
    }

    private static void collectAccessibleScenarios(final Iterable<KeyValue<_Scenario, Fraction>> pairs, final Set<_Scenario> scenarios) {

        if (isNull(pairs)) return;
        for (final KeyValue<_Scenario, Fraction> pair : pairs) {
            final _Scenario s = pair.k();
            if ((s.isExecutable() || isNotNull(s.weightConfigured())) && s.calculateCombinations(true) > 0) {
                scenarios.add(s);
                if (s instanceof _Node node) {
                    collectAccessibleScenarios(node.getPool().getPairList(), scenarios);
                }
            }
        }
    }

    /**
     * Halts the execution by logging an event and updating the state of the halt button on the dashboard.
     */
    private void halt() {

        logEvent(S2, b(c(MANUALLY), REQUESTED, c(SCENARIO), HALT, PERFORMED));
        core.getDashboard().setHalt(false);
    }

    /**
     * Handles the case when the root of the scenarios is empty.
     * It logs an event with a failure message and sets the failure message in the core.
     */
    private void handleEmptyRoot() {

        final String failureMessage = b(c(ROOT), q(Scenario.root), HAS, NO, c(SCENARIOS));
        logEvent(S0, failureMessage);
        if (isNotNull(core)) {
            core.setStopReason(failureMessage);
            core.setStopFlag(true);
        }
    }

    /**
     * Handles empty scenarios.
     */
    private void handleEmptyScenarios() {

        if (isNull(parent)) handleEmptyRoot();
        else promptNoNestedScenarios();
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private boolean isOk() throws SafeguardException {

        final _Scenario toDo = select();
        if (isNotNull(toDo)) return toDo.isOk();
        else return false;
    }

    /**
     * Prompts that there are no nested scenarios.
     * It logs an event with the information of the parent scenario.
     */
    private void promptNoNestedScenarios() {

        logEvent(S2, b(SCENARIO, q(parent.getName()), HAS, NO, NESTED, s(SCENARIOS, _DOT_),
                c(IS), THIS, JUST, s(A), c(LEAF), s(SCENARIO, QUS_MRK)));
    }

    /**
     * Selects a scenario to execute based on the available candidates and execution options.
     * If a scenario is selected, but it does not satisfy the requirements, it may be removed from the candidates list.
     *
     * @implNote This method uses several helper methods to determine the execution candidates, select a scenario among the
     * candidates,
     * check if a scenario fits, and handle various logging and error conditions.
     */
    private _Scenario select() throws SafeguardException {

        final String list = b(createReadableScenariosList(getWeightedList(), (Include.upstream || Include.downstream),
                isDebug()));
        section(b(c(SELECTING), SCENARIO, FROM, list));
        return (new Selector(this, attempts).call());
    }

    /**
     * Handles manual pause during execution.
     * Logs once upon pausing, polls in 100ms intervals, and logs once upon resuming.
     */
    private void handlePause() {

        if (!isPause()) return;

        log(b(c(TEST), EXECUTION, s(PAUSED, _DOT_), WAITING, s(ELLIPSIS)));
        while (isPause()) {
            sleep(100L);
        }
        log(b(c(RESUMING), TEST, EXECUTION, s(ELLIPSIS)));
    }
}
