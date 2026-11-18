package works.lysenko.base.output.loghtml.parser;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import static works.lysenko.base.output.loghtml.LogFormatting.escapeHtml;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * Utility class for parsing media links from log files.
 */
public final class LogMediaLinks {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger
            .getLogger(LogMediaLinks.class.getName());

    private LogMediaLinks() {
    }

    /**
     * Represents the configuration for building media links.
     *
     * @param scenarioProgressionEnabled scenario progression flag
     * @param treeProgressionEnabled     tree progression flag
     * @param scenarioMp4Enabled         scenario mp4 flag
     * @param treeMp4Enabled             tree mp4 flag
     */
    public record MediaLinksConfig(
            boolean scenarioProgressionEnabled,
            boolean treeProgressionEnabled,
            boolean scenarioMp4Enabled,
            boolean treeMp4Enabled
    ) {
    }

    /**
     * Result of media links generation.
     */
    public static class MediaLinksResult {

        /**
         * Progression link.
         */
        private String progressionLink = "";
        /**
         * Progression webp link.
         */
        private String progressionWebpLink = "";
        /**
         * Progression mp4 link.
         */
        private String progressionMp4Link = "";
        /**
         * Tree progression link.
         */
        private String treeProgressionLink = "";
        /**
         * Tree progression webp link.
         */
        private String treeProgressionWebpLink = "";
        /**
         * Tree progression mp4 link.
         */
        private String treeProgressionMp4Link = "";
        /**
         * Timestamp block.
         */
        private String timestampBlock = "";
        /**
         * Base prefix.
         */
        private String basePrefix = "";

        /**
         * @return progression link
         */
        public String getProgressionLink() {
            return progressionLink;
        }

        /**
         * @return progression webp link
         */
        public String getProgressionWebpLink() {
            return progressionWebpLink;
        }

        /**
         * @return progression mp4 link
         */
        public String getProgressionMp4Link() {
            return progressionMp4Link;
        }

        /**
         * @return tree progression link
         */
        public String getTreeProgressionLink() {
            return treeProgressionLink;
        }

        /**
         * @return tree progression webp link
         */
        public String getTreeProgressionWebpLink() {
            return treeProgressionWebpLink;
        }

        /**
         * @return tree progression mp4 link
         */
        public String getTreeProgressionMp4Link() {
            return treeProgressionMp4Link;
        }

        /**
         * @return timestamp block
         */
        public String getTimestampBlock() {
            return timestampBlock;
        }

        /**
         * @return base prefix
         */
        public String getBasePrefix() {
            return basePrefix;
        }
    }

    /**
     * Builds media links from a log file.
     *
     * @param logFile the log file
     * @param config  the configuration for building media links
     * @return a populated {@link MediaLinksResult}
     */
    public static MediaLinksResult build(final File logFile,
                                         final MediaLinksConfig config) {
        LOG.finest("Building media links for " + logFile);
        final MediaLinksResult r = new MediaLinksResult();
        final String runLogName = (null != logFile) ? logFile.getName() : "";
        r.basePrefix = runLogName.replace(".run.log", "");

        r.progressionLink = buildProgLink(logFile, r.basePrefix,
                ".progression.gif",
                config.scenarioProgressionEnabled(),
                "Scenario Coverage Progression Animation (10 fps)",
                "Progression", "#38bdf8");

        r.progressionWebpLink = buildProgLink(logFile, r.basePrefix,
                ".progression.webp",
                config.scenarioProgressionEnabled(),
                "Scenario Coverage Progression Animation (WebP)",
                "Progression WebP", "#38bdf8");

        r.progressionMp4Link = buildProgLink(logFile, r.basePrefix,
                ".progression.mp4",
                config.scenarioProgressionEnabled()
                        && config.scenarioMp4Enabled(),
                "Scenario Coverage Progression Animation (MP4)",
                "Progression MP4", "#38bdf8");

        r.treeProgressionLink = buildProgLink(logFile, r.basePrefix,
                ".tree.progression.gif",
                config.treeProgressionEnabled(),
                "Scenario Tree Progression Animation (10 fps)",
                "Tree Progression", "#34d399");

        r.treeProgressionWebpLink = buildProgLink(logFile, r.basePrefix,
                ".tree.progression.webp",
                config.treeProgressionEnabled(),
                "Scenario Tree Progression Animation (WebP)",
                "Tree WebP", "#34d399");

        r.treeProgressionMp4Link = buildProgLink(logFile, r.basePrefix,
                ".tree.progression.mp4",
                config.treeProgressionEnabled()
                        && config.treeMp4Enabled(),
                "Scenario Tree Progression Animation (MP4)",
                "Tree MP4", "#34d399");

        r.timestampBlock = buildTimestamp(r.basePrefix);

        return r;
    }

    private static String buildProgLink(final File logFile,
                                        final String prefix,
                                        final String suffix,
                                        final boolean enabled,
                                        final String title,
                                        final String btnText,
                                        final String color) {
        if (!enabled || null == logFile || null == logFile.getParentFile()) {
            return "";
        }
        final File file = new File(logFile.getParentFile(), s(prefix, suffix));
        if (!file.exists()) {
            return "";
        }
        return s("<a class=\"btn-link\" href=\"", escapeHtml(file.getName()),
                "\" target=\"_blank\" title=\"", title,
                "\"><button type=\"button\" style=\"border-color: ", color,
                "; color: ", color, ";\">", btnText, "</button></a>");
    }

    private static String buildTimestamp(final String basePrefix) {
        String timeStr = "";
        try {
            final long epoch = Long.parseLong(basePrefix);
            final DateTimeFormatter dtf = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            timeStr = dtf.format(Instant.ofEpochMilli(epoch));
        } catch (final Exception ignored) {
            // Ignored
        }

        return timeStr.isEmpty() ? "" : s(
                "    <div class=\"header-timestamp\">",
                "\uD83D\uDD52 ", timeStr,
                "<span class=\"ts-val\">(", basePrefix, ")</span></div>");
    }
}
