package works.lysenko.base.output;

import works.lysenko.util.spec.PropEnum;

/**
 * Run-scoped snapshot of progression configuration.
 */
public record ProgressionSettings(
        boolean scenarioEnabled,
        boolean treeEnabled,
        boolean treePerNodeEnabled,
        int maxFrames,
        int maxFramePixels,
        int maxTotalPixels,
        boolean scenarioGifEnabled,
        boolean scenarioWebpEnabled,
        boolean scenarioMp4Enabled,
        boolean treeGifEnabled,
        boolean treeWebpEnabled,
        boolean treeMp4Enabled,
        String treeSonification,
        String ffmpeg) {

    /**
     * @return all accepted values of the tree sonification property: {@code none} plus every registered mode
     */
    public static java.util.List<String> treeSonificationValues() {
        final java.util.List<String> values = new java.util.ArrayList<>();
        values.add("none");
        values.addAll(TreeProgressionAudio.modes());
        return values;
    }

    /**
     * @return sonifier selected by the tree sonification property, if any
     */
    public java.util.Optional<TreeSonifier> treeSonifier() {
        return TreeProgressionAudio.find(treeSonification);
    }

    public boolean treeSonificationEnabled() {
        return treeSonifier().isPresent();
    }

    private static volatile ProgressionSettings current;

    public static synchronized void initialize() {
        final Boolean scenarioEnabled = PropEnum._TEST_REPORT_PROGRESSION_SCENARIO.get();
        final Boolean treeEnabled = PropEnum._TEST_REPORT_PROGRESSION_TREE.get();
        final Boolean treePerNodeEnabled = PropEnum._TEST_REPORT_PROGRESSION_TREE_PER_NODE.get();
        final Integer maxFrames = PropEnum._TEST_REPORT_PROGRESSION_MAX_FRAMES.get();
        final Integer maxFramePixels = PropEnum._TEST_REPORT_PROGRESSION_MAX_FRAME_PIXELS.get();
        final Integer maxTotalPixels = PropEnum._TEST_REPORT_PROGRESSION_MAX_TOTAL_PIXELS.get();
        final Boolean scenarioGifEnabled = PropEnum._TEST_REPORT_PROGRESSION_SCENARIO_GIF.get();
        final Boolean scenarioWebpEnabled = PropEnum._TEST_REPORT_PROGRESSION_SCENARIO_WEBP.get();
        final Boolean scenarioMp4Enabled = PropEnum._TEST_REPORT_PROGRESSION_SCENARIO_MP4.get();
        final Boolean treeGifEnabled = PropEnum._TEST_REPORT_PROGRESSION_TREE_GIF.get();
        final Boolean treeWebpEnabled = PropEnum._TEST_REPORT_PROGRESSION_TREE_WEBP.get();
        final Boolean treeMp4Enabled = PropEnum._TEST_REPORT_PROGRESSION_TREE_MP4.get();
        final String treeSonification = PropEnum._TEST_REPORT_PROGRESSION_TREE_SONIFICATION.get();
        final String ffmpeg = PropEnum._TEST_REPORT_PROGRESSION_FFMPEG.get();
        current = new ProgressionSettings(
                Boolean.TRUE.equals(scenarioEnabled),
                Boolean.TRUE.equals(treeEnabled),
                Boolean.TRUE.equals(treePerNodeEnabled),
                maxFrames,
                maxFramePixels,
                maxTotalPixels,
                Boolean.TRUE.equals(scenarioGifEnabled),
                Boolean.TRUE.equals(scenarioWebpEnabled),
                Boolean.TRUE.equals(scenarioMp4Enabled),
                Boolean.TRUE.equals(treeGifEnabled),
                Boolean.TRUE.equals(treeWebpEnabled),
                Boolean.TRUE.equals(treeMp4Enabled),
                treeSonification,
                ffmpeg);
    }

    public static ProgressionSettings current() {
        ProgressionSettings settings = current;
        if (null == settings) {
            synchronized (ProgressionSettings.class) {
                settings = current;
                if (null == settings) {
                    initialize();
                    settings = current;
                }
            }
        }
        return settings;
    }
}
