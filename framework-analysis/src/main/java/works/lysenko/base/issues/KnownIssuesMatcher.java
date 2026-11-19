package works.lysenko.base.issues;

import works.lysenko.util.apis.data.*;
import works.lysenko.util.apis.log.*;
import works.lysenko.util.apis.scenario.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static works.lysenko.Base.*;
import static works.lysenko.util.func.type.Objects.*;

/**
 * Utility class for matching known issues against scenarios and logs.
 */
@SuppressWarnings("all")
public final class KnownIssuesMatcher {

    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, PreparedRecordPattern> RECORD_PATTERN_CACHE = new ConcurrentHashMap<>();
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    private record PreparedRecordPattern(Pattern regex, String lowerPattern, String normPattern) {
        static PreparedRecordPattern compile(final String patternStr) {
            Pattern compiled = null;
            try {
                compiled = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            } catch (final PatternSyntaxException ignored) {
            }
            final String lower = patternStr.toLowerCase(java.util.Locale.ROOT);
            final String norm = NON_ALPHANUMERIC.matcher(lower).replaceAll(EMPTY);
            return new PreparedRecordPattern(compiled, lower, norm);
        }
    }
    private static final String EMPTY = "";
    private static final String ASTERISK = "*";
    private static final String DOT = ".";
    private static final String DOT_ESCAPED = "\\.";
    private static final String DOT_STAR = ".*";
    private static final String FORMAT_CONCAT = "%s%s";
    private static final String FORMAT_REGEX = "(?i)%s";
    private static final String FORMAT_REGEX_EXACT = "(?i)^%s$";

    private KnownIssuesMatcher() {
        // Utility class
    }

    private static Pattern getCompiledPattern(final String regex, final int flags) {
        final String key = flags + ":" + regex;
        return PATTERN_CACHE.computeIfAbsent(key, k -> Pattern.compile(regex, flags));
    }

    /**
     * Checks if the given target scenario matches the fully qualified name or simple name.
     *
     * @param target     the target string to match
     * @param fqn        the fully qualified name of the scenario
     * @param simpleName the simple name of the scenario
     * @return true if matches, false otherwise
     */
    public static boolean matchesScenario(final String target, final String fqn, final String simpleName) {

        boolean match = false;
        if (null != target && !target.isBlank()) {
            final String trimmed = target.trim();
            if (trimmed.equalsIgnoreCase(fqn) || trimmed.equalsIgnoreCase(simpleName)) {
                match = true;
            } else if (isNotNull(fqn)) {
                final String lowerFqn = fqn.toLowerCase(java.util.Locale.ROOT);
                final String lowerTrimmed = trimmed.toLowerCase(java.util.Locale.ROOT);
                if (lowerFqn.equalsIgnoreCase(lowerTrimmed)) match = true;
                else if (lowerFqn.endsWith(String.format(FORMAT_CONCAT, DOT, lowerTrimmed)) || lowerFqn.endsWith(lowerTrimmed)) match = true;
                else if (lowerTrimmed.endsWith(String.format(FORMAT_CONCAT, DOT, lowerFqn)) || lowerTrimmed.endsWith(lowerFqn)) match = true;
            }

            if (!match && trimmed.contains(ASTERISK)) {
                String patternStr = trimmed.replace(DOT, DOT_ESCAPED).replace(ASTERISK, DOT_STAR);
                if (!patternStr.startsWith(DOT_STAR)) patternStr = String.format(FORMAT_CONCAT, DOT_STAR, patternStr);
                final String regex = String.format(FORMAT_REGEX_EXACT, patternStr);
                try {
                    final Pattern pattern = getCompiledPattern(regex, 0);
                    if (isNotNull(fqn) && pattern.matcher(fqn).matches()) match = true;
                    else if (isNotNull(simpleName) && pattern.matcher(simpleName).matches()) match = true;
                } catch (final PatternSyntaxException ignored) {
                }
            }
        }
        return match;
    }

    /**
     * Checks if any of the given scenarios match the target scenario string.
     *
     * @param scenarioTarget the target string to match
     * @param scenarios      the scenarios to check
     * @return true if matches, false otherwise
     */
    public static boolean matchesAnyScenario(final String scenarioTarget, final Iterable<? extends _Scenario> scenarios) {

        boolean match = false;
        if (null != scenarios) {
            for (final _Scenario scenario : scenarios) {
                if (null != scenario && matchesScenario(scenarioTarget, scenario.getName(), scenario.getSimpleName())) {
                    match = true;
                    break;
                }
            }
        }
        return match;
    }

    /**
     * Checks if the target scenario string is present in the given text.
     *
     * @param target the target string to match
     * @param text   the text to check
     * @return true if matches, false otherwise
     */
    public static boolean matchesScenarioInText(final String target, final String text) {

        boolean match = false;
        if (null != target && !target.isBlank() && null != text && !text.isBlank()) {
            final String trimmed = target.trim();
            final String lowerText = text.toLowerCase(java.util.Locale.ROOT);
            final String lowerTarget = trimmed.toLowerCase(java.util.Locale.ROOT);
            if (lowerText.contains(lowerTarget)) match = true;
            else if (trimmed.contains(ASTERISK)) {
                String patternStr = trimmed.replace(DOT, DOT_ESCAPED).replace(ASTERISK, DOT_STAR);
                if (!patternStr.startsWith(DOT_STAR)) patternStr = String.format(FORMAT_CONCAT, DOT_STAR, patternStr);
                final String regex = String.format(FORMAT_REGEX, patternStr);
                try {
                    final Pattern pattern = getCompiledPattern(regex, 0);
                    if (pattern.matcher(text).find()) {
                        match = true;
                    }
                } catch (final PatternSyntaxException e) {
                    log("Invalid known issue pattern: " + e.getMessage());
                }
            }
        }
        return match;
    }

    /**
     * Checks if the log pattern matches the execution result.
     *
     * @param patternStr the pattern string
     * @param result     the execution result
     * @return true if matches, false otherwise
     */
    public static boolean matchesPattern(final String patternStr, final _Result result) {

        boolean match = false;
        if (null == patternStr || patternStr.isBlank()) match = true;
        else if (null != result && null != result.getEvents() && !result.getEvents().isEmpty()) {
            for (final _LogRecord lr : result.getEvents()) {
                if (matchesPatternForRecord(patternStr, lr)) {
                    match = true;
                    break;
                }
            }
        }
        return match;
    }

    /**
     * Checks if the log pattern matches the given log record.
     *
     * @param patternStr the pattern string
     * @param logRecord  the log record
     * @return true if matches, false otherwise
     */
    public static boolean matchesPatternForRecord(final String patternStr, final _LogRecord logRecord) {

        if (null == patternStr || patternStr.isBlank()) {
            return true;
        }
        if (null == logRecord || null == logRecord.text()) {
            return false;
        }
        final PreparedRecordPattern prepared = RECORD_PATTERN_CACHE.computeIfAbsent(patternStr, PreparedRecordPattern::compile);
        final String text = logRecord.text();
        final String dataStr = (null != logRecord.data()) ? logRecord.data().toString() : EMPTY;
        final boolean hasData = !dataStr.isBlank();
        final String fullText = hasData ? dataStr + " " + text : text;

        // 1. Direct Regex match
        if (null != prepared.regex) {
            if (prepared.regex.matcher(text).find()) return true;
            if (hasData && prepared.regex.matcher(fullText).find()) return true;
        }

        // 2. Direct Substring match (case-insensitive)
        // Note: text is always a substring of fullText, so checking lowerFullText covers both.
        final String lowerFullText = fullText.toLowerCase(java.util.Locale.ROOT);
        if (lowerFullText.contains(prepared.lowerPattern)) {
            return true;
        }

        // 3. Normalized alphanumeric tolerance match
        if (!prepared.normPattern.isEmpty()) {
            final String normText = NON_ALPHANUMERIC.matcher(lowerFullText).replaceAll(EMPTY);
            return normText.contains(prepared.normPattern);
        }
        return false;
    }
}
