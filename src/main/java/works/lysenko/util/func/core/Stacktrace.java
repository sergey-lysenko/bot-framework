package works.lysenko.util.func.core;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.util.prop.core.StackTrace;
import works.lysenko.util.prop.tree.Scenario;

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
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * The Stacktrace class provides methods for manipulating and shortening stack traces.
 * It contains static methods for getting a shortened stack trace and determining if the full stack trace is required.
 */
@SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "MethodWithMultipleReturnPoints", "MethodCallInLoopCondition",
        "FeatureEnvy", "BooleanParameter", "NestedMethodCall", "AutoBoxing", "ChainedMethodCall", "AutoUnboxing",
        "ReturnOfNull", "StaticCollection", "BooleanVariableAlwaysNegated", "ClassIndependentOfModule"})
public record Stacktrace() {

    private static final List<String> references = new ArrayList<>(0);

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

    /**
     * Formats the output by enclosing the callers in brackets.
     *
     * @param callers the collection of callers
     * @return the formatted output string
     */
    private static String formatOutput(final Collection<String> callers) {

        if (callers.isEmpty()) return EMPTY;
        final String stacktrace = e(SQUARE, StringUtils.join(callers, _SPACE_));
        if (!StackTrace.references) return stacktrace;
        if (references.contains(stacktrace)) return e(SQUARE, references.indexOf(stacktrace));
        else references.add(stacktrace);
        return s(references.indexOf(stacktrace), _COLON_, stacktrace);
    }

    /**
     * Returns the class object with the specified name.
     *
     * @param name the fully qualified name of the class
     * @return the class object
     * @throws RuntimeException if the class cannot be found
     */
    private static Class<?> getClassForName(final String name) {

        try {
            return Class.forName(name);
        } catch (final ClassNotFoundException e) {
            // throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Retrieves the class object corresponding to the class name extracted from a given StackTraceElement.
     *
     * @param stackElement the StackTraceElement from which to extract the class name
     * @return the Class object corresponding to the class name
     */
    private static Class<?> getClassForNameFromElement(final StackTraceElement stackElement) {

        final String className = stackElement.getClassName();
        return getClassForName(className);
    }

    /**
     * Returns the short name of a class.
     *
     * @param classObj the class object
     * @return the short name of the class
     */
    private static String getClassName(final Class<?> classObj) {

        return isNull(classObj) ? EMPTY : classObj.getName().split(dot)[classObj.getName().split(dot).length - 1];
    }

    /**
     * Returns the list of ignored items.
     *
     * @return the list of ignored items
     */
    private static List<String> getIgnoredList() {

        if ((isNotNull(exec)) && (isNotNull(core)) && (properties.areTestPropertiesReady()))
            return getListOrEmpty();
        return new ArrayList<>(ZERO);
    }

    private static List<String> getListOrEmpty() {

        return (isNull(Scenario.ignoredList)) ? List.of() : List.of((Scenario.ignoredList.split(s(L1))));
    }

    /**
     * Retrieves the path from the element information of a class object.
     *
     * @param classObj the class object from which to retrieve the element information
     * @return the path from the element information, or an empty string if the element information is not available
     */
    private static String getPathFromElementInfo(final Class<?> classObj) {

        final CodeSource source = classObj.getProtectionDomain().getCodeSource();
        return (null == source) ? EMPTY : source.getLocation().getPath();
    }

    /**
     * Returns a short stack trace by filtering out ignored classes and methods.
     *
     * @param stack           the array of StackTraceElement representing the stack trace
     * @param forceUnfiltered if true, the filtering will be ignored and all callers will be included
     * @return a String containing the short stack trace
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
     * @return shortened stack
     */
    public static String getShort(final StackTraceElement[] stack) {

        return getShort(stack, false);
    }

    /**
     * Determines if the current stack trace should include the full stack trace.
     *
     * @return true if the full stack trace should be included, false otherwise
     */
    private static boolean isFull() {

        if ((isNotNull(exec)) && (isNotNull(core))) return StackTrace.full;
        else return false;
    }

    /**
     * Checks if a given string is not ignored and the path is not blank.
     *
     * @param s    the string to check if it is not ignored
     * @param path the path to check if it is not blank
     * @return true if the string is not in the ignored list and the path is not blank, false otherwise
     */
    private static boolean isNotIgnored(final String s, final String path) {

        final boolean ignored = getIgnoredList().contains(s);
        return (isFull() || (!ignored)) && !path.isBlank();
    }

    /**
     * Checks if there is something left based on the given inputs.
     *
     * @param stack the stack trace elements
     * @param path  the path string
     * @param index the index to check against the length of stack
     * @return true if there is something left, false otherwise
     */
    private static boolean isSomethingLeft(final StackTraceElement[] stack, final String path, final int index) {

        return !path.isBlank() && ((index + 1) < stack.length);
    }
}
