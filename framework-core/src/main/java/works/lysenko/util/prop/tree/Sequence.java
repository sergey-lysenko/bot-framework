package works.lysenko.util.prop.tree;

import works.lysenko.util.data.enums.SequenceMode;
import works.lysenko.util.data.enums.Severity;

import static works.lysenko.util.prop.PropEnum._TREE_SEQUENCE_FAILURE_SEVERITY;
import static works.lysenko.util.prop.PropEnum._TREE_SEQUENCE_MODE;

/**
 * Provides cached scenario sequence execution configuration for a test execution.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticNonFinalField"})
public record Sequence() {

    /**
     * Represents the sequence execution mode.
     */
    public static SequenceMode mode = _TREE_SEQUENCE_MODE.get();

    /**
     * Represents the severity logged when sequence script order fails.
     */
    public static Severity severity = _TREE_SEQUENCE_FAILURE_SEVERITY.get();

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {
        mode = _TREE_SEQUENCE_MODE.get();
        severity = _TREE_SEQUENCE_FAILURE_SEVERITY.get();
    }
}
