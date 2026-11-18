package works.lysenko.util.prop.tree;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a visual row (vertical) offset for a scenario in the rendered HTML tree.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VisualRowOffset {
    int value() default 0;
}
