package works.lysenko.util.grid;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.AMOUNT;
import static works.lysenko.util.lang.word.B.BORDER;
import static works.lysenko.util.lang.word.B.BRIGHTNESS;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.F.FENCES;
import static works.lysenko.util.lang.word.H.HUE;
import static works.lysenko.util.lang.word.I.IGNORE;
import static works.lysenko.util.lang.word.M.MARGIN;
import static works.lysenko.util.lang.word.M.METHOD;
import static works.lysenko.util.lang.word.P.POLYCHROMY;
import static works.lysenko.util.lang.word.S.SATURATION;
import static works.lysenko.util.lang.word.T.THRESHOLD;
import static works.lysenko.util.spec.Symbols._DOT_;

@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MissingJavadoc"})
public record Fields() {

    private static final String CD = s(COLOURS, _DOT_);
    public static final String CDI = s(CD, IGNORE);
    private static final String CDID = s(CDI, _DOT_);
    private static final String PD = s(POLYCHROMY, _DOT_);
    public static final String BDF = s(BRIGHTNESS, _DOT_, FENCES);
    public static final String BDT = s(BRIGHTNESS, _DOT_, THRESHOLD);
    public static final String CDA = s(CD, AMOUNT);
    public static final String CDB = s(CD, BORDER);
    public static final String CDD = s(CD, MARGIN);
    public static final String CDIB = s(CDID, BRIGHTNESS);
    public static final String CDIH = s(CDID, HUE);
    public static final String CDIS = s(CDID, SATURATION);
    public static final String HDF = s(HUE, _DOT_, FENCES);
    public static final String HDT = s(HUE, _DOT_, THRESHOLD);
    public static final String PDI = s(PD, IGNORE);
    public static final String PDM = s(PD, METHOD);
    public static final String SDF = s(SATURATION, _DOT_, FENCES);
    public static final String SDT = s(SATURATION, _DOT_, THRESHOLD);
}
