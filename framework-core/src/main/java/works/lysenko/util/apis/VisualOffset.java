package works.lysenko.util.apis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a visual column offset for a scenario in the rendered HTML tree.
 * This shifts the scenario and its subtree horizontally by the specified number of columns,
 * without affecting its logical depth in the execution tree.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VisualOffset {
    int value() default 0;
}
