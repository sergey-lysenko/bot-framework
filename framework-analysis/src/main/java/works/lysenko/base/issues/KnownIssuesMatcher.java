package works.lysenko.base.issues;

import works.lysenko.util.apis.data.*;
import works.lysenko.util.apis.log.*;
import works.lysenko.util.apis.scenario.*;

import java.util.Locale;
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

    private record PreparedRecordPattern(Pattern regex, String lowerPattern, boolean catchAll) {
        static PreparedRecordPattern compile(final String patternStr) {
            Pattern compiled = null;
            boolean isCatchAll = false;
            try {
                compiled = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                isCatchAll = compiled.matcher("").find()
                        || (compiled.matcher("a").matches()
                        && compiled.matcher("0").matches()
                        && compiled.matcher("_").matches());
            } catch (final PatternSyntaxException _) {
            }
            final String lower = patternStr.toLowerCase(Locale.ROOT);
            return new PreparedRecordPattern(compiled, lower, isCatchAll);
        }
    }

    private static final String EMPTY = "";
    private static final String ASTERISK = "*";
    private static final String DOT = ".";
    private static final String DOT_ESCAPED = "\\.";
    private static final String DOT_STAR = ".*";
    private static final String FORMAT_CONCAT = "%s%s";
    private static final String FORMAT_REGEX = "(?i)%s";

    private KnownIssuesMatcher() {
        // Utility class
    }

    private static Pattern getCompiledPattern(final String regex, final int flags) {
        final String key = flags + ":" + regex;
        return PATTERN_CACHE.computeIfAbsent(key, _ -> Pattern.compile(regex, flags));
    }

    /**
     * Returns {@code true} if the scenario target is the explicit global wildcard {@code "*"}.
     *
     * @param target the scenario target string
     * @return {@code true} when target trims to {@code "*"}
     */
    public static boolean isWildcardScenario(final String target) {
        return null != target && ASTERISK.equals(target.trim());
    }

    /**
     * Returns {@code true} if the pattern is blank/null or is a catch-all regex (such as {@code .*}, {@code .+},
     * or any regex matching an empty string or arbitrary single characters).
     *
     * @param patternStr the pattern string
     * @return {@code true} if the pattern is blank or catch-all
     */
    public static boolean isCatchAllPattern(final String patternStr) {
        if (null == patternStr || patternStr.isBlank()) {
            return true;
        }
        final String trimmed = patternStr.trim();
        if (".*".equals(trimmed) || ".+".equals(trimmed) || "^.*$".equals(trimmed) || "^.+$".equals(trimmed)) {
            return true;
        }
        final PreparedRecordPattern prepared = RECORD_PATTERN_CACHE.computeIfAbsent(patternStr, PreparedRecordPattern::compile);
        return prepared.catchAll;
    }

    /**
     * Returns {@code true} if the (scenario, pattern) rule is overly permissive and must not match automatically:
     * either the pattern is blank/null, the scenario is blank/null, or a global wildcard scenario {@code "*"}
     * is paired with a catch-all pattern.
     *
     * @param scenario   the scenario target
     * @param patternStr the pattern string
     * @return {@code true} if the rule should be rejected from automatic matching
     */
    public static boolean isCatchAllRule(final String scenario, final String patternStr) {
        if (null == scenario || scenario.isBlank() || null == patternStr || patternStr.isBlank()) {
            return true;
        }
        return isWildcardScenario(scenario) && isCatchAllPattern(patternStr);
    }

    /**
     * Checks if the given target scenario matches the fully qualified name or simple name.
     * Enforces dot-delimited package boundaries so suffix matches cannot bleed across partial identifiers
     * (e.g. {@code "Save"} will not match {@code "SpamSave"}).
     *
     * @param target     the target string to match
     * @param fqn        the fully qualified name of the scenario
     * @param simpleName the simple name of the scenario
     * @return true if matches, false otherwise
     */
    public static boolean matchesScenario(final String target, final String fqn, final String simpleName) {

        if (null == target || target.isBlank()) {
            return false;
        }
        final String trimmed = target.trim();
        if (ASTERISK.equals(trimmed)) {
            return true;
        }
        if (trimmed.equalsIgnoreCase(fqn) || trimmed.equalsIgnoreCase(simpleName)) {
            return true;
        }
        if (isNotNull(fqn) && !fqn.isBlank()) {
            final String lowerFqn = fqn.toLowerCase(Locale.ROOT);
            final String lowerTrimmed = trimmed.toLowerCase(Locale.ROOT);
            if (lowerFqn.endsWith(DOT + lowerTrimmed)) {
                return true;
            }
        }
        if (trimmed.contains(ASTERISK)) {
            final String regexBody = trimmed.replace(DOT, DOT_ESCAPED).replace(ASTERISK, DOT_STAR);
            final String regex = regexBody.startsWith(DOT_STAR)
                    ? "(?i)^" + regexBody + "$"
                    : "(?i)^(?:.+\\.)?" + regexBody + "$";
            try {
                final Pattern pattern = getCompiledPattern(regex, 0);
                if (isNotNull(fqn) && pattern.matcher(fqn).matches()) return true;
                if (isNotNull(simpleName) && pattern.matcher(simpleName).matches()) return true;
            } catch (final PatternSyntaxException _) {
            }
        }
        return false;
    }

    /**
     * Checks if any of the given scenarios match the target scenario string.
     *
     * @param scenarioTarget the target string to match
     * @param scenarios      the scenarios to check
     * @return true if matches, false otherwise
     */
    public static boolean matchesAnyScenario(final String scenarioTarget, final Iterable<? extends _Scenario> scenarios) {

        if (null == scenarios) {
            return false;
        }
        for (final _Scenario scenario : scenarios) {
            if (null != scenario && (matchesScenario(scenarioTarget, scenario.getName(), scenario.getSimpleName())
                    || matchesScenario(scenarioTarget, scenario.getShortName(), scenario.getSimpleName()))) {
                return true;
            }
        }
        return false;
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
            final String lowerText = text.toLowerCase(Locale.ROOT);
            final String lowerTarget = trimmed.toLowerCase(Locale.ROOT);
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
     * Checks if the log pattern matches any event in the execution result.
     * Blank/null patterns never match automatically.
     *
     * @param patternStr the pattern string
     * @param result     the execution result
     * @return true if matches, false otherwise
     */
    public static boolean matchesPattern(final String patternStr, final _Result result) {

        if (null == patternStr || patternStr.isBlank()) {
            return false;
        }
        if (null != result && null != result.getEvents() && !result.getEvents().isEmpty()) {
            for (final _LogRecord lr : result.getEvents()) {
                if (matchesPatternForRecord(patternStr, lr)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the log pattern matches the given log record using compiled regex or case-insensitive substring matching.
     * Blank/null patterns never match automatically.
     *
     * @param patternStr the pattern string
     * @param logRecord  the log record
     * @return true if matches, false otherwise
     */
    public static boolean matchesPatternForRecord(final String patternStr, final _LogRecord logRecord) {

        if (null == patternStr || patternStr.isBlank()) {
            return false;
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
        final String lowerFullText = fullText.toLowerCase(Locale.ROOT);
        return lowerFullText.contains(prepared.lowerPattern);
    }
}
