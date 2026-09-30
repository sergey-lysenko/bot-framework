package works.lysenko.base.util;

import works.lysenko.base.core.Routines;
import works.lysenko.base.telemetry.Data;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.math3.util.FastMath.min;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._DASH_;

/**
 * This class represents Telemetry data with the rendered string representation.
 * @param data
 * @param rendered
 */
@SuppressWarnings({"AutoBoxing", "FeatureEnvy", "NestedMethodCall", "ImplicitNumericConversion",
        "NumericCastThatLosesPrecision",
        "MethodWithMultipleReturnPoints", "OverloadedMethodsWithSameNumberOfParameters", "CallToSuspiciousStringMethod"})
public record Telemetry(Data data, String rendered) {

    private static final ThreadMXBean threadsMX = ManagementFactory.getThreadMXBean();
    private static final ClassLoadingMXBean classesMX = ManagementFactory.getClassLoadingMXBean();
    private static final Runtime env = Runtime.getRuntime();
    private static final String SAME = s(_DASH_);
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

    /**
     * Retrieves a new Telemetry object with the given time and comment.
     *
     * @param time    The time for the telemetry.
     * @param comment The comment for the telemetry.
     * @return A1 new Telemetry object with the fresh data and the rendered string representation.
     */
    public static Telemetry getNewTelemetry(final long time, final String comment) {

        final Data fresh = sampleNewTelemetryData(time, comment);
        final String spent = toString(fresh.spent(), previous.spent());
        final String loaded = toString(fresh.loadedC(), previous.loadedC());
        final String total = toString(fresh.totalC(), previous.totalC());
        final String s = b(_COMMA_,
                toString(fresh.timeShift(), previous.timeShift()),
                s(fresh.timestamp()),
                spent.substring(0, min(7, spent.length())),
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
     * Calculates the total CPU time spent by all threads in milliseconds.
     *
     * @return The total CPU time spent in milliseconds.
     */
    private static long getSpentTime() {

        final ThreadInfo[] threadInfos = threadsMX.dumpAllThreads(false, false);
        long time = 0;
        for (final ThreadInfo threadInfo : threadInfos) {
            final long thisTime = threadsMX.getThreadCpuTime(threadInfo.getThreadId());
            if (-1 != thisTime) time += thisTime;
        }
        return (time / 1000);
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
        final double rawSpent = (0 < our) ? ((getSpentTime() / 10.0) / our) : 0.0;
        final double spent = (our < 1000 || rawSpent > (cores * 100.0)) ? 0.0 : rawSpent;
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
    public String toString() {

        return rendered;
    }
}