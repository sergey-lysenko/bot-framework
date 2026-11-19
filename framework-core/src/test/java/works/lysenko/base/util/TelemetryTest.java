package works.lysenko.base.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Stopwatch;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryTest {

    private Stopwatch previousTimer;

    @BeforeEach
    void setUp() {
        previousTimer = Base.timer;
        Base.timer = new Stopwatch();
        Telemetry.resetCpuSampler();
    }

    @AfterEach
    void tearDown() {
        Base.timer = previousTimer;
        Telemetry.resetCpuSampler();
    }

    @Test
    void testGetNewTelemetry() {
        final Telemetry telemetry = Telemetry.getNewTelemetry(System.currentTimeMillis(), "test comment");
        assertNotNull(telemetry);
        assertNotNull(telemetry.data());
        assertNotNull(telemetry.rendered());
        assertNotNull(telemetry.toString());
        assertEquals(telemetry.rendered(), telemetry.toString());
        assertTrue(telemetry.data().spent() >= 0.0 && telemetry.data().spent() <= 100.0);
    }

    @Test
    void testComputeIntervalCpuPercentNormalizesAndClamps() {
        // 1 core busy out of 8 cores for 500 ms -> 12.5%
        assertEquals(12.5, Telemetry.computeIntervalCpuPercent(500_000_000L, 500_000_000L, 8));
        // All 8 cores busy -> 100.0%
        assertEquals(100.0, Telemetry.computeIntervalCpuPercent(4_000_000_000L, 500_000_000L, 8));
        // Over 100% due to clock skew clamps to 100.0%
        assertEquals(100.0, Telemetry.computeIntervalCpuPercent(5_000_000_000L, 500_000_000L, 8));
        // Tiny non-zero usage rounds to 2 decimals without scientific notation
        assertEquals(0.03, Telemetry.computeIntervalCpuPercent(2_000_000L, 1_000_000_000L, 8));
        // Zero / negative / invalid inputs return 0.0
        assertEquals(0.0, Telemetry.computeIntervalCpuPercent(0L, 500_000_000L, 8));
        assertEquals(0.0, Telemetry.computeIntervalCpuPercent(-10L, 500_000_000L, 8));
        assertEquals(0.0, Telemetry.computeIntervalCpuPercent(100L, 0L, 8));
        assertEquals(0.0, Telemetry.computeIntervalCpuPercent(100L, 500_000_000L, 0));
    }

    @Test
    void testIntervalWindowHoldsDuringBurstAndDropsWhenIdle() {
        final int cores = 8;
        // Baseline at t = 0
        assertEquals(0.0, Telemetry.sampleIntervalCpuPercent(0L, 1_000_000_000L, cores));

        // 50 ms later (< 250 ms minimum window) -> holds previous sample
        assertEquals(0.0, Telemetry.sampleIntervalCpuPercent(50_000_000L, 1_200_000_000L, cores));

        // 500 ms later: 2 cores worth of CPU consumed -> 25.0%
        assertEquals(25.0, Telemetry.sampleIntervalCpuPercent(500_000_000L, 2_000_000_000L, cores));

        // 600 ms (100 ms into next window < 250 ms) -> holds 25.0%
        assertEquals(25.0, Telemetry.sampleIntervalCpuPercent(600_000_000L, 2_005_000_000L, cores));

        // 1500 ms (1000 ms after last sample): almost idle (40 ms CPU across 8 cores = 0.5%)
        assertEquals(0.5, Telemetry.sampleIntervalCpuPercent(1_500_000_000L, 2_040_000_000L, cores));
    }

    @Test
    void testRealProcessCpuSampleIsPositiveUnderLoad() {
        Telemetry.getNewTelemetry(0L, "baseline");

        // Busy-burn 1 thread for ~300 ms (> 250 ms minimum window)
        final long burnUntil = System.nanoTime() + 300_000_000L;
        double sink = 0.0;
        long iter = 1L;
        while (System.nanoTime() < burnUntil) {
            sink += Math.sqrt(iter++);
        }
        assertTrue(Double.isFinite(sink) && sink > 0.0);
        final Telemetry afterBusy = Telemetry.getNewTelemetry(300L, "after busy");

        assertTrue(afterBusy.data().spent() > 0.0 && afterBusy.data().spent() <= 100.0,
                "Expected interval CPU in (0, 100] during busy burn, got " + afterBusy.data().spent());
    }
}
