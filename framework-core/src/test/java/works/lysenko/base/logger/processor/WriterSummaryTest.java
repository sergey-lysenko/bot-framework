package works.lysenko.base.logger.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Stopwatch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_MD_;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_MD_TEST_;

/**
 * Unit tests verifying that Writer routes per-test and postflight logs cleanly without leaking file handles.
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
        try {
            // Preflight / Test 0
            BufferedWriter w0 = writer.getMarkdownWriter(0);
            assertNotNull(w0);

            // Test 1
            BufferedWriter w1 = writer.getMarkdownWriter(1);
            assertNotNull(w1);
            assertNotSame(w0, w1);
            assertThrows(IOException.class, () -> w0.write("after-close"),
                    "Previous per-test writer should be closed when advancing to a new test");

            // Postflight (testNumber = 0 after maxTestNumber > 0)
            BufferedWriter wPostflight = writer.getMarkdownWriter(0);
            assertNotNull(wPostflight);
            assertNotSame(w0, wPostflight);
            assertNotSame(w1, wPostflight);
            assertThrows(IOException.class, () -> w1.write("after-close"),
                    "Test #1 writer should be closed when switching to postflight");

            // Verify that file for postflight exists
            String postflightPath = works.lysenko.util.spec.Layout.Files.name(RUN_LOG_MD_TEST_, "postflight");
            File postflightFile = new File(postflightPath);
            assertTrue(postflightFile.exists(), "md/postflight.md should exist for postflight logs");
        } finally {
            writer.closeAll();
        }
    }

    @Test
    void testReopeningEarlierTestAppendsWithoutDuplicatingHeaderOrIndex() throws IOException {
        Writer writer = new Writer();
        try {
            BufferedWriter w1 = writer.getMarkdownWriter(1);
            w1.write("first-entry\n");
            w1.flush();

            BufferedWriter w2 = writer.getMarkdownWriter(2);
            w2.write("test-2-entry\n");
            w2.flush();

            // Re-open earlier test #1
            BufferedWriter w1Reopened = writer.getMarkdownWriter(1);
            assertNotSame(w1, w1Reopened);
            w1Reopened.write("late-entry\n");
            w1Reopened.flush();
        } finally {
            writer.closeAll();
        }

        File test1File = new File(works.lysenko.util.spec.Layout.Files.name(RUN_LOG_MD_TEST_, "1"));
        String test1Content = Files.readString(test1File.toPath());
        assertTrue(test1Content.contains("first-entry"));
        assertTrue(test1Content.contains("late-entry"));
        assertEquals(1, test1Content.split("# Test Execution Log - Test #1", -1).length - 1,
                "Header should not be duplicated when reopening an earlier test file");

        File indexFile = new File(works.lysenko.util.spec.Layout.Files.name(RUN_LOG_MD_));
        String indexContent = Files.readString(indexFile.toPath());
        assertEquals(1, indexContent.split("\\(md/1\\.md\\)", -1).length - 1,
                "Index link should not be duplicated when reopening an earlier test file");
    }
}
