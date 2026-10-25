package works.lysenko.shared.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.S;

/**
 * Shared language word string constants starting with 'R'.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record R() {

    /**
     * String constant for "report".
     */
    public static final String REPORT = s(RE, PO, RT);

    /**
     * String constant for "reports".
     */
    public static final String REPORTS = s(RE, PO, RT, S);

    /**
     * String constant for "rules".
     */
    public static final String RULES = s(RULE, S);
}
