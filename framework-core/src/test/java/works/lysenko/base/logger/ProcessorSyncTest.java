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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"removal", "deprecation"})
class ProcessorSyncTest {

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
    void writesLogsSynchronouslyOnCallingThreadInStrictOrderAndFlushesImmediately() {
        final StringWriter logOut = new StringWriter();
        final StringWriter mdOut = new StringWriter();
        final StringWriter telemOut = new StringWriter();
        final AtomicInteger logFlushes = new AtomicInteger();
        final AtomicInteger mdFlushes = new AtomicInteger();
        final AtomicReference<String> writerThreadName = new AtomicReference<>();

        final BufferedWriter logWriter = new BufferedWriter(logOut) {
            @Override
            public void write(final String str) throws IOException {
                writerThreadName.compareAndSet(null, Thread.currentThread().getName());
                super.write(str);
            }

            @Override
            public void flush() throws IOException {
                logFlushes.incrementAndGet();
                super.flush();
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
            processor.log(Level.plain, "sync-msg-" + i, null, (long) (i + 1));
        }
        processor.flush();

        assertEquals(Thread.currentThread().getName(), writerThreadName.get(),
                "Log records must be written synchronously on the calling thread");

        final String rawLog = logOut.toString();
        final String rawMd = mdOut.toString();
        int lastLogPos = -1;
        int lastMdPos = -1;
        for (int i = 0; i < total; i++) {
            final String token = "sync-msg-" + i;
            final int logPos = rawLog.indexOf(token);
            final int mdPos = rawMd.indexOf(token);
            assertTrue(logPos > lastLogPos, "Log file missing or out-of-order token: " + token);
            assertTrue(mdPos > lastMdPos, "Markdown file missing or out-of-order token: " + token);
            lastLogPos = logPos;
            lastMdPos = mdPos;
        }
        assertTrue(telemOut.toString().lines().count() >= total, "Telemetry lines should be written for all records");
        assertTrue(logFlushes.get() >= total, "Log writer must flush immediately on each written record");
        assertTrue(mdFlushes.get() >= total, "Markdown writer must flush immediately on each written record");
    }
}
