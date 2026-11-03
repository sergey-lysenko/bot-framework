package works.lysenko.base.responses;

/**
 * Record representing an expected browser response or negative test side-effect.
 *
 * @param scenario the scenario pattern
 * @param pattern  the log pattern regex or substring
 * @param reason   the human-readable reason for the expected response
 */
public record ExpectedResponse(
    String scenario,
    String pattern,
    String reason
) {}
