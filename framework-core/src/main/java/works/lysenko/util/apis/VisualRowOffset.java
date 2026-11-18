package works.lysenko.util.apis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a visual row (vertical) offset for a scenario in the rendered HTML tree.
 * This shifts the target vertical position of the scenario in the layout solver.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VisualRowOffset {
    int value() default 0;
}
