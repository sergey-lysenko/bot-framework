package works.lysenko.base.logger.processor;

import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_MD_TEST_;

/**
 * Unit tests verifying that Writer places postflight summary logs into md/summary.md.
 */
class WriterSummaryTest {

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

        // Postflight / Summary (testNumber = 0 after maxTestNumber > 0)
        BufferedWriter wSummary = writer.getMarkdownWriter(0);
        assertNotNull(wSummary);
        assertNotSame(w0, wSummary);
        assertNotSame(w1, wSummary);

        // Verify that file for summary exists
        String summaryPath = works.lysenko.util.spec.Layout.Files.name(RUN_LOG_MD_TEST_, "summary");
        File summaryFile = new File(summaryPath);
        assertTrue(summaryFile.exists(), "md/summary.md should exist for postflight summary logs");
    }
}
