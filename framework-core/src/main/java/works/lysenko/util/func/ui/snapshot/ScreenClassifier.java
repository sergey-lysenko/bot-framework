package works.lysenko.util.func.ui.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Reusable, priority-ordered screen classifier evaluated against a single {@link UiSnapshot}.
 * <p>
 * Rules are evaluated in registration order; the first matching rule determines the screen.
 * Put overlay/dialog/sub-screen rules before base screen rules when background views remain
 * present in the accessibility hierarchy.
 *
 * @param <S> screen enum or descriptor type
 */
public final class ScreenClassifier<S> {

    private final S fallback;
    private final List<Rule<S>> rules;

    private ScreenClassifier(final S fallback, final List<Rule<S>> rules) {

        this.fallback = fallback;
        this.rules = List.copyOf(rules);
    }

    /**
     * Creates a new {@link Builder} with the given fallback screen (e.g. {@code Screen.UNKNOWN}).
     *
     * @param fallback value returned when no rule matches
     * @param <S>      screen type
     * @return builder instance
     */
    public static <S> Builder<S> builder(final S fallback) {

        return new Builder<>(fallback);
    }

    /**
     * Classifies the provided snapshot in one local pass (zero driver round-trips).
     *
     * @param snapshot captured UI snapshot
     * @return first matching screen, or the configured fallback
     */
    public S classify(final UiSnapshot snapshot) {

        if (null == snapshot || snapshot.isEmpty()) {
            return fallback;
        }
        for (final Rule<S> rule : rules) {
            if (rule.predicate().test(snapshot)) {
                return rule.screen();
            }
        }
        return fallback;
    }

    /**
     * Classifies the current screen using the short-TTL cached {@link Snapshots#snapshot()}.
     *
     * @return detected screen
     */
    public S current() {

        return classify(Snapshots.snapshot());
    }

    /**
     * Classifies the current screen using a freshly captured {@link Snapshots#freshSnapshot()}.
     *
     * @return detected screen
     */
    public S currentFresh() {

        return classify(Snapshots.freshSnapshot());
    }

    private record Rule<S>(S screen, Predicate<UiSnapshot> predicate) {}

    /**
     * Fluent builder for {@link ScreenClassifier}.
     *
     * @param <S> screen type
     */
    public static final class Builder<S> {

        private final S fallback;
        private final List<Rule<S>> rules = new ArrayList<>();

        private Builder(final S fallback) {

            this.fallback = Objects.requireNonNull(fallback, "fallback must not be null");
        }

        /**
         * Registers a screen classification rule evaluated in declaration order.
         *
         * @param screen    screen returned when {@code predicate} matches
         * @param predicate predicate evaluated against a {@link UiSnapshot}
         * @return this builder
         */
        public Builder<S> when(final S screen, final Predicate<UiSnapshot> predicate) {

            Objects.requireNonNull(screen, "screen must not be null");
            Objects.requireNonNull(predicate, "predicate must not be null");
            rules.add(new Rule<>(screen, predicate));
            return this;
        }

        /**
         * Convenience rule matching when any of the given resource IDs (short or full) is displayed.
         *
         * @param screen screen returned when any ID is present
         * @param ids    resource IDs (short or full)
         * @return this builder
         */
        public Builder<S> whenAnyId(final S screen, final String... ids) {

            return when(screen, snap -> snap.hasAnyId(ids));
        }

        /**
         * Builds the immutable {@link ScreenClassifier}.
         *
         * @return immutable classifier
         */
        public ScreenClassifier<S> build() {

            return new ScreenClassifier<>(fallback, rules);
        }
    }
}
