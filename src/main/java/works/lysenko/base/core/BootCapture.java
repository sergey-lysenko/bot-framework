package works.lysenko.base.core;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Captures console output during the bot boot sequence (before the logger file is created)
 * so it can be prepended to the run log and included in the Booting section of the HTML visualizer.
 */
public final class BootCapture {

    private static final ByteArrayOutputStream BUFFER = new ByteArrayOutputStream();
    private static PrintStream originalOut = null;
    private static boolean capturing = false;

    private BootCapture() {}

    public static synchronized void start() {
        if (capturing) return;
        capturing = true;
        originalOut = System.out;
        final OutputStream tee = new OutputStream() {
            @Override
            public void write(final int b) {
                originalOut.write(b);
                BUFFER.write(b);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) {
                originalOut.write(b, off, len);
                BUFFER.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                originalOut.flush();
                BUFFER.flush();
            }
        };
        System.setOut(new PrintStream(tee, true, StandardCharsets.UTF_8));
    }

    public static synchronized void flushTo(final BufferedWriter writer) {
        if (!capturing) return;
        capturing = false;
        if (null != originalOut) {
            System.setOut(originalOut);
        }
        if (null != writer) {
            try {
                final String captured = BUFFER.toString(StandardCharsets.UTF_8);
                if (!captured.isEmpty()) {
                    writer.write(captured);
                    if (!captured.endsWith(System.lineSeparator())) {
                        writer.write(System.lineSeparator());
                    }
                    writer.flush();
                }
            } catch (final IOException ignored) {
            }
        }
    }
}
