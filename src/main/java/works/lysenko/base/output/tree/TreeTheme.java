package works.lysenko.base.output.tree;

import java.awt.Color;
import java.util.Locale;

public record TreeTheme(
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
    public static final TreeTheme DEFAULT = new TreeTheme(
        220, // cardWidth
        44,  // cardHeight
        10,  // padding
        8,   // cornerRadius
        Color.decode("#052e16"), // passedBg
        Color.decode("#450a0a"), // failedBg
        Color.decode("#1e1b4b"), // upsetBg
        Color.decode("#422006"), // warningBg
        Color.decode("#131a2a"), // nonExecutableBg
        Color.decode("#1e293b"), // unvisitedBg
        Color.decode("#22c55e"), // passedBorder
        Color.decode("#ef4444"), // failedBorder
        Color.decode("#818cf8"), // upsetBorder
        Color.decode("#f59e0b"), // warningBorder
        Color.decode("#475569"), // nonExecutableBorder
        Color.decode("#64748b"), // unvisitedBorder
        Color.decode("#4ade80"), // passedText
        Color.decode("#fca5a5"), // failedText
        Color.decode("#a5b4fc"), // upsetText
        Color.decode("#fde047"), // warningText
        Color.decode("#64748b"), // nonExecutableText
        Color.decode("#94a3b8")  // unvisitedText
    );

    public String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    public String getExecutionPathStyles() {
        return String.format(Locale.ROOT, 
            "  .path-step.failed { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.failed:hover { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.child_failed { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.child_failed:hover { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.passed { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.passed:hover { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.warning { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.warning:hover { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.non_executable { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.non_executable:hover { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.unvisited { background: %s; border-color: %s; color: %s; }\n" +
            "  .path-step.unvisited:hover { background: %s; border-color: %s; color: %s; }\n",
            hex(failedBg), hex(failedBorder), hex(failedText), hex(failedBorder), hex(failedText), hex(failedBg),
            hex(upsetBg), hex(upsetBorder), hex(upsetText), hex(upsetBorder), hex(upsetText), hex(upsetBg),
            hex(passedBg), hex(passedBorder), hex(passedText), hex(passedBorder), hex(passedText), hex(passedBg),
            hex(warningBg), hex(warningBorder), hex(warningText), hex(warningBorder), hex(warningText), hex(warningBg),
            hex(nonExecutableBg), hex(nonExecutableBorder), hex(nonExecutableText), hex(nonExecutableBorder), hex(nonExecutableText), hex(nonExecutableBg),
            hex(unvisitedBg), hex(unvisitedBorder), hex(unvisitedText), hex(unvisitedBorder), hex(unvisitedText), hex(unvisitedBg)
        );
    }
}
