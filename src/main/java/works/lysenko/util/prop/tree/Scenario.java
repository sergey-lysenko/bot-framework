package works.lysenko.util.prop.tree;

import static works.lysenko.util.func.data.Regexes.l1;
import static works.lysenko.util.spec.PropEnum.*;

/**
 * Represents a Scenario used in a system.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticMethodOnlyUsedInOneClass"})
public record Scenario() {

    /**
     * Represents a list of ignored items.
     */
    public static final String ignoredList = getIgnoredList();
    /**
     * Represents the default weight for a scenario.
     */
    public static final String defaultWeight = _DEFAULT_WEIGHT.get();
    /**
     * Represents the number of retries for selecting a scenario.
     */
    public static final Integer selectionRetries = _DEFAULT_SCENARIO_SUFFICIENCY_ATTEMPTS.get();
    /**
     * Indicates whether failed scenario selection should be ignored.
     */
    public static final boolean ignoreFailedSelection = _IGNORE_FAILED_SCENARIO_SELECTION.get();
    /**
     * Represents the root identifier or path for a scenario within the system.
     */
    public static final String root = _ROOT.get();

    private static String getIgnoredList() {

        final String list = _IGNORED_IN_STACKTRACE.get();
        for (final String item : list.split(l1)) {
            try {
                Class.forName(item, true, Thread.currentThread().getContextClassLoader());
            } catch (final ClassNotFoundException e) {
                // NOP TODO: implement validation
            }
        }
        return list;
    }
}
