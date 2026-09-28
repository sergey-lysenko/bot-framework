package works.lysenko.util.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.NEGA;
import static works.lysenko.util.chrs.____.NOTI;
import static works.lysenko.util.chrs.____.TIVE;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.H;
import static works.lysenko.util.spec.Symbols.M;
import static works.lysenko.util.spec.Symbols.S;

@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MissingJavadoc"})
public record N() {

    public static final String NAMED = s(NA, M, ED);
    public static final String NATIVE = s(NA, TIVE);
    public static final String NEGATIVE = s(NEGA, TIVE);
    public static final String NEITHER = s(NE, IT, HER);
    public static final String NESTED = s(NE, ST, ED);
    public static final String NOTHING = s(NOT, H, ING);
    public static final String NOTICE = s(NOTI, CE);
    public static final String NOTICES = s(NOTICE, S);
}
