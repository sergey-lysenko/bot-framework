package works.lysenko.util.func.ui;

import static works.lysenko.Base.exec;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.func.type.Numbers.random;
import static works.lysenko.util.func.ui.Descriptors.fill;
import static works.lysenko.util.lang.word.A.ALLOW;
import static works.lysenko.util.lang.B.BUTTON_WITH_TEXT;
import static works.lysenko.util.spec.Numbers.THREE;
import static works.lysenko.util.spec.Waits.LONG_WAIT_FROM;
import static works.lysenko.util.spec.Waits.LONG_WAIT_TO;

/**
 * Represents a record for managing permissions in an application.
 * This class provides functionality to process optional permissions,
 * particularly in handling the appearance and interaction with permission-related UI elements.
 */
@SuppressWarnings({"ClassIndependentOfModule", "unused"})
public record Permission() {

    /**
     * Process any optional permissions that may appear on the screen.
     */
    @SuppressWarnings("MethodCallInLoopCondition")
    public static void processOptionalPermissions() {

        exec.sleep(random(LONG_WAIT_FROM * THREE, LONG_WAIT_TO * THREE));
        final String allow = fill(BUTTON_WITH_TEXT, c(ALLOW));
        while (exec.isPresent(allow)) {
            exec.clickOn(allow);
            exec.sleep(random(LONG_WAIT_FROM, LONG_WAIT_TO));
        }
    }
}