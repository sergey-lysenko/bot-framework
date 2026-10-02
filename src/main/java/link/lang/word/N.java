package interlink.lang.word;

import static works.lysenko.util.chrs.__.NE;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.W;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record N() {

    public static final String NEW = s(NE, W);
}
