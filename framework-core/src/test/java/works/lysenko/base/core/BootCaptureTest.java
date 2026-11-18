package works.lysenko.base.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BootCapture}.
 */
class BootCaptureTest {

    @AfterEach
    void tearDown() {
        BootCapture.stop();
    }

    @Test
    void testStartAndStop() {
        BootCapture.start();
        System.out.print("Bootcapture test output");

        StringWriter stringWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);

        BootCapture.attach(bufferedWriter);
        BootCapture.stop();

        assertTrue(stringWriter.toString().contains("Bootcapture test output"));
    }
}
