package works.lysenko.util.prop.tree;

import static works.lysenko.util.prop.PropEnum.*;

/**
 * Represents tree property that hides non-executable OFF branches.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass"})
public record HideOff() {

    /**
     * true if tree nodes that are entirely OFF should be hidden
     */
    public static final boolean branches = _TREE_HIDE_OFF_BRANCHES.get();
}
