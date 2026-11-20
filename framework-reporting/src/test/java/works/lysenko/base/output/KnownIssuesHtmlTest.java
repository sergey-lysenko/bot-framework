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
}
