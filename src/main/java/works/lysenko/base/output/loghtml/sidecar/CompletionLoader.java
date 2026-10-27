package works.lysenko.base.output.loghtml.sidecar;

import works.lysenko.base.output.loghtml.LogModels.EtaDebugItem;
import works.lysenko.base.output.loghtml.LogModels.LeafCompletion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * Reads leaf completion entries and ETA debug logs from sidecar files.
 */
public final class CompletionLoader {

    private static final Pattern ALL_LEAF_COMPLETION_RE = Pattern.compile("\\[ALL_LEAF_COMPLETION]\\s+(\\d+)\\s+(\\d+)(?:\\s+(\\d+))?(?:\\s+(\\d+))?\\s+(.+)$");
    private static final Pattern ETA_DEBUG_RE = Pattern.compile("\\[ETA_DEBUG]\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(.+)$");

    private CompletionLoader() {
    }

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

    public static File allLeafCompletionsFile(final File runLogFile) {
        final String suffix = ".run.log";
        final String name = runLogFile.getName();
        final String prefix = name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
        return new File(runLogFile.getParentFile(), s(prefix, ".all-leaf-completions.log"));
    }

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

    public static File etaDebugFile(final File runLogFile) {
        final String suffix = ".run.log";
        final String name = runLogFile.getName();
        final String prefix = name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
        return new File(runLogFile.getParentFile(), s(prefix, ".eta-debug.log"));
    }
}
