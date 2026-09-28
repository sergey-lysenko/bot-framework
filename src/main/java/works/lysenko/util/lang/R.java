package works.lysenko.util.lang;

import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.chrs.____.TEXT;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.M.MINIMUM;
import static works.lysenko.util.lang.word.R.*;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record R() {

    public static final String READING_TEXT = b(c(READING), TEXT, FROM);
    public static final String READING_VALUE = b(c(READING), VALUE, OF);
    public static final String REDEFINED_EXPECTED = b(REDEFINED, EXPECTED);
    public static final String REDEFINED_EXPECTED_MINIMUM_IS = b(REDEFINED, EXPECTED, MINIMUM, IS);
    public static final String RANGES_IN = b(RANGES, IN);
    public static final String RANGER_RESULT = s(c(RANGER), c(RESULT));
}
