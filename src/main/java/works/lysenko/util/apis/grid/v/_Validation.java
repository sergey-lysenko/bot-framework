package works.lysenko.util.apis.grid.v;

import works.lysenko.util.data.type.sets.GridValidationResults;

/**
 * Interface for performing validation operations and getting the validation result.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Validation {

    /**
     * Performs the validation operation and generates the validation result.
     */
    void perform();

    /**
     * Gets the result of a grid validation operation.
     *
     * @return the GridValidationResult representing the result of the validation
     */
    GridValidationResults results();
}
