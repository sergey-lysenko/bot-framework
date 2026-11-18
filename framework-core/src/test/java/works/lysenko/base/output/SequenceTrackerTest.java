package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import works.lysenko.tree.base.Leaf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SequenceTrackerTest {

    private static final String SEQUENCE = "SEQUENCE";
    private static final String FILE_PROPERTY = "tree.sequence.file";

    private String previousSequence;
    private String previousFile;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(null == System.getenv(SEQUENCE), "SEQUENCE environment variable overrides system properties");
        previousSequence = System.getProperty(SEQUENCE);
        previousFile = System.getProperty(FILE_PROPERTY);
        System.clearProperty(SEQUENCE);
        System.clearProperty(FILE_PROPERTY);
        SequenceTracker.reset();
    }

    @AfterEach
    void tearDown() {
        restore(SEQUENCE, previousSequence);
        restore(FILE_PROPERTY, previousFile);
        SequenceTracker.reset();
    }

    private static void restore(final String key, final String value) {
        if (null == value) System.clearProperty(key);
        else System.setProperty(key, value);
    }

    private static Path script(final Path dir, final String content) throws IOException {
        final Path file = dir.resolve("script.sequence");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void noScriptIsActiveWhenNothingIsConfigured() {
        assertFalse(SequenceTracker.hasActiveScript());
        assertNull(SequenceTracker.peekNextScenario());
    }

    @Test
    void scriptIsReadFromSystemPropertySkippingBlankLinesAndComments(@TempDir final Path dir) throws IOException {
        System.setProperty(SEQUENCE, script(dir, "# header\n\n  first.Scenario  \n\t\nsecond.Scenario\n# trailing\n").toString());

        assertTrue(SequenceTracker.hasActiveScript());
        assertEquals("first.Scenario", SequenceTracker.peekNextScenario());
    }

    @Test
    void fallbackPropertyNameIsUsedWhenSequenceIsNotSet(@TempDir final Path dir) throws IOException {
        System.setProperty(FILE_PROPERTY, script(dir, "only.Scenario\n").toString());

        assertTrue(SequenceTracker.hasActiveScript());
        assertEquals("only.Scenario", SequenceTracker.peekNextScenario());
    }

    @Test
    void sequencePropertyTakesPrecedenceOverFallbackProperty(@TempDir final Path dir) throws IOException {
        final Path primary = script(dir, "primary.Scenario\n");
        final Path other = dir.resolve("other.sequence");
        Files.writeString(other, "fallback.Scenario\n", StandardCharsets.UTF_8);
        System.setProperty(SEQUENCE, primary.toString());
        System.setProperty(FILE_PROPERTY, other.toString());

        assertEquals("primary.Scenario", SequenceTracker.peekNextScenario());
    }

    @Test
    void advancingWalksThroughScriptUntilItIsExhausted(@TempDir final Path dir) throws IOException {
        System.setProperty(SEQUENCE, script(dir, "a\nb\nc\n").toString());

        assertEquals("a", SequenceTracker.peekNextScenario());
        SequenceTracker.advanceSequence();
        assertEquals("b", SequenceTracker.peekNextScenario());
        SequenceTracker.advanceSequence();
        assertEquals("c", SequenceTracker.peekNextScenario());
        SequenceTracker.advanceSequence();

        assertFalse(SequenceTracker.hasActiveScript());
        assertNull(SequenceTracker.peekNextScenario());
        SequenceTracker.advanceSequence(); // advancing an exhausted script is a harmless no-op
        assertFalse(SequenceTracker.hasActiveScript());
    }

    @Test
    void peekDoesNotAdvance(@TempDir final Path dir) throws IOException {
        System.setProperty(SEQUENCE, script(dir, "a\nb\n").toString());

        assertEquals("a", SequenceTracker.peekNextScenario());
        assertEquals("a", SequenceTracker.peekNextScenario());
    }

    @Test
    void emptyScriptIsNeverActive(@TempDir final Path dir) throws IOException {
        System.setProperty(SEQUENCE, script(dir, "# nothing but comments\n\n").toString());

        assertFalse(SequenceTracker.hasActiveScript());
    }

    @Test
    void missingScriptFileLeavesNoActiveScript(@TempDir final Path dir) {
        System.setProperty(SEQUENCE, dir.resolve("does-not-exist.sequence").toString());

        assertFalse(SequenceTracker.hasActiveScript());
        assertNull(SequenceTracker.peekNextScenario());
    }

    @Test
    void directoryInsteadOfFileLeavesNoActiveScript(@TempDir final Path dir) {
        System.setProperty(SEQUENCE, dir.toString());

        assertFalse(SequenceTracker.hasActiveScript());
    }

    @Test
    void blankPropertyValueIsIgnored() {
        System.setProperty(SEQUENCE, "   ");

        assertFalse(SequenceTracker.hasActiveScript());
    }

    @Test
    void scriptIsLoadedOnlyOncePerInitialisation(@TempDir final Path dir) throws IOException {
        final Path file = script(dir, "a\nb\n");
        System.setProperty(SEQUENCE, file.toString());
        assertEquals("a", SequenceTracker.peekNextScenario());
        SequenceTracker.advanceSequence();

        Files.writeString(file, "x\ny\n", StandardCharsets.UTF_8);

        assertEquals("b", SequenceTracker.peekNextScenario(), "script must not be re-read once initialised");
    }

    @Test
    void resetDiscardsLoadedScriptAndAllowsReloading(@TempDir final Path dir) throws IOException {
        final Path file = script(dir, "a\nb\n");
        System.setProperty(SEQUENCE, file.toString());
        SequenceTracker.advanceSequence();
        assertEquals("b", SequenceTracker.peekNextScenario());

        Files.writeString(file, "x\ny\n", StandardCharsets.UTF_8);
        SequenceTracker.reset();

        assertEquals("x", SequenceTracker.peekNextScenario());
    }

    @Test
    void recordingNullScenarioIsIgnored() {
        assertDoesNotThrow(() -> SequenceTracker.recordScenarioExecution(null));
    }

    @Test
    void writeSequenceFileDoesNothingWhenNothingWasRecorded() throws IOException {
        final long before = sequenceArtifacts().size();

        SequenceTracker.writeSequenceFile();

        assertEquals(before, sequenceArtifacts().size());
    }

    @Test
    void recordedScenariosAreWrittenInExecutionOrder() throws IOException {
        final List<Path> before = sequenceArtifacts();
        final NamedLeaf first = new NamedLeaf("first");
        final NamedLeaf second = new NamedLeaf("second");

        SequenceTracker.recordScenarioExecution(first);
        SequenceTracker.recordScenarioExecution(second);
        SequenceTracker.recordScenarioExecution(first);
        SequenceTracker.writeSequenceFile();

        final List<Path> created = sequenceArtifacts();
        created.removeAll(before);
        try {
            assertEquals(1, created.size(), "exactly one sequence artifact should have been written");
            assertEquals(List.of(first.getShortName(), second.getShortName(), first.getShortName()),
                    Files.readAllLines(created.get(0), StandardCharsets.UTF_8));
        } finally {
            for (final Path path : created) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static List<Path> sequenceArtifacts() throws IOException {
        final Path runs = Path.of("result", "runs");
        if (!Files.isDirectory(runs)) return new java.util.ArrayList<>(0);
        try (Stream<Path> walk = Files.walk(runs)) {
            return walk.filter(p -> p.getFileName().toString().endsWith(".scenarios.sequence"))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        }
    }

    private static final class NamedLeaf extends Leaf {

        private final String simpleName;

        NamedLeaf(final String simpleName) {
            super(Fraction.ONE);
            this.simpleName = simpleName;
        }

        @Override
        public String getSimpleName() {
            return simpleName;
        }

        @Override
        public String getShortName() {
            return simpleName;
        }
    }
}
