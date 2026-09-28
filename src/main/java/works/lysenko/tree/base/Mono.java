package works.lysenko.tree.base;

import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.data.enums.ScenarioType;

import static works.lysenko.util.data.enums.ScenarioType.MONO;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "AbstractClassWithOnlyOneDirectInheritor",
        "BooleanVariableAlwaysNegated"})
public abstract class Mono extends Leaf {

    private boolean executed = false;

    /**
     * @return whether this scenario still runnable or it had been executed already
     */

    @Override
    public final boolean isExecutable() {

        return !executed && super.isExecutable();
    }


    /**
     * For a Mono Scenario, execution consist of
     * <p>
     * 1) default execution code defined in super class
     * 2) logic defined in action()
     * 3) final steps defined in finals() of this scenario
     * 4) Adding record about end of test
     * 5) performing shared Scenario finalization procedure
     *
     * @return true if everything is ok
     */
    @Override
    public final boolean isOk() throws SafeguardException {

        if (super.isOk()) {
            executed = true;
            return true;
        } else return false;
    }

    public final ScenarioType type() {

        return MONO;
    }
}