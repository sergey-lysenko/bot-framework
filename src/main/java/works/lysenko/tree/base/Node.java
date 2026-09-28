package works.lysenko.tree.base;

import org.apache.commons.math3.fraction.Fraction;
import org.openqa.selenium.NoSuchSessionException;
import works.lysenko.tree.Core;
import works.lysenko.tree.Ctrl;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.exception.unchecked.ScenarioRuntimeException;
import works.lysenko.util.apis.scenario._Node;
import works.lysenko.util.apis.scenario._Pool;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.Brackets;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.func.core.ClassLoader;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.NEW;
import static works.lysenko.util.chrs.___.SUB;
import static works.lysenko.util.chrs.____.LOST;
import static works.lysenko.util.data.enums.ScenarioType.NODE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.core.Weights.upstreamWeight;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.S.SCENARIO_EXECUTION_FAILED_DUE_TO;
import static works.lysenko.util.lang.word.A.ASSIGNING;
import static works.lysenko.util.lang.word.H.HALTED;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.S.SESSION;
import static works.lysenko.util.spec.Symbols.*;

/**
 * This is an abstract implementation of Node Scenario, which have defined both
 * actions to be performed and the set of child scenarios to be executed
 * randomly based on their weight coefficients
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"UseOfConcreteClass", "ThisEscapedInObjectConstruction", "DesignForExtension", "OverloadedVarargsMethod"})
public abstract class Node extends Core implements _Node {

    private Ctrl ctrl;
    private boolean halted = false;

    /**
     * Default constructor adds all scenarios from properly named package as sub-scenarios of one being created
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    protected Node() {

        ctrl = new Ctrl(this);
        String name = getClass().getName();
        { // Strings are immutable, we need to create a new one
            final int i = name.lastIndexOf(_DOT_) + 1; // save the position of character in question
            final char[] nameChars = name.toCharArray(); // create character array
            nameChars[i] = Character.toLowerCase(nameChars[i]); // Lowercase single char
            name = String.valueOf(nameChars); // DO NOT replace with s();
        }
        ctrl.getPool().add(ClassLoader.readFrom(name, false));
    }

    /**
     * Default constructor adds all scenarios from properly named package as sub-scenarios of one being created
     *
     * @param weight of this scenario
     */
    protected Node(final Fraction weight) {

        super(weight);
        ctrl = new Ctrl(this);
        String name = getClass().getName();
        { // Strings are immutable, we need to create a new one
            final int i = name.lastIndexOf(_DOT_) + 1; // save the position of character in question
            final char[] nameChars = name.toCharArray(); // create character array
            nameChars[i] = Character.toLowerCase(nameChars[i]); // Lowercase single char
            name = String.valueOf(nameChars); // DO NOT replace with s();
        }
        ctrl.getPool().add(ClassLoader.readFrom(name, false));
    }

    /**
     * Create an instance of Node Scenario with defined set of sub-scenarios
     *
     * @param scenarios array of child scenarios with weight coefficient defined within
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    protected Node(final _Scenario... scenarios) {

        if (isNotNull(scenarios)) ctrl = new Ctrl(this, new HashSet<>(Arrays.asList(scenarios)));
        else ctrl = new Ctrl(this);
    }

    /**
     * Create a Node Scenario and add all scenarios from defined package as
     * sub-scenarios to this one
     *
     * @param s name of package to load child scenarios from with weight coefficient
     *          defined within
     *          of Run object
     */
    @SuppressWarnings("unused")
    protected Node(final String s) {

        ctrl = new Ctrl(this, ClassLoader.readFrom(s, false));
    }

    /**
     * @param onlyConfigured whether to include all available paths or only ones
     *                       currently configured for execution
     * @return calculated amount of possible execution paths of underlying scenarios
     */
    @Override
    public final int calculateCombinations(final boolean onlyConfigured) {

        int combinations = 0;
        for (final KeyValue<_Scenario, Fraction> scenario : ctrl.getWeightedList())
            combinations = combinations + scenario.name().calculateCombinations(onlyConfigured);
        return combinations;
    }

    public final _Pool getPool() {

        return ctrl.getPool();
    }

    @Override
    public final Fraction weightUpstream() {

        return upstreamWeight(this);
    }

    /**
     * Avoid starting of sub-scenarios once during current test test.
     */
    public final void halt() {

        logDebug(b(getClass().getName(), HALTED));
        halted = true;
    }

    @SuppressWarnings("DesignForExtension")
    @Override
    public String info() {

        return (isScenarioHalted()) ? HALTED : EMPTY;
    }

    /**
     * For a Node Scenario, execution consist of
     * <p>
     * 1) default execution code defined in super class
     * <p>
     * 2) logic defined in action()
     * <p>
     * 3) random execution of one of nested scenarios
     * <p>
     * 4) final steps defined in finals() of this scenario
     *
     * @return true if test was executed nominally
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public final boolean isOk() throws SafeguardException {

        if (super.isOk()) {
            try {
                action();
                boolean ok = false;
                if (halted) halted = false;
                else ok = ctrl.exec();
                finals();
                done();
                return ok;
            } catch (final NoSuchSessionException e) {
                fail(b(c(SESSION), LOST));
                stopTests();
                return false;
            } catch (final RuntimeException e) {
                throw new ScenarioRuntimeException(b(type().tag(), q(getShortName()), SCENARIO_EXECUTION_FAILED_DUE_TO,
                        e(Brackets.CURLY, e.getMessage())), e, this);
            }
        } else return false;
    }

    /**
     * @return true if the current scenario execution is halted
     */
    public final boolean isScenarioHalted() {

        if (halted) logDebug(b(getClass().getName(), HALTED));
        return halted;
    }

    /**
     * Returns the set with names of all underlying scenarios
     *
     * @return set of scenarios
     */
    @Override
    public final Set<_Scenario> list() {

        final Set<_Scenario> list = super.list();
        list.addAll(ctrl.getPool().getSortedSet());
        return list;
    }

    public final void setSubScenarios(final _Scenario... newScenarios) {

        setSubScenarios(null, newScenarios);

    }

    public final void setSubScenarios(final Fraction weight, final _Scenario... newScenarios) {

        ctrl = new Ctrl(this);
        if (isNotNull(newScenarios)) { // TODO: reimplement this
            final Collection<String> list = new ArrayList<>(1);
            for (final _Scenario scenario : newScenarios)
                list.add(scenario.getName()); // TODO: update framework to allow using of shortName() here
            logDebug(b(b(c(ASSIGNING), NEW, s(SUB, _DASH_, SCENARIO, S, _COLON_)), list.toString(), TO, SCENARIO,
                    q(getName())));
            ctrl.getPool().add(new HashSet<>(Arrays.asList(newScenarios)), weight);
        }
    }

    public final void stopTest(final Severity severity) {

        super.stopTest(severity);
        halt();
    }

    public final ScenarioType type() {

        return NODE;
    }
}
