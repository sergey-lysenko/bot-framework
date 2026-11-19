package works.lysenko.base.logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Results;
import works.lysenko.base.Stopwatch;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.util.apis.log._LogsWriter;
import works.lysenko.util.spec.Level;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"removal", "deprecation"})
class ProcessorAsyncTest {

    private Core previousCore;
    private Stopwatch previousTimer;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousTimer = Base.timer;
        Base.timer = new Stopwatch();
        final Core core = (Core) SelectionFixture.allocate(Core.class);
        final Field rf = Core.class.getDeclaredField("results");
        rf.setAccessible(true);
        rf.set(core, new Results());
        Base.core = core;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
        Base.timer = previousTimer;
        Base.exec = null;
    }

    @Test
    void writesLogsAsynchronouslyInStrictOrderAndFlushesOnDemand() {
        final StringWriter logOut = new StringWriter();
        final StringWriter mdOut = new StringWriter();
        final StringWriter telemOut = new StringWriter();
        final AtomicInteger mdFlushes = new AtomicInteger();
        final AtomicReference<String> writerThreadName = new AtomicReference<>();

        final BufferedWriter logWriter = new BufferedWriter(logOut) {
            @Override
            public void write(final String str) throws IOException {
                writerThreadName.compareAndSet(null, Thread.currentThread().getName());
                super.write(str);
            }
        };
        final BufferedWriter mdWriter = new BufferedWriter(mdOut) {
            @Override
            public void flush() throws IOException {
                mdFlushes.incrementAndGet();
                super.flush();
            }
        };
        final BufferedWriter telemWriter = new BufferedWriter(telemOut);

        final _LogsWriter fakeWriter = new _LogsWriter() {
            @Override public BufferedWriter getLogWriter() { return logWriter; }
            @Override public BufferedWriter getMarkdownWriter() { return mdWriter; }
            @Override public BufferedWriter getTelemetryWriter() { return telemWriter; }
            @Override
            public void flushAll() {
                try {
                    logWriter.flush();
                    mdWriter.flush();
                    telemWriter.flush();
                } catch (final IOException ignored) {
                }
            }
        };

        final Processor processor = new Processor(Set.of(), fakeWriter);
        final int total = 60;
        for (int i = 0; i < total; i++) {
            processor.log(Level.plain, "async-msg-" + i, null, (long) (i + 1));
        }
        processor.flush();

        assertEquals("log-writer", writerThreadName.get(), "Log records must be written by dedicated log-writer thread");
        assertNotEquals(Thread.currentThread().getName(), writerThreadName.get());

        final String rawLog = logOut.toString();
        final String rawMd = mdOut.toString();
        int lastLogPos = -1;
        int lastMdPos = -1;
        for (int i = 0; i < total; i++) {
            final String token = "async-msg-" + i;
            final int logPos = rawLog.indexOf(token);
            final int mdPos = rawMd.indexOf(token);
            assertTrue(logPos > lastLogPos, "Log file missing or out-of-order token: " + token);
            assertTrue(mdPos > lastMdPos, "Markdown file missing or out-of-order token: " + token);
            lastLogPos = logPos;
            lastMdPos = mdPos;
        }
        assertTrue(telemOut.toString().lines().count() >= total, "Telemetry lines should be written for all records");
        assertTrue(mdFlushes.get() >= 1, "Markdown writer must flush at least once on drain/flush");
    }

    @Test
    void concurrentLoggingAndFlushingNeverDeadlocks() throws InterruptedException {
        final StringWriter logOut = new StringWriter();
        final StringWriter mdOut = new StringWriter();
        final StringWriter telemOut = new StringWriter();
        final BufferedWriter logWriter = new BufferedWriter(logOut);
        final BufferedWriter mdWriter = new BufferedWriter(mdOut);
        final BufferedWriter telemWriter = new BufferedWriter(telemOut);

        final _LogsWriter fakeWriter = new _LogsWriter() {
            @Override public BufferedWriter getLogWriter() { return logWriter; }
            @Override public BufferedWriter getMarkdownWriter() { return mdWriter; }
            @Override public BufferedWriter getTelemetryWriter() { return telemWriter; }
            @Override
            public void flushAll() {
                try {
                    logWriter.flush();
                    mdWriter.flush();
                    telemWriter.flush();
                } catch (final IOException ignored) {
                }
            }
        };

        final Processor processor = new Processor(Set.of(), fakeWriter);
        final Thread flusher = Thread.ofVirtual().start(() -> {
            for (int i = 0; i < 10; i++) {
                processor.flush();
            }
        });
        for (int i = 0; i < 40; i++) {
            processor.log(Level.debug, "concurrent-msg-" + i, null, (long) (i + 1));
        }
        flusher.join(5000);
        assertTrue(!flusher.isAlive(), "Concurrent flush thread should finish without deadlock");
        processor.flush();

        final String rawLog = logOut.toString();
        for (int i = 0; i < 40; i++) {
            assertTrue(rawLog.contains("concurrent-msg-" + i), "Missing concurrent log message " + i);
        }
    }
}
