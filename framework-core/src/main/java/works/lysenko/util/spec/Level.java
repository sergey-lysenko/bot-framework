package works.lysenko.util.spec;

import static works.lysenko.util.spec.Numbers.*;

/**
 * Defines logging and verbosity levels using named numeric constants.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Level() {

    /** Debug logging level. */
    public static final int debug = THREE;
    /** No logging level. */
    public static final int none = ZERO;
    /** Plain logging level. */
    public static final int plain = ONE;
    /** Supplementary logging level. */
    public static final int supplementary = TWO;
}
