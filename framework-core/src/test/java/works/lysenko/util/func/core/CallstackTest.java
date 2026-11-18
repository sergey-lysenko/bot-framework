package works.lysenko.util.func.core;

import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Stopwatch;
import works.lysenko.base.TestProperties;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.type.logr.type.Event;
import works.lysenko.util.data.type.logr.type.Log;
import works.lysenko.util.data.type.logr.type.Section;
import works.lysenko.util.spec.Layout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallstackTest {

    @Test
    @SuppressWarnings("deprecation")
    void testCallstackSidecarGenerationAndDeprecatedCompatibility() throws IOException {
        final var savedProperties = Base.properties;
        final var savedTimer = Base.timer;
        if (null == Base.properties) {
            Base.properties = new TestProperties();
        }
        if (null == Base.timer) {
            Base.timer = new Stopwatch();
        }
        Base.properties.setUserOverride(".logs.stack.trace.references", "true");
        Callstack.clearReferences();
        try {
            final StackTraceElement[] frames = new StackTraceElement[]{
                    new StackTraceElement(CallstackTest.class.getName(), "helperFrame", "CallstackTest.java", 42),
                    new StackTraceElement(Callstack.class.getName(), "getShort", "Callstack.java", 153)
            };

            final String first = Callstack.getShort(frames, true);
            final String second = Callstack.getShort(frames, true);
            final String viaDeprecatedDelegate = Stacktrace.getShort(frames, true);

            assertTrue(first.startsWith("0:["), "First call stack occurrence should define reference index 0, got: " + first);
            assertEquals("[0]", second, "Subsequent occurrence should emit compact reference [0]");
            assertEquals("[0]", viaDeprecatedDelegate, "Deprecated Stacktrace.getShort should share Callstack reference table");

            final Path callstacksFile = Path.of(Layout.Files.name(Layout.Templates.RUN_CALLSTACKS_));
            assertTrue(Files.exists(callstacksFile), "Sidecar .callstacks.properties file should exist on disk");
            assertTrue(callstacksFile.toString().endsWith(".callstacks.properties"),
                    "Sidecar file should use .callstacks.properties extension");

            final String fileContent = Files.readString(callstacksFile);
            assertTrue(fileContent.contains("0=["), "Sidecar file should persist reference index 0");

            final Log log = new Log(1L, 0, 0, "msg", first);
            assertEquals(first, log.callstack());
            assertEquals(first, log.stacktrace(), "Deprecated Log.stacktrace() must delegate to callstack()");

            final Section section = new Section(2L, 0, "Title", second);
            assertEquals(second, section.callstack());
            assertEquals(second, section.stacktrace(), "Deprecated Section.stacktrace() must delegate to callstack()");

            final Event event = new Event(3L, EventType.FAILURE, 0, "failure", second);
            assertEquals(second, event.callstack());
            assertEquals(second, event.stacktrace(), "Deprecated Event.stacktrace() must delegate to callstack()");
        } finally {
            if (null != Base.properties) {
                Base.properties.clearUserOverrides();
            }
            Base.properties = savedProperties;
            Base.timer = savedTimer;
        }
    }
}
