package works.lysenko.base.core;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Captures console output during the bot boot sequence
 * so it can be written to the run log and included in the Booting section of the HTML report.
 */
public final class BootCapture {

    private static final ByteArrayOutputStream BUFFER = new ByteArrayOutputStream();
    private static PrintStream originalOut = null;
    private static BufferedWriter attachedWriter = null;
    private static boolean capturing = false;

    private BootCapture() {}

    /**
     * Starts capturing standard output.
     */
    public static synchronized void start() {
        if (capturing) return;
        capturing = true;
        BUFFER.reset();
        originalOut = System.out;
        final OutputStream tee = new OutputStream() {
            @Override
            public void write(final int b) {
                if (null != originalOut) originalOut.write(b);
                if (null != attachedWriter) {
                    try {
                        attachedWriter.write(b);
                    } catch (final IOException ignored) {
                    }
                } else {
                    BUFFER.write(b);
                }
            }

            @Override
            public void write(final byte[] b, final int off, final int len) {
                if (null != originalOut) originalOut.write(b, off, len);
                if (null != attachedWriter) {
                    try {
                        attachedWriter.write(new String(b, off, len, StandardCharsets.UTF_8));
                    } catch (final IOException ignored) {
                    }
                } else {
                    BUFFER.write(b, off, len);
                }
            }

            @Override
            public void flush() throws IOException {
                if (null != originalOut) originalOut.flush();
                if (null != attachedWriter) {
                    try {
                        attachedWriter.flush();
                    } catch (final IOException ignored) {
                    }
                } else {
                    BUFFER.flush();
                }
            }
        };
        System.setOut(new PrintStream(tee, true, StandardCharsets.UTF_8));
    }

    /**
     * Attaches a BufferedWriter to receive captured output stream content.
     *
     * @param writer the writer to attach
     */
    public static synchronized void attach(final BufferedWriter writer) {
        if (!capturing) return;
        if (null != writer) {
            try {
                final String captured = BUFFER.toString(StandardCharsets.UTF_8);
                if (!captured.isEmpty()) {
                    writer.write(captured);
                    writer.flush();
                }
            } catch (final IOException ignored) {
            }
            attachedWriter = writer;
        }
    }

    /**
     * Flushes captured content to the provided writer.
     *
     * @param writer the writer to flush to
     */
    public static synchronized void flushTo(final BufferedWriter writer) {
        attach(writer);
    }

    /**
     * Stops capturing output and restores original System.out.
     */
    public static synchronized void stop() {
        if (!capturing) return;
        capturing = false;
        attachedWriter = null;
        if (null != originalOut) {
            System.setOut(originalOut);
            originalOut = null;
        }
    }
}
