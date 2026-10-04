package works.lysenko.base.output;

import works.lysenko.util.spec.PropEnum;

/**
 * Run-scoped snapshot of progression configuration.
 */
public record ProgressionSettings(
        boolean scenarioEnabled,
        boolean treeEnabled,
        int maxFrames,
        int maxFramePixels,
        int maxTotalPixels,
        boolean scenarioMp4Enabled,
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
        final Boolean scenarioEnabled = PropEnum._PROGRESSION_SCENARIO.get();
        final Boolean treeEnabled = PropEnum._PROGRESSION_TREE.get();
        final Integer maxFrames = PropEnum._PROGRESSION_MAX_FRAMES.get();
        final Integer maxFramePixels = PropEnum._PROGRESSION_MAX_FRAME_PIXELS.get();
        final Integer maxTotalPixels = PropEnum._PROGRESSION_MAX_TOTAL_PIXELS.get();
        final Boolean scenarioMp4Enabled = PropEnum._PROGRESSION_SCENARIO_MP4.get();
        final Boolean treeMp4Enabled = PropEnum._PROGRESSION_TREE_MP4.get();
        final String treeSonification = PropEnum._PROGRESSION_TREE_SONIFICATION.get();
        final String ffmpeg = PropEnum._PROGRESSION_FFMPEG.get();
        current = new ProgressionSettings(
                Boolean.TRUE.equals(scenarioEnabled),
                Boolean.TRUE.equals(treeEnabled),
                maxFrames,
                maxFramePixels,
                maxTotalPixels,
                Boolean.TRUE.equals(scenarioMp4Enabled),
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
