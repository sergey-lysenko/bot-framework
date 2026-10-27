package works.lysenko.base.output.plaque;

import works.lysenko.base.output.SummaryPlaque;

import java.util.Locale;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Renders HTML card markup and CSS styling for summary plaque cards.
 */
public final class PlaqueHtmlRenderer {

    private PlaqueHtmlRenderer() {
    }

    public static String renderHtml(final SummaryPlaque plaque) {
        final StringBuilder configHtml = new StringBuilder();
        if (isNotNull(plaque.suite()) || isNotNull(plaque.pool()) || isNotNull(plaque.domain()) || isNotNull(plaque.pathsSummary()) || isNotNull(plaque.mode()) || plaque.knownIssues() > 0) {
            configHtml.append("    <div class=\"plaque-config\">\n");
            if (isNotNull(plaque.suite())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Suite:</span> <b class=\"config-val\">").append(plaque.suite()).append("</b></span>\n");
            }
            if (isNotNull(plaque.pool())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Pool:</span> <b class=\"config-val\">").append(plaque.pool()).append("</b></span>\n");
            }
            if (isNotNull(plaque.domain())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Domain:</span> <b class=\"config-val\">").append(plaque.domain()).append("</b></span>\n");
            }
            if (isNotNull(plaque.origin())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Origin:</span> <b class=\"config-val\">").append(plaque.origin()).append("</b></span>\n");
            }
            if (isNotNull(plaque.tag())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Tag:</span> <b class=\"config-val\">").append(plaque.tag()).append("</b></span>\n");
            }
            if (isNotNull(plaque.pathsSummary())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Paths:</span> <b class=\"config-val\">").append(plaque.pathsSummary()).append("</b></span>\n");
            }
            if (plaque.knownIssues() > 0) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Known Issues:</span> <b class=\"config-val\" style=\"color:#c084fc;\">").append(plaque.knownIssues()).append("</b></span>\n");
            }
            if (isNotNull(plaque.mode())) {
                configHtml.append("      <span class=\"config-mode-badge\">").append(plaque.mode()).append("</span>\n");
            }
            configHtml.append("    </div>\n");
        }

        if (plaque.perNode()) {
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
                    "        <th class=\"col-upset\" title=\"Scenarios whose execution passed, but a child failed downstream\">Upset</th>\n" +
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
                    "        <td class=\"stat-upset\">%d</td>\n" +
                    "        <td class=\"stat-passed\">%d</td>\n" +
                    "        <td class=\"stat-warning\">%d</td>\n" +
                    "        <td class=\"stat-failed\">%d</td>\n" +
                    "      </tr>\n" +
                    "      <tr>\n" +
                    "        <td class=\"row-label\">Percentage</td>\n" +
                    "        <td class=\"col-total\">%s</td>\n" +
                    "        <td class=\"col-executable\">%s</td>\n" +
                    "        <td class=\"col-executed\">%s</td>\n" +
                    "        <td class=\"stat-upset\">%s</td>\n" +
                    "        <td class=\"stat-passed\">%s</td>\n" +
                    "        <td class=\"stat-warning\">%s</td>\n" +
                    "        <td class=\"stat-failed\">%s</td>\n" +
                    "      </tr>\n" +
                    "    </tbody>\n" +
                    "  </table>\n" +
                    "</div>\n",
                    configHtml,
                    plaque.total(), plaque.executable(), plaque.executed(), plaque.upset(), plaque.passed(), plaque.warning(), plaque.failed(),
                    SummaryPlaque.formatPct(plaque.total(), plaque.total()),
                    SummaryPlaque.formatPct(plaque.executable(), plaque.total()),
                    SummaryPlaque.formatPct(plaque.executed(), plaque.total()),
                    SummaryPlaque.formatPct(plaque.upset(), plaque.total()),
                    SummaryPlaque.formatPct(plaque.passed(), plaque.total()),
                    SummaryPlaque.formatPct(plaque.warning(), plaque.total()),
                    SummaryPlaque.formatPct(plaque.failed(), plaque.total())
            );
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
                "        <th class=\"col-executed\">Executed</th>\n" +
                "        <th class=\"col-passed\">Passed</th>\n" +
                "        <th class=\"col-warning\">Warning</th>\n" +
                "        <th class=\"col-failed\">Failed</th>\n" +
                "      </tr>\n" +
                "    </thead>\n" +
                "    <tbody>\n" +
                "      <tr>\n" +
                "        <td class=\"row-label\">Amount</td>\n" +
                "        <td class=\"col-executed\">%d</td>\n" +
                "        <td class=\"col-passed\">%d</td>\n" +
                "        <td class=\"col-warning\">%d</td>\n" +
                "        <td class=\"col-failed\">%d</td>\n" +
                "      </tr>\n" +
                "      <tr>\n" +
                "        <td class=\"row-label\">Percentage</td>\n" +
                "        <td class=\"col-executed\">%s</td>\n" +
                "        <td class=\"col-passed\">%s</td>\n" +
                "        <td class=\"col-warning\">%s</td>\n" +
                "        <td class=\"col-failed\">%s</td>\n" +
                "      </tr>\n" +
                "    </tbody>\n" +
                "  </table>\n" +
                "</div>\n",
                configHtml,
                plaque.executed(), plaque.passed(), plaque.warning(), plaque.failed(),
                SummaryPlaque.formatPct(plaque.executed(), plaque.total()),
                SummaryPlaque.formatPct(plaque.passed(), plaque.total()),
                SummaryPlaque.formatPct(plaque.warning(), plaque.total()),
                SummaryPlaque.formatPct(plaque.failed(), plaque.total())
        );
    }

    public static String css() {
        return "  .summary-plaque-card { background: var(--card-bg, #111827); border: 1px solid var(--border, #1e293b); border-radius: 8px; padding: 16px 20px; margin-top: 24px; margin-bottom: 24px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }\n" +
               "  .summary-plaque-card .card-title { font-size: 14px; font-weight: 600; color: #f8fafc; margin-bottom: 12px; display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 12px; }\n" +
               "  .summary-plaque-card .plaque-config { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; font-size: 11px; font-weight: normal; }\n" +
               "  .summary-plaque-card .config-item { color: #94a3b8; font-weight: normal; }\n" +
               "  .summary-plaque-card .config-label { color: #94a3b8; font-size: 10px; text-transform: uppercase; letter-spacing: 0.05em; font-weight: normal; }\n" +
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
               "  .summary-plaque-card .col-upset, .summary-plaque-card .stat-upset { color: #818cf8; font-weight: 600; }\n" +
               "  .summary-plaque-card .col-failed, .summary-plaque-card .stat-failed { color: #ef4444; font-weight: 600; }\n";
    }
}
