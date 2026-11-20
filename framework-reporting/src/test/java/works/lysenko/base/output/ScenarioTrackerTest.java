package works.lysenko.base.output;
import works.lysenko.testing.SelectionFixture;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import works.lysenko.util.prop.PropEnum;

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
class ScenarioTrackerTest {

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
        ScenarioTracker.reset();
        ProgressionSettings.initialize();

        final Core core = (Core) SelectionFixture.allocate(Core.class);
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
        ScenarioTracker.reset();
        ProgressionSettings.initialize();
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

        final BufferedImage img = ScenarioTracker.renderProgressionGraph(scenarios, 2, 1);
        assertNotNull(img);
        assertEquals(1280, img.getWidth());
        assertEquals(680, img.getHeight());
    }

    @Test
    void testFormatElapsedTime() {
        assertEquals("00:00:00", ScenarioTracker.formatElapsedTime(999));
        assertEquals("01:01:01", ScenarioTracker.formatElapsedTime(3_661_999));
        assertEquals("00:00:00", ScenarioTracker.formatElapsedTime(-1));
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

        final BufferedImage img = ScenarioTracker.renderProgressionGraph(scenarios, 5, 23);
        assertNotNull(img);
        assertEquals(1280, img.getWidth());
        assertEquals(680, img.getHeight());
    }

    @Test
    void testDenseScenarioSuiteUsesUnifiedChromeBranchBracketsAndStatusColors() {
        final String[] branches = {
                "auditLogs", "billing", "customers", "dashboard", "delivery",
                "discounts", "inventory", "locations", "menu", "notifications",
                "orders", "reports", "settings", "staff"
        };
        final List<_Scenario> scenarios = new ArrayList<>();
        for (int b = 0; b < branches.length; b++) {
            final int branchSize = 18;
            for (int i = 0; i < branchSize; i++) {
                final String name = (i == 0)
                        ? "signIn.login." + Character.toUpperCase(branches[b].charAt(0)) + branches[b].substring(1)
                        : "signIn.login." + branches[b] + ".sub" + (i % 3) + ".Action" + i;
                final TestLeaf leaf = new TestLeaf(name, fr(1.0));
                scenarios.add(leaf);
                if (b < 2 && i < 6) {
                    Base.core.getResults().count(leaf);
                    if (b == 0 && i == 1) {
                        Base.core.getResults().count(leaf);
                        Base.core.getResults().count(leaf);
                    }
                }
            }
        }
        scenarios.sort(java.util.Comparator.comparing(_Scenario::getShortName, String.CASE_INSENSITIVE_ORDER));
        final _Scenario active = scenarios.get(2);

        final String svg = ScenarioTracker.renderProgressionGraphSvg(
                scenarios, 1, 36, java.util.Set.of(active.getName()));

        assertTrue(svg.contains("Scenario Execution Progression"), "Title must use unified Title Case");
        assertFalse(svg.contains("SCENARIO EXECUTION PROGRESSION"), "Must not use legacy ALL CAPS title");
        assertTrue(svg.contains("Test #36"), "Cycle badge must use Title Case");
        assertTrue(svg.contains("Leafs: 12/252"), "Progress badge must use unified Leafs label");
        assertTrue(svg.contains("Target: 1"), "Target badge must use Title Case");
        assertTrue(svg.contains("ETA:"), "Header must include ETA badge");
        assertTrue(svg.contains("Elapsed:"), "Header must include Elapsed badge");
        assertTrue(svg.contains("id=\"branch-axis\""), "Dense suite (>45 scenarios) must use branch bracket axis");
        assertFalse(svg.contains("transform=\"rotate(-45"), "Dense suite must not render overlapping diagonal labels");
        assertTrue(svg.contains("id=\"active-footer\""), "Dense suite must render active-cycle footer callout");
        assertTrue(svg.contains("Root: signIn.login"), "Common root prefix must be extracted to legend");
        assertTrue(svg.contains("<title>"), "Bars must include SVG hover tooltips");
        assertTrue(svg.contains("auditLogs"), "Branch axis must display grouped branch names");
    }

    @Test
    void testSparseScenarioSuiteRendersPrefixStrippedDiagonalLabels() {
        final List<_Scenario> scenarios = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            final TestLeaf leaf = new TestLeaf("signIn.login.orders.OrderAction" + i, fr(1.0));
            scenarios.add(leaf);
            if (i < 4) {
                Base.core.getResults().count(leaf);
            }
        }

        final String svg = ScenarioTracker.renderProgressionGraphSvg(scenarios, 2, 5);
        assertTrue(svg.contains("id=\"scenario-labels\""), "Sparse suite (<=45 scenarios) must render diagonal labels");
        assertFalse(svg.contains("id=\"branch-axis\""), "Sparse suite must not switch to dense branch brackets");
        assertTrue(svg.contains("Root: signIn.login.orders"), "Common prefix must be stripped in sparse mode too");
        assertTrue(svg.contains(">OrderAction0<"), "Diagonal label should show concise stripped leaf name");
    }

    private static List<_Scenario> dominantBranchSuite() {
        final String[] thirdLevel = {"ReactCrashCheck", "Logout", "Remember", "Reset", "Captcha", "Session"};
        final List<_Scenario> scenarios = new ArrayList<>();
        for (final String third : thirdLevel) {
            for (int i = 0; i < 10; i++) {
                scenarios.add(new TestLeaf("signIn.login." + third + ".Step" + i, fr(1.0)));
            }
        }
        scenarios.add(new TestLeaf("signIn.WrongLogin", fr(1.0)));
        scenarios.add(new TestLeaf("signIn.WrongPassword", fr(1.0)));
        scenarios.sort(java.util.Comparator.comparing(_Scenario::getShortName, String.CASE_INSENSITIVE_ORDER));
        return scenarios;
    }

    @Test
    void testAutoLabelLevelDescendsPastSingleDominantBranch() {
        final String svg = ScenarioTracker.renderProgressionGraphSvg(dominantBranchSuite(), 1, 1);

        assertTrue(svg.contains("Root: signIn"), "Root must be shown in legend");
        assertTrue(svg.contains(">ReactCrashCheck</text>"), "Auto level must label by third tree level, not just 'login'");
        assertTrue(svg.contains(">WrongLogin</text>"), "Shallower siblings keep their own labels");
        assertTrue(svg.contains("8 branches"), "Footer must count third-level groups");
    }

    @Test
    void testExplicitLabelLevelOverridesAuto() throws Exception {
        final TestProperties previous = Base.properties;
        try {
            setTestProperty(".test.report.progression.scenario.label.level", "2");
            ProgressionSettings.initialize();
            final String svg = ScenarioTracker.renderProgressionGraphSvg(dominantBranchSuite(), 1, 1);

            assertFalse(svg.contains(">ReactCrashCheck</text>"), "Level 2 must not split login into third-level groups");
            assertTrue(svg.contains("3 branches"), "Level 2 yields login, WrongLogin, WrongPassword");
        } finally {
            Base.properties = previous;
            ProgressionSettings.initialize();
        }
    }


    @Test
    void testWriteAnimatedGif(@TempDir final Path tempDir) throws IOException {
        final File gifFile = tempDir.resolve("test_progression.gif").toFile();

        final List<BufferedImage> frames = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final BufferedImage frame = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
            frames.add(frame);
        }

        ScenarioTracker.writeAnimatedGif(frames, gifFile, 10, 150);

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
        ScenarioTracker.setCustomOutputDir(customRunDir);

        setTestProperty(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.getPropertyName(), "2");
        setTestProperty(PropEnum._TEST_REPORT_PROGRESSION_SCENARIO_GIF.getPropertyName(), "true");
        Base.parameters = new Parameters(new Properties());
        ProgressionSettings.initialize();

        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf("ScenarioAlpha", fr(1.0));
        final TestLeaf leaf2 = new TestLeaf("ScenarioBeta", fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf2, fr(1.0));

        final _Test mockTest = (_Test) SelectionFixture.allocate(works.lysenko.base.Test.class);
        final _Exec mockExec = (_Exec) SelectionFixture.allocate(works.lysenko.base.test.Exec.class);

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
        ScenarioTracker.onLimbo(1);

        // Limbo cycle 2: leaf 2 executed
        Base.core.getResults().count(leaf2);
        ScenarioTracker.onLimbo(2);

        // Limbo cycle 3: leaf 1 and 2 executed again to reach target
        Base.core.getResults().count(leaf1);
        Base.core.getResults().count(leaf2);
        ScenarioTracker.onLimbo(3);

        assertEquals(3, ScenarioTracker.getCapturedFrames().size());

        final File progressionDir = new File(customRunDir, "progression");
        assertTrue(new File(progressionDir, "frame_0001.svg").exists());
        assertTrue(new File(progressionDir, "frame_0002.svg").exists());
        assertTrue(new File(progressionDir, "frame_0003.svg").exists());

        // Complete tests
        ScenarioTracker.onComplete();
        ScenarioTracker.processFrames(customRunDir, "run", ProcessingProgress.NONE);

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
