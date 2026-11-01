package works.lysenko.base.output.loghtml;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model classes and records used during HTML log report generation.
 */
@SuppressWarnings({"ClassWithoutLogger", "StandardVariableNames"})
public final class LogModels {

    private LogModels() {
    }

    /**
     * Record representing a test execution artifact item (e.g., screenshot or data snapshot).
     *
     * @param rel  relative path to artifact file
     * @param test associated test number
     * @param ms   timestamp offset in milliseconds
     * @param sec  timestamp offset in seconds
     * @param ext  artifact file extension
     * @param name base artifact name
     */
    public record ArtifactItem(String rel, int test, int ms, double sec, String ext, String name) {
    }

    /**
     * Structured section of log records corresponding to an execution phase (e.g., booting, test run, limbo).
     */
    public static final class LogSection {
        public final String type;
        public String title;
        /** The prev test num. */
        public int prevTestNum;
        /** The status. */
        public String status;
        /** The test num. */
        public int testNum;
        /** The start op. */
        public Integer startOp;
        /** The end op. */
        public Integer endOp;
        /** The start time. */
        public String startTime;
        /** The end time. */
        public String endTime;
        /** The duration str. */
        public String durationStr;
        /** The duration sec. */
        public double durationSec;
        /** The scenarios. */
        public final List<String> scenarios = new ArrayList<>();
        /** The lines. */
        public final List<String> lines;

        /**
         * Constructs a new LogSection.
         * @param type the type
         * @param title the title
         * @param status the status
         * @param lineStore the lineStore
         */
        public LogSection(final String type, final String title, final String status, final LineStore lineStore) {
            this.type = type;
            this.title = title;
            this.status = status;
            lines = new SpoolingLines(lineStore);
        }
    }

    /**
     * Disk-backed list of log line strings to minimize memory overhead during report rendering.
     */
    public static final class SpoolingLines extends AbstractList<String> {
        private final LineStore store;
        private final List<Long> positions = new ArrayList<>();

        /**
         * Constructs a new SpoolingLines.
         * @param store the store
         */
        public SpoolingLines(final LineStore store) {
            this.store = store;
        }

        @Override
        public String get(final int index) {
            try {
                return store.read(positions.get(index));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public int size() {
            return positions.size();
        }

        @Override
        public boolean add(final String line) {
            try {
                positions.add(store.append(line));
                return true;
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Temporary disk file storage backing {@link SpoolingLines} instances.
     */
    public static final class LineStore implements AutoCloseable {
        private final Path path;
        private final RandomAccessFile file;

        /**
         * Constructs a new LineStore.
         */
        public LineStore() throws IOException {
            path = java.nio.file.Files.createTempFile("run-report-lines-", ".tmp");
            file = new RandomAccessFile(path.toFile(), "rw");
        }

        /**
         * Performs append.
         * @param line the line
         * @return the append result
         */
        public synchronized long append(final String line) throws IOException {
            final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
            final long position = file.length();
            file.seek(position);
            file.writeInt(bytes.length);
            file.write(bytes);
            return position;
        }

        /**
         * Performs read.
         * @param position the position
         * @return the read result
         */
        public synchronized String read(final long position) throws IOException {
            file.seek(position);
            final int length = file.readInt();
            final byte[] bytes = new byte[length];
            file.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public void close() throws IOException {
            try {
                file.close();
            } finally {
                java.nio.file.Files.deleteIfExists(path);
            }
        }
    }

    /**
     * Test path record capturing test number, execution duration, and path steps.
     */
    public static final class PathEntry {
        public final String num;
        public final String durStr;
        /** The steps. */
        public final List<String> steps;

        /**
         * Constructs a new PathEntry.
         * @param num the num
         * @param durStr the durStr
         * @param steps the steps
         */
        public PathEntry(final String num, final String durStr, final List<String> steps) {
            this.num = num;
            this.durStr = durStr;
            this.steps = steps;
        }
    }

    /**
     * Scenario tree leaf completion record capturing completion moment, ETA, and round index.
     */
    public static final class LeafCompletion {
        public final int testNum;
        public final long atMillis;
        /** The patience percentage. */
        public final double patiencePercentage;
        /** The eta ms. */
        public final long etaMs;
        /** The round index. */
        public final int roundIndex;
        /** The leaf. */
        public final String leaf;

        /**
         * Constructs a new LeafCompletion.
         * @param testNum the testNum
         * @param atMillis the atMillis
         * @param patiencePercentage the patiencePercentage
         * @param etaMs the etaMs
         * @param roundIndex the roundIndex
         * @param leaf the leaf
         */
        public LeafCompletion(final int testNum, final long atMillis, final double patiencePercentage, final long etaMs, final int roundIndex, final String leaf) {
            this.testNum = testNum;
            this.atMillis = atMillis;
            this.patiencePercentage = patiencePercentage;
            this.etaMs = etaMs;
            this.roundIndex = roundIndex;
            this.leaf = leaf;
        }
    }

    /**
     * ETA debug metric tracking predicted versus actual test execution times and deviations.
     */
    public static final class EtaDebugItem {
        public final int testNum;
        public final long realMs;
        /** The mean eta ms. */
        public final long meanEtaMs;
        /** The std dev eta ms. */
        public final long stdDevEtaMs;
        /** The predicted ms. */
        public final long predictedMs;
        /** The test name. */
        public final String testName;

        public EtaDebugItem(final int testNum, final long realMs, final long meanEtaMs,
                            final long stdDevEtaMs, final long predictedMs, final String testName) {
            this.testNum = testNum;
            this.realMs = realMs;
            this.meanEtaMs = meanEtaMs;
            this.stdDevEtaMs = stdDevEtaMs;
            this.predictedMs = predictedMs;
            this.testName = testName;
        }

        /**
         * Performs instability pct.
         * @return the instability pct result
         */
        public double instabilityPct() {
            if (meanEtaMs <= 0) return 0.0;
            return Math.min(100000.0, ((double) stdDevEtaMs / meanEtaMs) * 100.0);
        }

        /**
         * Performs error pct.
         * @return the error pct result
         */
        public double errorPct() {
            if (realMs <= 0) return 0.0;
            final long diff = Math.abs(predictedMs - realMs);
            return Math.min(100000.0, ((double) diff / realMs) * 100.0);
        }
    }

    /**
     * Scenario execution statistics entry capturing node symbol, scenario name, weights, and counts.
     */
    public static final class ScenEntry {
        public final String sym;
        public final String name;
        /** The weight. */
        public final String weight;
        /** The count. */
        public final String count;
        /** The events. */
        public final String events;

        /**
         * Constructs a new ScenEntry.
         * @param sym the sym
         * @param name the name
         * @param weight the weight
         * @param count the count
         * @param events the events
         */
        public ScenEntry(final String sym, final String name, final String weight, final String count, final String events) {
            this.sym = sym;
            this.name = name;
            this.weight = weight;
            this.count = count;
            this.events = events;
        }
    }

    /**
     * Telemetry item representing CPU and RAM resource measurements.
     */
    public static final class SystemResourceItem {
        public final int sampleNum;
        public final double cpuPct;
        /** The max cpu pct. */
        public final double maxCpuPct;
        /** The used ram mb. */
        public final double usedRamMb;
        /** The total ram mb. */
        public final double totalRamMb;
        /** The threads. */
        public final int threads;
        /** The test num. */
        public final int testNum;

        public SystemResourceItem(final int sampleNum, final double cpuPct,
                                  final double usedRamMb, final double totalRamMb, final int threads) {
            this(sampleNum, cpuPct, cpuPct, usedRamMb, totalRamMb, threads, 0);
        }

        public SystemResourceItem(final int sampleNum, final double cpuPct, final double maxCpuPct,
                                  final double usedRamMb, final double totalRamMb, final int threads) {
            this(sampleNum, cpuPct, maxCpuPct, usedRamMb, totalRamMb, threads, 0);
        }

        public SystemResourceItem(final int sampleNum, final double cpuPct, final double maxCpuPct,
                                  final double usedRamMb, final double totalRamMb, final int threads, final int testNum) {
            this.sampleNum = sampleNum;
            this.cpuPct = cpuPct;
            this.maxCpuPct = maxCpuPct;
            this.usedRamMb = usedRamMb;
            this.totalRamMb = totalRamMb;
            this.threads = threads;
            this.testNum = testNum;
        }
    }

    /**
     * Telemetry item representing test phase or limbo duration for chart rendering.
     */
    public static final class TelemetryItem {
        public final String label;
        public final String title;
        /** The sec. */
        public final double sec;
        /** The ms. */
        public final double ms;
        /** The status. */
        public final String status;
        /** The is limbo. */
        public final boolean isLimbo;
        /** The test num. */
        public final int testNum;

        public TelemetryItem(final String label, final String title, final double sec, final double ms,
                             final String status, final boolean isLimbo, final int testNum) {
            this.label = label;
            this.title = title;
            this.sec = sec;
            this.ms = ms;
            this.status = status;
            this.isLimbo = isLimbo;
            this.testNum = testNum;
        }
    }
}
