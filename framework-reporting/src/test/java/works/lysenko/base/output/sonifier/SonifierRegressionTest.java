package works.lysenko.base.output.sonifier;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import works.lysenko.base.issues.KnownIssue;
import works.lysenko.base.issues.KnownIssuesStore;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.responses.ExpectedResponsesStore;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.data.type.logr.type.Event;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SonifierRegressionTest {

    private KnownIssuesStore previousKnownStore;
    private KnownIssuesStore previousFixedStore;
    private ExpectedResponsesStore previousExpectedStore;

    @BeforeEach
    void setUp() {
        previousKnownStore = TreeHtml.getKnownIssuesStore();
        previousFixedStore = TreeHtml.getFixedIssuesStore();
        previousExpectedStore = TreeHtml.getExpectedResponsesStore();

        TreeHtml.setKnownIssuesStore(new KnownIssuesStore(List.of()));
        TreeHtml.setFixedIssuesStore(new KnownIssuesStore(List.of()));
        TreeHtml.setExpectedResponsesStore(new ExpectedResponsesStore(List.of()));
    }

    @AfterEach
    void tearDown() {
        TreeHtml.setKnownIssuesStore(previousKnownStore);
        TreeHtml.setFixedIssuesStore(previousFixedStore);
        TreeHtml.setExpectedResponsesStore(previousExpectedStore);
    }

    record SvgCapableSonifier(String name, TreeSonifier sonifier, Function<File, File> snapshotFileResolver) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<SvgCapableSonifier> svgCapableSonifiers() {
        return Stream.of(
                new SvgCapableSonifier("adp", new AdpSonifier(), AdpSonifier::snapshotFile),
                new SvgCapableSonifier("edm", new EdmSonifier(), EdmSonifier::snapshotFile),
                new SvgCapableSonifier("dnb", new DnbSonifier(), DnbSonifier::snapshotFile),
                new SvgCapableSonifier("neon", new NeonSonifier(), NeonSonifier::snapshotFile),
                new SvgCapableSonifier("jmj", new JmjSonifier(), JmjSonifier::snapshotFile),
                new SvgCapableSonifier("carbon", new CarbonSonifier(), CarbonSonifier::snapshotFile),
                new SvgCapableSonifier("orpheus", new OrpheusSonifier(), OrpheusSonifier::snapshotFile)
        );
    }

    @ParameterizedTest(name = "{0} distinguishes passed, warning, regression_warning, failed, and regression from SVG frames")
    @MethodSource("svgCapableSonifiers")
    void svgFallbackDistinguishesBothRegressionTiers(
            final SvgCapableSonifier entry,
            @TempDir final Path tempDir) throws Exception {
        final TreeSonifier sonifier = entry.sonifier();

        final File baseFrame = writeSvgFrame(tempDir.resolve("base_0001.svg"), "passed", "1/2", 1);
        final File passFrame = writeSvgFrame(tempDir.resolve("pass_0002.svg"), "passed", "2/2", 2);
        final File warnFrame = writeSvgFrame(tempDir.resolve("warn_0002.svg"), "warning", "2/2", 2);
        final File regWarnFrame = writeSvgFrame(tempDir.resolve("regwarn_0002.svg"), "regression_warning", "2/2", 2);
        final File failFrame = writeSvgFrame(tempDir.resolve("fail_0002.svg"), "failed", "2/2", 2);
        final File regFrame = writeSvgFrame(tempDir.resolve("reg_0002.svg"), "regression", "2/2", 2);

        final byte[] passWav = renderWav(sonifier, List.of(baseFrame, passFrame), tempDir.resolve("pass.wav"));
        final byte[] warnWav = renderWav(sonifier, List.of(baseFrame, warnFrame), tempDir.resolve("warn.wav"));
        final byte[] regWarnWav = renderWav(sonifier, List.of(baseFrame, regWarnFrame), tempDir.resolve("regwarn.wav"));
        final byte[] failWav = renderWav(sonifier, List.of(baseFrame, failFrame), tempDir.resolve("fail.wav"));
        final byte[] regWav = renderWav(sonifier, List.of(baseFrame, regFrame), tempDir.resolve("reg.wav"));

        assertValidNonSilentWav(passWav, entry.name() + " passed");
        assertValidNonSilentWav(warnWav, entry.name() + " warning");
        assertValidNonSilentWav(regWarnWav, entry.name() + " regression_warning");
        assertValidNonSilentWav(failWav, entry.name() + " failed");
        assertValidNonSilentWav(regWav, entry.name() + " regression");

        assertFalse(Arrays.equals(passWav, regWav),
                entry.name() + ": SVG regression frame must not fall back to passed");
        assertFalse(Arrays.equals(failWav, regWav),
                entry.name() + ": SVG regression frame must sound distinct from regular failure");
        assertFalse(Arrays.equals(passWav, regWarnWav),
                entry.name() + ": SVG regression_warning frame must not fall back to passed");
        assertFalse(Arrays.equals(warnWav, regWarnWav),
                entry.name() + ": SVG regression_warning frame must sound distinct from regular warning");
        assertFalse(Arrays.equals(regWarnWav, regWav),
                entry.name() + ": regression_warning and failure-tier regression must sound distinct");
    }

    @ParameterizedTest(name = "{0} detects tie-breaker escalation F->R and W->M without execution increment")
    @MethodSource("svgCapableSonifiers")
    void tieBreakerEscalationTriggersDistinctAudio(
            final SvgCapableSonifier entry,
            @TempDir final Path tempDir) throws Exception {
        final TreeSonifier sonifier = entry.sonifier();

        // F -> F vs F -> R (same execution count 1/2)
        final File fail1 = writeSvgFrame(tempDir.resolve("f1.svg"), "failed", "1/2", 1);
        final File fail2 = writeSvgFrame(tempDir.resolve("f2.svg"), "failed", "1/2", 2);
        final File reg2 = writeSvgFrame(tempDir.resolve("r2.svg"), "regression", "1/2", 2);

        final byte[] stayFailWav = renderWav(sonifier, List.of(fail1, fail2), tempDir.resolve("stay_fail.wav"));
        final byte[] escalateFailWav = renderWav(sonifier, List.of(fail1, reg2), tempDir.resolve("esc_fail.wav"));
        assertFalse(Arrays.equals(stayFailWav, escalateFailWav),
                entry.name() + ": escalating F -> R must trigger regression audio cue");

        // W -> W vs W -> M (same execution count 1/2)
        final File warn1 = writeSvgFrame(tempDir.resolve("w1.svg"), "warning", "1/2", 1);
        final File warn2 = writeSvgFrame(tempDir.resolve("w2.svg"), "warning", "1/2", 2);
        final File regWarn2 = writeSvgFrame(tempDir.resolve("m2.svg"), "regression_warning", "1/2", 2);

        final byte[] stayWarnWav = renderWav(sonifier, List.of(warn1, warn2), tempDir.resolve("stay_warn.wav"));
        final byte[] escalateWarnWav = renderWav(sonifier, List.of(warn1, regWarn2), tempDir.resolve("esc_warn.wav"));
        assertFalse(Arrays.equals(stayWarnWav, escalateWarnWav),
                entry.name() + ": escalating W -> M must trigger regression-warning audio cue");
    }

    @ParameterizedTest(name = "{0} captures R and M status codes from live fixed-issue layouts")
    @MethodSource("svgCapableSonifiers")
    void liveCaptureRecordsRegressionStatusCodesAndSynthesizesWav(
            final SvgCapableSonifier entry,
            @TempDir final Path tempDir) throws Exception {
        installFixedIssues();
        final TreeSonifier sonifier = entry.sonifier();

        final File f1 = tempDir.resolve("frame_0001.svg").toFile();
        final File f2 = tempDir.resolve("frame_0002.svg").toFile();
        final File f3 = tempDir.resolve("frame_0003.svg").toFile();

        sonifier.capture(f1, buildLayout(null, "Healthy tick", 1), 3);
        sonifier.capture(f2, buildLayout(EventType.WARNING, "FixedWarnSignature hit", 2), 3);
        sonifier.capture(f3, buildLayout(EventType.FAILURE, "FixedCrashSignature hit", 3), 3);

        final File snap1 = entry.snapshotFileResolver().apply(f1);
        final File snap2 = entry.snapshotFileResolver().apply(f2);
        final File snap3 = entry.snapshotFileResolver().apply(f3);

        assertTrue(snap1.isFile() && snap2.isFile() && snap3.isFile(), entry.name() + " snapshots must exist");
        final String content2 = Files.readString(snap2.toPath(), StandardCharsets.UTF_8);
        final String content3 = Files.readString(snap3.toPath(), StandardCharsets.UTF_8);
        assertTrue(content2.contains("\tM"),
                () -> entry.name() + " frame 2 trace must record status 'M', got:\n" + content2);
        assertTrue(content3.contains("\tR"),
                () -> entry.name() + " frame 3 trace must record status 'R', got:\n" + content3);

        final byte[] wav = renderWav(sonifier, List.of(f1, f2, f3), tempDir.resolve("live.wav"));
        assertValidNonSilentWav(wav, entry.name() + " live capture");
    }

    @Test
    @DisplayName("AuroraSonifier records 8-column regression counts and sounds distinct from regular failure/warning")
    void auroraSonifierCapturesAndDistinguishesRegressions(@TempDir final Path tempDir) throws Exception {
        installFixedIssues();
        final AuroraSonifier aurora = new AuroraSonifier();

        // Run A: clean -> warning-tier regression -> failure-tier regression
        final Path regDir = Files.createDirectory(tempDir.resolve("reg"));
        final File r1 = regDir.resolve("frame_0001.svg").toFile();
        final File r2 = regDir.resolve("frame_0002.svg").toFile();
        final File r3 = regDir.resolve("frame_0003.svg").toFile();
        aurora.capture(r1, buildLayout(null, "Healthy tick", 1), 3);
        aurora.capture(r2, buildLayout(EventType.WARNING, "FixedWarnSignature hit", 2), 3);
        aurora.capture(r3, buildLayout(EventType.FAILURE, "FixedCrashSignature hit", 3), 3);

        // Verify 8-column snapshot format: col total active completed failed warning regression regressionWarning
        final String[] cols2 = Files.readString(AuroraSonifier.snapshotFile(r2).toPath(), StandardCharsets.UTF_8)
                .trim().split("\\t");
        assertEquals(8, cols2.length, "Aurora snapshot must have 8 tab-separated columns");
        assertEquals("0", cols2[5], "Warning-tier regression must not double-count in regular warning column");
        assertEquals("1", cols2[7], "Warning-tier regression must increment column 8 (regressionWarning)");

        final String[] cols3 = Files.readString(AuroraSonifier.snapshotFile(r3).toPath(), StandardCharsets.UTF_8)
                .trim().split("\\t");
        assertEquals(8, cols3.length, "Aurora snapshot must have 8 tab-separated columns");
        assertEquals("0", cols3[4], "Failure-tier regression must not double-count in regular failed column");
        assertEquals("1", cols3[6], "Failure-tier regression must increment column 7 (regression)");

        // Run B: clean -> ordinary warning -> ordinary failure (no fixed-issue match)
        final Path ordDir = Files.createDirectory(tempDir.resolve("ord"));
        final File o1 = ordDir.resolve("frame_0001.svg").toFile();
        final File o2 = ordDir.resolve("frame_0002.svg").toFile();
        final File o3 = ordDir.resolve("frame_0003.svg").toFile();
        aurora.capture(o1, buildLayout(null, "Healthy tick", 1), 3);
        aurora.capture(o2, buildLayout(EventType.WARNING, "UnrelatedWarning", 2), 3);
        aurora.capture(o3, buildLayout(EventType.FAILURE, "UnrelatedCrash", 3), 3);

        final byte[] regWav = renderWav(aurora, List.of(r1, r2, r3), tempDir.resolve("aurora_reg.wav"));
        final byte[] ordWav = renderWav(aurora, List.of(o1, o2, o3), tempDir.resolve("aurora_ord.wav"));
        assertValidNonSilentWav(regWav, "aurora regression");
        assertValidNonSilentWav(ordWav, "aurora ordinary");
        assertFalse(Arrays.equals(regWav, ordWav),
                "Aurora regression audio must be distinct from ordinary warning/failure audio");
    }

    @Test
    @DisplayName("AuroraSonifier stays backward-compatible with legacy 6-column snapshot files")
    void auroraSonifierReadsLegacySixColumnSnapshots(@TempDir final Path tempDir) throws Exception {
        final AuroraSonifier aurora = new AuroraSonifier();
        final File f1 = tempDir.resolve("legacy_0001.svg").toFile();
        final File f2 = tempDir.resolve("legacy_0002.svg").toFile();

        final File s1 = AuroraSonifier.snapshotFile(f1);
        final File s2 = AuroraSonifier.snapshotFile(f2);
        Files.createDirectories(s1.toPath().getParent());
        Files.writeString(s1.toPath(), "0\t1\t1\t0\t0\t0\n", StandardCharsets.UTF_8);
        Files.writeString(s2.toPath(), "0\t1\t0\t1\t1\t0\n", StandardCharsets.UTF_8);

        assertTrue(aurora.canSonify(List.of(f1, f2)));
        final byte[] wav = renderWav(aurora, List.of(f1, f2), tempDir.resolve("aurora_legacy.wav"));
        assertValidNonSilentWav(wav, "aurora legacy 6-column");
    }

    private static void installFixedIssues() {
        final KnownIssue fixedCrash = new KnownIssue(
                "CheckoutLeaf", "FIX-500", "Fixed checkout crash",
                "https://jira.example.com/FIX-500", "FixedCrashSignature");
        final KnownIssue fixedWarn = new KnownIssue(
                "CheckoutLeaf", "FIX-200", "Fixed checkout warning",
                "https://jira.example.com/FIX-200", "FixedWarnSignature");
        TreeHtml.setFixedIssuesStore(new KnownIssuesStore(List.of(fixedCrash, fixedWarn)));
    }

    private static TreeLayout buildLayout(final EventType eventType, final String message, final int executions) {
        final _Scenario checkout = new Leaf(Fraction.ONE) {
            @Override public String getName() { return "demo.CheckoutLeaf"; }
            @Override public String getShortName() { return "CheckoutLeaf"; }
            @Override public String getSimpleName() { return "CheckoutLeaf"; }
            @Override public ScenarioType type() { return ScenarioType.LEAF; }
        };
        final Result res = new Result(checkout);
        for (int i = 0; i < executions; i++) {
            res.tick();
        }
        if (null != eventType) {
            res.addEvent(new LogRecord(executions, 100L * executions,
                    new Event(executions, eventType, 0, message, null), checkout, null));
        }
        final TreeMap<String, Result> sorted = new TreeMap<>();
        sorted.put(works.lysenko.util.data.strs.Bind.b(checkout.getShortName(), checkout.type().tag()), res);
        return TreeHtml.computeLayout(List.of(checkout), sorted);
    }

    private static File writeSvgFrame(
            final Path path,
            final String leafCssClass,
            final String badge,
            final int testNumber) throws IOException {
        final String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"600\">\n"
                + "  <g transform=\"translate(0.00, 0.00) scale(1.0000)\">\n"
                + "    <g id=\"scenario_0_Root\" class=\"node-card passed\" transform=\"translate(40, 92)\">\n"
                + "      <title>Root ▷ (Passed)</title><text class=\"tx1\">" + testNumber + " exec</text>\n"
                + "    </g>\n"
                + "    <g id=\"scenario_1_CheckoutLeaf\" class=\"node-card " + leafCssClass + "\" transform=\"translate(310, 92)\">\n"
                + "      <title>CheckoutLeaf ◼</title><text class=\"tx1\">" + badge + "</text>\n"
                + "    </g>\n"
                + "  </g>\n"
                + "  <text class=\"tx3\">Leaves: 1/1</text><text class=\"tx3\">Test #" + testNumber + "</text>\n"
                + "</svg>\n";
        Files.writeString(path, svg, StandardCharsets.UTF_8);
        return path.toFile();
    }

    private static byte[] renderWav(
            final TreeSonifier sonifier,
            final List<File> frames,
            final Path wavPath) throws IOException {
        assertTrue(sonifier.canSonify(frames), sonifier.mode() + " must be able to sonify frames");
        sonifier.writeWav(frames, wavPath.toFile(), 10);
        return Files.readAllBytes(wavPath);
    }

    private static void assertValidNonSilentWav(final byte[] wav, final String label) {
        assertTrue(wav.length > 44, label + ": WAV must contain PCM payload");
        assertEquals("RIFF", new String(wav, 0, 4, StandardCharsets.US_ASCII), label + ": missing RIFF header");
        assertEquals("WAVE", new String(wav, 8, 4, StandardCharsets.US_ASCII), label + ": missing WAVE header");
        boolean audible = false;
        for (int i = 44; i + 1 < wav.length; i += 2) {
            final short sample = (short) ((wav[i] & 0xFF) | ((wav[i + 1] & 0xFF) << 8));
            if (Math.abs(sample) > 200) {
                audible = true;
                break;
            }
        }
        assertTrue(audible, label + ": WAV output must be non-silent");
    }
}
