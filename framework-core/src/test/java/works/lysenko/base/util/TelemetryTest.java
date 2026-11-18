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
    }

    @AfterEach
    void tearDown() {
        Base.timer = previousTimer;
    }

    @Test
    void testGetNewTelemetry() {
        final Telemetry telemetry = Telemetry.getNewTelemetry(System.currentTimeMillis(), "test comment");
        assertNotNull(telemetry);
        assertNotNull(telemetry.data());
        assertNotNull(telemetry.rendered());
        assertNotNull(telemetry.toString());
        assertEquals(telemetry.rendered(), telemetry.toString());
    }
}
