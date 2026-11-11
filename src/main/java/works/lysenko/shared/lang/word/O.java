package works.lysenko.shared.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Shared language word string constants starting with 'O'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record O() {

    /**
     * String constant for "orders".
     */
    public static final String ORDERS = s(OR, DE, RS);

    /**
     * String constant for "open".
     */
    public static final String OPEN = s(OP, EN);

    public static final String OWNER = s(OW, N, ER);
}
