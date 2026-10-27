package works.lysenko.base.logger.renderer;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Ansi;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.logr.type.Event;
import works.lysenko.util.data.type.logr.type.LineFeed;
import works.lysenko.util.data.type.logr.type.Log;
import works.lysenko.util.data.type.logr.type.Section;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownLogRendererTest {

    @Test
    void testLineFeedDefersTableHeaderUntilNextRow() {
        final MarkdownLogRenderer renderer = new MarkdownLogRenderer();
        final LogRecord lineFeed = new LogRecord(1, 1000L, new LineFeed(1L));
        assertEquals("", renderer.render(lineFeed, 1, 0, null));

        final LogRecord row = new LogRecord(1, 1001L, new Log(2L, 0, 0, "First line"));
        final String result = renderer.render(row, 1, 0, null);

        assertTrue(result.contains("| Log Message | Stack Trace | Duration (ms) |"));
        assertTrue(result.contains("| First line |  |  |"));
    }

    @Test
    void testRenderSectionHeaderDeferredUntilRow() {
        final MarkdownLogRenderer renderer = new MarkdownLogRenderer();
        final String headerMsg = "= Preflight =";
        final String stacktrace = "29:[Preflight.run:20]";
        final LogRecord sectionRecord = new LogRecord(1, 1000L, new Log(1L, 0, 0, headerMsg, stacktrace));
        assertEquals("", renderer.render(sectionRecord, 1, 0, null));

        final LogRecord stepRecord = new LogRecord(1, 1001L, new Log(2L, 0, 0, "Step 1"));
        final String result = renderer.render(stepRecord, 1, 0, null);

        assertTrue(result.contains("### Preflight `29:[Preflight.run:20]`"));
        assertTrue(result.contains("| Log Message | Stack Trace | Duration (ms) |"));
        assertTrue(result.contains("| Step 1 |  |  |"));
    }

    @Test
    void testRenderScenarioNodeHeader() {
        final MarkdownLogRenderer renderer = new MarkdownLogRenderer();
        final String nodeMsg = "▷ signIn.login.aiUsageReport.Export : (1) → [0] 1";
        final LogRecord sectionRecord = new LogRecord(1, 1000L, new Log(1L, 0, 0, nodeMsg));
        assertEquals("", renderer.render(sectionRecord, 1, 0, null));

        final LogRecord stepRecord = new LogRecord(1, 1001L, new Log(2L, 0, 0, "Opening export dialog"));
        final String result = renderer.render(stepRecord, 1, 0, null);

        assertTrue(result.contains("### ▷ signIn.login.aiUsageReport.Export : (1) → [0] 1"));
        assertTrue(result.contains("| Log Message | Stack Trace | Duration (ms) |"));
        assertTrue(result.contains("| Opening export dialog |  |  |"));
    }

    @Test
    void testRenderIndentedLogMessageWithNbsp() {
        final MarkdownLogRenderer renderer = new MarkdownLogRenderer();
        final Log log = new Log(1L, 3, 0, "Finding 'Email'");
        final LogRecord record = new LogRecord(1, 1000L, log);
        final String result = renderer.render(record, 1, 0, null);
        assertTrue(result.contains("| &nbsp;&nbsp;&nbsp;Finding 'Email' |  |  |"));
    }

    @Test
    void testEmptySectionDoesNotEmitTable() {
        final MarkdownLogRenderer renderer = new MarkdownLogRenderer();
        final Section sec1 = new Section(1L, 0, "Empty Section 1", "10:[Sec1]");
        final Section sec2 = new Section(2L, 0, "Populated Section 2", "20:[Sec2]");

        assertEquals("", renderer.render(new LogRecord(1, 1000L, sec1), 1, 0, null));
        assertEquals("", renderer.render(new LogRecord(1, 1001L, sec2), 1, 0, null));

        final LogRecord row = new LogRecord(1, 1002L, new Log(3L, 0, 0, "Step in Sec2"));
        final String result = renderer.render(row, 1, 0, null);

        assertTrue(!result.contains("Empty Section 1"));
        assertTrue(result.contains("### Populated Section 2 `20:[Sec2]`"));
        assertTrue(result.contains("| Step in Sec2 |  |  |"));
    }

    @Test
    void testRenderFractionDeUnicoded() {
        final MarkdownLogRenderer renderer = new MarkdownLogRenderer();
        final String msg = "Current UI resolution is 1920x993, scale is ⁶⁴⁰⁄₃₃₁, native is ⁹⁄₁₉";
        final LogRecord record = new LogRecord(1, 1000L, new Log(1L, 0, 0, msg));
        final String result = renderer.render(record, 1, 0, null);
        assertTrue(result.contains("| Current UI resolution is 1920x993, scale is 640/331, native is 9/19 |  |  |"));
    }

    @Test
    void testRenderEventStripsAnsiCodesFromMessageAndStacktrace() {
        final MarkdownLogRenderer renderer = new MarkdownLogRenderer();
        final String ansiMessage = Ansi.rb("Error occurred");
        final String ansiStack = Ansi.gray("at com.example.Test.run(Test.java:10)");
        final Event event = new Event(1L, EventType.FAILURE, 1, ansiMessage, ansiStack);
        final LogRecord record = new LogRecord(1, 1000L, event);
        final String result = renderer.render(record, 1, 0, null);
        assertTrue(result.contains("| &nbsp;[FAILURE] Error occurred | `at com.example.Test.run(Test.java:10)` |  |"));
    }
}
