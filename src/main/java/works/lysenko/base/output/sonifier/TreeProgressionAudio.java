package works.lysenko.base.output.sonifier;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Registry of available tree progression sonifiers, selected by the {@code .progression.tree.sonification} value.
 * To add a variant: implement {@link TreeSonifier} and list it in {@link #ALL}.
 */
public final class TreeProgressionAudio {

    private static final List<TreeSonifier> ALL = List.of(new CopilotSonifier(), new ClaudeSonifier(), new GeminiSonifier());

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
}
