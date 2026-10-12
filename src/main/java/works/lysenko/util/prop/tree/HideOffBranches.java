package works.lysenko.util.prop.tree;

import static works.lysenko.util.spec.PropEnum._TREE_HIDE_OFF_BRANCHES;

/**
 * Represents tree property that hides non-executable OFF branches.
 */
@SuppressWarnings({"ClassWithoutLogger", "UtilityClass"})
public final class HideOffBranches {

    private HideOffBranches() {
    }

    /**
     * @return true if tree nodes that are entirely OFF should be hidden
     */
    public static boolean b() {

        return Boolean.TRUE.equals(_TREE_HIDE_OFF_BRANCHES.get());
    }
}
