package works.lysenko.util.prop.tree;

import org.apache.commons.math3.fraction.Fraction;

import static works.lysenko.util.prop.PropEnum.*;

@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticNonFinalField"})
public record Wandering() {

    public static Fraction patience = _TREE_ALL_LEAFS_PATIENCE.get();
}
