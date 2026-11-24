package works.lysenko.base.output.sonifier;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Registry of available tree progression sonifiers, selected by the {@code .progression.tree.sonification} value.
 * To add a variant: implement {@link TreeSonifier} and list it in {@link #ALL}.
 */
public final class TreeProgressionAudio {

    private static final List<TreeSonifier> ALL = List.of(
            new CopilotSonifier(),
            new ClaudeSonifier(),
            new GeminiSonifier(),
            new OrpheusSonifier(),
            new AuroraSonifier(),
            new CarbonSonifier(),
            new NeonSonifier(),
            new JmjSonifier(),
            new DnbSonifier());

    private TreeProgressionAudio() {
    }

    /**
     * @return modes of all registered sonifiers
     */
    public static List<String> modes() {

        return ALL.stream().map(TreeSonifier::mode).toList();
    }

    /**
     * @param mode property value (case-insensitive)
     * @return matching sonifier, or empty if sonification is disabled or the mode is unknown
     */
    public static Optional<TreeSonifier> find(final String mode) {

        if (null == mode) return Optional.empty();
        final String normalized = mode.trim().toLowerCase(Locale.ROOT);
        return ALL.stream().filter(sonifier -> sonifier.mode().equals(normalized)).findFirst();
    }

    /**
     * Selects a random sonifier mode from a comma-separated list of candidate mode names.
     * Whitespace is trimmed, empty tokens are ignored, and candidates are checked against registered modes.
     *
     * @param candidatesCommaSeparated comma-separated list of mode names, e.g. "dnb, neon, jmj, carbon"
     * @return selected mode name, or "none" if no valid candidates found
     */
    public static String selectRandom(final String candidatesCommaSeparated) {
        if (null == candidatesCommaSeparated || candidatesCommaSeparated.isBlank()) {
            return "none";
        }
        final List<String> available = new java.util.ArrayList<>();
        for (final String part : candidatesCommaSeparated.split(",")) {
            final String trimmed = part.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty() && !"none".equals(trimmed) && find(trimmed).isPresent()) {
                available.add(trimmed);
            }
        }
        if (available.isEmpty()) {
            return "none";
        }
        final int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(available.size());
        return available.get(index);
    }
}
