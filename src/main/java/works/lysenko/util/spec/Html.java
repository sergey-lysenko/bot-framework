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
 * HTML tags
 */
@SuppressWarnings({"ClassWithoutLogger", "AutoBoxing", "MissingJavadoc"})
public record Html() {

    public static final String BODY_ = s(LSS_THN, BODY, GRT_THN);
    public static final String TABLE_ = s(LSS_THN, TABLE, GRT_THN);
    public static final String TR_ = s(LSS_THN, TR, GRT_THN);
    public static final String TD_ = s(LSS_THN, TD, GRT_THN);
    public static final String _TABLE = s(LSS_THN, _SLASH_, TABLE, GRT_THN);
    public static final String _TD = s(LSS_THN, _SLASH_, TD, GRT_THN);
    public static final String _TR = s(LSS_THN, _SLASH_, TR, GRT_THN);
    private static final String HEAD_ = s(LSS_THN, HEAD, GRT_THN);
    private static final String HTML_ = s(LSS_THN, HTML, GRT_THN);
    private static final String H_1 = s(H, _1_);
    public static final String H_1_ = s(LSS_THN, H_1, GRT_THN);
    public static final String _H_1 = s(LSS_THN, _SLASH_, H_1, GRT_THN);
    private static final String H_2 = s(H, _2_);
    public static final String H_2_ = s(LSS_THN, H_2, GRT_THN);
    public static final String _H_2 = s(LSS_THN, _SLASH_, H_2, GRT_THN);
    private static final String TH_ = s(LSS_THN, TH, GRT_THN);
    public static final String TABLE_TR_TH_ = s(TABLE_, TR_, TH_);
    private static final String TITLE_ = s(LSS_THN, TITLE, GRT_THN);
    public static final String HTML_HEAD_TITLE_ = s(HTML_, HEAD_, TITLE_);
    private static final String _BODY = s(LSS_THN, _SLASH_, BODY, GRT_THN);
    private static final String _HEAD = s(LSS_THN, _SLASH_, HEAD, GRT_THN);
    private static final String _HTML = s(LSS_THN, _SLASH_, HTML, GRT_THN);
    public static final String _TABLE_BODY_HTML = s(_TABLE, _BODY, _HTML);
    private static final String _TH = s(LSS_THN, _SLASH_, TH, GRT_THN);
    public static final String _TH_TH_ = s(_TH, TH_);
    public static final String _TH_TR = s(_TH, _TR);
    private static final String _TITLE = s(LSS_THN, _SLASH_, TITLE, GRT_THN);
    public static final String _TITLE_HEAD = s(_TITLE, _HEAD);
}
