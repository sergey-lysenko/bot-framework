package works.lysenko.base.eta.model;

/**
 * Per-cycle runtime ETA observation captured during test execution.
 *
 * @param testNum  1-based test cycle number
 * @param passedMs elapsed run duration in milliseconds when sampled
 * @param etaMs    estimated remaining duration in milliseconds when sampled
 */
public record TestSample(int testNum, long passedMs, long etaMs) {
}
