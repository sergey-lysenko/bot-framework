package works.lysenko.util.prop.tree;

import static works.lysenko.util.prop.PropEnum._TREE_HIDE_OFF_BRANCHES;

/**
 * Represents tree property that hides non-executable OFF branches.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "StaticNonFinalField"})
public record HideOff() {

    /**
     * true if tree nodes that are entirely OFF should be hidden
     */
    public static boolean branches = _TREE_HIDE_OFF_BRANCHES.get();

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {
        branches = _TREE_HIDE_OFF_BRANCHES.get();
    }
}
