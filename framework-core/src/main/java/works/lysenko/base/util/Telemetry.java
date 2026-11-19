package works.lysenko.base.util;

import com.sun.management.OperatingSystemMXBean;
import works.lysenko.base.core.Routines;
import works.lysenko.base.telemetry.Data;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._DASH_;

/**
 * This class represents Telemetry data with the rendered string representation.
 * @param data telemetry data payload
 * @param rendered formatted telemetry string
 */
@SuppressWarnings({"AutoBoxing", "FeatureEnvy", "NestedMethodCall", "ImplicitNumericConversion",
        "NumericCastThatLosesPrecision",
        "MethodWithMultipleReturnPoints", "OverloadedMethodsWithSameNumberOfParameters", "CallToSuspiciousStringMethod"})
public record Telemetry(Data data, String rendered) {

    private static final ThreadMXBean threadsMX = ManagementFactory.getThreadMXBean();
    private static final OperatingSystemMXBean osMX =
            (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean bean) ? bean : null;
    private static final ClassLoadingMXBean classesMX = ManagementFactory.getClassLoadingMXBean();
    private static final Runtime env = Runtime.getRuntime();
    private static final String SAME = s(_DASH_);
    /**
     * Minimum wall-clock interval between CPU counter reads so sub-millisecond log bursts
     * do not produce noisy spikes or extra OS syscalls.
     */
    static final long MIN_CPU_SAMPLE_WINDOW_NS = 250_000_000L;
    /**
     * A1 small positive value used for floating-point comparisons with a fractionTolerance.
     * The value of EPSILON is 0.0000001.
     */
    private static final double EPSILON = 0.0000001;
    /**
     * Variable representing the previous telemetry data.
     */
    private static Data previous = new Data(0L, 0, 0, 0.0, 0, 0L, 0L,
            0L, 0L, 0L, 0, 0L, EMPTY);
    private static long lastCpuWallNs = -1L;
    private static long lastProcessCpuNs = -1L;
    private static double lastCpuPercent = 0.0;

    /**
     * Retrieves a new Telemetry object with the given time and comment.
     *
     * @param time    The time for the telemetry.
     * @param comment The comment for the telemetry.
     * @return A1 new Telemetry object with the fresh data and the rendered string representation.
     */
    public static synchronized Telemetry getNewTelemetry(final long time, final String comment) {

        final Data fresh = sampleNewTelemetryData(time, comment);
        final String spent = toString(fresh.spent(), previous.spent());
        final String loaded = toString(fresh.loadedC(), previous.loadedC());
        final String total = toString(fresh.totalC(), previous.totalC());
        final String s = b(_COMMA_,
                toString(fresh.timeShift(), previous.timeShift()),
                s(fresh.timestamp()),
                spent,
                toString(fresh.runTime(), previous.runTime()),
                toString(fresh.cores(), previous.cores()),
                toString(fresh.threads(), previous.threads()),
                loaded,
                toString(fresh.unloadedC(), previous.unloadedC()),
                toString(total, loaded),
                toString(fresh.freeM(), previous.freeM()),
                toString(fresh.totalM(), previous.totalM()),
                toString(fresh.maxM(), previous.maxM()),
                n(EMPTY, comment)
        );
        previous = fresh;
        return new Telemetry(fresh, s);
    }

    /**
     * Reads cumulative process CPU time in nanoseconds across all JVM threads (including exited,
     * GC, and JIT threads) via {@link OperatingSystemMXBean#getProcessCpuTime()}, falling back to
     * summing live thread CPU times if unavailable.
     *
     * @return process CPU time in nanoseconds
     */
    private static long readProcessCpuTimeNs() {

        if (null != osMX) {
            final long processCpuTime = osMX.getProcessCpuTime();
            if (processCpuTime >= 0L) return processCpuTime;
        }
        final long[] ids = threadsMX.getAllThreadIds();
        long time = 0L;
        for (final long id : ids) {
            final long thisTime = threadsMX.getThreadCpuTime(id);
            if (-1L != thisTime) time += thisTime;
        }
        return time;
    }

    /**
     * Computes interval CPU usage percentage normalized across available logical cores.
     *
     * @param deltaCpuNs  process CPU nanoseconds elapsed during the window
     * @param deltaWallNs wall-clock nanoseconds elapsed during the window
     * @param cores       available logical CPU cores
     * @return normalized CPU percentage in {@code [0.0, 100.0]} rounded to 2 decimal places
     */
    static double computeIntervalCpuPercent(final long deltaCpuNs, final long deltaWallNs, final int cores) {

        if (deltaWallNs <= 0L || deltaCpuNs <= 0L || cores <= 0) return 0.0;
        final double rawPct = (deltaCpuNs * 100.0) / ((double) deltaWallNs * cores);
        final double clamped = Math.min(100.0, Math.max(0.0, rawPct));
        return Math.round(clamped * 100.0) / 100.0;
    }

    /**
     * Samples interval JVM CPU usage percentage, refreshing at most once per
     * {@link #MIN_CPU_SAMPLE_WINDOW_NS} to avoid sub-millisecond jitter on rapid log bursts.
     */
    private static double sampleIntervalCpuPercent(final long nowNs, final int cores) {

        if (lastCpuWallNs < 0L) {
            lastCpuWallNs = nowNs;
            lastProcessCpuNs = readProcessCpuTimeNs();
            final double initialLoad = (null != osMX) ? osMX.getProcessCpuLoad() : -1.0;
            lastCpuPercent = (initialLoad >= 0.0 && Double.isFinite(initialLoad))
                    ? Math.round(Math.min(100.0, initialLoad * 100.0) * 100.0) / 100.0
                    : 0.0;
            return lastCpuPercent;
        }
        final long deltaWallNs = nowNs - lastCpuWallNs;
        if (deltaWallNs < MIN_CPU_SAMPLE_WINDOW_NS) {
            return lastCpuPercent;
        }
        final long nowCpuNs = readProcessCpuTimeNs();
        final long deltaCpuNs = nowCpuNs - lastProcessCpuNs;
        lastCpuWallNs = nowNs;
        lastProcessCpuNs = nowCpuNs;
        lastCpuPercent = computeIntervalCpuPercent(deltaCpuNs, deltaWallNs, cores);
        return lastCpuPercent;
    }

    static synchronized void resetCpuSampler() {

        lastCpuWallNs = -1L;
        lastProcessCpuNs = -1L;
        lastCpuPercent = 0.0;
    }

    /**
     * Creates a new telemetry data with the given time and comment.
     *
     * @param time    The timestamp for the telemetry data.
     * @param comment The comment for the telemetry data.
     * @return A1 new telemetry data object.
     */
    private static Data sampleNewTelemetryData(final long time, final String comment) {

        final long start = System.nanoTime();
        final int cores = env.availableProcessors();
        final int threads = threadsMX.getThreadCount();
        final int load = classesMX.getLoadedClassCount();
        final long unload = classesMX.getUnloadedClassCount();
        final long totalC = classesMX.getTotalLoadedClassCount();
        final long free = env.freeMemory();
        final long max = env.maxMemory();
        final long totalM = env.totalMemory();
        final long our = Routines.msSinceStart();
        final double spent = sampleIntervalCpuPercent(start, cores);
        final int timeShift = (int) (our - time);
        final long runTime = System.nanoTime() - start;
        return new Data(time, cores, threads, spent, load, unload, totalC, free, totalM, max, timeShift, runTime, comment);
    }

    private static String toString(final int fresh, final int stored) {

        if (fresh == stored) return s(SAME);
        else return s(fresh);
    }

    private static String toString(final long fresh, final long stored) {

        if (fresh == stored) return s(SAME);
        else return s(fresh);
    }

    private static String toString(final double fresh, final double stored) {

        if (EPSILON > Math.abs(fresh - stored)) return s(SAME);
        else return s(fresh);
    }

    private static String toString(final String fresh, final String stored) {

        if (fresh.equals(stored)) return s(SAME);
        else return s(fresh);
    }

    @SuppressWarnings("NullableProblems")
    /**
     * Performs to string.
     * @return the to string result
     */
    public String toString() {

        return rendered;
    }
}