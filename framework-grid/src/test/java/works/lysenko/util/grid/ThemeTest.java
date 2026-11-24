package works.lysenko.util.grid;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.data.enums.ColoursIgnore;
import works.lysenko.util.data.enums.Shape;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.data.records.diff.Pair;
import works.lysenko.util.data.type.Grid;
import works.lysenko.util.grid.expected.ColoursQuotas;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.gsrc.Geometry;
import works.lysenko.util.grid.record.gsrc.Resolution;
import works.lysenko.util.grid.record.misc.IgnoreHSB;
import works.lysenko.util.grid.record.misc.Limits;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ThemeTest {

    @AfterEach
    void tearDown() {
        Theme.clearActive();
    }

    @Test
    void parseColourSupportsSignedIntAndHex() {
        assertEquals(-16184042, Theme.parseColour("-16184042"));
        assertEquals(0xFF090D16, Theme.parseColour("#090d16"));
        assertEquals(0xFF090D16, Theme.parseColour("0x090D16"));
        assertEquals(0x80FFFFFF, Theme.parseColour("#80ffffff"));
        assertEquals(-1, Theme.parseColour("#ffffff"));
        assertNull(Theme.parseColour(""));
        assertNull(Theme.parseColour("#12345"));
        assertNull(Theme.parseColour("NON_EXISTENT_TOKEN_XYZ"));
    }

    @Test
    void singleGridSpecResolvesAcrossContrastingThemes() {
        final String spec = "SURFACE_BG;8|0.10|0.70, SURFACE_CARD;6|0.10|0.60";
        final Theme dark = Theme.of("dark", Map.of(
                "SURFACE_BG", 0xFF090D16,
                "SURFACE_CARD", 0xFF111827));
        final Theme light = Theme.of("light", Map.of(
                "SURFACE_BG", 0xFFF8FAFC,
                "SURFACE_CARD", 0xFFFFFFFF));

        final ColoursQuotas darkQuotas = new ColoursQuotas(
                "dashboard", spec, Limits.of(new Fraction(1, 10), null),
                Set.of(ColoursIgnore.ORDER), IgnoreHSB.of(), null, true, dark);
        final ColoursQuotas lightQuotas = new ColoursQuotas(
                "dashboard", spec, Limits.of(new Fraction(1, 10), null),
                Set.of(ColoursIgnore.ORDER), IgnoreHSB.of(), null, true, light);

        assertEquals(2, darkQuotas.get().size());
        assertEquals(0xFF090D16, darkQuotas.get().get(0).value());
        assertEquals(8, darkQuotas.get().get(0).precision());
        assertEquals("SURFACE_BG", darkQuotas.get().get(0).symbol());

        assertEquals(2, lightQuotas.get().size());
        assertEquals(0xFFF8FAFC, lightQuotas.get().get(0).value());
        assertEquals(0xFFFFFFFF, lightQuotas.get().get(1).value());
        assertEquals("SURFACE_CARD", lightQuotas.get().get(1).symbol());

        // Serialization preserves symbolic tokens rather than writing raw theme ARGBs
        final String serialized = darkQuotas.getPropertyValue();
        assertTrue(serialized.contains("SURFACE_BG;8"), "Expected symbol preserved: " + serialized);
        assertTrue(serialized.contains("SURFACE_CARD;6"), "Expected symbol preserved: " + serialized);
    }

    @Test
    void distinctTokensResolvingToSameColourInFlatThemeAreMerged() {
        final String spec = "SURFACE_CARD;4|0.05|0.50, SURFACE_ELEVATED;8|0.02|0.40";
        final Theme flatLight = Theme.of("enterprise-light", Map.of(
                "SURFACE_CARD", 0xFFFFFFFF,
                "SURFACE_ELEVATED", 0xFFFFFFFF));

        final ColoursQuotas quotas = new ColoursQuotas(
                "cards", spec, Limits.of(new Fraction(1, 10), null),
                Set.of(ColoursIgnore.ORDER), IgnoreHSB.of(), null, true, flatLight);

        assertEquals(1, quotas.get().size());
        final _Quota<?> merged = quotas.get().get(0);
        assertEquals(0xFFFFFFFF, merged.value());
        assertEquals(8, merged.precision());
        assertEquals(0.02, merged.min().doubleValue(), 1e-6);
        assertEquals(0.90, merged.max().doubleValue(), 1e-6);
    }

    @Test
    void calculatorClustersSlightlyShiftedPixelsIntoExpectedPrecisionBucket() {
        final Theme dark = Theme.of("terminal-dark", Map.of(
                "SURFACE_BG", 0xFF090D16,
                "SURFACE_CARD", 0xFF111827));
        final ColoursQuotas expected = new ColoursQuotas(
                "dashboard", "SURFACE_BG;8|0.10|0.80, SURFACE_CARD;6|0.10|0.80",
                Limits.of(new Fraction(1, 10), null),
                Set.of(ColoursIgnore.ORDER), IgnoreHSB.of(), null, true, dark);

        // 100x100 image split into 4 quadrants:
        // - Top-left: exact SURFACE_BG (#090d16)
        // - Top-right: slightly shifted SURFACE_BG (#0b0f18, Euclidean distance ~3.46 < 8)
        // - Bottom-left: slightly shifted SURFACE_CARD (#121928, Euclidean distance ~1.73 < 6)
        // - Bottom-right: off-theme red (#ff0000, distance > 200 -> MUST NOT snap)
        final BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                final int rgb;
                if (y < 50 && x < 50) rgb = 0xFF090D16;
                else if (y < 50) rgb = 0xFF0B0F18;
                else if (x < 50) rgb = 0xFF121928;
                else rgb = 0xFFFF0000;
                img.setRGB(x, y, rgb);
            }
        }
        final Geometry geometry = new Geometry(new Location(50f, 50f), 40f, 1f);
        final Resolution resolution = new Resolution(4, 4, Fraction.ONE, Shape.RECTANGLE);
        final Request request = new Request("dashboard", img, null, Fraction.ONE, resolution, Collections.emptyList(), null);
        final Grid grid = new Grid(img, geometry, request);

        final Map<Integer, Pair<Integer, String>> raw = grid.calculator().countPixelsByColor();
        assertEquals(4, raw.size(), "Raw unclustered count should see 4 distinct RGBs");

        final Map<Integer, Pair<Integer, String>> clustered = grid.calculator().countPixelsByColor(expected);
        assertEquals(3, clustered.size(), "Clustered count should merge shifted BG into canonical BG");
        assertTrue(clustered.containsKey(0xFF090D16), "Canonical BG bucket present");
        assertTrue(clustered.containsKey(0xFF111827), "Shifted CARD snapped to canonical CARD bucket");
        assertTrue(clustered.containsKey(0xFFFF0000), "Off-theme red must remain unclustered");
        assertEquals(
                raw.get(0xFF090D16).left() + raw.get(0xFF0B0F18).left(),
                clustered.get(0xFF090D16).left(),
                "Exact + shifted BG pixels should sum together");
    }

    @Test
    void exactMatchTakesPrecedenceOverEarlierPrecisionQuota() throws Exception {
        final Theme dark = Theme.of("terminal-dark", Map.of(
                "SURFACE_CARD", 0xFF111827,
                "SURFACE_OVERLAY", 0xFF0F1728)); // RGB distance ~2.45 from SURFACE_CARD
        final ColoursQuotas quotas = new ColoursQuotas(
                "runs-report", "SURFACE_CARD;10|0.10|0.80, SURFACE_OVERLAY;10|0.10|0.80",
                Limits.of(new Fraction(1, 10), null),
                Set.of(ColoursIgnore.ORDER), IgnoreHSB.of(), null, true, dark);

        final _Quota<?> first = quotas.get().get(0);
        final _Quota<?> second = quotas.get().get(1);
        assertEquals(0xFF111827, first.value());
        assertEquals(0xFF0F1728, second.value());
        // Direct exact-match lookup via Get.getSubstitute must select SURFACE_OVERLAY rather than SURFACE_CARD
        final works.lysenko.util.data.records.Noun noun = new works.lysenko.util.data.records.Noun("colour", "colours");
        final works.lysenko.util.grid.record.meta.Subject subject = new works.lysenko.util.grid.record.meta.Subject(
                noun, works.lysenko.util.grid.record.meta.Method.COMPULSIVE);
        final works.lysenko.util.grid.record.meta.ValidationMeta meta = new works.lysenko.util.grid.record.meta.ValidationMeta(
                works.lysenko.util.data.enums.Severity.S0, subject, null, 1, null);
        final java.lang.reflect.Method method = works.lysenko.util.func.grid.ranger.Get.class.getDeclaredMethod(
                "getSubstitute",
                works.lysenko.util.grid.record.meta.ValidationMeta.class,
                works.lysenko.util.apis.grid.q._Quotas.class,
                Object.class,
                boolean.class);
        method.setAccessible(true);
        final _Quota<?> matched = (_Quota<?>) method.invoke(null, meta, quotas, 0xFF0F1728, false);
        assertNotNull(matched);
        assertEquals(0xFF0F1728, matched.value());
        assertEquals("SURFACE_OVERLAY", matched.symbol());
    }

    @Test
    void activeThemeLifecycle() {
        assertNull(Theme.active());
        final Theme custom = Theme.of("custom", Map.of("SURFACE_BG", 0xFF102030));
        Theme.setActive(custom);
        assertEquals(custom, Theme.active());
        assertEquals(0xFF102030, Theme.resolveToken("SURFACE_BG", Theme.active()));
        Theme.clearActive();
        assertNull(Theme.active());
    }
}
