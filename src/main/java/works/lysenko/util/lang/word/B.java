package works.lysenko.util.lang.word;

import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.ING;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols.S;

@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing"})
public record B() {

    public static final String BACKGROUND = s(BACK, GR, OUND);
    public static final String BEFORE = s(BE, FO, RE);
    public static final String BETTER = s(BE, TTER);
    public static final String BIGGER = s(BI, GG, ER);
    public static final String BINDING = s(BI, ND, ING);
    public static final String BORDER = s(BO, RD, ER);
    public static final String BOUNDARIES = s(BO, UN, DA, RI, ES);
    public static final String BOUNDS = s(BO, UN, DS);
    public static final String BRIGHTNESS = s(BR, IG, HT, NE, SS);
    public static final String BROWSER = s(BR, OW, S, ER);
    public static final String BUILDING = s(BU, IL, DI, NG);
    public static final String BUNDLE = s(BU, ND, LE);
    public static final String BUTTON = s(BU, TT, ON);
    public static final String BUTTONS = s(BUTTON, S);
    public static final String BUTTON_WITH_TEXT = b(c(BUTTON), WITH, c(TEXT));
}
