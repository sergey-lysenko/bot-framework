package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Parameters;
import works.lysenko.base.Results;
import works.lysenko.base.TestProperties;
import works.lysenko.tree.Ctrl;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Exec;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.spec.PropEnum;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.func.type.fractions.Factory.fr;

@SuppressWarnings({"removal", "deprecation"})
class ProgressionTrackerTest {

    private Core previousCore;
    private Parameters previousParameters;
    private TestProperties previousProperties;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousParameters = Base.parameters;
        previousProperties = Base.properties;
        Base.parameters = null;
        Base.properties = null;
        ProgressionTracker.reset();

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final Core core = (Core) unsafe.allocateInstance(Core.class);
        final Results results = new Results();
        final Field rf = Core.class.getDeclaredField("results");
        rf.setAccessible(true);
        rf.set(core, results);
        Base.core = core;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
        Base.parameters = previousParameters;
        Base.properties = previousProperties;
        ProgressionTracker.reset();
    }

    private static void setTestProperty(final String key, final String value) {
        try {
            final TestProperties testProperties = new TestProperties();
            final Field theField = TestProperties.class.getDeclaredField("the");
            theField.setAccessible(true);
            final Properties props = new Properties();
            props.put(key, value);
            theField.set(testProperties, props);
            Base.properties = testProperties;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testRenderProgressionGraph() {
        final List<_Scenario> scenarios = new ArrayList<>();
        final TestLeaf leaf1 = new TestLeaf("LeafA", fr(1.0));
        final TestLeaf leaf2 = new TestLeaf("LeafB", fr(1.0));
        scenarios.add(leaf1);
        scenarios.add(leaf2);

        final BufferedImage img = ProgressionTracker.renderProgressionGraph(scenarios, 2, 1);
        assertNotNull(img);
        assertEquals(1280, img.getWidth());
        assertEquals(680, img.getHeight());
    }

    @Test
    void testFormatElapsedTime() {
        assertEquals("00:00:00", ProgressionTracker.formatElapsedTime(999));
        assertEquals("01:01:01", ProgressionTracker.formatElapsedTime(3_661_999));
        assertEquals("00:00:00", ProgressionTracker.formatElapsedTime(-1));
    }

    @Test
    void testRenderProgressionGraphWithManyAndLongScenarios() {
        final List<_Scenario> scenarios = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            final String name = (i % 2 == 0)
                    ? "signIn.correctLogin.settings.configureProducts.Option" + i + ".Execute"
                    : "signIn.short" + i;
            scenarios.add(new TestLeaf(name, fr(1.0)));
        }

        final BufferedImage img = ProgressionTracker.renderProgressionGraph(scenarios, 5, 23);
        assertNotNull(img);
        assertEquals(1280, img.getWidth());
        assertEquals(680, img.getHeight());
    }

    @Test
    void testWriteAnimatedGif(@TempDir final Path tempDir) throws IOException {
        final File gifFile = tempDir.resolve("test_progression.gif").toFile();

        final List<BufferedImage> frames = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final BufferedImage frame = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
            frames.add(frame);
        }

        ProgressionTracker.writeAnimatedGif(frames, gifFile, 10, 150);

        assertTrue(gifFile.exists());
        assertTrue(gifFile.length() > 0);

        // Verify ImageReader can read back all 5 frames
        try (final ImageInputStream iis = ImageIO.createImageInputStream(gifFile)) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            assertTrue(readers.hasNext(), "Should have a GIF reader");
            final ImageReader reader = readers.next();
            reader.setInput(iis);
            final int numImages = reader.getNumImages(true);
            assertEquals(5, numImages, "Should have 5 frames in GIF sequence");
            reader.dispose();
        }
    }

    @Test
    void testOnLimboAndOnCompleteLifecycle(@TempDir final Path tempDir) throws Exception {
        final File customRunDir = tempDir.toFile();
        ProgressionTracker.setCustomOutputDir(customRunDir);

        setTestProperty(PropEnum._ALL_LEAFS_COUNT.getPropertyName(), "2");
        Base.parameters = new Parameters(new Properties());

        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf("ScenarioAlpha", fr(1.0));
        final TestLeaf leaf2 = new TestLeaf("ScenarioBeta", fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf2, fr(1.0));

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final _Test mockTest = (_Test) unsafe.allocateInstance(works.lysenko.base.Test.class);
        final _Exec mockExec = (_Exec) unsafe.allocateInstance(works.lysenko.base.test.Exec.class);

        final Field ctrlField = works.lysenko.base.test.Exec.class.getDeclaredField("ctrl");
        ctrlField.setAccessible(true);
        ctrlField.set(mockExec, rootCtrl);

        final Field execField = works.lysenko.base.Test.class.getDeclaredField("executor");
        execField.setAccessible(true);
        execField.set(mockTest, mockExec);

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(Base.core, mockTest);

        // Limbo cycle 1: leaf 1 executed
        Base.core.getResults().count(leaf1);
        ProgressionTracker.onLimbo(1);

        // Limbo cycle 2: leaf 2 executed
        Base.core.getResults().count(leaf2);
        ProgressionTracker.onLimbo(2);

        // Limbo cycle 3: leaf 1 and 2 executed again to reach target
        Base.core.getResults().count(leaf1);
        Base.core.getResults().count(leaf2);
        ProgressionTracker.onLimbo(3);

        assertEquals(3, ProgressionTracker.getCapturedFrames().size());

        final File progressionDir = new File(customRunDir, "progression");
        assertTrue(new File(progressionDir, "frame_0001.png").exists());
        assertTrue(new File(progressionDir, "frame_0002.png").exists());
        assertTrue(new File(progressionDir, "frame_0003.png").exists());

        // Complete tests
        ProgressionTracker.onComplete();

        final File[] gifFiles = customRunDir.listFiles((d, name) -> name.endsWith(".progression.gif"));
        assertNotNull(gifFiles);
        assertEquals(1, gifFiles.length);
        assertTrue(gifFiles[0].length() > 0);

        final File[] webpFiles = customRunDir.listFiles((d, name) -> name.endsWith(".progression.webp"));
        assertNotNull(webpFiles);
        assertEquals(1, webpFiles.length);
        assertEquals(3, AnimatedWebPTest.countFrames(webpFiles[0]));

        // Verify animated GIF content
        try (final ImageInputStream iis = ImageIO.createImageInputStream(gifFiles[0])) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            final ImageReader reader = readers.next();
            reader.setInput(iis);
            assertEquals(3, reader.getNumImages(true));
            reader.dispose();
        }
    }

    private static class TestLeaf extends Leaf {
        private final String shortName;

        TestLeaf(final String shortName, final Fraction weight) {
            super(weight);
            this.shortName = shortName;
        }

        @Override
        public String getShortName() {
            return shortName;
        }

        @Override
        public String getName() {
            return shortName;
        }
    }
}
