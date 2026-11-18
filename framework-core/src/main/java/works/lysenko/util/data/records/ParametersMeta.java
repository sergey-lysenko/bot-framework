package works.lysenko.util.data.records;

import java.util.List;
import java.util.Map;

/**
 * Represents metadata for execution parameters rendering and validation.
 *
 * @param defaults             sorted map containing default key-value parameter pairs
 * @param unknownKeys          list collecting unknown parameter keys encountered
 * @param expectedDefaultsSize total number of known parameters (standard + additional) before rendering
 * @param additionalCount      number of declared additional parameters
 */
public record ParametersMeta(
        Map<String, String> defaults,
        List<String> unknownKeys,
        int expectedDefaultsSize,
        int additionalCount) {
}
