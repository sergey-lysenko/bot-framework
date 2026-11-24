package works.lysenko.base.eta.model;

import java.util.Objects;

/**
 * Configuration fingerprint identifying compatible test runs for historical ETA calibration.
 *
 * @param test             active test suite name or fallback root
 * @param tests            configured test count string (or empty)
 * @param exhaustive       whether exhaustive mode is enabled
 * @param exhaustivePasses target passes per leaf scenario
 * @param root             configured scenario root package
 * @param app              target mobile application path/name (or empty)
 * @param pauseLength      configured pause duration in milliseconds
 * @param platform         target execution platform identifier
 */
public record EtaMetadata(
        String test,
        String tests,
        boolean exhaustive,
        int exhaustivePasses,
        String root,
        String app,
        int pauseLength,
        String platform) {

    /**
     * Checks whether another configuration fingerprint matches all calibration parameters.
     *
     * @param other the metadata instance to compare against
     * @return {@code true} if all configuration fields match
     */
    public boolean matches(final EtaMetadata other) {
        if (null == other) return false;
        return Objects.equals(test, other.test)
                && Objects.equals(tests, other.tests)
                && exhaustive == other.exhaustive
                && exhaustivePasses == other.exhaustivePasses
                && Objects.equals(root, other.root)
                && Objects.equals(app, other.app)
                && pauseLength == other.pauseLength
                && Objects.equals(platform, other.platform);
    }
}
