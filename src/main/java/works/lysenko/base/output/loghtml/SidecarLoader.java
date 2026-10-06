package works.lysenko.base.output.loghtml;

import works.lysenko.base.output.loghtml.LogModels.ArtifactItem;
import works.lysenko.base.output.loghtml.LogModels.EtaDebugItem;
import works.lysenko.base.output.loghtml.LogModels.LeafCompletion;
import works.lysenko.base.output.loghtml.LogModels.SystemResourceItem;

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

    private static final Pattern ALL_LEAF_COMPLETION_RE = Pattern.compile("\\[ALL_LEAF_COMPLETION]\\s+(\\d+)(?:\\s+(\\d+))?(?:\\s+(\\d+))?\\s+(.+)$");
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
                    final long atMillis = Long.parseLong(matcher.group(1));
                    final String g2 = matcher.group(2);
                    final String g3 = matcher.group(3);
                    final String leaf = matcher.group(4).trim();

                    long etaMs = 0L;
                    int roundIndex = 0;

                    if (null != g2 && !g2.isEmpty()) {
                        if (null != g3 && !g3.isEmpty()) {
                            etaMs = Long.parseLong(g2);
                            roundIndex = Integer.parseInt(g3);
                        } else {
                            etaMs = Long.parseLong(g2);
                            legacyRoundCount++;
                            roundIndex = legacyRoundCount;
                        }
                    } else {
                        legacyRoundCount++;
                        roundIndex = legacyRoundCount;
                    }

                    completions.add(new LeafCompletion(atMillis, etaMs, roundIndex, leaf));
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
        return items;
    }
}
