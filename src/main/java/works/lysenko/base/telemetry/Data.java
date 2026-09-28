package works.lysenko.base.telemetry;

/**
 * DataStorage represents telemetry data with various properties such as timestamp, cores, threads, etc.
 *
 * @param timestamp The timestamp of the telemetry data.
 * @param cores     The number of CPU cores.
 * @param threads   The number of threads.
 * @param spent     The CPU time spent in milliseconds.
 * @param loadedC   The number of classes loaded.
 * @param unloadedC The number of classes unloaded.
 * @param totalC    The total number of loaded classes.
 * @param freeM     The amount of free memory in bytes.
 * @param totalM    The total amount of memory in bytes.
 * @param maxM      The maximum amount of memory in bytes.
 * @param timeShift The time shift from the start of telemetry data in milliseconds.
 * @param runTime   The runtime of telemetry capturing in nanoseconds.
 * @param comment   The comment for the telemetry data.
 */
@SuppressWarnings("ClassWithTooManyFields")
public record Data(Long timestamp,
                   int cores,
                   int threads,
                   double spent,
                   int loadedC, long unloadedC, long totalC,
                   long freeM, long totalM, long maxM, int timeShift, long runTime, String comment) {


}
