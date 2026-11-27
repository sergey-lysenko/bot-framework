package works.lysenko.base.output.sonifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.sonifier.DnbSynth.DnbEvent;
import works.lysenko.base.output.sonifier.DnbSynth.VoiceKind;
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

class DnbSonifierTest {

    @Test
    void resolveLiquidPitchStaysInLoweredOctaveRegisterAcrossTreeGeometry() {
        final int[] subRoots = {29, 30, 32, 34, 36, 37, 39};
        final int[][] chords = {
                {0, 3, 7, 10, 14},
                {0, 4, 7, 11, 14},
                {0, 4, 7, 9, 14},
                {0, 5, 7, 10, 14},
                {0, 4, 7, 11, 18},
                {0, 4, 7, 14, 19},
                {0, 4, 7, 10, 13}
        };

        for (final int subRoot : subRoots) {
            for (final int[] chord : chords) {
                for (int col = 0; col <= 8; col++) {
                    for (int rowTenths = 0; rowTenths <= 120; rowTenths += 5) {
                        final double rowRatio = 1.0 - Math.min(1.0, rowTenths / 120.0);
                        final DnbSonifier.NodeState node = new DnbSonifier.NodeState(
                                "root/n_" + col + "_" + rowTenths, col, rowTenths, 'L', 1, 2, 'P', false);
                        final double pitch = DnbSonifier.resolveLiquidPitch(subRoot, chord, node, rowRatio);
                        assertTrue(pitch >= 48.0 && pitch <= 69.0,
                                () -> "Expected lowered mid-register MIDI [48..69], got " + pitch);
                    }
                }
            }
        }
    }

    @Test
    void mainExecutionEventsUseLoweredOctaveRegister(@TempDir final Path tempDir) throws Exception {
        final DnbSonifier dnb = new DnbSonifier();
        final File frame1 = tempDir.resolve("frame_0001.svg").toFile();
        final File frame2 = tempDir.resolve("frame_0002.svg").toFile();

        final Result rootRes1 = new Result(ScenarioType.NODE, null, null, null, List.of(), 1, 0);
        rootRes1.updateStatus(ExecutionStatus.PASSED);
        final TreeHtml.NodeData root1 = new TreeHtml.NodeData("root", "Root", "Suite", 0, 0, rootRes1);
        final TreeHtml.NodeData leafPartial1 = new TreeHtml.NodeData(
                "leafPart", "PartialLeaf", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 1, 0));
        final TreeHtml.NodeData leafDone1 = new TreeHtml.NodeData(
                "leafDone", "DoneLeaf", "Suite", 4, 6, new Result(ScenarioType.LEAF, null, null, null, List.of(), 1, 0));
        leafPartial1.setParent(root1);
        leafDone1.setParent(root1);
        root1.children().addAll(List.of(leafPartial1, leafDone1));

        final Result rootRes2 = new Result(ScenarioType.NODE, null, null, null, List.of(), 2, 0);
        rootRes2.updateStatus(ExecutionStatus.PASSED);
        final TreeHtml.NodeData root2 = new TreeHtml.NodeData("root", "Root", "Suite", 0, 0, rootRes2);
        final TreeHtml.NodeData leafPartial2 = new TreeHtml.NodeData(
                "leafPart", "PartialLeaf", "Suite", 2, 0, new Result(ScenarioType.LEAF, null, null, null, List.of(), 2, 0));
        final TreeHtml.NodeData leafDone2 = new TreeHtml.NodeData(
                "leafDone", "DoneLeaf", "Suite", 4, 6, new Result(ScenarioType.LEAF, null, null, null, List.of(), 3, 0));
        leafPartial2.setParent(root2);
        leafDone2.setParent(root2);
        root2.children().addAll(List.of(leafPartial2, leafDone2));

        dnb.capture(frame1, new TreeHtml.TreeLayout(new ArrayList<>(List.of(root1, leafPartial1, leafDone1)), List.of()), 3);
        dnb.capture(frame2, new TreeHtml.TreeLayout(new ArrayList<>(List.of(root2, leafPartial2, leafDone2)), List.of()), 3);

        final Map<Integer, List<DnbEvent>> eventsByFrame =
                DnbSonifier.composeDnbEvents(List.of(frame1, frame2), DnbSynth.SAMPLE_RATE / 10);

        final List<DnbEvent> pluckEvents = eventsByFrame.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.kind() == VoiceKind.LIQUID_PLUCK)
                .toList();
        assertFalse(pluckEvents.isEmpty(), "Expected LIQUID_PLUCK events for branch and partial leaf executions");
        for (final DnbEvent event : pluckEvents) {
            assertTrue(event.midiPitch() >= 36.0 && event.midiPitch() <= 76.0,
                    () -> "LIQUID_PLUCK pitch out of lowered register [36..76]: " + event.midiPitch());
        }

        final List<DnbEvent> arpEvents = eventsByFrame.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.kind() == VoiceKind.LIQUID_ARP)
                .toList();
        assertFalse(arpEvents.isEmpty(), "Expected LIQUID_ARP events for completed leaf executions");
        for (final DnbEvent event : arpEvents) {
            assertTrue(event.midiPitch() >= 46.0 && event.midiPitch() <= 83.0,
                    () -> "LIQUID_ARP pitch out of lowered register [46..83]: " + event.midiPitch());
        }
    }
}
