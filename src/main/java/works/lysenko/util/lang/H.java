package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.__.WE;
import static works.lysenko.util.chrs.___.GET;
import static works.lysenko.util.chrs.___.HAS;
import static works.lysenko.util.chrs.___.HOW;
import static works.lysenko.util.chrs.____.HERE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.D.DID;
import static works.lysenko.util.lang.word.W.WIDTH;
import static works.lysenko.util.spec.Symbols.QUS_MRK;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record H() {

    public static final String HAS_NO_WIDTH = b(HAS, NO, WIDTH);
    public static final String HOW_DID_WE_GET_HERE = b(c(HOW), DID, WE, GET, s(HERE, QUS_MRK));
}
