package interlink.lang.word;

import static works.lysenko.util.chrs.__.DE;
import static works.lysenko.util.chrs.__.OR;
import static works.lysenko.util.chrs.__.RS;
import static works.lysenko.util.data.strs.Swap.s;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record O() {

    public static final String ORDERS = s(OR, DE, RS);
}
