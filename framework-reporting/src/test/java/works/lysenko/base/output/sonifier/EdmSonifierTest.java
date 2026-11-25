package works.lysenko.base.output.sonifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.sonifier.EdmSynth.EdmEvent;
import works.lysenko.base.output.sonifier.EdmSynth.VoiceKind;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdmSonifierTest {

    @Test
    void resolveEdmPitchStaysInWarmMidRegisterAcrossTreeGeometry() {
        final int[] subRoots = {31, 33, 34, 36, 38, 39, 41};
        final int[][] chords = {
                {0, 3, 7, 10, 14},
                {0, 4, 7, 11, 14},
                {0, 4, 7, 10, 13},
                {0, 4, 7, 11, 18},
                {0, 4, 7, 14, 19}
        };

        for (final int subRoot : subRoots) {
            for (final int[] chord : chords) {
                for (int col = 0; col <= 8; col++) {
                    for (int rowTenths = 0; rowTenths <= 120; rowTenths += 5) {
                        final double rowRatio = 1.0 - Math.min(1.0, rowTenths / 120.0);
                        final EdmSonifier.NodeState node = new EdmSonifier.NodeState(
                                "root/n_" + col + "_" + rowTenths, col, rowTenths, 'L', 1, 2, 'P', false);
                        final double pitch = EdmSonifier.resolveEdmPitch(subRoot, chord, node, rowRatio);
                        assertTrue(pitch >= 55.0 && pitch <= 72.0,
                                () -> "Expected warm mid-register MIDI [55..72], got " + pitch);
                    }
                }
            }
        }
    }

    @Test
    void regularExecutionPlucksStayBelowHighPiercingRegister(@TempDir final Path tempDir) throws Exception {
        final EdmSonifier edm = new EdmSonifier();
        final File frame1 = tempDir.resolve("frame_0001.svg").toFile();
        final File frame2 = tempDir.resolve("frame_0002.svg").toFile();

        final Result rootRes1 = new Result(ScenarioType.NODE, null, null, null, List.of(), 1, 0);
        rootRes1.updateStatus(ExecutionStatus.PASSED);
        final TreeHtml.NodeData root1 = new TreeHtml.NodeData("root", "Root", "Suite", 0, 0, rootRes1);
        final TreeHtml.NodeData leafTop1 = new TreeHtml.NodeData(
                "leafTop", "TopLeaf", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 1, 0));
        final TreeHtml.NodeData leafBot1 = new TreeHtml.NodeData(
                "leafBot", "BotLeaf", "Suite", 4, 6, new Result(ScenarioType.LEAF, null, null, null, List.of(), 1, 0));
        leafTop1.setParent(root1);
        leafBot1.setParent(root1);
        root1.children().addAll(List.of(leafTop1, leafBot1));

        final Result rootRes2 = new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0);
        rootRes2.updateStatus(ExecutionStatus.PASSED);
        final TreeHtml.NodeData root2 = new TreeHtml.NodeData("root", "Root", "Suite", 0, 0, rootRes2);
        final TreeHtml.NodeData leafTop2 = new TreeHtml.NodeData(
                "leafTop", "TopLeaf", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leafBot2 = new TreeHtml.NodeData(
                "leafBot", "BotLeaf", "Suite", 4, 6, new Result(ScenarioType.LEAF, null, null, null, List.of(), 2, 0));
        leafTop2.setParent(root2);
        leafBot2.setParent(root2);
        root2.children().addAll(List.of(leafTop2, leafBot2));

        edm.capture(frame1, new TreeHtml.TreeLayout(new ArrayList<>(List.of(root1, leafTop1, leafBot1)), List.of()), 3);
        edm.capture(frame2, new TreeHtml.TreeLayout(new ArrayList<>(List.of(root2, leafTop2, leafBot2)), List.of()), 3);

        final Map<Integer, List<EdmEvent>> eventsByFrame =
                EdmSonifier.composeEdmEvents(List.of(frame1, frame2), EdmSynth.SAMPLE_RATE / 10);

        final List<EdmEvent> pluckEvents = eventsByFrame.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.kind() == VoiceKind.PLUCK_LEAD)
                .toList();

        assertFalse(pluckEvents.isEmpty(), "Expected PLUCK_LEAD events for regular branch and leaf executions");
        for (final EdmEvent event : pluckEvents) {
            assertTrue(event.midiPitch() >= 43.0 && event.midiPitch() <= 72.0,
                    () -> "Regular execution PLUCK_LEAD pitch out of warm register [43..72]: " + event.midiPitch());
        }
    }
}
