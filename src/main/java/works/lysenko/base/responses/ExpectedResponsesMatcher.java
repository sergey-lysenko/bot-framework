package works.lysenko.base.responses;

import works.lysenko.base.issues.KnownIssuesMatcher;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;

/**
 * Utility matcher for expected browser responses.
 */
public final class ExpectedResponsesMatcher {

    private ExpectedResponsesMatcher() {
        // Utility class
    }

    /**
     * Checks if target scenario matches current scenario name.
     *
     * @param targetScenario  the target scenario pattern
     * @param currentScenario the current scenario name
     * @return true if matches
     */
    public static boolean matchesScenario(final String targetScenario, final String currentScenario) {
        final String clean = stripTags(currentScenario);
        final String simple = extractSimpleName(clean);
        return KnownIssuesMatcher.matchesScenario(targetScenario, clean, simple);
    }

    /**
     * Checks if target scenario matches the given scenario object.
     *
     * @param targetScenario the target scenario pattern
     * @param scenario       the scenario instance
     * @return true if matches
     */
    public static boolean matchesScenario(final String targetScenario, final _Scenario scenario) {
        if (null == scenario) return false;
        return KnownIssuesMatcher.matchesScenario(targetScenario, scenario.getName(), scenario.getSimpleName())
                || KnownIssuesMatcher.matchesScenario(targetScenario, scenario.getShortName(), scenario.getSimpleName());
    }

    /**
     * Checks if target scenario matches a tree NodeData instance.
     *
     * @param targetScenario the target scenario pattern
     * @param node           the tree node
     * @return true if matches
     */
    public static boolean matchesNodeScenario(final String targetScenario, final NodeData node) {
        if (null == node) return false;
        if (null != node.scenario() && matchesScenario(targetScenario, node.scenario())) {
            return true;
        }
        final String cleanLabel = stripTags(node.label());
        final String shortKey = extractSimpleName(cleanLabel);
        final String cleanGroup = stripTags(node.group());
        final String fullKey = (cleanGroup.isEmpty() || "tests".equalsIgnoreCase(cleanGroup))
                ? cleanLabel
                : cleanGroup + "." + shortKey;
        return KnownIssuesMatcher.matchesScenario(targetScenario, fullKey, shortKey)
                || KnownIssuesMatcher.matchesScenario(targetScenario, cleanLabel, shortKey);
    }

    /**
     * Checks if log pattern matches any event in the execution result.
     *
     * @param patternStr the pattern string or regex
     * @param result     the execution result
     * @return true if matches
     */
    public static boolean matchesPattern(final String patternStr, final _Result result) {
        return KnownIssuesMatcher.matchesPattern(patternStr, result);
    }

    /**
     * Checks if log record matches pattern.
     *
     * @param patternStr the pattern string or regex
     * @param logRecord  the log record
     * @return true if matches
     */
    public static boolean matchesPatternForRecord(final String patternStr, final _LogRecord logRecord) {
        return KnownIssuesMatcher.matchesPatternForRecord(patternStr, logRecord);
    }

    private static String stripTags(final String raw) {
        if (null == raw) return "";
        return raw.replace(" ▷", "").replace(" ◼", "").replace(" ◆", "").trim();
    }

    private static String extractSimpleName(final String clean) {
        if (null == clean || clean.isEmpty()) return "";
        final String firstToken = clean.split(" ")[0];
        final int lastDot = firstToken.lastIndexOf('.');
        return (lastDot >= 0 && lastDot + 1 < firstToken.length()) ? firstToken.substring(lastDot + 1) : firstToken;
    }
}
