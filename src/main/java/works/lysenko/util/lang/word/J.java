package works.lysenko.util.lang.word;

import static works.lysenko.util.chrs.__.JU;
import static works.lysenko.util.chrs.__.ST;
import static works.lysenko.util.chrs.__.VA;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.A;
import static works.lysenko.util.spec.Symbols.GRV_ACC;
import static works.lysenko.util.spec.Symbols._LFD_;

/**
 * Language utility words starting with J.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record J() {

    private static final String FENCE = s(GRV_ACC, GRV_ACC, GRV_ACC);

    /** Constant representing the word JAVA. */
    public static final String JAVA = s(works.lysenko.util.spec.Symbols.J, A, VA);
    /** Constant representing the word JUST. */
    public static final String JUST = s(JU, ST);
    /** Constant representing Markdown Java code block closing fence. */
    public static final String JAVA_END = s(_LFD_, FENCE);
    /** Constant representing Markdown Java code block opening fence. */
    public static final String JAVA_START = s(JAVA_END, JAVA, _LFD_);
}
