package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._TREE_FITS_BY_DEFAULT;

/**
 * Configuration for default tree node fitting behavior.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Fits() {

    /**
     * Indicates whether tree nodes should be fitted by default during layout calculation.
     */
    public static final boolean byDefault = _TREE_FITS_BY_DEFAULT.get();
}
