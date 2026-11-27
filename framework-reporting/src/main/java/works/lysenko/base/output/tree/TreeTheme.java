package works.lysenko.base.output.tree;

import java.awt.Color;
import java.util.Locale;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols._LFD_;

/**
 * The TreeTheme record.
 */
public record TreeTheme(
    int cardWidth,
    int cardHeight,
    int padding,
    int cornerRadius,
    Color passedBg,
    Color failedBg,
    Color regressionBg,
    Color upsetBg,
    Color warningBg,
    Color regressionWarningBg,
    Color noticeBg,
    Color nonExecutableBg,
    Color unvisitedBg,
    Color passedBorder,
    Color failedBorder,
    Color regressionBorder,
    Color upsetBorder,
    Color warningBorder,
    Color regressionWarningBorder,
    Color noticeBorder,
    Color nonExecutableBorder,
    Color unvisitedBorder,
    Color passedText,
    Color failedText,
    Color regressionText,
    Color upsetText,
    Color warningText,
    Color regressionWarningText,
    Color noticeText,
    Color nonExecutableText,
    Color unvisitedText
) {
    /** Default vivid magenta for high-severity (S0/S1) regressions. */
    public static final Color DEFAULT_REGRESSION_BG = Color.decode("#4a044e");
    public static final Color DEFAULT_REGRESSION_BORDER = Color.decode("#e879f9");
    public static final Color DEFAULT_REGRESSION_TEXT = Color.decode("#fae8ff");
    /** Default soft rose-magenta for warning-level (S2) regressions. */
    public static final Color DEFAULT_REGRESSION_WARNING_BG = Color.decode("#3b0d2c");
    public static final Color DEFAULT_REGRESSION_WARNING_BORDER = Color.decode("#f472b6");
    public static final Color DEFAULT_REGRESSION_WARNING_TEXT = Color.decode("#fbcfe8");

    /** Backward-compatible constructor without regression colors. */
    public TreeTheme(
        int cardWidth,
        int cardHeight,
        int padding,
        int cornerRadius,
        Color passedBg,
        Color failedBg,
        Color upsetBg,
        Color warningBg,
        Color noticeBg,
        Color nonExecutableBg,
        Color unvisitedBg,
        Color passedBorder,
        Color failedBorder,
        Color upsetBorder,
        Color warningBorder,
        Color noticeBorder,
        Color nonExecutableBorder,
        Color unvisitedBorder,
        Color passedText,
        Color failedText,
        Color upsetText,
        Color warningText,
        Color noticeText,
        Color nonExecutableText,
        Color unvisitedText
    ) {
        this(
            cardWidth, cardHeight, padding, cornerRadius,
            passedBg, failedBg, DEFAULT_REGRESSION_BG, upsetBg, warningBg, DEFAULT_REGRESSION_WARNING_BG, noticeBg, nonExecutableBg, unvisitedBg,
            passedBorder, failedBorder, DEFAULT_REGRESSION_BORDER, upsetBorder, warningBorder, DEFAULT_REGRESSION_WARNING_BORDER, noticeBorder, nonExecutableBorder, unvisitedBorder,
            passedText, failedText, DEFAULT_REGRESSION_TEXT, upsetText, warningText, DEFAULT_REGRESSION_WARNING_TEXT, noticeText, nonExecutableText, unvisitedText
        );
    }

    /** Compact constructor for backward compatibility without notice or regression colors. */
    public TreeTheme(
        int cardWidth,
        int cardHeight,
        int padding,
        int cornerRadius,
        Color passedBg,
        Color failedBg,
        Color upsetBg,
        Color warningBg,
        Color nonExecutableBg,
        Color unvisitedBg,
        Color passedBorder,
        Color failedBorder,
        Color upsetBorder,
        Color warningBorder,
        Color nonExecutableBorder,
        Color unvisitedBorder,
        Color passedText,
        Color failedText,
        Color upsetText,
        Color warningText,
        Color nonExecutableText,
        Color unvisitedText
    ) {
        this(
            cardWidth, cardHeight, padding, cornerRadius,
            passedBg, failedBg, DEFAULT_REGRESSION_BG, upsetBg, warningBg, DEFAULT_REGRESSION_WARNING_BG, Color.decode("#083344"), nonExecutableBg, unvisitedBg,
            passedBorder, failedBorder, DEFAULT_REGRESSION_BORDER, upsetBorder, warningBorder, DEFAULT_REGRESSION_WARNING_BORDER, Color.decode("#06b6d4"), nonExecutableBorder, unvisitedBorder,
            passedText, failedText, DEFAULT_REGRESSION_TEXT, upsetText, warningText, DEFAULT_REGRESSION_WARNING_TEXT, Color.decode("#67e8f9"), nonExecutableText, unvisitedText
        );
    }

    /** The default. */
    public static final TreeTheme DEFAULT = new TreeTheme(
        220, // cardWidth
        44,  // cardHeight
        10,  // padding
        8,   // cornerRadius
        Color.decode("#052e16"), // passedBg
        Color.decode("#450a0a"), // failedBg
        DEFAULT_REGRESSION_BG,   // regressionBg
        Color.decode("#1e1b4b"), // upsetBg
        Color.decode("#422006"), // warningBg
        DEFAULT_REGRESSION_WARNING_BG, // regressionWarningBg
        Color.decode("#083344"), // noticeBg
        Color.decode("#131a2a"), // nonExecutableBg
        Color.decode("#1e293b"), // unvisitedBg
        Color.decode("#22c55e"), // passedBorder
        Color.decode("#ef4444"), // failedBorder
        DEFAULT_REGRESSION_BORDER, // regressionBorder
        Color.decode("#818cf8"), // upsetBorder
        Color.decode("#f59e0b"), // warningBorder
        DEFAULT_REGRESSION_WARNING_BORDER, // regressionWarningBorder
        Color.decode("#06b6d4"), // noticeBorder
        Color.decode("#475569"), // nonExecutableBorder
        Color.decode("#64748b"), // unvisitedBorder
        Color.decode("#4ade80"), // passedText
        Color.decode("#fca5a5"), // failedText
        DEFAULT_REGRESSION_TEXT, // regressionText
        Color.decode("#a5b4fc"), // upsetText
        Color.decode("#fde047"), // warningText
        DEFAULT_REGRESSION_WARNING_TEXT, // regressionWarningText
        Color.decode("#67e8f9"), // noticeText
        Color.decode("#64748b"), // nonExecutableText
        Color.decode("#94a3b8")  // unvisitedText
    );

    /**
     * Performs hex.
     * @param color the color
     * @return the hex result
     */
    public String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    /**
     * Performs get execution path styles.
     * @return the get execution path styles result
     */
    public String getExecutionPathStyles() {
        return String.format(Locale.ROOT, 
            s("  .path-step { display: inline-flex; align-items: center; gap: 5px; }", _LFD_,
            "  .path-step.regression { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.regression:hover { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.regression_warning { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.regression_warning:hover { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.failed { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.failed:hover { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.child_failed { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.child_failed:hover { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.passed { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.passed:hover { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.warning { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.warning:hover { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.notice { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.notice:hover { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.non_executable { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.non_executable:hover { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.unvisited { background: %s; border-color: %s; color: %s; }", _LFD_,
            "  .path-step.unvisited:hover { background: %s; border-color: %s; color: %s; }", _LFD_),
            hex(regressionBg), hex(regressionBorder), hex(regressionText), hex(regressionBorder), hex(regressionText), hex(regressionBg),
            hex(regressionWarningBg), hex(regressionWarningBorder), hex(regressionWarningText), hex(regressionWarningBorder), hex(regressionWarningText), hex(regressionWarningBg),
            hex(failedBg), hex(failedBorder), hex(failedText), hex(failedBorder), hex(failedText), hex(failedBg),
            hex(upsetBg), hex(upsetBorder), hex(upsetText), hex(upsetBorder), hex(upsetText), hex(upsetBg),
            hex(passedBg), hex(passedBorder), hex(passedText), hex(passedBorder), hex(passedText), hex(passedBg),
            hex(warningBg), hex(warningBorder), hex(warningText), hex(warningBorder), hex(warningText), hex(warningBg),
            hex(noticeBg), hex(noticeBorder), hex(noticeText), hex(noticeBorder), hex(noticeText), hex(noticeBg),
            hex(nonExecutableBg), hex(nonExecutableBorder), hex(nonExecutableText), hex(nonExecutableBorder), hex(nonExecutableText), hex(nonExecutableBg),
            hex(unvisitedBg), hex(unvisitedBorder), hex(unvisitedText), hex(unvisitedBorder), hex(unvisitedText), hex(unvisitedBg)
        );
    }
}
