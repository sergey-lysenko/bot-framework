package works.lysenko.base.logger.processor;

import works.lysenko.util.apis.log._LogsWriter;
import works.lysenko.util.spec.Layout;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.___.LOG;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.OPEN;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.W.WRITING;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_;
import static works.lysenko.util.spec.Layout.Templates.TELEMETRY_;

@SuppressWarnings({"NestedMethodCall", "MissingJavadoc"})
public class Writer implements _LogsWriter {

    private final BufferedWriter logWriter;
    private final BufferedWriter telemetryWriter;


    public Writer() {

        logWriter = createFileWriter(RUN_LOG_);
        telemetryWriter = createFileWriter(TELEMETRY_);
    }

    private static BufferedWriter createFileWriter(final String fileName) {

        try {
            return new BufferedWriter(new FileWriter(Layout.Files.name(fileName), StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new IllegalStateException(b(c(UNABLE), TO, OPEN, LOG, FILE, q(fileName), FOR, WRITING), e);
        }
    }

    @Override
    public final BufferedWriter getLogWriter() {

        return logWriter;
    }

    @Override
    public final BufferedWriter getTelemetryWriter() {

        return telemetryWriter;
    }
}
