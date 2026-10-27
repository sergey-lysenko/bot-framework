package works.lysenko.base.output.loghtml.sidecar;

import works.lysenko.base.output.loghtml.LogModels.SystemResourceItem;
import works.lysenko.util.prop.PropEnum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads CPU and system resource telemetry logs and performs bounds scanning,
 * resampling, and downsampling.
 */
public final class TelemetryLogLoader {

    private TelemetryLogLoader() {
    }

    public static Map<Integer, Double> loadTelemetryCpuMap(final File runLogFile) {
        final Map<Integer, Double> cpuMap = new HashMap<>();
        if (null == runLogFile || null == runLogFile.getParentFile()) return cpuMap;
        final String runLogName = runLogFile.getName();
        final String telemName = runLogName.replace(".run.log", ".telemetry.log");
        final File telemFile = new File(runLogFile.getParentFile(), telemName);
        if (!telemFile.exists()) return cpuMap;

        double lastVal = 0.0;
        int lastCores = Runtime.getRuntime().availableProcessors();
        int sampleNum = 0;
        try (final BufferedReader br = new BufferedReader(new FileReader(telemFile))) {
            String line;
            while (null != (line = br.readLine())) {
                final String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                final String[] parts = trimmed.split(",");
                if (parts.length >= 3) {
                    sampleNum++;
                    final String valStr = parts[2].trim();
                    if (!"-".equals(valStr)) {
                        try {
                            lastVal = Double.parseDouble(valStr);
                        } catch (final NumberFormatException ignored) {
                        }
                    }
                    if (parts.length >= 5) {
                        final String coresStr = parts[4].trim();
                        if (!"-".equals(coresStr)) {
                            try {
                                final int parsedCores = Integer.parseInt(coresStr);
                                if (parsedCores > 0) lastCores = parsedCores;
                            } catch (final NumberFormatException ignored) {
                            }
                        }
                    }
                    double normalizedVal = lastVal;
                    if (normalizedVal > 100.0 && lastCores > 0) {
                        normalizedVal /= lastCores;
                    }

                    cpuMap.put(sampleNum, normalizedVal);
                }
            }
        } catch (final IOException ignored) {
        }
        return cpuMap;
    }

    public static List<Double> loadTelemetryCpu(final File runLogFile) {
        final List<Double> cpuLoads = new ArrayList<>();
        if (null == runLogFile || null == runLogFile.getParentFile()) return cpuLoads;
        final String runLogName = runLogFile.getName();
        final String telemName = runLogName.replace(".run.log", ".telemetry.log");
        final File telemFile = new File(runLogFile.getParentFile(), telemName);
        if (!telemFile.exists()) return cpuLoads;

        double lastVal = 0.0;
        int lastCores = Runtime.getRuntime().availableProcessors();
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
                    if (parts.length >= 5) {
                        final String coresStr = parts[4].trim();
                        if (!"-".equals(coresStr)) {
                            try {
                                final int parsedCores = Integer.parseInt(coresStr);
                                if (parsedCores > 0) lastCores = parsedCores;
                            } catch (final NumberFormatException ignored) {
                            }
                        }
                    }
                    double normalizedVal = lastVal;
                    if (normalizedVal > 100.0 && lastCores > 0) {
                        normalizedVal /= lastCores;
                    }
                    cpuLoads.add(normalizedVal);
                }
            }
        } catch (final IOException ignored) {
        }
        return cpuLoads;
    }

    public static List<SystemResourceItem> loadTelemetryResources(final File runLogFile) {
        return loadTelemetryResources(runLogFile, 0);
    }

    public static List<SystemResourceItem> loadTelemetryResources(final File runLogFile, final int testCount) {
        final int[] bounds = findTestOpBounds(runLogFile);
        return loadTelemetryResources(runLogFile, testCount, bounds[0], bounds[1]);
    }

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
        int lastCores = Runtime.getRuntime().availableProcessors();
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
                    final String coresStr = parts.length >= 5 ? parts[4].trim() : "-";
                    final String threadsStr = parts[5].trim();
                    final String freeStr = parts[9].trim();
                    final String totalStr = parts[10].trim();

                    if (!"-".equals(coresStr)) {
                        try {
                            final int parsedCores = Integer.parseInt(coresStr);
                            if (parsedCores > 0) lastCores = parsedCores;
                        } catch (final NumberFormatException ignored) {}
                    }
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

                    double normalizedCpu = lastCpu;
                    if (normalizedCpu > 100.0 && lastCores > 0) {
                        normalizedCpu /= lastCores;
                    }

                    final double usedMb = Math.max(0.0, (lastTotalM - lastFreeM) / (1024.0 * 1024.0));
                    final double totalMb = Math.max(0.0, lastTotalM / (1024.0 * 1024.0));
                    items.add(new SystemResourceItem(sampleNum, normalizedCpu, usedMb, totalMb, lastThreads));
                }
            }
        } catch (final IOException ignored) {
        }

        List<SystemResourceItem> trimmedItems = trimTelemetryToTestPeriod(items, minTestOp, maxTestOp);

        if (testCount > 0 && !trimmedItems.isEmpty()) {
            final List<int[]> perTestBounds = findTestOpBoundsPerTest(runLogFile, testCount);
            if (!perTestBounds.isEmpty()) {
                final List<SystemResourceItem> taggedItems = new ArrayList<>(trimmedItems.size());
                for (final SystemResourceItem item : trimmedItems) {
                    int testNum = 0;
                    for (int k = 0; k < perTestBounds.size(); k++) {
                        final int[] bounds = perTestBounds.get(k);
                        if (item.sampleNum >= bounds[0] && item.sampleNum <= bounds[1]) {
                            testNum = k + 1;
                            break;
                        }
                    }
                    taggedItems.add(new SystemResourceItem(item.sampleNum, item.cpuPct, item.maxCpuPct,
                            item.usedRamMb, item.totalRamMb, item.threads, testNum));
                }
                trimmedItems = taggedItems;
            }
        }

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

            final SystemResourceItem midItem = rawItems.get((startIndex + endIndex) / 2);
            final int midSampleNum = midItem.sampleNum;
            final int midTestNum = midItem.testNum;
            final double avgCpu = sumCpu / count;
            final double avgUsedRam = sumUsedRam / count;
            final double avgTotalRam = sumTotalRam / count;
            final int avgThreads = (int) Math.round((double) sumThreads / count);

            downsampled.add(new SystemResourceItem(midSampleNum, avgCpu, maxCpu, avgUsedRam, avgTotalRam, avgThreads, midTestNum));
        }

        return downsampled;
    }
}
