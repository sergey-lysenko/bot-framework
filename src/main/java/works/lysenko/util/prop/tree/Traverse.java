package works.lysenko.util.prop.tree;

import static works.lysenko.util.spec.PropEnum._TREE_TRAVERSE_EXTENSIONS;

/**
 * Provides cached tree traversal configuration for a test execution.
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
public record Traverse() {

    public static Boolean extensions = _TREE_TRAVERSE_EXTENSIONS.get();

    /**
     * Refreshes traversal flags after loading a test configuration.
     */
    public static void refresh() {

        extensions = _TREE_TRAVERSE_EXTENSIONS.get();
    }
}
