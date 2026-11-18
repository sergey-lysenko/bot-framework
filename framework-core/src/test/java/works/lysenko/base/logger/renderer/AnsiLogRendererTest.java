package works.lysenko.base.logger.renderer;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.logr.type.Event;
import works.lysenko.util.data.type.logr.type.LineFeed;
import works.lysenko.util.data.type.logr.type.Log;
import works.lysenko.util.data.type.logr.type.Section;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnsiLogRendererTest {

    @Test
    void testRenderWithNegativeSpanLengthDoesNotThrow() {
        final AnsiLogRenderer renderer = new AnsiLogRenderer();
        final Log logWithSpan = new Log(1L, 0, 100, "Test message");
        final LogRecord record = new LogRecord(1, 1000L, logWithSpan);

        assertDoesNotThrow(() -> {
            final String result = renderer.render(record, 1, -1, null);
            assertNotNull(result);
            assertTrue(result.contains("Test message"));
        });
    }

    @Test
    void testRenderWithSpanLengthSmallerThanStringedSpan() {
        final AnsiLogRenderer renderer = new AnsiLogRenderer();
        final Log logWithSpan = new Log(1L, 0, 0, "Long span message");
        final LogRecord record = new LogRecord(1, 1000L, logWithSpan);
        record.setSpan(1000L + 12345);

        assertDoesNotThrow(() -> {
            final String result = renderer.render(record, 1, 2, null);
            assertNotNull(result);
            final String cleanResult = works.lysenko.util.data.enums.Ansi.removeANSICodes(result);
            assertTrue(cleanResult.contains("12345"), "Clean result should contain 12345: " + cleanResult);
        });
    }

    @Test
    void testRenderWithSpanMinusOne() {
        final AnsiLogRenderer renderer = new AnsiLogRenderer();
        final Log logNoSpan = new Log(1L, 0, -1, "No span message");
        final LogRecord record = new LogRecord(1, 1000L, logNoSpan);

        assertDoesNotThrow(() -> {
            final String result = renderer.render(record, 1, -1, null);
            assertNotNull(result);
            assertTrue(result.contains("No span message"));
        });
    }

    @Test
    void testRenderEventAndSection() {
        final AnsiLogRenderer renderer = new AnsiLogRenderer();
        final Event event = new Event(1L, EventType.WARNING, 0, "Event msg", "Stacktrace");
        final LogRecord eventRecord = new LogRecord(1, 1000L, event);

        final String eventResult = renderer.render(eventRecord, 1, 3, null);
        assertNotNull(eventResult);
        assertTrue(eventResult.contains("Event msg"));

        final Section section = new Section(2L, 0, "Section Title", "Section Stacktrace");
        final LogRecord sectionRecord = new LogRecord(1, 1000L, section);

        final String sectionResult = renderer.render(sectionRecord, 1, 3, null);
        assertNotNull(sectionResult);
        assertTrue(sectionResult.contains("Section Title"));

        final LineFeed lineFeed = new LineFeed(3L);
        final LogRecord lineFeedRecord = new LogRecord(1, 1000L, lineFeed);

        final String lineFeedResult = renderer.render(lineFeedRecord, 1, 3, null);
        assertNotNull(lineFeedResult);
    }
}
