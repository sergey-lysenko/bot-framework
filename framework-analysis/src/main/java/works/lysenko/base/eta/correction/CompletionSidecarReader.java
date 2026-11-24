package works.lysenko.base.eta.correction;

import works.lysenko.base.eta.model.TestSample;
import works.lysenko.util.spec.Layout;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fallback reader extracting {@link TestSample} observations from {@code .exhaustive-completions.log} sidecar files.
 */
public record CompletionSidecarReader() {

    private static final Pattern COMPLETION_LINE_RE = Pattern.compile(
            "^\\[EXHAUSTIVE_COMPLETION]\\s+(\\d+)\\s+(\\d+)(?:\\s+(\\d+(?:\\.\\d+)?))?(?:\\s+(\\d+))?(?:\\s+(\\d+))?(?:\\s+(.+))?$");

    /**
     * Reads per-cycle {@link TestSample} observations from the current run's exhaustive completions sidecar file.
     *
     * @return list of parsed {@link TestSample} entries, or empty list if absent/unreadable
     */
    public static List<TestSample> readCurrentRunSamples() {
        try {
            final String sidecarPath = Layout.Files.name(Layout.Templates.RUN_EXHAUSTIVE_COMPLETIONS_);
            final File file = new File(sidecarPath);
            if (!file.isFile()) return List.of();
            return parseLines(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (final Exception _) {
            return List.of();
        }
    }

    /**
     * Parses {@link TestSample} observations from raw {@code [EXHAUSTIVE_COMPLETION]} sidecar lines.
     * Supports modern lines ({@code testNum elapsedMs patiencePct etaMs goalIndex leaf}) as well as legacy formats.
     *
     * @param lines raw sidecar log lines
     * @return parsed list of valid {@link TestSample} entries
     */
    public static List<TestSample> parseLines(final List<String> lines) {
        if (null == lines || lines.isEmpty()) return List.of();

        final List<TestSample> samples = new ArrayList<>();
        for (final String raw : lines) {
            if (null == raw) continue;
            final Matcher m = COMPLETION_LINE_RE.matcher(raw.trim());
            if (!m.matches()) continue;

            try {
                final int testNum = Integer.parseInt(m.group(1));
                final long passedMs = Long.parseLong(m.group(2));
                final String g3 = m.group(3);
                final String g4 = m.group(4);
                final String g5 = m.group(5);

                final long etaMs;
                if (null != g5 && !g5.isEmpty()) {
                    // 3 numeric columns after passedMs: patiencePercentage, etaMs, goalIndex
                    etaMs = Long.parseLong(g4);
                } else if (null != g4 && !g4.isEmpty()) {
                    // 2 numeric columns after passedMs: etaMs, goalIndex
                    etaMs = Math.round(Double.parseDouble(g3));
                } else if (null != g3 && !g3.isEmpty()) {
                    // 1 numeric column after passedMs: etaMs
                    etaMs = Math.round(Double.parseDouble(g3));
                } else {
                    continue;
                }

                if (passedMs > 0L && etaMs >= 0L) {
                    samples.add(new TestSample(testNum, passedMs, etaMs));
                }
            } catch (final NumberFormatException _) {
            }
        }
        return samples;
    }
}
