package link;

import static link.util.Agenda.*;
import static works.lysenko.base.test.Routines.*;

/**
 * The BotRunner class is responsible for executing the test suites of the application.
 */
public record BotRunner() {

    /**
     * The main method is the entry point of the application.
     *
     * @param args The command line arguments ignored by the application.
     */
    public static void main(final String[] args) {

        run(workflow);
    }
}
