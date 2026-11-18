package works.lysenko.util.func.core;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.util.prop.core.StackTrace;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.spec.Layout;

import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.properties;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.data.Regexes.dot;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.prop.data.Delimeters.L1;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._DOT_;
import static works.lysenko.util.spec.Symbols._EQUAL_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * The Callstack record provides methods for formatting, shortening, and indexing compact one-liner call stacks.
 */
@SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "MethodWithMultipleReturnPoints", "MethodCallInLoopCondition",
        "FeatureEnvy", "BooleanParameter", "NestedMethodCall", "AutoBoxing", "ChainedMethodCall", "AutoUnboxing",
        "ReturnOfNull", "StaticCollection", "BooleanVariableAlwaysNegated", "ClassIndependentOfModule"})
public record Callstack() {

    private static final List<String> references = new ArrayList<>(0);
    private static final ThreadLocal<Boolean> CALLSTACK_GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Clears the in-memory call stack reference table.
     */
    public static void clearReferences() {

        references.clear();
    }

    /**
     * Formats the method name based on the stack trace element and the short class name.
     *
     * @param stackElement   the stack trace element representing the method
     * @param shortClassName the short name of the class
     * @return the formatted method name as a string
     */
    private static String formatMethodName(final StackTraceElement stackElement, final String shortClassName) {

        final String method = stackElement.getMethodName();
        final String line = (StackTrace.numbers ? s(_COLON_, s(stackElement.getLineNumber())) : EMPTY);
        return s(shortClassName, _DOT_, method, line);
    }

    private static void saveCallstackReference(final int index, final String callstack) {
        if (Boolean.TRUE.equals(CALLSTACK_GUARD.get())) return;
        CALLSTACK_GUARD.set(Boolean.TRUE);
        try {
            final String filePath = Layout.Files.name(Layout.Templates.RUN_CALLSTACKS_);
            final java.io.File file = new java.io.File(filePath);
            if (null != file.getParentFile() && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            final String entry = s(index, _EQUAL_, callstack, System.lineSeparator());
            java.nio.file.Files.writeString(
                    file.toPath(),
                    entry,
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (final Exception ignored) {
            // Suppressed to prevent failure during callstack formatting
        } finally {
            CALLSTACK_GUARD.set(Boolean.FALSE);
        }
    }

    /**
     * Formats the output by enclosing the callers in brackets and optionally indexing into callstacks.properties.
     *
     * @param callers the collection of callers
     * @return the formatted output string
     */
    private static String formatOutput(final Collection<String> callers) {

        if (callers.isEmpty()) return EMPTY;
        final String callstack = e(SQUARE, StringUtils.join(callers, _SPACE_));
        final boolean useReferences = Boolean.TRUE.equals(StackTrace.references);
        if (!useReferences) return callstack;
        if (references.contains(callstack)) return e(SQUARE, references.indexOf(callstack));
        else {
            references.add(callstack);
            final int index = references.indexOf(callstack);
            saveCallstackReference(index, callstack);
            return s(index, _COLON_, callstack);
        }
    }

    private static Class<?> getClassForName(final String name) {

        try {
            return Class.forName(name);
        } catch (final ClassNotFoundException e) {
            // Ignored when class is synthetic or unavailable
        }
        return null;
    }

    private static Class<?> getClassForNameFromElement(final StackTraceElement stackElement) {

        final String className = stackElement.getClassName();
        return getClassForName(className);
    }

    private static String getClassName(final Class<?> classObj) {

        return isNull(classObj) ? EMPTY : classObj.getName().split(dot)[classObj.getName().split(dot).length - 1];
    }

    private static List<String> getIgnoredList() {

        if ((isNotNull(exec)) && (isNotNull(core)) && (isNotNull(properties)) && (properties.areTestPropertiesReady()))
            return getListOrEmpty();
        return new ArrayList<>(ZERO);
    }

    private static List<String> getListOrEmpty() {

        return (isNull(Scenario.ignoredList)) ? List.of() : List.of((Scenario.ignoredList.split(s(L1))));
    }

    private static String getPathFromElementInfo(final Class<?> classObj) {

        final CodeSource source = classObj.getProtectionDomain().getCodeSource();
        return (null == source) ? EMPTY : source.getLocation().getPath();
    }

    /**
     * Returns a compact one-liner call stack by filtering out ignored classes and methods.
     *
     * @param stack           the array of StackTraceElement representing the call stack
     * @param forceUnfiltered if true, the filtering will be ignored and all callers will be included
     * @return a String containing the shortened call stack
     */
    public static String getShort(final StackTraceElement[] stack, final boolean forceUnfiltered) {

        String path = EMPTY;
        final Collection<String> callers = new ArrayList<>(0);
        int index = 0;
        String className;
        do {
            final StackTraceElement stackElement = stack[++index];
            final Class<?> classObj = getClassForNameFromElement(stackElement);
            className = getClassName(classObj);
            if (isNotNull(classObj)) {
                path = getPathFromElementInfo(classObj);
                if (forceUnfiltered || isNotIgnored(className, path)) callers.add(formatMethodName(stackElement, className));
            }
        } while (isSomethingLeft(stack, path, index));
        return formatOutput(callers);
    }

    /**
     * @param stack to shorten
     * @return shortened call stack
     */
    public static String getShort(final StackTraceElement[] stack) {

        return getShort(stack, false);
    }

    private static boolean isFull() {

        if ((isNotNull(exec)) && (isNotNull(core))) return StackTrace.full;
        else return false;
    }

    private static boolean isNotIgnored(final String s, final String path) {

        final boolean ignored = getIgnoredList().contains(s);
        return (isFull() || (!ignored)) && !path.isBlank();
    }

    private static boolean isSomethingLeft(final StackTraceElement[] stack, final String path, final int index) {

        return !path.isBlank() && ((index + 1) < stack.length);
    }
}
