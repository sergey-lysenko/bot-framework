package works.lysenko.base.output;

import works.lysenko.util.apis.core._Reports;

import static works.lysenko.Base.section;

/**
 * The Reports class provides methods for generating and displaying various statistics and reports.
 */
public class Reports implements _Reports {

    public final void run() {

        NewIssues.run();
        KnownIssues.run();
        Tests.run();
        Scenarios.run();
        ResultMarker.run();
    }
}
