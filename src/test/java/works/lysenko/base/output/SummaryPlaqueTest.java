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
        assertEquals(1, plaque.upset());
        assertEquals(1, plaque.failed());

        final String html = plaque.renderHtml();
        assertNotNull(html);
        assertTrue(html.contains("Amount"));
        assertTrue(html.contains("Percentage"));
        assertTrue(html.contains("Total"));
        assertTrue(html.contains("Executable"));
        assertTrue(html.contains("Executed"));
        assertTrue(html.contains("Passed"));
        assertTrue(html.contains("Warning"));
        assertTrue(html.contains("Upset"));
        assertTrue(html.contains("Failed"));
        assertTrue(html.contains("20.0%"));
    }

    @Test
    void testCss() {
        final String css = SummaryPlaque.css();
        assertNotNull(css);
        assertTrue(css.contains(".summary-plaque"));
        assertTrue(css.contains(".col-upset"));
    }
}
