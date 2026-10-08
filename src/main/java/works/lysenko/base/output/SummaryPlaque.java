package works.lysenko.base.output;

import works.lysenko.Base;
import works.lysenko.base.Parameters;
import works.lysenko.base.output.loghtml.LogModels.ScenEntry;
import works.lysenko.util.apis.data._Result;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.util.func.type.Objects.isNotNull;
import static java.util.Objects.isNull;

/**
 * Generates Node-styled summary statistics plaques for Tree and Log HTML reports.
 */
public record SummaryPlaque(
        String suite,
        String pool,
        String domain,
        String mode,
        String pathsSummary,
        int total,
        int executable,
        int executed,
        int passed,
        int warning,
        int failed
) {

    public SummaryPlaque(
            final int total,
            final int executable,
            final int executed,
            final int passed,
            final int warning,
            final int failed
    ) {
        this(null, null, null, null, null, total, executable, executed, passed, warning, failed);
    }

    /**
     * Run configuration descriptor extracted from execution parameter settings or log messages.
     */
    public record RunConfig(String suite, String pool, String domain, String mode, String pathsSummary) {

        public RunConfig(final String suite, final String pool, final String domain, final String mode) {
            this(suite, pool, domain, mode, null);
        }

        public static RunConfig fromParameters(final Parameters parameters) {

            if (isNull(parameters)) return new RunConfig(null, null, null, null, null);
            final String suite = (null != parameters.getTest() && !parameters.getTest().isBlank()) ? parameters.getTest() : null;
            final String pool = (null != parameters.getPool() && !parameters.getPool().isBlank()) ? parameters.getPool() : null;
            final String domain = (null != parameters.getDomain() && !parameters.getDomain().isBlank()) ? parameters.getDomain() : null;
            final String mode = parameters.isAllLeafs() ? "ALL_LEAFS" : null;
            String pathsSummary = null;
            if (isNotNull(Base.core)) {
                try {
                    pathsSummary = String.format("%d / %d / %d", Base.core.getExecutedLeafsCount(), Base.core.getActiveScenarioPaths(), Base.core.getTotalScenarioPaths());
                } catch (final Exception ignored) {
                }
            }
            return new RunConfig(suite, pool, domain, mode, pathsSummary);
        }

        public static RunConfig parse(final String logLine, final String pathsSummary) {

            final RunConfig base = parse(logLine);
            return new RunConfig(base.suite(), base.pool(), base.domain(), base.mode(), pathsSummary);
        }

        public static RunConfig parse(final String logLine) {

            if (isNull(logLine) || logLine.isBlank()) return new RunConfig(null, null, null, null, null);

            String text = logLine;
            int ofIdx = text.indexOf(" of ");
            if (ofIdx >= 0) {
                text = text.substring(ofIdx + 4).trim();
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
            return new RunConfig(suite, pool, domain, mode, null);
        }
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

    /**
     * Formats percentage string given value and base total.
     *
     * @param val  numerator value
     * @param base denominator base total
     * @return formatted percentage string
     */
    public static String formatPct(final int val, final int base) {

        if (0 == base) return "0%";
        final double pct = (val * 100.0) / base;
        if (pct == 0.0) return "0%";
        if (pct == 100.0) return "100%";
        return String.format(Locale.US, "%.1f%%", pct);
    }

    public static SummaryPlaque computeFromNodes(final List<TreeHtml.NodeData> nodes) {

        final RunConfig config = RunConfig.fromParameters(Base.parameters);
        return computeFromNodes(nodes, config);
    }

    public static SummaryPlaque computeFromNodes(final List<TreeHtml.NodeData> nodes, final RunConfig config) {

        int total = nodes.size();
        int executable = 0;
        int executed = 0;
        int passed = 0;
        int warning = 0;
        int failed = 0;

        for (final TreeHtml.NodeData n : nodes) {
            final boolean isExec = (null == n.scenario()) || n.scenario().isExecutable();
            if (isExec) executable++;

            final _Result res = n.result();
            final int execs = (null != res) ? res.getExecutions() : 0;
            final int eventCount = (null != res && null != res.getEvents()) ? res.getEvents().size() : 0;
            final boolean failure = TreeHtml.hasFailure(n) || TreeHtml.hasChildFailure(n);

            if (failure) {
                failed++;
                if (execs > 0) executed++;
            } else if (execs > 0) {
                executed++;
                if (eventCount > 0) {
                    warning++;
                } else {
                    passed++;
                }
            }
        }

        return new SummaryPlaque(
                config.suite(), config.pool(), config.domain(), config.mode(), config.pathsSummary(),
                total, executable, executed, passed, warning, failed);
    }

    public static SummaryPlaque computeFromScenEntries(final List<ScenEntry> entries) {

        return computeFromScenEntries(entries, null, null);
    }

    public static SummaryPlaque computeFromScenEntries(final List<ScenEntry> entries, final String execConfigStr) {

        return computeFromScenEntries(entries, execConfigStr, null);
    }

    public static SummaryPlaque computeFromScenEntries(final List<ScenEntry> entries, final String execConfigStr, final String pathsSummary) {

        final RunConfig config = RunConfig.parse(execConfigStr, pathsSummary);
        int total = entries.size();
        int executable = 0;
        int executed = 0;
        int passed = 0;
        int warning = 0;
        int failed = 0;

        for (final ScenEntry e : entries) {
            int count = 0;
            try {
                final String cStr = e.count.contains(":") ? e.count.split(":")[0] : e.count;
                count = Integer.parseInt(cStr.trim());
            } catch (final Exception ignored) {
            }

            boolean isExec = true;
            if (null != e.weight) {
                final String cleanWeight = e.weight.replaceAll("[\\[\\]()]", "").trim();
                try {
                    isExec = Double.parseDouble(cleanWeight) != 0.0;
                } catch (final Exception ignored) {
                    isExec = !("0".equals(cleanWeight) || "0.0".equals(cleanWeight));
                }
            }
            if (isExec || count > 0) executable++;

            final String ev = (null != e.events) ? e.events.toLowerCase(Locale.ROOT) : "";
            final boolean isFail = ev.contains("fail") || ev.contains("error") || ev.contains("exception") || ev.contains("child_failed") || "child_failed".equalsIgnoreCase(e.sym);
            final boolean isWarn = ev.contains("event") || ev.contains("warn") || ev.contains("issue") || (!ev.isEmpty() && !isFail);

            if (isFail) {
                failed++;
                if (count > 0) executed++;
            } else if (count > 0) {
                executed++;
                if (isWarn) {
                    warning++;
                } else {
                    passed++;
                }
            }
        }

        return new SummaryPlaque(
                config.suite(), config.pool(), config.domain(), config.mode(), config.pathsSummary(),
                total, executable, executed, passed, warning, failed);
    }

    /**
     * Renders the Node-styled HTML summary plaque with top configuration bar.
     *
     * @return HTML snippet of the plaque
     */
    public String renderHtml() {

        final StringBuilder configHtml = new StringBuilder();
        if (isNotNull(suite) || isNotNull(pool) || isNotNull(domain) || isNotNull(pathsSummary) || isNotNull(mode)) {
            configHtml.append("    <div class=\"plaque-config\">\n");
            if (isNotNull(suite)) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Suite:</span> <b class=\"config-val\">").append(suite).append("</b></span>\n");
            }
            if (isNotNull(pool)) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Pool:</span> <b class=\"config-val\">").append(pool).append("</b></span>\n");
            }
            if (isNotNull(domain)) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Domain:</span> <b class=\"config-val\">").append(domain).append("</b></span>\n");
            }
            if (isNotNull(pathsSummary)) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Paths:</span> <b class=\"config-val\">").append(pathsSummary).append("</b></span>\n");
            }
            if (isNotNull(mode)) {
                configHtml.append("      <span class=\"config-mode-badge\">").append(mode).append("</span>\n");
            }
            configHtml.append("    </div>\n");
        }

        return String.format(Locale.US,
                "<div class=\"summary-card summary-plaque-card\">\n" +
                "  <div class=\"card-title\">\n" +
                "    <span>⚡ Execution Statistics</span>\n" +
                "%s" +
                "  </div>\n" +
                "  <table class=\"plaque-table\">\n" +
                "    <thead>\n" +
                "      <tr>\n" +
                "        <th></th>\n" +
                "        <th class=\"col-total\">Total</th>\n" +
                "        <th class=\"col-executable\">Executable</th>\n" +
                "        <th class=\"col-executed\">Executed</th>\n" +
                "        <th class=\"col-passed\">Passed</th>\n" +
                "        <th class=\"col-warning\">Warning</th>\n" +
                "        <th class=\"col-failed\">Failed</th>\n" +
                "      </tr>\n" +
                "    </thead>\n" +
                "    <tbody>\n" +
                "      <tr>\n" +
                "        <td class=\"row-label\">Amount</td>\n" +
                "        <td class=\"col-total\">%d</td>\n" +
                "        <td class=\"col-executable\">%d</td>\n" +
                "        <td class=\"col-executed\">%d</td>\n" +
                "        <td class=\"stat-passed\">%d</td>\n" +
                "        <td class=\"stat-warning\">%d</td>\n" +
                "        <td class=\"stat-failed\">%d</td>\n" +
                "      </tr>\n" +
                "      <tr>\n" +
                "        <td class=\"row-label\">Percentage</td>\n" +
                "        <td class=\"col-total\">%s</td>\n" +
                "        <td class=\"col-executable\">%s</td>\n" +
                "        <td class=\"col-executed\">%s</td>\n" +
                "        <td class=\"stat-passed\">%s</td>\n" +
                "        <td class=\"stat-warning\">%s</td>\n" +
                "        <td class=\"stat-failed\">%s</td>\n" +
                "      </tr>\n" +
                "    </tbody>\n" +
                "  </table>\n" +
                "</div>\n",
                configHtml,
                total, executable, executed, passed, warning, failed,
                formatPct(total, total),
                formatPct(executable, total),
                formatPct(executed, total),
                formatPct(passed, total),
                formatPct(warning, total),
                formatPct(failed, total)
        );
    }

    /**
     * Common CSS styling for the Node-styled summary plaque.
     *
     * @return CSS snippet
     */
    public static String css() {

        return "  .summary-plaque-card { background: var(--card-bg, #111827); border: 1px solid var(--border, #1e293b); border-radius: 8px; padding: 16px 20px; margin-bottom: 20px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }\n" +
               "  .summary-plaque-card .card-title { font-size: 14px; font-weight: 600; color: #f8fafc; margin-bottom: 12px; display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 12px; }\n" +
               "  .summary-plaque-card .plaque-config { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; font-size: 11px; font-weight: normal; }\n" +
               "  .summary-plaque-card .config-item { color: #94a3b8; }\n" +
               "  .summary-plaque-card .config-label { color: #64748b; font-size: 10px; text-transform: uppercase; letter-spacing: 0.05em; }\n" +
               "  .summary-plaque-card .config-val { color: #f8fafc; font-weight: 600; }\n" +
               "  .summary-plaque-card .config-hint { color: #64748b; font-size: 10px; font-weight: normal; }\n" +
               "  .summary-plaque-card .config-mode-badge { background: rgba(168, 85, 247, 0.15); color: #c084fc; border: 1px solid rgba(192, 132, 252, 0.3); font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 12px; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }\n" +
               "  .summary-plaque-card table.plaque-table { width: 100%; border-collapse: collapse; text-align: center; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; margin-top: 4px; }\n" +
               "  .summary-plaque-card th, .summary-plaque-card td { padding: 8px 14px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }\n" +
               "  .summary-plaque-card th { font-weight: 600; color: #94a3b8; border-bottom: 2px solid #1e293b; font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; }\n" +
               "  .summary-plaque-card td.row-label { font-weight: 600; text-align: left; color: #cbd5e1; padding-right: 16px; font-size: 11px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }\n" +
               "  .summary-plaque-card .col-total { color: #38bdf8; font-weight: 600; }\n" +
               "  .summary-plaque-card .col-executable { color: #c084fc; font-weight: 600; }\n" +
               "  .summary-plaque-card .col-executed { color: #60a5fa; font-weight: 600; }\n" +
               "  .summary-plaque-card .col-passed, .summary-plaque-card .stat-passed { color: #22c55e; font-weight: 600; }\n" +
               "  .summary-plaque-card .col-warning, .summary-plaque-card .stat-warning { color: #f59e0b; font-weight: 600; }\n" +
               "  .summary-plaque-card .col-failed, .summary-plaque-card .stat-failed { color: #ef4444; font-weight: 600; }\n";
    }
}
