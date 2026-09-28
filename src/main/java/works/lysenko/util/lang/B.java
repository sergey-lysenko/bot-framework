package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.___.BUT;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.____.TEXT;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.lang.word.A.ACTUAL;
import static works.lysenko.util.lang.word.B.BUTTON;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.I.IGNORED;
import static works.lysenko.util.lang.word.O.ORDER;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record B() {

    public static final String BY_ACTUAL_VALUE = b(BY, ACTUAL, VALUE);
    public static final String BUT___ = b(BUT, COLOURS, ORDER, IS, NOT, IGNORED, BY);
    public static final String BUTTON_WITH_TEXT = b(c(BUTTON), WITH, c(TEXT));
}
