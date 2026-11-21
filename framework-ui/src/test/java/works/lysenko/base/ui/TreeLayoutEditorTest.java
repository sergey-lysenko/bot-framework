package works.lysenko.base.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.ui.tree.ScenarioOffsetSourceCodec;
import works.lysenko.base.ui.tree.ScenarioOffsetSourceCodec.Offsets;
import works.lysenko.base.ui.tree.TreeLayoutAutoSolver;
import works.lysenko.base.ui.tree.TreeLayoutAutoSolver.AspectPreset;
import works.lysenko.base.ui.tree.TreeLayoutAutoSolver.SolveResult;
import works.lysenko.base.ui.tree.TreeLayoutEditorCanvas;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.PreviewResult;
import works.lysenko.base.ui.tree.TreeLayoutEditorModel.ScenarioBlueprint;
import works.lysenko.util.prop.tree.Optimization;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeLayoutEditorTest {

    @Test
    void codecParsesOffsetsIncludingValueSyntaxAndTrailingCommentsWithDigits() {
        final List<String> lines = List.of(
                "package com.example;",
                "",
                "import works.lysenko.util.prop.tree.VisualOffset;",
                "import works.lysenko.util.prop.tree.VisualRowOffset;",
                "",
                "@VisualOffset(value = 6) // issue #42",
                "@VisualRowOffset(-5) // shifted 2 rows",
                "public class Billing {}"
        );
        final Offsets parsed = ScenarioOffsetSourceCodec.parseOffsets(lines);
        assertEquals(new Offsets(6, -5), parsed);
    }

    @Test
    void codecAddsUpdatesAndCleansUnusedImportsWhenResetToZero(@TempDir final Path tempDir) throws Exception {
        final Path file = tempDir.resolve("Billing.java");
        final String initial = """
                package com.example;

                import java.util.List;

                @SuppressWarnings("unused")
                public final class Billing {
                }
                """;
        Files.writeString(file, initial, StandardCharsets.UTF_8);

        // Add both offsets
        ScenarioOffsetSourceCodec.writeOffsets(file, new Offsets(6, -7));
        final String withBoth = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(withBoth.contains("import works.lysenko.util.prop.tree.VisualOffset;"));
        assertTrue(withBoth.contains("import works.lysenko.util.prop.tree.VisualRowOffset;"));
        assertTrue(withBoth.contains("@VisualOffset(6)\n@VisualRowOffset(-7)\npublic final class Billing"));
        assertTrue(withBoth.contains("@SuppressWarnings(\"unused\")"));

        // Reset colOffset to 0, keep rowOffset = -3 -> VisualOffset import removed, VisualRowOffset kept
        ScenarioOffsetSourceCodec.writeOffsets(file, new Offsets(0, -3));
        final String onlyRow = Files.readString(file, StandardCharsets.UTF_8);
        assertFalse(onlyRow.contains("VisualOffset;"));
        assertFalse(onlyRow.contains("@VisualOffset("));
        assertTrue(onlyRow.contains("import works.lysenko.util.prop.tree.VisualRowOffset;"));
        assertTrue(onlyRow.contains("@VisualRowOffset(-3)\npublic final class Billing"));

        // Reset both to 0 -> both annotations and both imports removed
        ScenarioOffsetSourceCodec.writeOffsets(file, Offsets.ZERO);
        final String cleaned = Files.readString(file, StandardCharsets.UTF_8);
        assertFalse(cleaned.contains("VisualOffset"));
        assertFalse(cleaned.contains("VisualRowOffset"));
        assertEquals(Offsets.ZERO, ScenarioOffsetSourceCodec.readOffsets(file, new Offsets(9, 9)));
    }

    @Test
    void codecAddsBlankLineAfterPackageWhenNoImportsExist() throws Exception {
        final List<String> lines = List.of(
                "package com.example;",
                "",
                "public class Simple {}"
        );
        final List<String> updated = ScenarioOffsetSourceCodec.applyOffsets(lines, new Offsets(3, 0));
        assertEquals("package com.example;", updated.get(0));
        assertEquals("", updated.get(1));
        assertEquals("import works.lysenko.util.prop.tree.VisualOffset;", updated.get(2));
        assertEquals("", updated.get(3));
        assertEquals("@VisualOffset(3)", updated.get(4));
        assertEquals("public class Simple {}", updated.get(5));
    }

    @Test
    void modelDistinguishesDuplicateSimpleNamesByFqnAndSupportsUndoRedoAndIsolatedSettings() {
        final ScenarioBlueprint root = new ScenarioBlueprint("pkg.Login", "Login", "Login ▷", null);
        final ScenarioBlueprint topSettings = new ScenarioBlueprint("pkg.Settings", "Settings", "Settings ▷", null);
        final ScenarioBlueprint partners = new ScenarioBlueprint("pkg.Partners", "Partners", "Partners ▷", null);
        final ScenarioBlueprint nestedSettings = new ScenarioBlueprint("pkg.partners.Settings", "Settings", "Settings ▷", null);
        root.children().add(topSettings);
        root.children().add(partners);
        partners.children().add(nestedSettings);

        final TreeLayoutEditorModel model = new TreeLayoutEditorModel();
        model.loadBlueprints(List.of(root), Map.of(
                "pkg.Settings", new Offsets(6, -5),
                "pkg.partners.Settings", Offsets.ZERO
        ));

        // Duplicate simple name "Settings" must keep distinct offsets by FQN
        assertEquals(new Offsets(6, -5), model.currentOffsets("pkg.Settings"));
        assertEquals(Offsets.ZERO, model.currentOffsets("pkg.partners.Settings"));

        // Edit topSettings and verify undo/redo
        assertTrue(model.setOffsets("pkg.Settings", 4, -2));
        assertEquals(new Offsets(4, -2), model.currentOffsets("pkg.Settings"));
        assertEquals(Offsets.ZERO, model.currentOffsets("pkg.partners.Settings"));
        assertEquals(1, model.modifiedCount());
        assertTrue(model.canUndo());
        assertFalse(model.canRedo());

        assertTrue(model.undo());
        assertEquals(new Offsets(6, -5), model.currentOffsets("pkg.Settings"));
        assertEquals(0, model.modifiedCount());
        assertTrue(model.canRedo());

        assertTrue(model.redo());
        assertEquals(new Offsets(4, -2), model.currentOffsets("pkg.Settings"));
        assertEquals(1, model.modifiedCount());

        // Revert all & undo revert
        assertTrue(model.revertAll());
        assertEquals(0, model.modifiedCount());
        assertTrue(model.undo());
        assertEquals(1, model.modifiedCount());

        // Reset all to zero (single undoable step) & undo
        assertTrue(model.setOffsets("pkg.partners.Settings", 2, 3));
        assertEquals(2, model.shiftedCount());
        assertTrue(model.resetAllToZero());
        assertEquals(0, model.shiftedCount());
        assertEquals(Offsets.ZERO, model.currentOffsets("pkg.Settings"));
        assertEquals(Offsets.ZERO, model.currentOffsets("pkg.partners.Settings"));
        assertFalse(model.resetAllToZero());
        assertTrue(model.undo());
        assertEquals(2, model.shiftedCount());
        assertEquals(new Offsets(4, -2), model.currentOffsets("pkg.Settings"));
        assertEquals(new Offsets(2, 3), model.currentOffsets("pkg.partners.Settings"));

        // Preview settings must NOT leak into global static Optimization fields
        final boolean origHOff = Optimization.reduceHorizontalOffsets;
        final int origMinSep = Optimization.minOffsetSeparation;
        model.previewSettings().compacted = true;
        model.previewSettings().reduceHorizontalOffsets = !origHOff;
        model.previewSettings().minOffsetSeparation = origMinSep + 3;
        final PreviewResult preview = model.recomputePreview();
        assertEquals(origHOff, Optimization.reduceHorizontalOffsets);
        assertEquals(origMinSep, Optimization.minOffsetSeparation);
        assertEquals(4, preview.layout().nodes().size());
        assertEquals(0, preview.overlapCount());
    }

    @Test
    void panelAndCanvasRenderAndToggleTreeTableViewsCleanly() throws Exception {
        final ScenarioBlueprint root = new ScenarioBlueprint("pkg.Login", "Login", "Login ▷", null);
        final ScenarioBlueprint billing = new ScenarioBlueprint("pkg.Billing", "Billing", "Billing ▷", null);
        final ScenarioBlueprint child = new ScenarioBlueprint("pkg.billing.Invoices", "Invoices", "Invoices ◼", null);
        root.children().add(billing);
        billing.children().add(child);

        final TreeLayoutEditorModel model = new TreeLayoutEditorModel();
        model.loadBlueprints(List.of(root), Map.of("pkg.Billing", new Offsets(4, -2)));

        SwingUtilities.invokeAndWait(() -> {
            final TreeLayoutEditorPanel panel = new TreeLayoutEditorPanel(model, false);
            assertTrue(panel.isTreeViewActive());
            assertEquals(AspectPreset.WIDESCREEN_16_9, panel.getSelectedAspectPreset());
            panel.setSelectedAspectPreset(AspectPreset.CLASSIC_4_3);
            assertEquals(AspectPreset.CLASSIC_4_3, panel.getSelectedAspectPreset());

            panel.setTreeViewActive(false);
            assertFalse(panel.isTreeViewActive());

            panel.setTreeViewActive(true);
            assertTrue(panel.isTreeViewActive());

            final TreeLayoutEditorCanvas canvas = panel.getCanvas();
            canvas.setSize(900, 500);
            canvas.setShowGrid(true);
            canvas.setShowRawGhosts(true);
            canvas.setSelectedClassName("pkg.Billing");
            canvas.fitToView();

            final BufferedImage img = new BufferedImage(900, 500, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g2 = img.createGraphics();
            canvas.paint(g2);
            g2.dispose();

            // Focus on Billing and verify hitTest at center of canvas resolves Billing
            canvas.focusNode("pkg.Billing", true);
            final NodeData hit = canvas.hitTest(450, 250);
            assertNotNull(hit, "Focused node should be hit-testable at canvas center");
            assertEquals("pkg.Billing", model.latestPreview().classNameByNodeId().get(hit.id()));
        });
    }

    @Test
    void autoSolverImprovesTallTreeRespectsAspectAndSupportsBatchUndo() {
        final ScenarioBlueprint root = buildTallSyntheticSuite();
        final TreeLayoutEditorModel model = new TreeLayoutEditorModel();
        model.loadBlueprints(List.of(root), Map.of());

        final SolveResult widescreen = TreeLayoutAutoSolver.solve(model, AspectPreset.WIDESCREEN_16_9);
        assertFalse(widescreen.cancelled());
        assertTrue(widescreen.improved(), "Auto-solver should improve tall all-zero tree for 16:9");
        assertEquals(0, widescreen.after().overlaps(), "Solved layout must have zero overlaps");
        assertTrue(widescreen.after().score() < widescreen.before().score());
        assertTrue(widescreen.after().rows() < widescreen.before().rows(),
                "16:9 optimization should reduce total rows on a tall tree");
        assertTrue(widescreen.after().aspectRatio() > widescreen.before().aspectRatio(),
                "16:9 optimization should widen a narrow tall tree");

        // Apply batch as a single undoable step
        assertTrue(model.applyOffsetsBatch(widescreen.offsets()));
        assertTrue(model.shiftedCount() > 0);
        assertEquals(0, model.latestPreview().overlapCount());

        // Running again from current offsets must never degrade the score
        final SolveResult refined = TreeLayoutAutoSolver.solve(model, AspectPreset.WIDESCREEN_16_9);
        assertFalse(refined.cancelled());
        assertEquals(0, refined.after().overlaps());
        assertTrue(refined.after().score() <= refined.before().score() + 1e-6);

        // Single undo restores all zero offsets cleanly
        assertTrue(model.undo());
        assertEquals(0, model.shiftedCount());

        // Portrait preset should keep a narrower aspect ratio than widescreen
        final SolveResult portrait = TreeLayoutAutoSolver.solve(model, AspectPreset.PORTRAIT_9_16);
        assertFalse(portrait.cancelled());
        assertEquals(0, portrait.after().overlaps());
        assertTrue(portrait.after().aspectRatio() < widescreen.after().aspectRatio(),
                "9:16 layout should be narrower than 16:9 layout");

        // Cancellation stops immediately
        final SolveResult cancelled = TreeLayoutAutoSolver.solve(
                model.roots(),
                model.snapshotCurrentOffsets(),
                model.previewSettings().copy(),
                AspectPreset.WIDESCREEN_16_9,
                () -> true,
                null);
        assertTrue(cancelled.cancelled());
        assertFalse(cancelled.improved());
    }

    private static ScenarioBlueprint buildTallSyntheticSuite() {
        final ScenarioBlueprint signIn = new ScenarioBlueprint("pkg.SignIn", "SignIn", "SignIn ▷", null);
        final ScenarioBlueprint login = new ScenarioBlueprint("pkg.Login", "Login", "Login ▷", null);
        signIn.children().add(login);

        final String[] branches = {"AppManagement", "AuthSecurity", "Billing", "Partners", "Reports", "StoreFront"};
        for (final String branch : branches) {
            final ScenarioBlueprint b = new ScenarioBlueprint("pkg." + branch, branch, branch + " ▷", null);
            login.children().add(b);
            for (int g = 1; g <= 3; g++) {
                final String gName = branch + "Group" + g;
                final ScenarioBlueprint group = new ScenarioBlueprint(
                        "pkg." + branch + "." + gName, gName, gName + " ▷", null);
                b.children().add(group);
                for (int l = 1; l <= 4; l++) {
                    final String lName = gName + "Leaf" + l;
                    group.children().add(new ScenarioBlueprint(
                            "pkg." + branch + "." + lName, lName, lName + " ◼", null));
                }
            }
        }
        return signIn;
    }
}
