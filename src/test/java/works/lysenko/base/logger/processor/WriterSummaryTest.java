package works.lysenko.base.logger.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Stopwatch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_MD_TEST_;

/**
 * Unit tests verifying that Writer places postflight logs into md/postflight.md.
 */
class WriterSummaryTest {

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
    void testSummaryRouting() throws IOException {
        Writer writer = new Writer();

        // Preflight / Test 0
        BufferedWriter w0 = writer.getMarkdownWriter(0);
        assertNotNull(w0);

        // Test 1
        BufferedWriter w1 = writer.getMarkdownWriter(1);
        assertNotNull(w1);
        assertNotSame(w0, w1);

        // Postflight (testNumber = 0 after maxTestNumber > 0)
        BufferedWriter wPostflight = writer.getMarkdownWriter(0);
        assertNotNull(wPostflight);
        assertNotSame(w0, wPostflight);
        assertNotSame(w1, wPostflight);

        // Verify that file for postflight exists
        String postflightPath = works.lysenko.util.spec.Layout.Files.name(RUN_LOG_MD_TEST_, "postflight");
        File postflightFile = new File(postflightPath);
        assertTrue(postflightFile.exists(), "md/postflight.md should exist for postflight logs");
    }
}
