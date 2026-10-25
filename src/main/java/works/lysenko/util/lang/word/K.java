package works.lysenko.util.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.KEY;
import static works.lysenko.util.chrs.___.OWN;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.K;
import static works.lysenko.util.spec.Symbols.S;

/**
 * Language utility words starting with K.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record K() {

    /** Constant representing the word KEYS. */
    public static final String KEYS = s(KEY, S);
    /** Constant representing the word KNOWN. */
    public static final String KNOWN = s(KN, OWN);
    /** Constant representing the word KRUSKAL. */
    public static final String KRUSKAL = s(K, RU, SK, AL);
}
