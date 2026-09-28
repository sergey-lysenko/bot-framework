package works.lysenko.util.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.KEY;
import static works.lysenko.util.chrs.___.OWN;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.K;
import static works.lysenko.util.spec.Symbols.S;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record K() {

    public static final String KEYS = s(KEY, S);
    public static final String KNOWN = s(KN, OWN);
    public static final String KRUSKAL = s(K, RU, SK, AL);
}
