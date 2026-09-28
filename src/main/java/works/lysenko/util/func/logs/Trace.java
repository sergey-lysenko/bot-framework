package works.lysenko.util.func.logs;

import works.lysenko.Base;
import works.lysenko.util.apis.log._Traceable;
import works.lysenko.util.data.records.KeyValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.isNull;
import static works.lysenko.Base.isTrace;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols.FAT_BUL;
import static works.lysenko.util.spec.Symbols._COLON_;

/**
 * The Trace record provides a utility for logging method parameters and their values
 * at runtime for debugging and tracing purposes. It extracts information about the calling
 * method from the runtime stack trace and logs its parameters.
 * <p>
 * This class requires the calling method to have parameter names available and expects
 * the provided parameter values array to exactly match the method's parameter definitions
 * in both count and order.
 */
@SuppressWarnings({"CallToPrintStackTrace", "ProhibitedExceptionThrown", "CallToSuspiciousStringMethod",
        "LocalVariableNamingConvention", "BreakStatement", "MethodWithMultipleReturnPoints", "ObjectAllocationInLoop"})
public record Trace() {

    @SuppressWarnings("StandardVariableNames")
    private static Method getCallingMethod() {

        try {
            final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String callingClassName = null;
            String callingMethodName = null;

            // Find the method in the stack calling Trace.log()
            for (final StackTraceElement element : stack) {
                if (!element.getClassName().equals(Trace.class.getName())
                        && !element.getClassName().startsWith("java.lang.")) {
                    callingClassName = element.getClassName();
                    callingMethodName = element.getMethodName();
                    break;
                }
            }

            if (isAnyNull(callingClassName, callingMethodName)) return null;

            final Class<?> clazz = Class.forName(callingClassName);
            final String finalCallingMethodName = callingMethodName;
            return Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.getName().equals(finalCallingMethodName))
                    .findFirst()
                    .orElse(null);
        } catch (final ClassNotFoundException | SecurityException e) {
            throw new RuntimeException(e);
        } catch (final RuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Logs the details of the calling method along with its parameters and their values for debugging purposes.
     * The method retrieves the calling method from the runtime stack trace, extracts the parameters' names and values,
     * and logs them in a structured format.
     *
     * @param data The _Traceable object containing the parameter names and values of the calling method.
     *             It provides methods to retrieve the number of parameters, each parameter's name,
     *             and each parameter's value.
     * @throws IllegalStateException If the calling method cannot be determined from the runtime stack trace.
     */
    public static void log(final _Traceable data) {

        if (isTrace()) {

            final Method callingMethod = getCallingMethod();

            if (isNull(callingMethod)) throw new IllegalStateException("Unable to determine the calling method.");

            final List<KeyValue<?, ?>> list = new ArrayList<>(ZERO);
            for (int i = 0; i < data.size(); i++) {
                final String paramName = data.name(i);
                final Object paramValue = data.value(i);
                list.add(kv(paramName, paramValue));
            }
            Base.logDebug(b(s(FAT_BUL), s(callingMethod, _COLON_), a(list, COMMA_SPACE)));
        }
    }

}
