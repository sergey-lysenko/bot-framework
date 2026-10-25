package works.lysenko.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Stopwatch}.
 */
class StopwatchTest {

    @Test
    void testStopwatchTiming() throws InterruptedException {
        Stopwatch stopwatch = new Stopwatch();
        long start = stopwatch.startedAt();
        assertTrue(start > 0);

        Thread.sleep(10);
        long elapsed = stopwatch.msSinceStart();
        assertTrue(elapsed >= 0);

        long currentTime = stopwatch.now();
        assertTrue(currentTime >= start);
    }
}
