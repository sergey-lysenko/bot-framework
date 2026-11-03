package works.lysenko.base.responses;

import works.lysenko.base.issues.KnownIssuesMatcher;
import works.lysenko.util.apis.log._LogRecord;

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
        return KnownIssuesMatcher.matchesScenario(targetScenario, currentScenario, currentScenario);
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
}
