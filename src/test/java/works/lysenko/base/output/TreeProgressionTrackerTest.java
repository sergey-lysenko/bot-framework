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
import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.tree.Ctrl;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Exec;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.spec.PropEnum;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static works.lysenko.util.func.type.fractions.Factory.fr;

@SuppressWarnings({"removal", "deprecation"})
class TreeProgressionTrackerTest {

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
        TreeProgressionTracker.reset();

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
        TreeProgressionTracker.reset();
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

    private static float getHue(final Color c) {
        final float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        return hsb[0];
    }

    @Test
    void testGetProgressColorMonotonicHueTransition() {
        // Zero or negative executions -> unvisited slate border color
        final Color unvisited = TreeProgressionTracker.getProgressColor(0, 5);
        assertEquals(new Color(0x33, 0x41, 0x55), unvisited);

        // Target = 1 -> immediate green
        final Color targetOne = TreeProgressionTracker.getProgressColor(1, 1);
        assertEquals(new Color(0x22, 0xC5, 0x5E), targetOne);

        // Target = 5: hue should monotonically shift from warm amber toward emerald green
        final int target = 5;
        final float hue1 = getHue(TreeProgressionTracker.getProgressColor(1, target));
        final float hue2 = getHue(TreeProgressionTracker.getProgressColor(2, target));
        final float hue3 = getHue(TreeProgressionTracker.getProgressColor(3, target));
        final float hue4 = getHue(TreeProgressionTracker.getProgressColor(4, target));
        final float hue5 = getHue(TreeProgressionTracker.getProgressColor(5, target));

        assertTrue(hue1 < hue2, "Hue should increase from 1 to 2");
        assertTrue(hue2 < hue3, "Hue should increase from 2 to 3");
        assertTrue(hue3 < hue4, "Hue should increase from 3 to 4");
        assertTrue(hue4 < hue5, "Hue should increase from 4 to 5");

        // Verify bounds: 1 is in amber range (~38-60 deg = ~0.10-0.17), 5 is emerald green (~142 deg = ~0.39)
        assertTrue(hue1 >= 0.10f && hue1 <= 0.20f, "Hue 1 should be warm amber/gold");
        assertTrue(hue5 >= 0.35f && hue5 <= 0.42f, "Hue 5 should be emerald green");
    }

    @Test
    void testRenderTreeProgressionLayout() {
        final List<NodeData> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();

        final NodeData root = new NodeData("col_0_0", "root.App", "Root", 0, 1.0, null);
        final NodeData leaf1 = new NodeData("col_1_0", "root.login.Success", "Root", 1, 1.0, null);
        final NodeData leaf2 = new NodeData("col_1_1", "root.login.Failure", "Root", 1, 2.0, null);

        leaf1.setParent(root);
        leaf2.setParent(root);
        root.children().add(leaf1);
        root.children().add(leaf2);

        nodes.add(root);
        nodes.add(leaf1);
        nodes.add(leaf2);
        edges.add(new Edge(root, leaf1));
        edges.add(new Edge(root, leaf2));

        final TreeLayout layout = new TreeLayout(nodes, edges);
        final BufferedImage img = TreeProgressionTracker.renderTreeProgression(layout, 3, 1);

        assertNotNull(img);
        assertTrue(img.getWidth() >= 1280);
        assertTrue(img.getHeight() >= 720);
    }

    @Test
    void testTreeProgressionLifecycleAndGifGeneration(@TempDir final Path tempDir) throws Exception {
        final File customRunDir = tempDir.toFile();
        TreeProgressionTracker.setCustomOutputDir(customRunDir);

        setTestProperty(PropEnum._ALL_LEAFS_COUNT.getPropertyName(), "2");
        Base.parameters = new Parameters(new Properties());

        final Ctrl rootCtrl = new Ctrl(null);
        final TestLeaf leaf1 = new TestLeaf("root.auth.LoginSuccess", fr(1.0));
        final TestLeaf leaf2 = new TestLeaf("root.auth.LoginFailure", fr(1.0));
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
        TreeProgressionTracker.onLimbo(1);

        // Limbo cycle 2: leaf 2 executed
        Base.core.getResults().count(leaf2);
        TreeProgressionTracker.onLimbo(2);

        // Limbo cycle 3: leaf 1 and 2 executed again
        Base.core.getResults().count(leaf1);
        Base.core.getResults().count(leaf2);
        TreeProgressionTracker.onLimbo(3);

        assertEquals(3, TreeProgressionTracker.getCapturedFrames().size());

        final File treeProgressionDir = new File(customRunDir, "tree_progression");
        assertTrue(new File(treeProgressionDir, "frame_0001.png").exists());
        assertTrue(new File(treeProgressionDir, "frame_0002.png").exists());
        assertTrue(new File(treeProgressionDir, "frame_0003.png").exists());

        // Complete tests -> animated GIF should be produced
        TreeProgressionTracker.onComplete();

        final File[] gifFiles = customRunDir.listFiles((d, name) -> name.endsWith(".tree.progression.gif"));
        assertNotNull(gifFiles);
        assertEquals(1, gifFiles.length);
        assertTrue(gifFiles[0].length() > 0);

        // Verify animated GIF content
        try (final ImageInputStream iis = ImageIO.createImageInputStream(gifFiles[0])) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            assertTrue(readers.hasNext(), "Should have a GIF reader");
            final ImageReader reader = readers.next();
            reader.setInput(iis);
            assertEquals(3, reader.getNumImages(true));
            reader.dispose();
        }
    }

    @Test
    void testLayoutCoordinatesRemainStableAcrossCycles() {
        final TestLeaf leaf1 = new TestLeaf("root.auth.LoginSuccess", fr(1.0));
        final TestLeaf leaf2 = new TestLeaf("root.auth.LoginFailure", fr(1.0));

        final Ctrl rootCtrl = new Ctrl(null);
        rootCtrl.getPool().appendScenarioWithWeight(leaf1, fr(1.0));
        rootCtrl.getPool().appendScenarioWithWeight(leaf2, fr(1.0));

        try {
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
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Before any execution: both leafs exist in sortedStrings as stubs
        final TreeMap<String, Result> initialSorted = Base.core.getResults().getSortedStrings(false);
        final TreeLayout layoutInitial = TreeHtml.computeLayout(initialSorted);

        // Cycle 1: leaf 1 executed
        Base.core.getResults().count(leaf1);
        final TreeMap<String, Result> cycle1Sorted = Base.core.getResults().getSortedStrings(false);
        final TreeLayout layoutCycle1 = TreeHtml.computeLayout(cycle1Sorted);

        // Cycle 2: leaf 2 executed
        Base.core.getResults().count(leaf2);
        final TreeMap<String, Result> cycle2Sorted = Base.core.getResults().getSortedStrings(false);
        final TreeLayout layoutCycle2 = TreeHtml.computeLayout(cycle2Sorted);

        assertEquals(layoutInitial.nodes().size(), layoutCycle1.nodes().size());
        assertEquals(layoutCycle1.nodes().size(), layoutCycle2.nodes().size());

        for (int i = 0; i < layoutInitial.nodes().size(); i++) {
            final NodeData nInit = layoutInitial.nodes().get(i);
            final NodeData nC1 = layoutCycle1.nodes().get(i);
            final NodeData nC2 = layoutCycle2.nodes().get(i);

            assertEquals(nInit.id(), nC1.id());
            assertEquals(nInit.id(), nC2.id());
            assertEquals(nInit.col(), nC1.col());
            assertEquals(nInit.col(), nC2.col());
            assertEquals(nInit.row(), nC1.row(), 1e-6, "Row must not shift across cycles");
            assertEquals(nInit.row(), nC2.row(), 1e-6, "Row must not shift across cycles");
        }
    }

    @Test
    void testLogHtmlIncludesTreeProgressionLink(@TempDir final Path tempDir) throws IOException {
        final File logFile = tempDir.resolve("123456789.run.log").toFile();
        Files.writeString(logFile.toPath(), "[0] 12:00:00 # Applied test configuration\n[1][1] 12:00:01 Test passed\nTest session completed\n");

        final File treeGifFile = tempDir.resolve("123456789.tree.progression.gif").toFile();
        Files.write(treeGifFile.toPath(), new byte[]{0, 1, 2, 3});

        final File htmlFile = tempDir.resolve("123456789.log.html").toFile();
        LogHtml.generateReport(logFile, htmlFile);

        assertTrue(htmlFile.exists());
        final String htmlContent = Files.readString(htmlFile.toPath());
        assertTrue(htmlContent.contains("Tree Progression"), "HTML report must contain 'Tree Progression' button");
        assertTrue(htmlContent.contains("123456789.tree.progression.gif"), "HTML report must link to tree progression GIF");
    }

    private static class TestLeaf extends Leaf {
        private final String name;

        TestLeaf(final String name, final Fraction weight) {
            super(weight);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getShortName() {
            return name;
        }
    }
}
