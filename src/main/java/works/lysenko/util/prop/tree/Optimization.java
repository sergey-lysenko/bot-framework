package works.lysenko.util.prop.tree;

import static works.lysenko.util.spec.PropEnum._TREE_PROGRESSION_OPTIMIZATION;

/**
 * Represents tree layout progression optimization configuration.
 */
@SuppressWarnings({"MissingJavadoc", "ClassWithoutLogger"})
public record Optimization() {

    public static boolean enabled = Boolean.TRUE.equals(_TREE_PROGRESSION_OPTIMIZATION.get());

    /**
     * Refreshes optimization status from properties.
     */
    public static void refresh() {
        enabled = Boolean.TRUE.equals(_TREE_PROGRESSION_OPTIMIZATION.get());
    }
}
