package works.lysenko.util.lang.word;

import static works.lysenko.util.chrs.__.AY;
import static works.lysenko.util.chrs.__.ET;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.Y;

/**
 * Language utility words starting with Y.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record Y() {

    /** Constant representing the word YAY. */
    public static final String YAY = s(Y, AY);
    /** Constant representing the word YET. */
    public static final String YET = s(Y, ET);
}
