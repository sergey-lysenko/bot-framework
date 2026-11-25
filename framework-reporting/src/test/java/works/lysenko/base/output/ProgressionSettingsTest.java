package works.lysenko.base.output;

import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.TestProperties;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionSettingsTest {

    @Test
    void mp4GenerationIsEnabledByDefault() {
        final TestProperties previousProperties = Base.properties;
        try {
            Base.properties = null;
            ProgressionSettings.initialize();

            assertTrue(ProgressionSettings.current().scenarioMp4Enabled());
            assertTrue(ProgressionSettings.current().treeMp4Enabled());
            assertTrue(ProgressionSettings.current().scenarioWebpEnabled());
            assertTrue(ProgressionSettings.current().treeWebpEnabled());
            assertFalse(ProgressionSettings.current().scenarioGifEnabled());
            assertFalse(ProgressionSettings.current().treeGifEnabled());
            assertFalse(ProgressionSettings.current().treePerNodeEnabled());
            assertTrue(ProgressionSettings.current().treeSonificationEnabled());
            assertEquals(1.0, ProgressionSettings.current().treeMovementSpeed(), 1e-9);
        } finally {
            Base.properties = previousProperties;
            ProgressionSettings.initialize();
        }
    }

    @Test
    void defaultSonificationsPropertyDerivesFromRegisteredSonifiers() throws Exception {
        final String expected = String.join(", ", works.lysenko.base.output.sonifier.TreeProgressionAudio.defaultModes());
        assertEquals("adp, edm, dnb, neon, jmj, carbon", expected);
        assertEquals(expected, works.lysenko.util.prop.PropEnum._TEST_REPORT_PROGRESSION_TREE_SONIFICATIONS.defaultValue());

        try (var stream = ProgressionSettings.class.getResourceAsStream("/config/parts/tests.part")) {
            assertTrue(stream != null, "tests.part resource should be on classpath");
            final String content = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(content.contains("Default: " + expected),
                    "tests.part documented default should match derived sonifications default: " + expected);
        }
    }

    @Test
    void cachesProgressionPropertiesUntilExplicitlyReinitialized() throws Exception {
        final TestProperties previousProperties = Base.properties;
        try {
            Base.properties = properties(
                    ".test.report.progression.scenario", "false",
                    ".test.report.progression.tree", "true",
                    ".test.report.progression.tree.per.node", "true",
                    ".tree.progression.movement.speed", "3/2",
                    ".test.report.progression.max.frames", "17",
                    ".test.report.progression.max.frame.pixels", "1234",
                    ".test.report.progression.max.total.pixels", "5678",
                    ".test.report.progression.scenario.gif", "true",
                    ".test.report.progression.scenario.webp", "false",
                    ".test.report.progression.scenario.mp4", "false",
                    ".test.report.progression.tree.gif", "true",
                    ".test.report.progression.tree.webp", "false",
                    ".test.report.progression.tree.mp4", "true",
                    ".test.report.progression.tree.sonification", "true",
                    ".test.report.progression.tree.sonifications", "copilot",
                    ".test.report.progression.ffmpeg", "/tmp/ffmpeg");
            ProgressionSettings.initialize();
            final ProgressionSettings snapshot = ProgressionSettings.current();

            assertFalse(snapshot.scenarioEnabled());
            assertTrue(snapshot.treeEnabled());
            assertTrue(snapshot.treePerNodeEnabled());
            assertEquals(1.5, snapshot.treeMovementSpeed(), 1e-9);
            assertEquals(17, snapshot.maxFrames());
            assertEquals(1234, snapshot.maxFramePixels());
            assertEquals(5678, snapshot.maxTotalPixels());
            assertTrue(snapshot.scenarioGifEnabled());
            assertFalse(snapshot.scenarioWebpEnabled());
            assertFalse(snapshot.scenarioMp4Enabled());
            assertTrue(snapshot.treeGifEnabled());
            assertFalse(snapshot.treeWebpEnabled());
            assertTrue(snapshot.treeMp4Enabled());
            assertTrue(snapshot.treeSonificationEnabled());
            assertEquals("copilot", snapshot.treeSonifier().orElseThrow().mode());
            assertEquals("/tmp/ffmpeg", snapshot.ffmpeg());

            Base.properties.put(".test.report.progression.max.frames", "29");
            Base.properties.put(".tree.progression.movement.speed", "1/4");
            assertSame(snapshot, ProgressionSettings.current());
            assertEquals(17, ProgressionSettings.current().maxFrames());
            assertEquals(1.5, ProgressionSettings.current().treeMovementSpeed(), 1e-9);

            ProgressionSettings.initialize();
            assertEquals(29, ProgressionSettings.current().maxFrames());
            assertEquals(0.25, ProgressionSettings.current().treeMovementSpeed(), 1e-9);

            // Non-positive value safely falls back to 1.0
            Base.properties.put(".tree.progression.movement.speed", "0");
            ProgressionSettings.initialize();
            assertEquals(1.0, ProgressionSettings.current().treeMovementSpeed(), 1e-9);

            // Sonification disabled sets treeSonification to "none"
            Base.properties.put(".test.report.progression.tree.sonification", "false");
            ProgressionSettings.initialize();
            assertFalse(ProgressionSettings.current().treeSonificationEnabled());
            assertEquals("none", ProgressionSettings.current().treeSonification());
        } finally {
            Base.properties = previousProperties;
            ProgressionSettings.initialize();
        }
    }

    private static TestProperties properties(final String... keyValues) throws Exception {
        final TestProperties testProperties = new TestProperties();
        final Properties values = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            values.setProperty(keyValues[i], keyValues[i + 1]);
        }
        final Field theField = TestProperties.class.getDeclaredField("the");
        theField.setAccessible(true);
        theField.set(testProperties, values);
        return testProperties;
    }
}
