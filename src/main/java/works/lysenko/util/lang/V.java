package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.C.CONVERT;
import static works.lysenko.util.lang.word.E.EQUAL;
import static works.lysenko.util.lang.word.F.FRACTION;
import static works.lysenko.util.lang.word.V.VALUES;
import static works.lysenko.util.spec.Symbols._COLON_;

@SuppressWarnings({"MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record V() {

    public static final String VALUE_IS_TO_BIG___ = b(c(VALUE), IS, TOO, BIG, TO, CONVERT, TO, s(c(FRACTION), _COLON_));
    public static final String VALUES_ARE_NOT_EQUAL = b(c(VALUES), ARE, NOT, EQUAL);
}
