package works.lysenko.util.spec;

import static works.lysenko.util.chrs.__.TD;
import static works.lysenko.util.chrs.__.TH;
import static works.lysenko.util.chrs.__.TR;
import static works.lysenko.util.chrs.____.BODY;
import static works.lysenko.util.chrs.____.HEAD;
import static works.lysenko.util.chrs.____.HTML;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.T.TABLE;
import static works.lysenko.util.lang.word.T.TITLE;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Utility class providing formatted HTML tag strings and sequences for report generation.
 */
@SuppressWarnings({"ClassWithoutLogger", "AutoBoxing"})
public record Html() {

    /** Formatted body open tag. */
    public static final String BODY_ = s(LSS_THN, BODY, GRT_THN);
    /** Formatted table open tag. */
    public static final String TABLE_ = s(LSS_THN, TABLE, GRT_THN);
    /** Formatted table row open tag. */
    public static final String TR_ = s(LSS_THN, TR, GRT_THN);
    /** Formatted table data open tag. */
    public static final String TD_ = s(LSS_THN, TD, GRT_THN);
    /** Formatted table close tag. */
    public static final String _TABLE = s(LSS_THN, _SLASH_, TABLE, GRT_THN);
    /** Formatted table data close tag. */
    public static final String _TD = s(LSS_THN, _SLASH_, TD, GRT_THN);
    /** Formatted table row close tag. */
    public static final String _TR = s(LSS_THN, _SLASH_, TR, GRT_THN);
    private static final String HEAD_ = s(LSS_THN, HEAD, GRT_THN);
    private static final String HTML_ = s(LSS_THN, HTML, GRT_THN);
    private static final String H_1 = s(H, _1_);
    /** Formatted level 1 heading open tag. */
    public static final String H_1_ = s(LSS_THN, H_1, GRT_THN);
    /** Formatted level 1 heading close tag. */
    public static final String _H_1 = s(LSS_THN, _SLASH_, H_1, GRT_THN);
    private static final String H_2 = s(H, _2_);
    /** Formatted level 2 heading open tag. */
    public static final String H_2_ = s(LSS_THN, H_2, GRT_THN);
    /** Formatted level 2 heading close tag. */
    public static final String _H_2 = s(LSS_THN, _SLASH_, H_2, GRT_THN);
    private static final String TH_ = s(LSS_THN, TH, GRT_THN);
    /** Formatted table header cell open tag sequence. */
    public static final String TABLE_TR_TH_ = s(TABLE_, TR_, TH_);
    private static final String TITLE_ = s(LSS_THN, TITLE, GRT_THN);
    /** Formatted HTML, HEAD, and TITLE open tag sequence. */
    public static final String HTML_HEAD_TITLE_ = s(HTML_, HEAD_, TITLE_);
    private static final String _BODY = s(LSS_THN, _SLASH_, BODY, GRT_THN);
    private static final String _HEAD = s(LSS_THN, _SLASH_, HEAD, GRT_THN);
    private static final String _HTML = s(LSS_THN, _SLASH_, HTML, GRT_THN);
    /** Formatted sequence of closing table, body, and HTML tags. */
    public static final String _TABLE_BODY_HTML = s(_TABLE, _BODY, _HTML);
    private static final String _TH = s(LSS_THN, _SLASH_, TH, GRT_THN);
    /** Formatted table header closing and opening tag sequence. */
    public static final String _TH_TH_ = s(_TH, TH_);
    /** Formatted table header closing and row closing tag sequence. */
    public static final String _TH_TR = s(_TH, _TR);
    private static final String _TITLE = s(LSS_THN, _SLASH_, TITLE, GRT_THN);
    /** Formatted title closing and head closing tag sequence. */
    public static final String _TITLE_HEAD = s(_TITLE, _HEAD);
}
