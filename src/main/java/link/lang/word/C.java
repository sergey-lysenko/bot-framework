package link.lang.word;

import static works.lysenko.util.chrs.__.CA;
import static works.lysenko.util.chrs.__.CL;
import static works.lysenko.util.chrs.__.CU;
import static works.lysenko.util.chrs.__.EA;
import static works.lysenko.util.chrs.__.EL;
import static works.lysenko.util.chrs.__.ER;
import static works.lysenko.util.chrs.__.IG;
import static works.lysenko.util.chrs.__.NC;
import static works.lysenko.util.chrs.__.OM;
import static works.lysenko.util.chrs.__.ST;
import static works.lysenko.util.chrs.__.UR;
import static works.lysenko.util.chrs.____.CONF;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.C;
import static works.lysenko.util.spec.Symbols.E;
import static works.lysenko.util.spec.Symbols.R;
import static works.lysenko.util.spec.Symbols.S;
import static works.lysenko.util.spec.Symbols.V;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record C() {

    public static final String CANCEL = s(CA, NC, EL);
    public static final String CLEAR = s(CL, EA, R);
    public static final String CONFIG = s(CONF, IG);
    public static final String CONFIGURE = s(CONF, IG, UR, E);
    public static final String CSV = s(C, S, V);
    public static final String CUSTOMERS = s(CU, ST, OM, ER, S);
}
