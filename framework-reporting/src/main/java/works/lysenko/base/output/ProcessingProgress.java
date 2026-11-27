package works.lysenko.base.output;

import static works.lysenko.util.data.strs.Swap.sn;

/**
 * Reports progress for detached test-run post-processing.
 */
public interface ProcessingProgress {

    ProcessingProgress NONE = (task, percentage) -> { };

    void update(String task, int percentage);

    default void complete(final String task) {
        update(task, 100);
    }

    default void skipped(final String task) {
        update(task, 100);
    }

    default void failed(final String task, final Exception failure) {
        System.err.println(sn("Failed to process ", task, ": ", failure.getMessage()));
        update(task, 100);
    }

    default void close() {
    }
}
