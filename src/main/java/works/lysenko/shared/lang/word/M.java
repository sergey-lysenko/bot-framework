package works.lysenko.shared.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Shared language word string constants starting with 'M'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record M() {

    /**
     * String constant for "management".
     */
    public static final String MANAGEMENT = s(MA, NA, GE, ME, NT);

    /**
     * String constant for "many".
     */
    public static final String MANY = s(M, ANY);

    /**
     * String constant for "module".
     */
    public static final String MODULE = s(MO, DU, LE);

    /**
     * String constant for "modules".
     */
    public static final String MODULES = s(MO, DU, LE, S);
}
