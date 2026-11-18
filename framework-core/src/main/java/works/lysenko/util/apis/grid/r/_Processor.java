package works.lysenko.util.apis.grid.r;

import works.lysenko.util.grid.record.Request;

/**
 * Represents an interface for a Processor that processes negative results and updates expected property values.
 */
public interface _Processor {

    /**
     * Processes the negative result of a validation request.
     *
     * @param vr The validation request to process.
     */
    void processNegativeResult(Request vr);

    /**
     * Processes the positive result of a validation request.
     *
     * @param vr The validation request to process.
     */
    void processPositiveResult(Request vr);

    /**
     * Updates the expected property value for a given name.
     *
     * @param name             The name of the property.
     * @param newPropertyValue The new property value.
     */
    void updateExpected(String name, String newPropertyValue);

}
