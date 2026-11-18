package works.lysenko.base.ui;

import org.apache.commons.lang3.StringUtils;

import static works.lysenko.util.spec.Symbols._LFD_;

/**
 * Utility class for operations on arrays.
 */
public record Arrays() {

    /**
     * Converts an array of StackTraceElement objects into a single string with each element separated by a line feed.
     *
     * @param stackTrace an array of StackTraceElement representing a stack trace
     * @return a string representation of the stack trace, with each element separated by a line feed
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    static String toLines(final StackTraceElement[] stackTrace) {

        return StringUtils.join(stackTrace, _LFD_);
    }
}
