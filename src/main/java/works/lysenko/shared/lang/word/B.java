package works.lysenko.shared.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.X;

/**
 * Shared language word string constants starting with 'B'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record B() {

    /**
     * String constant for "box".
     */
    public static final String BOX = s(BO, X);

    /**
     * String constant for "billing".
     */
    public static final String BILLING = s(BI, LL, ING);
}
