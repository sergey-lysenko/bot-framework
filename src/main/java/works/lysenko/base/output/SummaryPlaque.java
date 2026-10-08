package works.lysenko.base.output;

import works.lysenko.base.output.loghtml.LogModels.ScenEntry;
import works.lysenko.util.apis.data._Result;

import java.util.List;
import java.util.Locale;

/**
 * Generates Node-styled summary statistics plaques for Tree and Log HTML reports.
 */
public record SummaryPlaque(
        int total,
        int executable,
        int executed,
        int passed,
        int warning,
        int upset,
        int failed
) {

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

    /**
     * Computes summary plaque statistics from tree NodeData instances, differentiating
     * direct failures from upset parent scenarios with child failures.
     *
     * @param nodes list of tree nodes
     * @return SummaryPlaque instance
     */
    public static SummaryPlaque computeFromNodes(final List<TreeHtml.NodeData> nodes) {

        int total = nodes.size();
        int executable = 0;
        int executed = 0;
        int passed = 0;
        int warning = 0;
        int upset = 0;
        int failed = 0;

        for (final TreeHtml.NodeData n : nodes) {
            final boolean isExec = (null == n.scenario()) || n.scenario().isExecutable();
            if (isExec) executable++;

            final _Result res = n.result();
            final int execs = (null != res) ? res.getExecutions() : 0;
            final int eventCount = (null != res && null != res.getEvents()) ? res.getEvents().size() : 0;
            final boolean failure = TreeHtml.hasFailure(n);
            final boolean childFailure = TreeHtml.hasChildFailure(n);

            if (failure) {
                failed++;
                if (execs > 0) executed++;
            } else if (childFailure) {
                upset++;
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

        return new SummaryPlaque(total, executable, executed, passed, warning, upset, failed);
    }

    /**
     * Computes summary plaque statistics from log ScenEntry instances.
     *
     * @param entries list of scenario log entries
     * @return SummaryPlaque instance
     */
    public static SummaryPlaque computeFromScenEntries(final List<ScenEntry> entries) {

        int total = entries.size();
        int executable = 0;
        int executed = 0;
        int passed = 0;
        int warning = 0;
        int upset = 0;
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
            final boolean isChildFail = ev.contains("child_failed") || ev.contains("child failed") || "child_failed".equalsIgnoreCase(e.sym);
            final boolean isDirectFail = !isChildFail && (ev.contains("fail") || ev.contains("error") || ev.contains("exception"));
            final boolean isWarn = ev.contains("event") || ev.contains("warn") || ev.contains("issue") || (!ev.isEmpty() && !isDirectFail && !isChildFail);

            if (isDirectFail) {
                failed++;
                if (count > 0) executed++;
            } else if (isChildFail) {
                upset++;
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

        return new SummaryPlaque(total, executable, executed, passed, warning, upset, failed);
    }

    /**
     * Renders the Node-styled HTML summary plaque.
     *
     * @return HTML snippet of the plaque
     */
    public String renderHtml() {

        return String.format(Locale.US,
                "<div class=\"summary-plaque\">\n" +
                "  <div class=\"plaque-title\">⚡ Execution Statistics</div>\n" +
                "  <table class=\"plaque-table\">\n" +
                "    <thead>\n" +
                "      <tr>\n" +
                "        <th></th>\n" +
                "        <th class=\"col-total\">Total</th>\n" +
                "        <th class=\"col-executable\">Executable</th>\n" +
                "        <th class=\"col-executed\">Executed</th>\n" +
                "        <th class=\"col-passed\">Passed</th>\n" +
                "        <th class=\"col-warning\">Warning</th>\n" +
                "        <th class=\"col-upset\" title=\"Scenarios whose execution passed, but a child failed downstream\">Upset</th>\n" +
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
                "        <td class=\"stat-upset\">%d</td>\n" +
                "        <td class=\"stat-failed\">%d</td>\n" +
                "      </tr>\n" +
                "      <tr>\n" +
                "        <td class=\"row-label\">Percentage</td>\n" +
                "        <td class=\"col-total\">%s</td>\n" +
                "        <td class=\"col-executable\">%s</td>\n" +
                "        <td class=\"col-executed\">%s</td>\n" +
                "        <td class=\"stat-passed\">%s</td>\n" +
                "        <td class=\"stat-warning\">%s</td>\n" +
                "        <td class=\"stat-upset\">%s</td>\n" +
                "        <td class=\"stat-failed\">%s</td>\n" +
                "      </tr>\n" +
                "    </tbody>\n" +
                "  </table>\n" +
                "</div>\n",
                total, executable, executed, passed, warning, upset, failed,
                formatPct(total, total),
                formatPct(executable, total),
                formatPct(executed, total),
                formatPct(passed, total),
                formatPct(warning, total),
                formatPct(upset, total),
                formatPct(failed, total)
        );
    }

    /**
     * Common CSS styling for the Node-styled summary plaque.
     *
     * @return CSS snippet
     */
    public static String css() {

        return "  .summary-plaque { background: #1e293b; border: 1.5px solid #475569; border-radius: 8px; padding: 10px 14px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; color: #f8fafc; display: inline-block; margin-bottom: 16px; }\n" +
               "  .summary-plaque .plaque-title { font-weight: 600; font-size: 12px; color: #38bdf8; margin-bottom: 6px; letter-spacing: 0.5px; text-transform: uppercase; }\n" +
               "  .summary-plaque table { border-collapse: collapse; text-align: center; width: 100%; }\n" +
               "  .summary-plaque th, .summary-plaque td { padding: 5px 12px; border-bottom: 1px solid #334155; }\n" +
               "  .summary-plaque th { font-weight: 600; color: #94a3b8; border-bottom: 2px solid #334155; font-size: 11px; }\n" +
               "  .summary-plaque td.row-label { font-weight: 600; text-align: left; color: #cbd5e1; padding-right: 14px; font-size: 11px; }\n" +
               "  .summary-plaque .col-total { color: #38bdf8; font-weight: 600; }\n" +
               "  .summary-plaque .col-executable { color: #c084fc; font-weight: 600; }\n" +
               "  .summary-plaque .col-executed { color: #60a5fa; font-weight: 600; }\n" +
               "  .summary-plaque .col-passed, .summary-plaque .stat-passed { color: #22c55e; font-weight: 600; }\n" +
               "  .summary-plaque .col-warning, .summary-plaque .stat-warning { color: #f59e0b; font-weight: 600; }\n" +
               "  .summary-plaque .col-upset, .summary-plaque .stat-upset { color: #fbbf24; font-weight: 600; }\n" +
               "  .summary-plaque .col-failed, .summary-plaque .stat-failed { color: #ef4444; font-weight: 600; }\n";
    }
}
