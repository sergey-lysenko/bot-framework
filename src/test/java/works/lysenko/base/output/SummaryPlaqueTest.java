package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import works.lysenko.base.output.loghtml.LogModels.ScenEntry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SummaryPlaqueTest {

    @Test
    void testFormatPct() {
        assertEquals("0%", SummaryPlaque.formatPct(0, 0));
        assertEquals("0%", SummaryPlaque.formatPct(0, 10));
        assertEquals("100%", SummaryPlaque.formatPct(10, 10));
        assertEquals("50.0%", SummaryPlaque.formatPct(5, 10));
        assertEquals("33.3%", SummaryPlaque.formatPct(1, 3));
    }

    @Test
    void testComputeFromScenEntriesAndRenderHtml() {
        final List<ScenEntry> entries = new ArrayList<>();
        entries.add(new ScenEntry("▷", "Scenario1", "1.0", "1", ""));
        entries.add(new ScenEntry("▷", "Scenario2", "1.0", "1", "warn"));
        entries.add(new ScenEntry("▷", "Scenario3", "1.0", "1", "fail"));
        entries.add(new ScenEntry("▷", "Scenario4", "1.0", "1", "child_failed"));
        entries.add(new ScenEntry("▷", "Scenario5", "0.0", "0", ""));

        final SummaryPlaque plaque = SummaryPlaque.computeFromScenEntries(entries);
        assertEquals(5, plaque.total());
        assertEquals(4, plaque.executable());
        assertEquals(4, plaque.executed());
        assertEquals(1, plaque.passed());
        assertEquals(1, plaque.warning());
        assertEquals(2, plaque.failed());

        final String html = plaque.renderHtml();
        assertNotNull(html);
        assertTrue(html.contains("Amount"));
        assertTrue(html.contains("Percentage"));
        assertFalse(html.contains("<th class=\"col-total\">Total</th>"));
        assertFalse(html.contains("<th class=\"col-executable\">Executable</th>"));
        assertTrue(html.contains("Executed"));
        assertTrue(html.contains("Passed"));
        assertTrue(html.contains("Warning"));
        assertTrue(html.contains("Failed"));
        assertTrue(html.contains("20.0%"));
    }

    @Test
    void testRunConfigParsingAndRendering() {
        final String logLine = "[  ][37][.795][  2] Executing tests of Development with development-local on https://stg-dashboard.myordering.online in ALL_LEAFS mode";
        final SummaryPlaque.RunConfig config = SummaryPlaque.RunConfig.parse(logLine);

        assertEquals("Development", config.suite());
        assertEquals("development-local", config.pool());
        assertEquals("https://stg-dashboard.myordering.online", config.domain());
        assertEquals("ALL_LEAFS", config.mode());

        final List<ScenEntry> entries = new ArrayList<>();
        entries.add(new ScenEntry("▷", "Scenario1", "1.0", "1", ""));

        final SummaryPlaque plaque = SummaryPlaque.computeFromScenEntries(entries, logLine, "14 / 17 / 48 (82.4% executed)");
        final String html = plaque.renderHtml();

        assertTrue(html.contains("Development"), "Plaque HTML must contain test suite name");
        assertTrue(html.contains("development-local"), "Plaque HTML must contain pool name");
        assertTrue(html.contains("https://stg-dashboard.myordering.online"), "Plaque HTML must contain domain URL");
        assertTrue(html.contains("14 / 17 / 48"), "Plaque HTML must contain path summary");
        assertTrue(html.contains("ALL_LEAFS"), "Plaque HTML must contain mode badge");
    }

    @Test
    void testComputeFromSections() {
        final List<works.lysenko.base.output.loghtml.LogModels.LogSection> sections = new ArrayList<>();
        sections.add(new works.lysenko.base.output.loghtml.LogModels.LogSection("booting", "Booting", "neutral", null));

        // 6 passed tests
        for (int i = 1; i <= 6; i++) {
            final works.lysenko.base.output.loghtml.LogModels.LogSection sec =
                    new works.lysenko.base.output.loghtml.LogModels.LogSection("test", "Test #" + i, "passed", null);
            sec.testNum = i;
            sections.add(sec);
        }

        // 2 warning tests
        for (int i = 7; i <= 8; i++) {
            final works.lysenko.base.output.loghtml.LogModels.LogSection sec =
                    new works.lysenko.base.output.loghtml.LogModels.LogSection("test", "Test #" + i, "warning", null);
            sec.testNum = i;
            sections.add(sec);
        }

        // 8 failed tests
        for (int i = 9; i <= 16; i++) {
            final works.lysenko.base.output.loghtml.LogModels.LogSection sec =
                    new works.lysenko.base.output.loghtml.LogModels.LogSection("test", "Test #" + i, "failed", null);
            sec.testNum = i;
            sections.add(sec);
        }

        sections.add(new works.lysenko.base.output.loghtml.LogModels.LogSection("postflight", "Postflight", "neutral", null));

        final String logLine = "Executing tests of Development with development-local on https://stg-dashboard.myordering.online in ALL_LEAFS mode";
        final SummaryPlaque plaque = SummaryPlaque.computeFromSections(sections, logLine, "14 / 17 / 48");

        assertEquals(16, plaque.total());
        assertEquals(16, plaque.executable());
        assertEquals(16, plaque.executed());
        assertEquals(6, plaque.passed());
        assertEquals(2, plaque.warning());
        assertEquals(8, plaque.failed());

        assertEquals("100%", SummaryPlaque.formatPct(plaque.executed(), plaque.total()));
        assertEquals("37.5%", SummaryPlaque.formatPct(plaque.passed(), plaque.total()));
        assertEquals("12.5%", SummaryPlaque.formatPct(plaque.warning(), plaque.total()));
        assertEquals("50.0%", SummaryPlaque.formatPct(plaque.failed(), plaque.total()));

        final String html = plaque.renderHtml();
        assertNotNull(html);
        assertTrue(html.contains("col-executed\">16</td>"));
        assertTrue(html.contains("col-passed\">6</td>"));
        assertTrue(html.contains("col-warning\">2</td>"));
        assertTrue(html.contains("col-failed\">8</td>"));
        assertTrue(html.contains("37.5%"));
        assertTrue(html.contains("12.5%"));
        assertTrue(html.contains("50.0%"));
    }

    @Test
    void testCss() {
        final String css = SummaryPlaque.css();
        assertNotNull(css);
        assertTrue(css.contains(".summary-plaque-card"));
        assertTrue(css.contains(".col-failed"));
    }
}
