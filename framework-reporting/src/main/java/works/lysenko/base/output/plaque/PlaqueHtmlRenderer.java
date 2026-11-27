package works.lysenko.base.output.plaque;

import works.lysenko.base.output.SummaryPlaque;

import java.util.Locale;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Symbols._LFD_;

/**
 * Renders HTML card markup and CSS styling for summary plaque cards.
 */
public final class PlaqueHtmlRenderer {

    private PlaqueHtmlRenderer() {
    }

    public static String renderHtml(final SummaryPlaque plaque) {
        final StringBuilder configHtml = new StringBuilder();
        if (isNotNull(plaque.suite()) || isNotNull(plaque.pool()) || isNotNull(plaque.domain()) || isNotNull(plaque.pathsSummary()) || isNotNull(plaque.mode()) || plaque.knownIssues() > 0) {
            configHtml.append("    <div class=\"plaque-config\">").append(_LFD_);
            if (isNotNull(plaque.suite())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Suite:</span> <b class=\"config-val\">").append(plaque.suite()).append("</b></span>").append(_LFD_);
            }
            if (isNotNull(plaque.pool())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Pool:</span> <b class=\"config-val\">").append(plaque.pool()).append("</b></span>").append(_LFD_);
            }
            if (isNotNull(plaque.domain())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Domain:</span> <b class=\"config-val\">").append(plaque.domain()).append("</b></span>").append(_LFD_);
            }
            if (isNotNull(plaque.origin())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Origin:</span> <b class=\"config-val\">").append(plaque.origin()).append("</b></span>").append(_LFD_);
            }
            if (isNotNull(plaque.tag())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Tag:</span> <b class=\"config-val\">").append(plaque.tag()).append("</b></span>").append(_LFD_);
            }
            if (isNotNull(plaque.pathsSummary())) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Paths:</span> <b class=\"config-val\">").append(plaque.pathsSummary()).append("</b></span>").append(_LFD_);
            }
            if (plaque.knownIssues() > 0) {
                configHtml.append("      <span class=\"config-item\"><span class=\"config-label\">Known Issues:</span> <b class=\"config-val\" style=\"color:#c084fc;\">").append(plaque.knownIssues()).append("</b></span>").append(_LFD_);
            }
            if (isNotNull(plaque.mode())) {
                configHtml.append("      <span class=\"config-mode-badge\">").append(plaque.mode()).append("</span>").append(_LFD_);
            }
            configHtml.append("    </div>").append(_LFD_);
        }

        if (plaque.perNode()) {
            final java.util.List<SummaryPlaque.MetricColumn> cols = plaque.visibleTreeColumns();
            final StringBuilder headerCells = new StringBuilder();
            final StringBuilder amountCells = new StringBuilder();
            final StringBuilder pctCells = new StringBuilder();
            for (final SummaryPlaque.MetricColumn col : cols) {
                headerCells.append("        <th class=\"").append(col.headerClass()).append("\"");
                if (isNotNull(col.title())) {
                    headerCells.append(" title=\"").append(col.title()).append("\"");
                }
                headerCells.append(">").append(col.label()).append("</th>").append(_LFD_);
                amountCells.append("        <td class=\"").append(col.cellClass()).append("\">").append(col.amount()).append("</td>").append(_LFD_);
                pctCells.append("        <td class=\"").append(col.cellClass()).append("\">").append(col.percentage()).append("</td>").append(_LFD_);
            }
            return s("<div class=\"summary-card summary-plaque-card\">", _LFD_,
                    "  <div class=\"card-title\">", _LFD_,
                    "    <span>⚡ Execution Statistics</span>", _LFD_,
                    configHtml,
                    "  </div>", _LFD_,
                    "  <table class=\"plaque-table\">", _LFD_,
                    "    <thead>", _LFD_,
                    "      <tr>", _LFD_,
                    "        <th></th>", _LFD_,
                    headerCells,
                    "      </tr>", _LFD_,
                    "    </thead>", _LFD_,
                    "    <tbody>", _LFD_,
                    "      <tr>", _LFD_,
                    "        <td class=\"row-label\">Amount</td>", _LFD_,
                    amountCells,
                    "      </tr>", _LFD_,
                    "      <tr>", _LFD_,
                    "        <td class=\"row-label\">Percentage</td>", _LFD_,
                    pctCells,
                    "      </tr>", _LFD_,
                    "    </tbody>", _LFD_,
                    "  </table>", _LFD_,
                    "</div>", _LFD_);
        }

        final boolean hasHidden = plaque.hidden() > 0;
        final String passedAmount = hasHidden
                ? String.format(Locale.US, "%d <span style=\"color:#94a3b8;font-weight:normal;\">(%d)</span>", plaque.totalPassed(), plaque.hidden())
                : String.valueOf(plaque.passed());
        final String passedPct = SummaryPlaque.formatPct(plaque.totalPassed(), plaque.total());
        final String regWarnTh = (plaque.regressionWarning() > 0)
                ? s("        <th class=\"col-regression-warning\" title=\"Tests with warning-level regressions matching fixed.issues.json\">Reg. Warn</th>", _LFD_) : "";
        final String regWarnAmt = (plaque.regressionWarning() > 0)
                ? String.format(Locale.US, s("        <td class=\"col-regression-warning\">%d</td>", _LFD_), plaque.regressionWarning()) : "";
        final String regWarnPct = (plaque.regressionWarning() > 0)
                ? String.format(Locale.US, s("        <td class=\"col-regression-warning\">%s</td>", _LFD_), SummaryPlaque.formatPct(plaque.regressionWarning(), plaque.total())) : "";
        final String regTh = (plaque.regression() > 0)
                ? s("        <th class=\"col-regression\" title=\"Tests that regressed on a previously fixed issue (fixed.issues.json)\">Regression</th>", _LFD_) : "";
        final String regAmt = (plaque.regression() > 0)
                ? String.format(Locale.US, s("        <td class=\"col-regression\">%d</td>", _LFD_), plaque.regression()) : "";
        final String regPct = (plaque.regression() > 0)
                ? String.format(Locale.US, s("        <td class=\"col-regression\">%s</td>", _LFD_), SummaryPlaque.formatPct(plaque.regression(), plaque.total())) : "";

        return String.format(Locale.US,
                s("<div class=\"summary-card summary-plaque-card\">", _LFD_,
                "  <div class=\"card-title\">", _LFD_,
                "    <span>⚡ Execution Statistics</span>", _LFD_,
                "%s",
                "  </div>", _LFD_,
                "  <table class=\"plaque-table\">", _LFD_,
                "    <thead>", _LFD_,
                "      <tr>", _LFD_,
                "        <th></th>", _LFD_,
                "        <th class=\"col-executed\">Executed</th>", _LFD_,
                "        <th class=\"col-passed\">%s</th>", _LFD_,
                "        <th class=\"col-warning\">Warning</th>", _LFD_,
                "%s",
                "        <th class=\"col-failed\">Failed</th>", _LFD_,
                "%s",
                "      </tr>", _LFD_,
                "    </thead>", _LFD_,
                "    <tbody>", _LFD_,
                "      <tr>", _LFD_,
                "        <td class=\"row-label\">Amount</td>", _LFD_,
                "        <td class=\"col-executed\">%d</td>", _LFD_,
                "        <td class=\"col-passed\">%s</td>", _LFD_,
                "        <td class=\"col-warning\">%d</td>", _LFD_,
                "%s",
                "        <td class=\"col-failed\">%d</td>", _LFD_,
                "%s",
                "      </tr>", _LFD_,
                "      <tr>", _LFD_,
                "        <td class=\"row-label\">Percentage</td>", _LFD_,
                "        <td class=\"col-executed\">%s</td>", _LFD_,
                "        <td class=\"col-passed\">%s</td>", _LFD_,
                "        <td class=\"col-warning\">%s</td>", _LFD_,
                "%s",
                "        <td class=\"col-failed\">%s</td>", _LFD_,
                "%s",
                "      </tr>", _LFD_,
                "    </tbody>", _LFD_,
                "  </table>", _LFD_,
                "</div>", _LFD_),
                configHtml,
                "Passed",
                regWarnTh,
                regTh,
                plaque.executed(), passedAmount, plaque.warning(), regWarnAmt, plaque.failed(), regAmt,
                SummaryPlaque.formatPct(plaque.executed(), plaque.total()),
                passedPct,
                SummaryPlaque.formatPct(plaque.warning(), plaque.total()),
                regWarnPct,
                SummaryPlaque.formatPct(plaque.failed(), plaque.total()),
                regPct
        );
    }

    public static String css() {
        return s("  .summary-plaque-card { width: fit-content; max-width: 100%; box-sizing: border-box; background: rgba(15, 23, 42, 0.55) !important; backdrop-filter: blur(8px) !important; -webkit-backdrop-filter: blur(8px) !important; border: 1px solid rgba(56, 189, 248, 0.35) !important; box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37) !important; border-radius: 8px; padding: 16px 20px; margin-top: 24px; margin-bottom: 24px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }", _LFD_,
               "  .summary-plaque-card .card-title { font-size: 14px; font-weight: 600; color: #f8fafc; margin-bottom: 12px; display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 12px; }", _LFD_,
               "  .summary-plaque-card .plaque-config { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; font-size: 11px; font-weight: normal; }", _LFD_,
               "  .summary-plaque-card .config-item { color: #94a3b8; font-weight: normal; }", _LFD_,
               "  .summary-plaque-card .config-label { color: #94a3b8; font-size: 10px; text-transform: uppercase; letter-spacing: 0.05em; font-weight: normal; }", _LFD_,
               "  .summary-plaque-card .config-val { color: #f8fafc; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .config-hint { color: #64748b; font-size: 10px; font-weight: normal; }", _LFD_,
               "  .summary-plaque-card .config-mode-badge { background: rgba(168, 85, 247, 0.15); color: #c084fc; border: 1px solid rgba(192, 132, 252, 0.3); font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 12px; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }", _LFD_,
               "  .summary-plaque-card table.plaque-table { width: 100%; border-collapse: collapse; text-align: center; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; margin-top: 4px; }", _LFD_,
               "  .summary-plaque-card th, .summary-plaque-card td { padding: 8px 14px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }", _LFD_,
               "  .summary-plaque-card th { font-weight: 600; color: #94a3b8; border-bottom: 2px solid #1e293b; font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; }", _LFD_,
               "  .summary-plaque-card td.row-label { font-weight: 600; text-align: left; color: #cbd5e1; padding-right: 16px; font-size: 11px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }", _LFD_,
               "  .summary-plaque-card .col-total { color: #38bdf8; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-executable { color: #c084fc; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-off { color: #64748b; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-not-executed { color: #94a3b8; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-executed { color: #60a5fa; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-hidden { color: #6ee7b7; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-passed, .summary-plaque-card .stat-passed { color: #22c55e; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-notice, .summary-plaque-card .stat-notice { color: #06b6d4; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-warning, .summary-plaque-card .stat-warning { color: #f59e0b; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-regression-warning, .summary-plaque-card .stat-regression-warning { color: #f472b6; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-upset, .summary-plaque-card .stat-upset { color: #818cf8; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-failed, .summary-plaque-card .stat-failed { color: #ef4444; font-weight: 600; }", _LFD_,
               "  .summary-plaque-card .col-regression, .summary-plaque-card .stat-regression { color: #e879f9; font-weight: 600; }", _LFD_);
    }
}
