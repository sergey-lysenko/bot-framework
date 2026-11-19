package works.lysenko.base.eta.model;

/**
 * Post-run ETA estimation error computed for a single test cycle sample.
 *
 * @param testNum          1-based test cycle number
 * @param passedMs         elapsed run duration in milliseconds at the sample point
 * @param etaMs            estimated remaining duration in milliseconds at the sample point
 * @param projectedTotalMs projected total duration ({@code passedMs + etaMs})
 * @param errorMs          signed estimation error ({@code projectedTotalMs - actualTotalMs})
 * @param errorPercent     signed percentage error relative to {@code actualTotalMs}
 */
public record TestError(
        int testNum,
        long passedMs,
        long etaMs,
        long projectedTotalMs,
        long errorMs,
        double errorPercent) {
}
