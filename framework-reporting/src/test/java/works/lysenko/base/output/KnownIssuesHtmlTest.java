package works.lysenko.base.output;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnownIssuesHtmlTest {

    @Test
    @DisplayName("Renders confirmed issues with tracker link and per-test log links")
    void rendersConfirmedIssuesWithTests() {
        final Map<String, KnownIssuesSummary.Entry> confirmed = new LinkedHashMap<>();
        final String link = "https://example.atlassian.net/browse/NODEJS-3194";
        confirmed.put(link, new KnownIssuesSummary.Entry(link, link, "Timeout <on> login", "Slow \"backend\"", new TreeSet<>(Set.of(1, 12, 249))));
        final Map<String, KnownIssuesSummary.Entry> notReproduced = new LinkedHashMap<>();
        final String link2 = "https://example.atlassian.net/browse/NODEJS-3250";
        notReproduced.put(link2, new KnownIssuesSummary.Entry(link2, link2, "", "", Collections.emptySet()));

        final String html = KnownIssuesHtml.render(new KnownIssuesSummary(confirmed, notReproduced, Map.of()));

        assertTrue(html.contains(">NODEJS-3194</a>"));
        assertTrue(html.contains("href=\"" + link + "\""));
        assertTrue(html.contains("navigateToLog(event,'test-249')"));
        assertTrue(html.contains("navigateToLog(event,'test-12')"));
        assertTrue(html.contains("Timeout &lt;on&gt; login"));
        assertTrue(html.contains("Slow &quot;backend&quot;"));
        assertTrue(html.contains(">NODEJS-3250</a>"));
        assertTrue(html.contains("Known Issues NOT Checked"));
        assertFalse(html.contains("{{"));
    }

    @Test
    @DisplayName("Renders None placeholders and does not link non-http references")
    void rendersEmptySections() {
        final Map<String, KnownIssuesSummary.Entry> confirmed = new LinkedHashMap<>();
        confirmed.put("javascript:alert(1)", new KnownIssuesSummary.Entry("javascript:alert(1)", "javascript:alert(1)", "", "", Collections.emptySet()));

        final String html = KnownIssuesHtml.render(new KnownIssuesSummary(confirmed, Map.of(), Map.of()));

        assertTrue(html.contains("<div class=\"none\">None</div>"));
        assertFalse(html.contains("href=\"javascript:"));
        assertTrue(html.contains("scenario-level match"));
    }

    @Test
    @DisplayName("Renders two-magenta regressions and fixed-issues verification sections with resolved metadata")
    void rendersFixedIssueRegressionsAndVerification() {
        final Map<String, KnownIssuesSummary.Entry> regressed = new LinkedHashMap<>();
        final String regLink = "https://example.atlassian.net/browse/FIX-101";
        regressed.put(regLink, new KnownIssuesSummary.Entry(
                regLink, regLink, "Crash returned", "NPE on checkout",
                new TreeSet<>(Set.of(3, 14)), "2026-04-10T09:00:00Z", "run-20260410"));

        final Map<String, KnownIssuesSummary.Entry> regWarnings = new LinkedHashMap<>();
        final String regWarnLink = "https://example.atlassian.net/browse/FIX-102";
        regWarnings.put(regWarnLink, new KnownIssuesSummary.Entry(
                regWarnLink, regWarnLink, "Deprecation warning returned", "Console warn",
                new TreeSet<>(Set.of(7)), "2026-04-12T11:30:00Z", ""));

        final Map<String, KnownIssuesSummary.Entry> verifiedFixed = new LinkedHashMap<>();
        final String verLink = "https://example.atlassian.net/browse/FIX-103";
        verifiedFixed.put(verLink, new KnownIssuesSummary.Entry(
                verLink, verLink, "Old timeout stayed fixed", "",
                Collections.emptySet(), "2026-04-01T00:00:00Z", "run-20260401"));

        final Map<String, KnownIssuesSummary.Entry> fixedNotChecked = new LinkedHashMap<>();
        final String uncheckedLink = "https://example.atlassian.net/browse/FIX-104";
        fixedNotChecked.put(uncheckedLink, new KnownIssuesSummary.Entry(
                uncheckedLink, uncheckedLink, "Unchecked fixed bug", "",
                Collections.emptySet(), "", ""));

        final KnownIssuesSummary summary = new KnownIssuesSummary(
                Map.of(), Map.of(), Map.of(),
                regressed, regWarnings, verifiedFixed, fixedNotChecked);

        final String html = KnownIssuesHtml.render(summary);

        assertTrue(html.contains("Regressions (Fixed Issues Reproduced)"));
        assertTrue(html.contains("Regression Warnings"));
        assertTrue(html.contains("Verified Fixed Issues"));
        assertTrue(html.contains("Fixed Issues NOT Checked"));
        assertTrue(html.contains(">FIX-101</a>"));
        assertTrue(html.contains(">FIX-102</a>"));
        assertTrue(html.contains(">FIX-103</a>"));
        assertTrue(html.contains(">FIX-104</a>"));
        assertTrue(html.contains("navigateToLog(event,'test-3')"));
        assertTrue(html.contains("navigateToLog(event,'test-14')"));
        assertTrue(html.contains("navigateToLog(event,'test-7')"));
        assertTrue(html.contains("Resolved: 2026-04-10T09:00:00Z"));
        assertTrue(html.contains("Verified in run: run-20260410"));
        assertTrue(html.contains("stat regression"));
        assertTrue(html.contains("stat regression-warning"));
        assertTrue(html.contains("stat verified-fixed"));
        assertTrue(html.contains("stat fixed-not-checked"));
        assertFalse(html.contains("{{"));
    }
}

