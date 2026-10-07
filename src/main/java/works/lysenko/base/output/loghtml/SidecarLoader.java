package works.lysenko.base.output.loghtml;

import works.lysenko.base.output.loghtml.LogModels.ArtifactItem;
import works.lysenko.base.output.loghtml.LogModels.EtaDebugItem;
import works.lysenko.base.output.loghtml.LogModels.LeafCompletion;
import works.lysenko.base.output.loghtml.LogModels.SystemResourceItem;
import works.lysenko.util.spec.PropEnum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * Sidecar file loader for reading leaf completions, ETA debug logs, CPU telemetry logs, and test artifacts.
 */
@SuppressWarnings({"ClassWithoutLogger", "MethodWithMultipleLoops", "NestedMethodCall"})
public final class SidecarLoader {

    private static final Pattern ALL_LEAF_COMPLETION_RE = Pattern.compile("\\[ALL_LEAF_COMPLETION]\\s+(\\d+)\\s+(\\d+)(?:\\s+(\\d+))?(?:\\s+(\\d+))?\\s+(.+)$");
    private static final Pattern ETA_DEBUG_RE = Pattern.compile("\\[ETA_DEBUG]\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(.+)$");

    private SidecarLoader() {
    }

    /**
     * Reads leaf completion entries from a sidecar file if present.
     *
     * @param runLogFile input run log file
     * @return list of parsed leaf completion records
     */
    public static List<LeafCompletion> loadLeafCompletions(final File runLogFile) {
        final File completionFile = allLeafCompletionsFile(runLogFile);
        if (!completionFile.isFile()) return List.of();

        final List<LeafCompletion> completions = new ArrayList<>();
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(completionFile), StandardCharsets.UTF_8))) {
            String line;
            int legacyRoundCount = 0;
            while (null != (line = reader.readLine())) {
                final Matcher matcher = ALL_LEAF_COMPLETION_RE.matcher(line.trim());
                if (matcher.matches()) {
                    final int testNum = Integer.parseInt(matcher.group(1));
                    final long atMillis = Long.parseLong(matcher.group(2));
                    final String g3 = matcher.group(3);
                    final String g4 = matcher.group(4);
                    final String leaf = matcher.group(5).trim();

                    long etaMs = 0L;
                    int roundIndex = 0;

                    if (null != g3 && !g3.isEmpty()) {
                        if (null != g4 && !g4.isEmpty()) {
                            etaMs = Long.parseLong(g3);
                            roundIndex = Integer.parseInt(g4);
                        } else {
                            etaMs = Long.parseLong(g3);
                            legacyRoundCount++;
                            roundIndex = legacyRoundCount;
                        }
                    } else {
                        legacyRoundCount++;
                        roundIndex = legacyRoundCount;
                    }

                    completions.add(new LeafCompletion(testNum, atMillis, etaMs, roundIndex, leaf));
                }
            }
        } catch (final IOException | NumberFormatException e) {
            logEvent(S2, s("Failed to read all-leaf completion log ", completionFile, ": ", e.getMessage()));
        }
        return completions;
    }

    /**
     * Resolves the location of the leaf completion log sidecar file.
     *
     * @param runLogFile input run log file
     * @return corresponding leaf completion file handle
     */
    public static File allLeafCompletionsFile(final File runLogFile) {
        final String suffix = ".run.log";
        final String name = runLogFile.getName();
        final String prefix = name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
        return new File(runLogFile.getParentFile(), s(prefix, ".all-leaf-completions.log"));
    }

    /**
     * Reads ETA debug metrics from a sidecar file if present.
     *
     * @param runLogFile input run log file
     * @return list of parsed ETA debug items
     */
    public static List<EtaDebugItem> loadEtaDebugItems(final File runLogFile) {
        final File etaDebugFile = etaDebugFile(runLogFile);
        if (!etaDebugFile.isFile()) return List.of();

        final List<EtaDebugItem> items = new ArrayList<>();
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(etaDebugFile), StandardCharsets.UTF_8))) {
            String line;
            while (null != (line = reader.readLine())) {
                final Matcher matcher = ETA_DEBUG_RE.matcher(line.trim());
                if (matcher.matches()) {
                    items.add(new EtaDebugItem(
                            Integer.parseInt(matcher.group(1)),
                            Long.parseLong(matcher.group(2)),
                            Long.parseLong(matcher.group(3)),
                            Long.parseLong(matcher.group(4)),
                            Long.parseLong(matcher.group(5)),
                            matcher.group(6).trim()));
                }
            }
        } catch (final IOException | NumberFormatException e) {
            logEvent(S2, s("Failed to read eta debug log ", etaDebugFile, ": ", e.getMessage()));
        }
        return items;
    }

    /**
     * Resolves the location of the ETA debug log sidecar file.
     *
     * @param runLogFile input run log file
     * @return corresponding ETA debug file handle
     */
    public static File etaDebugFile(final File runLogFile) {
        final String suffix = ".run.log";
        final String name = runLogFile.getName();
        final String prefix = name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
        return new File(runLogFile.getParentFile(), s(prefix, ".eta-debug.log"));
    }

    /**
     * Discovers snapshot and data artifact files in the run log directory.
     *
     * @param runLogFile input run log file
     * @return list of discovered artifact items
     */
    public static List<ArtifactItem> loadRunArtifacts(final File runLogFile) {
        final List<ArtifactItem> artifacts = new ArrayList<>();
        if (null == runLogFile || null == runLogFile.getParentFile()) return artifacts;
        final File runDir = runLogFile.getParentFile();
        final Pattern p = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(.+)$");
        final String[] subdirs = new String[]{"snapshots", "data"};
        for (final String sub : subdirs) {
            final File dir = new File(runDir, sub);
            if (dir.exists() && dir.isDirectory()) {
                final File[] files = dir.listFiles();
                if (null != files) {
                    for (final File f : files) {
                        final String fname = f.getName();
                        final Matcher m = p.matcher(fname);
                        if (m.matches()) {
                            final int testNum = Integer.parseInt(m.group(2));
                            final int ms = Integer.parseInt(m.group(3));
                            final double sec = ms / 1000.0;
                            final String ext = fname.contains(".") ? fname.substring(fname.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
                            artifacts.add(new ArtifactItem(s(sub, "/", fname), testNum, ms, sec, ext, m.group(4)));
                        }
                    }
                }
            }
        }
        return artifacts;
    }

    /**
     * Reads CPU load telemetry samples from the sibling telemetry log file.
     *
     * @param runLogFile input run log file
     * @return list of CPU load percentage values
     */
    public static List<Double> loadTelemetryCpu(final File runLogFile) {
        final List<Double> cpuLoads = new ArrayList<>();
        if (null == runLogFile || null == runLogFile.getParentFile()) return cpuLoads;
        final String runLogName = runLogFile.getName();
        final String telemName = runLogName.replace(".run.log", ".telemetry.log");
        final File telemFile = new File(runLogFile.getParentFile(), telemName);
        if (!telemFile.exists()) return cpuLoads;

        double lastVal = 0.0;
        try (final BufferedReader br = new BufferedReader(new FileReader(telemFile))) {
            String line;
            while (null != (line = br.readLine())) {
                final String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                final String[] parts = trimmed.split(",");
                if (parts.length >= 3) {
                    final String valStr = parts[2].trim();
                    if (!"-".equals(valStr)) {
                        try {
                            lastVal = Double.parseDouble(valStr);
                        } catch (final NumberFormatException ignored) {
                        }
                    }
                    cpuLoads.add(lastVal);
                }
            }
        } catch (final IOException ignored) {
        }
        return cpuLoads;
    }

    /**
     * Reads system resource telemetry (CPU % and heap RAM in MB) from the sibling telemetry log file.
     *
     * @param runLogFile input run log file
     * @return list of SystemResourceItem records
     */
    public static List<SystemResourceItem> loadTelemetryResources(final File runLogFile) {
        return loadTelemetryResources(runLogFile, 0);
    }

    /**
     * Reads system resource telemetry (CPU % and heap RAM in MB) from the sibling telemetry log file,
     * trimming samples outside the test execution period.
     *
     * @param runLogFile input run log file
     * @param testCount  total number of tests performed in the run
     * @return list of SystemResourceItem records
     */
    public static List<SystemResourceItem> loadTelemetryResources(final File runLogFile, final int testCount) {
        final int[] bounds = findTestOpBounds(runLogFile);
        return loadTelemetryResources(runLogFile, testCount, bounds[0], bounds[1]);
    }

    /**
     * Reads system resource telemetry (CPU % and heap RAM in MB) from the sibling telemetry log file,
     * trimming samples outside the specified test operation bounds.
     *
     * @param runLogFile input run log file
     * @param testCount  total number of tests performed in the run
     * @param minTestOp  first operation number of the test period
     * @param maxTestOp  last operation number of the test period
     * @return list of SystemResourceItem records
     */
    public static List<SystemResourceItem> loadTelemetryResources(
            final File runLogFile, final int testCount, final int minTestOp, final int maxTestOp) {
        final List<SystemResourceItem> items = new ArrayList<>();
        if (null == runLogFile || null == runLogFile.getParentFile()) return items;
        final String runLogName = runLogFile.getName();
        final String telemName = runLogName.replace(".run.log", ".telemetry.log");
        final File telemFile = new File(runLogFile.getParentFile(), telemName);
        if (!telemFile.exists()) return items;

        int sampleNum = 0;
        double lastCpu = 0.0;
        double lastFreeM = 0.0;
        double lastTotalM = 0.0;
        int lastThreads = 0;

        try (final BufferedReader br = new BufferedReader(new FileReader(telemFile))) {
            String line;
            while (null != (line = br.readLine())) {
                final String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                final String[] parts = trimmed.split(",");
                if (parts.length >= 11) {
                    sampleNum++;
                    final String cpuStr = parts[2].trim();
                    final String threadsStr = parts[5].trim();
                    final String freeStr = parts[9].trim();
                    final String totalStr = parts[10].trim();

                    if (!"-".equals(cpuStr)) {
                        try { lastCpu = Double.parseDouble(cpuStr); } catch (final NumberFormatException ignored) {}
                    }
                    if (!"-".equals(threadsStr)) {
                        try { lastThreads = Integer.parseInt(threadsStr); } catch (final NumberFormatException ignored) {}
                    }
                    if (!"-".equals(freeStr)) {
                        try { lastFreeM = Double.parseDouble(freeStr); } catch (final NumberFormatException ignored) {}
                    }
                    if (!"-".equals(totalStr)) {
                        try { lastTotalM = Double.parseDouble(totalStr); } catch (final NumberFormatException ignored) {}
                    }

                    final double usedMb = Math.max(0.0, (lastTotalM - lastFreeM) / (1024.0 * 1024.0));
                    final double totalMb = Math.max(0.0, lastTotalM / (1024.0 * 1024.0));
                    items.add(new SystemResourceItem(sampleNum, lastCpu, usedMb, totalMb, lastThreads));
                }
            }
        } catch (final IOException ignored) {
        }

        final List<SystemResourceItem> trimmedItems = trimTelemetryToTestPeriod(items, minTestOp, maxTestOp);

        if (Boolean.TRUE.equals(PropEnum._TEST_REPORT_CPU_DENSITY_PER_TEST.get()) && testCount > 0) {
            final int totalSamples = trimmedItems.size();
            final int samplesPerTest = Math.max(1, (int) Math.round((double) totalSamples / testCount));
            final List<int[]> perTestBounds = findTestOpBoundsPerTest(runLogFile, testCount);

            final List<SystemResourceItem> perTestResampled = new ArrayList<>(testCount * samplesPerTest);
            for (int k = 0; k < testCount; k++) {
                final int testNum = k + 1;
                final List<SystemResourceItem> testRaw = new ArrayList<>();
                if (k < perTestBounds.size()) {
                    final int[] bounds = perTestBounds.get(k);
                    final int minOp = bounds[0];
                    final int maxOp = bounds[1];
                    for (final SystemResourceItem item : trimmedItems) {
                        if (item.sampleNum >= minOp && item.sampleNum <= maxOp) {
                            testRaw.add(item);
                        }
                    }
                }
                if (testRaw.isEmpty() && !trimmedItems.isEmpty()) {
                    final int startIdx = k * totalSamples / testCount;
                    int endIdx = (k + 1) * totalSamples / testCount;
                    if (k == testCount - 1) endIdx = totalSamples;
                    for (int j = startIdx; j < endIdx; j++) {
                        testRaw.add(trimmedItems.get(j));
                    }
                }
                perTestResampled.addAll(resampleTestTelemetry(testRaw, samplesPerTest, testNum));
            }
            return perTestResampled;
        }

        final Integer configuredDensity = PropEnum._TEST_REPORT_CPU_DENSITY.get();
        final int targetDensity = (null != configuredDensity && configuredDensity > 0) ? configuredDensity : 5000;
        return downsampleTelemetry(trimmedItems, targetDensity);
    }

    /**
     * Filters telemetry resource samples to retain only those within the test execution period.
     *
     * @param rawItems  unfiltered telemetry items
     * @param minTestOp lower bound operation index
     * @param maxTestOp upper bound operation index
     * @return filtered list of telemetry items
     */
    public static List<SystemResourceItem> trimTelemetryToTestPeriod(
            final List<SystemResourceItem> rawItems, final int minTestOp, final int maxTestOp) {
        if (null == rawItems || rawItems.isEmpty()) return List.of();
        if (minTestOp == Integer.MAX_VALUE || maxTestOp == Integer.MIN_VALUE || minTestOp > maxTestOp) {
            return rawItems;
        }
        final List<SystemResourceItem> trimmed = new ArrayList<>();
        for (final SystemResourceItem item : rawItems) {
            if (item.sampleNum >= minTestOp && item.sampleNum <= maxTestOp) {
                trimmed.add(item);
            }
        }
        return trimmed.isEmpty() ? rawItems : trimmed;
    }

    /**
     * Scans the run log file to locate the operation number range for the test execution phase.
     *
     * @param runLogFile input run log file
     * @return two-element array with [minTestOp, maxTestOp]
     */
    public static int[] findTestOpBounds(final File runLogFile) {
        if (null == runLogFile || !runLogFile.exists()) {
            return new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
        }

        int minOp = Integer.MAX_VALUE;
        int maxOp = Integer.MIN_VALUE;
        boolean inPostflight = false;

        final Pattern lineWithTestRe = Pattern.compile("^\\[\\s*(\\d+)\\s*\\]\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
        final Pattern lineNoTestRe = Pattern.compile("^(?:\\[\\s*\\])?\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
        final Pattern ansiPat = Pattern.compile("\\x1b\\[[0-9;]*m");
        final Pattern testRunSummaryRe = Pattern.compile(".*\\b\\d+\\s+tests?\\s+of\\s+.+\\s+done\\s+in\\s+.*");

        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(runLogFile), StandardCharsets.UTF_8))) {
            String line;
            while (null != (line = reader.readLine())) {
                final String clean = ansiPat.matcher(line).replaceAll("").trim();
                if (clean.isEmpty()) continue;

                if (!inPostflight && (clean.contains("Closing test service") || clean.contains("Event summary")
                        || clean.contains("Events summary") || clean.contains("Postflight")
                        || clean.contains("Test session completed")
                        || testRunSummaryRe.matcher(clean).matches())) {
                    inPostflight = true;
                }

                if (!inPostflight) {
                    final Matcher mTest = lineWithTestRe.matcher(clean);
                    if (mTest.matches()) {
                        final int tNum = Integer.parseInt(mTest.group(1));
                        final int opNum = Integer.parseInt(mTest.group(2));
                        if (tNum > 0) {
                            if (opNum < minOp) minOp = opNum;
                            if (opNum > maxOp) maxOp = opNum;
                        }
                    } else {
                        final Matcher mNoTest = lineNoTestRe.matcher(clean);
                        if (mNoTest.matches()) {
                            final int opNum = Integer.parseInt(mNoTest.group(1));
                            if (minOp != Integer.MAX_VALUE) {
                                if (opNum > maxOp) maxOp = opNum;
                            }
                        }
                    }
                }
            }
        } catch (final IOException | NumberFormatException ignored) {
        }

        return new int[]{minOp, maxOp};
    }

    /**
     * Scans the run log file to locate the operation number range for each test in the run.
     *
     * @param runLogFile input run log file
     * @param testCount  total number of tests
     * @return list of two-element arrays with [minOp, maxOp] for each test
     */
    public static List<int[]> findTestOpBoundsPerTest(final File runLogFile, final int testCount) {
        if (null == runLogFile || !runLogFile.exists() || testCount <= 0) {
            return List.of();
        }

        final int[][] bounds = new int[testCount][2];
        for (int i = 0; i < testCount; i++) {
            bounds[i][0] = Integer.MAX_VALUE;
            bounds[i][1] = Integer.MIN_VALUE;
        }

        boolean inPostflight = false;
        int currentTestIdx = -1;

        final Pattern lineWithTestRe = Pattern.compile("^\\[\\s*(\\d+)\\s*\\]\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
        final Pattern lineNoTestRe = Pattern.compile("^(?:\\[\\s*\\])?\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
        final Pattern ansiPat = Pattern.compile("\\x1b\\[[0-9;]*m");
        final Pattern testRunSummaryRe = Pattern.compile(".*\\b\\d+\\s+tests?\\s+of\\s+.+\\s+done\\s+in\\s+.*");

        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(runLogFile), StandardCharsets.UTF_8))) {
            String line;
            while (null != (line = reader.readLine())) {
                final String clean = ansiPat.matcher(line).replaceAll("").trim();
                if (clean.isEmpty()) continue;

                if (!inPostflight && (clean.contains("Closing test service") || clean.contains("Event summary")
                        || clean.contains("Events summary") || clean.contains("Postflight")
                        || clean.contains("Test session completed")
                        || testRunSummaryRe.matcher(clean).matches())) {
                    inPostflight = true;
                }

                if (!inPostflight) {
                    final Matcher mTest = lineWithTestRe.matcher(clean);
                    if (mTest.matches()) {
                        final int tNum = Integer.parseInt(mTest.group(1));
                        final int opNum = Integer.parseInt(mTest.group(2));
                        if (tNum > 0 && tNum <= testCount) {
                            currentTestIdx = tNum - 1;
                            if (opNum < bounds[currentTestIdx][0]) bounds[currentTestIdx][0] = opNum;
                            if (opNum > bounds[currentTestIdx][1]) bounds[currentTestIdx][1] = opNum;
                        }
                    } else {
                        final Matcher mNoTest = lineNoTestRe.matcher(clean);
                        if (mNoTest.matches()) {
                            final int opNum = Integer.parseInt(mNoTest.group(1));
                            if (currentTestIdx >= 0) {
                                if (opNum > bounds[currentTestIdx][1]) bounds[currentTestIdx][1] = opNum;
                            }
                        }
                    }
                }
            }
        } catch (final IOException | NumberFormatException ignored) {
        }

        final List<int[]> result = new ArrayList<>(testCount);
        for (int i = 0; i < testCount; i++) {
            result.add(bounds[i]);
        }
        return result;
    }

    /**
     * Resamples raw telemetry items belonging to a single test to match a fixed target count.
     *
     * @param rawItems    raw telemetry items for the test
     * @param targetCount desired number of samples
     * @param testNum     test identifier
     * @return resampled list of SystemResourceItem records tagged with testNum
     */
    public static List<SystemResourceItem> resampleTestTelemetry(
            final List<SystemResourceItem> rawItems, final int targetCount, final int testNum) {
        if (targetCount <= 0) return List.of();
        final List<SystemResourceItem> resampled = new ArrayList<>(targetCount);
        final int R = (null == rawItems) ? 0 : rawItems.size();

        if (R == 0) {
            for (int i = 0; i < targetCount; i++) {
                resampled.add(new SystemResourceItem(i + 1, 0.0, 0.0, 0.0, 0.0, 0, testNum));
            }
            return resampled;
        }

        if (R == targetCount) {
            for (final SystemResourceItem item : rawItems) {
                resampled.add(new SystemResourceItem(item.sampleNum, item.cpuPct, item.maxCpuPct,
                        item.usedRamMb, item.totalRamMb, item.threads, testNum));
            }
            return resampled;
        }

        if (R > targetCount) {
            for (int i = 0; i < targetCount; i++) {
                final int startIndex = (int) ((long) i * R / targetCount);
                int endIndex = (int) ((long) (i + 1) * R / targetCount);
                endIndex = Math.max(startIndex + 1, Math.min(R, endIndex));

                double sumCpu = 0.0;
                double maxCpu = 0.0;
                double sumUsedRam = 0.0;
                double sumTotalRam = 0.0;
                long sumThreads = 0L;
                final int count = endIndex - startIndex;

                for (int j = startIndex; j < endIndex; j++) {
                    final SystemResourceItem item = rawItems.get(j);
                    sumCpu += item.cpuPct;
                    maxCpu = Math.max(maxCpu, Math.max(item.cpuPct, item.maxCpuPct));
                    sumUsedRam += item.usedRamMb;
                    sumTotalRam += item.totalRamMb;
                    sumThreads += item.threads;
                }

                final int midSampleNum = rawItems.get((startIndex + endIndex) / 2).sampleNum;
                final double avgCpu = sumCpu / count;
                final double avgUsedRam = sumUsedRam / count;
                final double avgTotalRam = sumTotalRam / count;
                final int avgThreads = (int) Math.round((double) sumThreads / count);

                resampled.add(new SystemResourceItem(midSampleNum, avgCpu, maxCpu, avgUsedRam, avgTotalRam, avgThreads, testNum));
            }
            return resampled;
        }

        for (int i = 0; i < targetCount; i++) {
            final int idx = Math.min(R - 1, (int) ((long) i * R / targetCount));
            final SystemResourceItem src = rawItems.get(idx);
            resampled.add(new SystemResourceItem(src.sampleNum, src.cpuPct, src.maxCpuPct,
                    src.usedRamMb, src.totalRamMb, src.threads, testNum));
        }
        return resampled;
    }

    /**
     * Downsamples telemetry resource items into uniform buckets with peak CPU tracking.
     *
     * @param rawItems      raw telemetry items
     * @param targetDensity maximum number of samples to retain
     * @return downsampled list of SystemResourceItem records
     */
    public static List<SystemResourceItem> downsampleTelemetry(final List<SystemResourceItem> rawItems, final int targetDensity) {
        if (null == rawItems || rawItems.size() <= targetDensity || targetDensity <= 0) {
            return (null == rawItems) ? List.of() : rawItems;
        }

        final int N = rawItems.size();
        final int M = targetDensity;
        final List<SystemResourceItem> downsampled = new ArrayList<>(M);

        for (int i = 0; i < M; i++) {
            final int startIndex = (int) ((long) i * N / M);
            int endIndex = (int) ((long) (i + 1) * N / M);
            endIndex = Math.max(startIndex + 1, Math.min(N, endIndex));

            double sumCpu = 0.0;
            double maxCpu = 0.0;
            double sumUsedRam = 0.0;
            double sumTotalRam = 0.0;
            long sumThreads = 0L;
            final int count = endIndex - startIndex;

            for (int j = startIndex; j < endIndex; j++) {
                final SystemResourceItem item = rawItems.get(j);
                sumCpu += item.cpuPct;
                maxCpu = Math.max(maxCpu, Math.max(item.cpuPct, item.maxCpuPct));
                sumUsedRam += item.usedRamMb;
                sumTotalRam += item.totalRamMb;
                sumThreads += item.threads;
            }

            final int midSampleNum = rawItems.get((startIndex + endIndex) / 2).sampleNum;
            final double avgCpu = sumCpu / count;
            final double avgUsedRam = sumUsedRam / count;
            final double avgTotalRam = sumTotalRam / count;
            final int avgThreads = (int) Math.round((double) sumThreads / count);

            downsampled.add(new SystemResourceItem(midSampleNum, avgCpu, maxCpu, avgUsedRam, avgTotalRam, avgThreads));
        }

        return downsampled;
    }
}
