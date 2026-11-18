package works.lysenko.base.output.loghtml;

import works.lysenko.base.output.loghtml.LogModels.ArtifactItem;
import works.lysenko.base.output.loghtml.LogModels.EtaDebugItem;
import works.lysenko.base.output.loghtml.LogModels.LeafCompletion;
import works.lysenko.base.output.loghtml.LogModels.SystemResourceItem;
import works.lysenko.base.output.loghtml.sidecar.ArtifactLoader;
import works.lysenko.base.output.loghtml.sidecar.CompletionLoader;
import works.lysenko.base.output.loghtml.sidecar.TelemetryLogLoader;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Entry point facade for reading leaf completions, ETA debug logs, CPU telemetry logs, and test artifacts.
 */
@SuppressWarnings({"ClassWithoutLogger"})
public final class SidecarLoader {

    private SidecarLoader() {
    }

    public static List<LeafCompletion> loadLeafCompletions(final File runLogFile) {
        return CompletionLoader.loadLeafCompletions(runLogFile);
    }

    public static File allLeafCompletionsFile(final File runLogFile) {
        return CompletionLoader.allLeafCompletionsFile(runLogFile);
    }

    public static List<EtaDebugItem> loadEtaDebugItems(final File runLogFile) {
        return CompletionLoader.loadEtaDebugItems(runLogFile);
    }

    public static File etaDebugFile(final File runLogFile) {
        return CompletionLoader.etaDebugFile(runLogFile);
    }

    public static List<ArtifactItem> loadRunArtifacts(final File runLogFile) {
        return ArtifactLoader.loadRunArtifacts(runLogFile);
    }

    public static Map<Integer, Double> loadTelemetryCpuMap(final File runLogFile) {
        return TelemetryLogLoader.loadTelemetryCpuMap(runLogFile);
    }

    public static List<Double> loadTelemetryCpu(final File runLogFile) {
        return TelemetryLogLoader.loadTelemetryCpu(runLogFile);
    }

    public static List<SystemResourceItem> loadTelemetryResources(final File runLogFile) {
        return TelemetryLogLoader.loadTelemetryResources(runLogFile);
    }

    public static List<SystemResourceItem> loadTelemetryResources(final File runLogFile, final int testCount) {
        return TelemetryLogLoader.loadTelemetryResources(runLogFile, testCount);
    }

    public static List<SystemResourceItem> loadTelemetryResources(
            final File runLogFile, final int testCount, final int minTestOp, final int maxTestOp) {
        return TelemetryLogLoader.loadTelemetryResources(runLogFile, testCount, minTestOp, maxTestOp);
    }

    public static List<SystemResourceItem> trimTelemetryToTestPeriod(
            final List<SystemResourceItem> rawItems, final int minTestOp, final int maxTestOp) {
        return TelemetryLogLoader.trimTelemetryToTestPeriod(rawItems, minTestOp, maxTestOp);
    }

    public static int[] findTestOpBounds(final File runLogFile) {
        return TelemetryLogLoader.findTestOpBounds(runLogFile);
    }

    public static List<int[]> findTestOpBoundsPerTest(final File runLogFile, final int testCount) {
        return TelemetryLogLoader.findTestOpBoundsPerTest(runLogFile, testCount);
    }

    public static List<SystemResourceItem> resampleTestTelemetry(
            final List<SystemResourceItem> rawItems, final int targetCount, final int testNum) {
        return TelemetryLogLoader.resampleTestTelemetry(rawItems, targetCount, testNum);
    }

    public static List<SystemResourceItem> downsampleTelemetry(final List<SystemResourceItem> rawItems, final int targetDensity) {
        return TelemetryLogLoader.downsampleTelemetry(rawItems, targetDensity);
    }
}
