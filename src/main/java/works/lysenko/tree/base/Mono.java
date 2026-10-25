package works.lysenko.tree.base;

import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.data.enums.ScenarioType;

import static works.lysenko.util.data.enums.ScenarioType.MONO;

/**
 * Represents a single-execution (Mono) scenario that runs at most once during a test suite execution.
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "AbstractClassWithOnlyOneDirectInheritor",
        "BooleanVariableAlwaysNegated"})
public abstract class Mono extends Leaf {

    /**
     * Flag tracking whether this single-execution scenario has already been executed.
     */
    private boolean executed = false;

    /**
     * Constructs a Mono scenario with default configuration.
     */
    protected Mono() {

    }

    /**
     * Checks if this single-execution scenario is still executable (has not been run yet).
     *
     * @return true if not executed and super isExecutable() returns true, false otherwise
     */
    @Override
    public final boolean isExecutable() {

        return !executed && super.isExecutable();
    }

    /**
     * Executes the scenario and marks it as executed.
     *
     * @return true if execution succeeded, false otherwise
     * @throws SafeguardException if execution safeguard triggers
     */
    @Override
    public final boolean isOk() throws SafeguardException {

        if (super.isOk()) {
            executed = true;
            return true;
        } else return false;
    }

    /**
     * Returns the scenario type for Mono.
     *
     * @return ScenarioType.MONO
     */
    public final ScenarioType type() {

        return MONO;
    }
}