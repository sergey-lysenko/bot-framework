package works.lysenko.util.spec;

import static works.lysenko.util.spec.Numbers.*;

@SuppressWarnings({"MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record Level() {

    public static final int debug = THREE;
    public static final int none = ZERO;
    public static final int plain = ONE;
    public static final int supplementary = TWO;
}
