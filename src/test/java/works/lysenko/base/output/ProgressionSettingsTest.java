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
            assertFalse(ProgressionSettings.current().treePerNodeEnabled());
            assertFalse(ProgressionSettings.current().treeSonificationEnabled());
        } finally {
            Base.properties = previousProperties;
            ProgressionSettings.initialize();
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
                    ".test.report.progression.max.frames", "17",
                    ".test.report.progression.max.frame.pixels", "1234",
                    ".test.report.progression.max.total.pixels", "5678",
                    ".test.report.progression.scenario.mp4", "false",
                    ".test.report.progression.tree.mp4", "true",
                    ".test.report.progression.tree.sonification", "copilot",
                    ".test.report.progression.ffmpeg", "/tmp/ffmpeg");
            ProgressionSettings.initialize();
            final ProgressionSettings snapshot = ProgressionSettings.current();

            assertFalse(snapshot.scenarioEnabled());
            assertTrue(snapshot.treeEnabled());
            assertTrue(snapshot.treePerNodeEnabled());
            assertEquals(17, snapshot.maxFrames());
            assertEquals(1234, snapshot.maxFramePixels());
            assertEquals(5678, snapshot.maxTotalPixels());
            assertFalse(snapshot.scenarioMp4Enabled());
            assertTrue(snapshot.treeMp4Enabled());
            assertTrue(snapshot.treeSonificationEnabled());
            assertEquals("copilot", snapshot.treeSonifier().orElseThrow().mode());
            assertEquals("/tmp/ffmpeg", snapshot.ffmpeg());

            Base.properties.put(".test.report.progression.max.frames", "29");
            assertSame(snapshot, ProgressionSettings.current());
            assertEquals(17, ProgressionSettings.current().maxFrames());

            ProgressionSettings.initialize();
            assertEquals(29, ProgressionSettings.current().maxFrames());
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
