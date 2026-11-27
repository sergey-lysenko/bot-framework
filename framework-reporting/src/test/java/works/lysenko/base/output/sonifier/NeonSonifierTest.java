package works.lysenko.base.output.sonifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.sonifier.NeonSynth.NeonEvent;
import works.lysenko.base.output.sonifier.NeonSynth.VoiceKind;
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

class NeonSonifierTest {

    @Test
    void resolveNeonPitchStaysInLoweredOctaveRegisterAcrossTreeGeometry() {
        final int[] bassRoots = {37, 38, 40, 42, 43};
        final int[][] chords = {
                {0, 3, 7, 10, 14},
                {0, 4, 7, 11, 14},
                {0, 4, 7, 9, 14},
                {0, 3, 7, 10, 12},
                {0, 3, 7, 10, 13}
        };

        for (final int bassRoot : bassRoots) {
            for (final int[] chord : chords) {
                for (int col = 0; col <= 8; col++) {
                    for (int rowTenths = 0; rowTenths <= 120; rowTenths += 5) {
                        final double rowRatio = 1.0 - Math.min(1.0, rowTenths / 120.0);
                        final NeonSonifier.NodeState node = new NeonSonifier.NodeState(
                                "root/n_" + col + "_" + rowTenths, col, rowTenths, 'L', 1, 2, 'P', false);
                        final double pitch = NeonSonifier.resolveNeonPitch(bassRoot, chord, node, rowRatio);
                        assertTrue(pitch >= 49.0 && pitch <= 73.0,
                                () -> "Expected lowered synthwave register MIDI [49..73], got " + pitch);
                    }
                }
            }
        }
    }

    @Test
    void mainExecutionEventsUseLoweredOctaveRegister(@TempDir final Path tempDir) throws Exception {
        final NeonSonifier neon = new NeonSonifier();
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

        neon.capture(frame1, new TreeHtml.TreeLayout(new ArrayList<>(List.of(root1, leafPartial1, leafDone1)), List.of()), 3);
        neon.capture(frame2, new TreeHtml.TreeLayout(new ArrayList<>(List.of(root2, leafPartial2, leafDone2)), List.of()), 3);

        final Map<Integer, List<NeonEvent>> eventsByFrame =
                NeonSonifier.composeNeonEvents(List.of(frame1, frame2), NeonSynth.SAMPLE_RATE / 10);

        final List<NeonEvent> pluckEvents = eventsByFrame.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.kind() == VoiceKind.ARP_PLUCK)
                .toList();
        assertFalse(pluckEvents.isEmpty(), "Expected ARP_PLUCK events for branch executions");
        for (final NeonEvent event : pluckEvents) {
            assertTrue(event.midiPitch() >= 37.0 && event.midiPitch() <= 61.0,
                    () -> "ARP_PLUCK pitch out of lowered register [37..61]: " + event.midiPitch());
        }

        final List<NeonEvent> laserEvents = eventsByFrame.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.kind() == VoiceKind.LASER_LEAD)
                .toList();
        assertFalse(laserEvents.isEmpty(), "Expected LASER_LEAD events for partial leaf executions");
        for (final NeonEvent event : laserEvents) {
            assertTrue(event.midiPitch() >= 49.0 && event.midiPitch() <= 73.0,
                    () -> "LASER_LEAD pitch out of lowered register [49..73]: " + event.midiPitch());
        }

        final List<NeonEvent> shimmerEvents = eventsByFrame.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.kind() == VoiceKind.NEON_SHIMMER)
                .toList();
        assertFalse(shimmerEvents.isEmpty(), "Expected NEON_SHIMMER events for completed leaf executions");
        for (final NeonEvent event : shimmerEvents) {
            assertTrue(event.midiPitch() >= 49.0 && event.midiPitch() <= 85.0,
                    () -> "NEON_SHIMMER pitch out of lowered register [49..85]: " + event.midiPitch());
        }
    }
}
