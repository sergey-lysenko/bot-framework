package works.lysenko.util.prop.tree;

import static works.lysenko.util.spec.PropEnum._TREE_TRAVERSE_EXTENSIONS;

/**
 * Provides cached tree traversal configuration for a test execution.
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
public record Traverse() {

    public static boolean extensions = Boolean.TRUE.equals(_TREE_TRAVERSE_EXTENSIONS.get());
    private static boolean frozen = false;

    /**
     * Refreshes traversal flags after loading a test configuration.
     */
    public static void refresh() {

        if (!frozen) extensions = Boolean.TRUE.equals(_TREE_TRAVERSE_EXTENSIONS.get());
    }

    /**
     * Prevents configuration-preview refreshes from changing traversal behavior during a test run.
     */
    public static void freeze() {

        refresh();
        frozen = true;
    }

    /**
     * Allows the next test configuration to refresh traversal behavior.
     */
    public static void unfreeze() {

        frozen = false;
    }
}
