package works.lysenko.base.output.svg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionChromeTest {

    @Test
    void formatsLeafBadgeAndSelectsSemanticStyle() {
        assertEquals("Leaves: 0/0", ProgressionChrome.formatLeafsBadge(0, 0));
        assertEquals(ProgressionChrome.BADGE_SLATE, ProgressionChrome.leafsBadgeStyle(0, 0));

        assertEquals("Leaves: 36/253 (14%)", ProgressionChrome.formatLeafsBadge(36, 253));
        assertEquals(ProgressionChrome.BADGE_AMBER, ProgressionChrome.leafsBadgeStyle(36, 253));

        assertEquals("Leaves: 253/253 (100%)", ProgressionChrome.formatLeafsBadge(253, 253));
        assertEquals(ProgressionChrome.BADGE_GREEN, ProgressionChrome.leafsBadgeStyle(253, 253));
    }

    @Test
    void rendersTranslucentBorderedBadgeWithXmlEscaping() {
        final StringBuilder sb = new StringBuilder();
        final int nextX = ProgressionChrome.appendBorderedBadge(
                sb, "ETA: <1m & fast", 1200, 18, ProgressionChrome.BADGE_INDIGO);
        final String svg = sb.toString();

        assertTrue(nextX < 1200);
        assertTrue(svg.contains("fill-opacity=\"0.14\""));
        assertTrue(svg.contains("stroke-opacity=\"0.65\""));
        assertTrue(svg.contains("ETA: &lt;1m &amp; fast"));
    }

    @Test
    void formatsElapsedTimeSafely() {
        assertEquals("00:00:00", ProgressionChrome.formatElapsedTime(-100L));
        assertEquals("00:10:52", ProgressionChrome.formatElapsedTime((10 * 60 + 52) * 1000L));
        assertEquals("02:05:09", ProgressionChrome.formatElapsedTime((2 * 3600 + 5 * 60 + 9) * 1000L));
    }
}
