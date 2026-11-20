package works.lysenko.base.output;

import works.lysenko.base.output.loghtml.TemplateEngine;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.Base.logEvent;
import static works.lysenko.base.output.tree.TreeHtmlRenderer.escapeHtml;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Files.writeToFile;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_ISSUES_HTML_;

/**
 * Renders the "Known Issues summary" of a run as a standalone HTML report, listing every
 * known (tracker) issue together with the tests in which it was reproduced.
 */
@SuppressWarnings({"HardCodedStringLiteral", "ClassWithoutLogger"})
final class KnownIssuesHtml {

    private static final String TEMPLATE_RESOURCE = "works/lysenko/base/output/issues-report-template.html";
    private static final Pattern ISSUE_KEY = Pattern.compile("([A-Za-z][A-Za-z0-9]+-\\d+)(?:[/?#].*)?$");

    private KnownIssuesHtml() {
    }

    /**
     * Renders and writes the report next to the other run artifacts. Never throws; failures are logged.
     *
     * @param summary summary to render
     */
    static void write(final KnownIssuesSummary summary) {
        try {
            writeToFile(render(summary), name(RUN_ISSUES_HTML_));
        } catch (final Throwable t) {
            logEvent(S2, s("Failed to generate issues.html: ", t.getMessage()));
        }
    }

    /**
     * Renders the complete HTML document.
     *
     * @param summary summary to render
     * @return HTML document
     */
    static String render(final KnownIssuesSummary summary) {
        final StringBuilder content = new StringBuilder(4096);
        appendSection(content, "confirmed", "Confirmed Known Issues", "Reproduced in at least one test", summary.confirmed().values(), true);
        appendSection(content, "not-reproduced", "Known Issues NOT Reproduced", "Scenario executed, issue did not show up", summary.notReproduced().values(), false);
        appendSection(content, "not-checked", "Known Issues NOT Checked", "Scenario was not executed in this run", summary.notChecked().values(), false);

        final Map<String, String> replacements = new LinkedHashMap<>(4);
        replacements.put("{{CONFIRMED_COUNT}}", String.valueOf(summary.confirmed().size()));
        replacements.put("{{NOT_REPRODUCED_COUNT}}", String.valueOf(summary.notReproduced().size()));
        replacements.put("{{NOT_CHECKED_COUNT}}", String.valueOf(summary.notChecked().size()));
        replacements.put("{{CONTENT}}", content.toString());
        return TemplateEngine.replaceTemplate(TemplateEngine.loadTemplate(TEMPLATE_RESOURCE), replacements);
    }

    private static void appendSection(
            final StringBuilder sb,
            final String cssClass,
            final String title,
            final String subtitle,
            final Collection<KnownIssuesSummary.Entry> entries,
            final boolean withTests) {
        sb.append("<section class=\"group ").append(cssClass).append("\">\n")
          .append("  <div class=\"group-head\"><h2>").append(escapeHtml(title))
          .append("</h2><span class=\"count\">").append(entries.size()).append("</span>")
          .append("<span class=\"sub\">").append(escapeHtml(subtitle)).append("</span></div>\n");
        if (entries.isEmpty()) {
            sb.append("  <div class=\"none\">None</div>\n");
        } else {
            sb.append("  <table>\n    <thead><tr><th class=\"c-issue\">Issue</th><th class=\"c-title\">Summary</th>");
            if (withTests) {
                sb.append("<th class=\"c-tests\">Tests</th>");
            }
            sb.append("</tr></thead>\n    <tbody>\n");
            for (final KnownIssuesSummary.Entry entry : entries) {
                appendRow(sb, entry, withTests);
            }
            sb.append("    </tbody>\n  </table>\n");
        }
        sb.append("</section>\n");
    }

    private static void appendRow(final StringBuilder sb, final KnownIssuesSummary.Entry entry, final boolean withTests) {
        final String key = issueKey(entry);
        final String link = entry.link();
        final boolean linkable = link.regionMatches(true, 0, "http://", 0, 7) || link.regionMatches(true, 0, "https://", 0, 8);
        final String search = (key + ' ' + entry.title() + ' ' + entry.description() + ' ' + entry.ref()).toLowerCase(Locale.ROOT);

        sb.append("      <tr class=\"issue\" data-search=\"").append(escapeHtml(search)).append("\">");
        sb.append("<td class=\"c-issue\">");
        if (linkable) {
            sb.append("<a class=\"key\" href=\"").append(escapeHtml(link)).append("\" target=\"_blank\" rel=\"noopener\">")
              .append(escapeHtml(key)).append("</a>");
        } else {
            sb.append("<span class=\"key\">").append(escapeHtml(key)).append("</span>");
        }
        sb.append("</td><td class=\"c-title\">");
        if (!entry.title().isEmpty() && !entry.title().equals(key)) {
            sb.append("<div class=\"title\">").append(escapeHtml(entry.title())).append("</div>");
        }
        if (!entry.description().isEmpty()) {
            sb.append("<div class=\"desc\">").append(escapeHtml(entry.description())).append("</div>");
        }
        sb.append("</td>");
        if (withTests) {
            sb.append("<td class=\"c-tests\">");
            if (entry.tests().isEmpty()) {
                sb.append("<span class=\"muted\">scenario-level match</span>");
            } else {
                sb.append("<span class=\"tcount\">").append(entry.tests().size()).append("</span>");
                for (final Integer test : entry.tests()) {
                    sb.append("<a class=\"test\" href=\"#test-").append(test)
                      .append("\" onclick=\"navigateToLog(event,'test-").append(test).append("')\">#")
                      .append(test).append("</a>");
                }
            }
            sb.append("</td>");
        }
        sb.append("</tr>\n");
    }

    private static String issueKey(final KnownIssuesSummary.Entry entry) {
        final Matcher m = ISSUE_KEY.matcher(entry.link());
        if (m.find()) {
            return m.group(1);
        }
        return entry.ref();
    }
}
