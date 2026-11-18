package works.lysenko.util.func.core;

/**
 * Legacy delegate preserved for backwards compatibility.
 *
 * @deprecated Use {@link Callstack} instead.
 */
@Deprecated
public record Stacktrace() {

    /**
     * Clears the in-memory call stack reference table.
     */
    public static void clearReferences() {

        Callstack.clearReferences();
    }

    /**
     * Returns a compact one-liner call stack by filtering out ignored classes and methods.
     *
     * @param stack           the array of StackTraceElement representing the call stack
     * @param forceUnfiltered if true, the filtering will be ignored and all callers will be included
     * @return a String containing the shortened call stack
     */
    public static String getShort(final StackTraceElement[] stack, final boolean forceUnfiltered) {

        return Callstack.getShort(stack, forceUnfiltered);
    }

    /**
     * @param stack to shorten
     * @return shortened call stack
     */
    public static String getShort(final StackTraceElement[] stack) {

        return Callstack.getShort(stack);
    }
}
