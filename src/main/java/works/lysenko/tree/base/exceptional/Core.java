package works.lysenko.tree.base.exceptional;

import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._ExceptionalExecution;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.records.test.Triplet;

import static works.lysenko.util.data.enums.ScenarioType.CORE;

/**
 * This Scenario executed to handle Exceptions during test run
 */
@SuppressWarnings({"AbstractClassWithOnlyOneDirectInheritor", "BooleanVariableAlwaysNegated", "ClassNameSameAsAncestorName"})
public abstract class Core extends Leaf implements _ExceptionalExecution {

    private Exception exception = null;
    private Triplet triplet = null;
    private boolean resolved = false;

    public final Exception exception() {

        return exception;
    }

    public final Triplet triplet() {

        return triplet;
    }

    @SuppressWarnings({"UnnecessarySuperQualifier", "ParameterNameDiffersFromOverriddenParameter"})
    public final boolean isOk(final Exception e, final Triplet t) throws SafeguardException {

        exception = e;
        triplet = t;
        resolved = false;
        return super.isOk();
    }

    public final void markAsResolved() {

        resolved = true;
    }

    public final ScenarioType type() {

        return CORE;
    }

    public final boolean unresolved() {

        return !resolved;
    }
}