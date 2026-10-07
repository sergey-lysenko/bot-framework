package works.lysenko.tree.base;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.tree.Core;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.exception.unchecked.ScenarioRuntimeException;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.Brackets;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.sets.SortedScenario;

import works.lysenko.Base;
import works.lysenko.util.prop.tree.Scenario;

import java.util.Set;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.enums.ScenarioType.LEAF;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.lang.S.SCENARIO_EXECUTION_FAILED_DUE_TO;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "NestedMethodCall"})
public abstract class Leaf extends Core {

    /**
     * @param weight for Scenario
     */
    protected Leaf(final Fraction weight) {

        super(weight);
    }

    /**
     * This class is an abstract implementation of a Leaf Scenario.
     * It provides default execution code and defines methods for custom logic and finalization steps.
     */
    protected Leaf() {

    }

    /**
     * Determines whether this leaf scenario has reached or exceeded its target execution goal.
     *
     * @return true if execution goal is fulfilled, false otherwise
     */
    public boolean isGoalFulfilled() {

        if (isNull(Base.core) || isNull(Base.core.getResults())) return false;
        final int target = (this instanceof Mono) ? 1 : works.lysenko.base.Core.resolveAllLeafsTarget();
        return target <= Base.core.getResults().getExecutions(this);
    }

    @Override
    public boolean isExecutable() {

        if (hasFailed()) return false;
        if (Scenario.forbidOverexecution && isGoalFulfilled()) return false;
        return super.isExecutable();
    }

    /**
     * Upstream weight of Leaf scenario is always zero.
     *
     * @return the upstream weight of the scenario
     */
    @Override
    public final Fraction weightUpstream() {

        return Fraction.ZERO;
    }

    /**
     * For a Leaf Scenario, execution consist of
     * <p>
     * 1) default execution code defined in super class
     * 2) logic defined in action()
     * 3) final steps defined in finals() of this scenario
     * 4) Adding record about end of test
     * 5) performing shared Scenario finalization procedure
     *
     * @return true if execution was successful
     */
    @SuppressWarnings("DesignForExtension")
    @Override
    public boolean isOk() throws SafeguardException {

        if (super.isOk()) {
            try {
                action();
                finals();
                return true;
            } catch (final RuntimeException e) {
                markAsFailed();
                logEvent(works.lysenko.util.data.enums.Severity.S0, e.getMessage());
                throw new ScenarioRuntimeException(b(type().tag(), q(getShortName()), SCENARIO_EXECUTION_FAILED_DUE_TO,
                        e(Brackets.CURLY, e.getMessage())), e, this);
            } finally {
                done();
            }
        } else return false;
    }

    /**
     * Returns the set with one element - name of the scenario in requested format
     *
     * @return set object with this scenario as single element
     */
    @Override
    public final Set<_Scenario> list() {

        final Set<_Scenario> scenarios = new SortedScenario();
        scenarios.add(this);
        return scenarios;
    }

    @SuppressWarnings("DesignForExtension")
    public ScenarioType type() {

        return LEAF;
    }
}