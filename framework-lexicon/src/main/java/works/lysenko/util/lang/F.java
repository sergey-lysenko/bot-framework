package works.lysenko.util.lang;

import static works.lysenko.util.chrs.___.RAW;
import static works.lysenko.util.chrs.____.GRID;
import static works.lysenko.util.chrs.____.NODE;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.lang.word.F.FULLSCREEN;
import static works.lysenko.util.lang.word.I.IMAGE;
import static works.lysenko.util.lang.word.O.OPTIMIZING;
import static works.lysenko.util.lang.word.P.PARENT;
import static works.lysenko.util.lang.word.S.SUBTREE;
import static works.lysenko.util.spec.Symbols.*;
import static works.lysenko.util.spec.Synth.*;

/**
 * Language utility constants starting with F.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
public record F() {

    /** Format specifier '%s'. */
    public static final String FMT_PERCENT_S = s(_PRCNT_, S);
    /** Format specifier '%d'. */
    public static final String FMT_PERCENT_D = s(_PRCNT_, D);
    /** Format specifier '%+d'. */
    public static final String FMT_PERCENT_PLUS_D = s(_PRCNT_, _PLUS_, D);
    /** Format specifier '%.2f'. */
    public static final String FMT_PERCENT_2F = s(_PRCNT_, _DOT_, _2_, F);
    /** Format template for aspect ratio '(%.2f:1)'. */
    public static final String FMT_RATIO = e(ROUND, s(FMT_PERCENT_2F, _COLON_, _1_));
    /** Format template for space-prefixed aspect ratio ' (%.2f:1)'. */
    public static final String FMT_RATIO_SUFFIX = s(_SPACE_, FMT_RATIO);
    /** Format template for optimizing progress status. */
    public static final String FMT_OPTIMIZING_PROGRESS = b(
            s(c(OPTIMIZING), _SPACE_, e(ROUND, FMT_PERCENT_S), ELLIPSIS),
            FMT_PERCENT_S, s(MID_DOT), s(FMT_PERCENT_D, MLT_SGN, FMT_PERCENT_D), FMT_RATIO);
    /** Format template for inspector node metadata. */
    public static final String FMT_INSPECTOR_META = b(
            s(c(PARENT), _COLON_), FMT_PERCENT_S, s(MID_DOT),
            s(c(SUBTREE), _COLON_), FMT_PERCENT_D, s(NODE, FMT_PERCENT_S));
    /** Format template for compacted grid position. */
    public static final String FMT_GRID_POS = b(
            s(c(GRID), _COLON_), s(u(C), FMT_PERCENT_D, _COMMA_), s(u(R), FMT_PERCENT_2F));
    /** Format template for raw grid position suffix. */
    public static final String FMT_RAW_POS = s(_SPACE_, _SPACE_,
            e(ROUND, b(s(RAW, _COLON_), s(u(C), FMT_PERCENT_D, _COMMA_), s(u(R), FMT_PERCENT_2F))));
    /** Format template for offset diff line. */
    public static final String FMT_DIFF_LINE = s(
            _PRCNT_, _DASH_, _3_, _2_, S, _SPACE_, _SPACE_,
            e(ROUND, b(s(FMT_PERCENT_PLUS_D, _COMMA_), FMT_PERCENT_PLUS_D)),
            _SPACE_, _SPACE_, RHT_ARR, _SPACE_, _SPACE_,
            e(ROUND, b(s(FMT_PERCENT_PLUS_D, _COMMA_), FMT_PERCENT_PLUS_D)),
            _PRCNT_, N);
    /** Format template for combined column and row offset tag. */
    public static final String FMT_COL_ROW_TAG = b(s(C, _PLUS_, FMT_PERCENT_D, _COMMA_), s(R, FMT_PERCENT_PLUS_D));
    /** Format template for column offset tag. */
    public static final String FMT_COL_TAG = s(C, _PLUS_, FMT_PERCENT_D);
    /** Format template for row offset tag. */
    public static final String FMT_ROW_TAG = s(R, FMT_PERCENT_PLUS_D);
    /** Constant representing full screen image. */
    public static final String FULLSCREEN_IMAGE = b(FULLSCREEN, IMAGE);
}
