package works.lysenko.util.call;

import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.tree.Core;
import works.lysenko.tree.Ctrl;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.call.selector.Utils;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.tree.Include;
import works.lysenko.util.prop.tree.Notify;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.spec.Numbers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.core;
import static works.lysenko.Base.isDebug;
import static works.lysenko.Base.isTrace;
import static works.lysenko.util.call.selector.Utils.logEvent;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
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
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Symbols._COLON_;

/**
 * The Selector class represents a scenario selector in a control system.
 * It is responsible for selecting and executing scenarios based on their weights
 * and other criteria.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public final class Selector implements Callable<_Scenario> {

    private final Ctrl ctrl; // TODO: use interface instead of concrete class
    private int attemptsLeft;

    /**
     * Represents a Selector object that is responsible for selecting scenarios based on weights.
     *
     * @param ctrl     an instance of the Ctrl class which is responsible for the main control functionalities such as
     *                 scenario selections and decisions.
     * @param attempts the number of retry attempts left if the current selection process fails.
     */
    public Selector(final Ctrl ctrl, final int attempts) {

        this.ctrl = ctrl;
        attemptsLeft = attempts;
    }

    /**
     * Executes the scenario selection process to select a scenario based on weights.
     *
     * @return The selected scenario, or null if no scenario is selected.
     * @throws SafeguardException if an exception occurs during the scenario selection process.
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
                }
            }
            logStateDebug(fits, candidates);
        } while (Utils.isStillSelectable(fits, attemptsLeft, candidates));
        ctrl.sleep(1L, true);
        if (fits) return Utils.verifyScenarioSelection(scenario);
        if (candidates.isEmpty())
            selectionFailed(s(b(c(UNABLE), TO, FIT, ANY, SCENARIO, AMONG,
                    Utils.createReadableScenariosList(ctrl.getWeightedList(), false, isDebug()), s(_COLON_), ALL, SCENARIOS,
                    HAVE, UNMET, REQUIREMENTS, OR, ARE, NOT, EXECUTABLE)));
        if (!(0 < attemptsLeft)) logEvent(S2, b(c(SCENARIO), SELECTION, EXHAUSTED, AFTER, s1(ctrl.getAttempts(), ATTEMPT)));
        return null;
    }

    /**
     * Retrieves the execution candidates based on the scenarios and their weights.
     *
     * @return A1 list of pairs containing the selected scenarios and their adjusted weights.
     */
    @SuppressWarnings("ObjectAllocationInLoop")
    private List<KeyValue<_Scenario, Fraction>> getExecutionCandidates() {

        final List<KeyValue<_Scenario, Fraction>> candidates = new LinkedList<>();
        for (final KeyValue<_Scenario, Fraction> pair : ctrl.getWeightedList()) {
            final _Scenario scenario = pair.k();
            Fraction weight = pair.v();
            if (scenario.isExecutable()) {
                if (Include.upstream) weight = fr(weight.doubleValue() + scenario.weightUpstream().doubleValue());
                if (Include.downstream) weight = fr(weight.doubleValue() + downstreamWeight(scenario).doubleValue());
                final KeyValue<_Scenario, Fraction> newPair = kv(scenario, weight);
                candidates.add(newPair);
            } else Utils.logScenarioNotExecutable(scenario);
        }
        return candidates;
    }

    @SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "NestedConditionalExpression"})
    private void logStateDebug(final boolean fits, final Collection<KeyValue<_Scenario, Fraction>> candidates) {

        Utils.logDebug(b(false, L0, s(fits ? EMPTY : b(NO, FIT)), s(candidates.isEmpty() ? EMPTY : b(s1(candidates.size(),
                CANDIDATE))), s(0 < --attemptsLeft ? b(s(attemptsLeft), (Numbers.ONE == attemptsLeft) ? RETRY : RETRIES) :
                s(core.getStopFlag() ? STOP : EMPTY))));
    }

    /**
     * Selects a scenario among the given candidates based on their weights.
     *
     * @param candidates The list of candidate pairs consisting of ProvidesScenario objects and their weights
     * @return The selected scenario
     */
    @SuppressWarnings({"ChainedMethodCall", "ObjectAllocationInLoop"})
    private Core selectAmongCandidates(final Collection<KeyValue<_Scenario, Fraction>> candidates) {

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
     * Fails the scenario selection process and performs necessary logging.
     *
     * @param message The error message to log.
     */
    private void selectionFailed(final String message) {

        Utils.logAccounting();
        Utils.logDebug(a(kv(s(IGNORE, c(FAILED), c(SELECTION)), s(Scenario.ignoreFailedSelection))));

        if (Scenario.ignoreFailedSelection) {
            if (isTrace()) logEvent(S3, message);
        } else logEvent(S2, message);

        if (Scenario.ignoreFailedSelection) {
            if (isTrace()) logEvent(S3, b(c(TESTS), WILL, GO, ON, AS, REQUESTED));
        } else ctrl.stopTests();
    }

}