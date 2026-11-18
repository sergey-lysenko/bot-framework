package works.lysenko.tree.base.exceptional;

import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._ExceptionalExecution;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.records.test.Triplet;

import static works.lysenko.util.data.enums.ScenarioType.CORE;

/**
 * Base abstract exceptional scenario executed to handle exceptions during a test run.
 */
@SuppressWarnings({"AbstractClassWithOnlyOneDirectInheritor", "BooleanVariableAlwaysNegated", "ClassNameSameAsAncestorName"})
public abstract class Core extends Leaf implements _ExceptionalExecution {

    /**
     * The exception being processed by this exceptional execution.
     */
    private Exception exception = null;

    /**
     * Triplet holding diagnostic exception trace objects (ultimate, penultimate, antepenultimate).
     */
    private Triplet triplet = null;

    /**
     * Flag indicating whether the exception state was successfully resolved.
     */
    private boolean resolved = false;

    /**
     * Default constructor for Core exceptional scenario.
     */
    protected Core() {

    }

    /**
     * Retrieves the exception currently being processed.
     *
     * @return handled Exception instance
     */
    public final Exception exception() {

        return exception;
    }

    /**
     * Retrieves the diagnostic exception trace triplet.
     *
     * @return Triplet instance
     */
    public final Triplet triplet() {

        return triplet;
    }

    /**
     * Initializes exceptional context with exception and triplet and executes scenario lifecycle.
     *
     * @param e exception instance
     * @param t diagnostic exception triplet
     * @return true if execution was successful
     * @throws SafeguardException if execution safeguard triggers
     */
    @SuppressWarnings({"UnnecessarySuperQualifier", "ParameterNameDiffersFromOverriddenParameter"})
    public final boolean isOk(final Exception e, final Triplet t) throws SafeguardException {

        exception = e;
        triplet = t;
        resolved = false;
        return super.isOk();
    }

    /**
     * Marks the exception condition as resolved.
     */
    public final void markAsResolved() {

        resolved = true;
    }

    /**
     * Returns the scenario type for exceptional Core.
     *
     * @return ScenarioType.CORE
     */
    public final ScenarioType type() {

        return CORE;
    }

    /**
     * Checks if the exception condition remains unresolved.
     *
     * @return true if unresolved, false if resolved
     */
    public final boolean unresolved() {

        return !resolved;
    }
}