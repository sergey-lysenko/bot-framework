package works.lysenko.base.eta;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Stopwatch;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.util.spec.Layout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static works.lysenko.util.spec.Layout.Templates.RUN_ETA_DEBUG_;

class EtaDebugTest {

    private Core previousCore;
    private Stopwatch previousTimer;
    private Path etaDebugPath;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousTimer = Base.timer;
        EtaDebug.reset();
        AllLeafCompletions.reset();
        etaDebugPath = Path.of(Layout.Files.name(RUN_ETA_DEBUG_));
        Files.deleteIfExists(etaDebugPath);
    }

    @AfterEach
    void tearDown() throws Exception {
        Base.core = previousCore;
        Base.timer = previousTimer;
        EtaDebug.reset();
        AllLeafCompletions.reset();
        if (null != etaDebugPath) {
            Files.deleteIfExists(etaDebugPath);
        }
    }

    @Test
    void startRecordAndEndCycleWritesEtaDebugSidecarEntry() throws Exception {
        final Core core = (Core) SelectionFixture.allocate(Core.class);
        Base.core = core;

        final long[] clockMs = {10_000L};
        Base.timer = new Stopwatch() {
            @Override
            public long msSinceStart() {
                return clockMs[0];
            }
        };

        etaDebugPath = Path.of(Layout.Files.name(RUN_ETA_DEBUG_));
        Files.deleteIfExists(etaDebugPath);

        EtaDebug.startCycle("SampleLeaf");
        EtaDebug.recordSample(90_000L);
        EtaDebug.recordSample(70_000L);
        clockMs[0] = 15_000L;
        EtaDebug.endCycle("SampleLeaf");

        assertTrue(Files.exists(etaDebugPath), "ETA debug sidecar file should be created");
        final List<String> lines = Files.readAllLines(etaDebugPath);
        assertEquals(1, lines.size());
        assertEquals("[ETA_DEBUG] 0 5000 80000 10000 80000 SampleLeaf", lines.get(0));
    }
}
