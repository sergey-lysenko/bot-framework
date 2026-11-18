package works.lysenko.base.output.plaque;

import works.lysenko.Base;
import works.lysenko.base.Parameters;
import works.lysenko.base.output.SummaryPlaque.RunConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.util.func.type.Objects.isNotNull;
import static java.util.Objects.isNull;

/**
 * Parses execution parameters and log lines into {@link RunConfig} descriptors
 * and cleans paths summary strings.
 */
public final class RunConfigParser {

    private RunConfigParser() {
    }

    public static RunConfig fromParameters(final Parameters parameters) {
        if (isNull(parameters)) return new RunConfig(null, null, null, null, null, null, null);
        final String suite = (null != parameters.getTest() && !parameters.getTest().isBlank()) ? parameters.getTest() : null;
        final String pool = (null != parameters.getPool() && !parameters.getPool().isBlank()) ? parameters.getPool() : null;
        final String domain = (null != parameters.getDomain() && !parameters.getDomain().isBlank()) ? parameters.getDomain() : null;
        final String origin = (null != parameters.getOrigin() && !parameters.getOrigin().isBlank()) ? parameters.getOrigin() : null;
        final String tag = (null != parameters.getTag() && !parameters.getTag().isBlank()) ? parameters.getTag() : null;
        final String mode = parameters.isAllLeafs() ? "ALL_LEAFS" : null;
        String pathsSummary = null;
        if (isNotNull(Base.core)) {
            try {
                pathsSummary = String.format("%d / %d / %d", Base.core.getExecutedLeafsCount(), Base.core.getActiveScenarioPaths(), Base.core.getTotalScenarioPaths());
            } catch (final Exception ignored) {
            }
        }
        return new RunConfig(suite, pool, domain, mode, pathsSummary, origin, tag);
    }

    public static RunConfig parse(final String logLine, final String pathsSummary) {
        final RunConfig base = parse(logLine);
        return new RunConfig(base.suite(), base.pool(), base.domain(), base.mode(), pathsSummary, base.origin(), base.tag());
    }

    public static RunConfig parse(final String logLine) {
        if (isNull(logLine) || logLine.isBlank()) return new RunConfig(null, null, null, null, null, null, null);

        String text = logLine;
        int ofIdx = text.indexOf(" of ");
        if (ofIdx >= 0) {
            text = text.substring(ofIdx + 4).trim();
        }

        String tag = null;
        if (text.endsWith("]") && text.contains("[")) {
            int tagOpen = text.lastIndexOf("[");
            tag = text.substring(tagOpen + 1, text.length() - 1).trim();
            text = text.substring(0, tagOpen).trim();
        }

        String origin = null;
        if (text.contains(" from ")) {
            int fromIdx = text.indexOf(" from ");
            origin = text.substring(fromIdx + 6).trim();
            text = text.substring(0, fromIdx).trim();
        }

        String mode = null;
        if (text.contains(" in ") && text.contains(" mode")) {
            int inIdx = text.indexOf(" in ");
            int modeIdx = text.indexOf(" mode", inIdx);
            if (modeIdx > inIdx) {
                mode = text.substring(inIdx + 4, modeIdx).trim();
                text = text.substring(0, inIdx) + text.substring(modeIdx + 5);
            }
        }

        String domain = null;
        if (text.contains(" on ")) {
            int onIdx = text.indexOf(" on ");
            domain = text.substring(onIdx + 4).trim();
            text = text.substring(0, onIdx).trim();
        }

        String pool = null;
        if (text.contains(" with ")) {
            int withIdx = text.indexOf(" with ");
            pool = text.substring(withIdx + 6).trim();
            text = text.substring(0, withIdx).trim();
        }

        String suite = text.isBlank() ? null : text.trim();
        return new RunConfig(suite, pool, domain, mode, null, origin, tag);
    }

    public static String cleanPathsSummary(final String pathsExecutedStr, final String pathsChanceStr, final String pathsPossibleStr) {
        if ((isNull(pathsExecutedStr) || pathsExecutedStr.isBlank())
                && (isNull(pathsChanceStr) || pathsChanceStr.isBlank())
                && (isNull(pathsPossibleStr) || pathsPossibleStr.isBlank())) {
            return null;
        }
        final String numExec = extractFirstNumber(pathsExecutedStr);
        final String numChance = extractFirstNumber(pathsChanceStr);
        final String numPossible = extractFirstNumber(pathsPossibleStr);
        final String pctExec = extractPct(pathsExecutedStr);

        if (!numExec.isEmpty() && !numChance.isEmpty() && !numPossible.isEmpty()) {
            if (!pctExec.isEmpty()) {
                return String.format("%s / %s / %s <span class=\"config-hint\">(%s executed)</span>", numExec, numChance, numPossible, pctExec);
            }
            return String.format("%s / %s / %s", numExec, numChance, numPossible);
        }
        return null;
    }

    private static String extractFirstNumber(final String text) {
        if (isNull(text) || text.isBlank()) return "";
        final Matcher m = Pattern.compile("\\b(\\d+)\\b").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private static String extractPct(final String text) {
        if (isNull(text) || text.isBlank()) return "";
        final Matcher m = Pattern.compile("\\b(\\d+(?:\\.\\d+)?%)\\b").matcher(text);
        return m.find() ? m.group(1) : "";
    }
}
