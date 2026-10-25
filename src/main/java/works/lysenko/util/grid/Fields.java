package works.lysenko.util.grid;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.A.AMOUNT;
import static works.lysenko.util.lang.word.B.BORDER;
import static works.lysenko.util.lang.word.B.BRIGHTNESS;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.C.CUTOFF;
import static works.lysenko.util.lang.word.F.FENCES;
import static works.lysenko.util.lang.word.H.HUE;
import static works.lysenko.util.lang.word.I.IGNORE;
import static works.lysenko.util.lang.word.M.MARGIN;
import static works.lysenko.util.lang.word.M.METHOD;
import static works.lysenko.util.lang.word.P.POLYCHROMY;
import static works.lysenko.util.lang.word.S.SATURATION;
import static works.lysenko.util.lang.word.T.THRESHOLD;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * Utility record containing constant field name definitions for grid properties.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Fields() {

    private static final String CD = s(COLOURS, _DOT_);
    /** Colour ignore property field. */
    public static final String CDI = s(CD, IGNORE);
    private static final String CDID = s(CDI, _DOT_);
    private static final String PD = s(POLYCHROMY, _DOT_);
    /** Brightness fences property field. */
    public static final String BDF = s(BRIGHTNESS, _DOT_, FENCES);
    /** Brightness threshold property field. */
    public static final String BDT = s(BRIGHTNESS, _DOT_, THRESHOLD);
    /** Colours amount property field. */
    public static final String CDA = s(CD, AMOUNT);
    /** Colours border property field. */
    public static final String CDB = s(CD, BORDER);
    /** Colours cutoff property field. */
    public static final String CDC = s(CD, CUTOFF);
    /** Colours margin property field. */
    public static final String CDD = s(CD, MARGIN);
    /** Colour ignore brightness property field. */
    public static final String CDIB = s(CDID, BRIGHTNESS);
    /** Colour ignore hue property field. */
    public static final String CDIH = s(CDID, HUE);
    /** Colour ignore saturation property field. */
    public static final String CDIS = s(CDID, SATURATION);
    /** Hue fences property field. */
    public static final String HDF = s(HUE, _DOT_, FENCES);
    /** Hue threshold property field. */
    public static final String HDT = s(HUE, _DOT_, THRESHOLD);
    /** Polychromy ignore property field. */
    public static final String PDI = s(PD, IGNORE);
    /** Polychromy method property field. */
    public static final String PDM = s(PD, METHOD);
    /** Saturation fences property field. */
    public static final String SDF = s(SATURATION, _DOT_, FENCES);
    /** Saturation threshold property field. */
    public static final String SDT = s(SATURATION, _DOT_, THRESHOLD);
}
