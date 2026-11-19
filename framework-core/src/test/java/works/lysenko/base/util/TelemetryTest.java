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
    void testIntervalCpuRespondsToBusyVsIdleWindows() throws InterruptedException {
        Telemetry.getNewTelemetry(0L, "baseline");

        // Busy-burn 1 thread for ~300 ms (> 250 ms minimum window)
        final long burnUntil = System.nanoTime() + 300_000_000L;
        double sink = 0.0;
        while (System.nanoTime() < burnUntil) {
            sink += Math.sqrt(burnUntil - System.nanoTime());
        }
        assertTrue(Double.isFinite(sink));
        final Telemetry afterBusy = Telemetry.getNewTelemetry(300L, "after busy");

        // Idle sleep for ~300 ms
        Thread.sleep(300L);
        final Telemetry afterIdle = Telemetry.getNewTelemetry(600L, "after idle");

        assertTrue(afterBusy.data().spent() > 0.0,
                "Expected positive interval CPU during busy burn, got " + afterBusy.data().spent());
        assertTrue(afterBusy.data().spent() > afterIdle.data().spent(),
                "Expected busy interval (" + afterBusy.data().spent() + "%) > idle interval ("
                        + afterIdle.data().spent() + "%)");
    }
}
