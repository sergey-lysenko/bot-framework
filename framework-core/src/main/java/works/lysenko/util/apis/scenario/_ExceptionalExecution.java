package works.lysenko.util.apis.scenario;

import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.data.records.test.Triplet;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _ExceptionalExecution {

    /**
     * Returns the underlying cause of this exceptional situation.
     *
     * @return the cause of this exceptional execution, or null if the cause is nonexistent or unknown
     */
    Exception exception();

    /**
     * Retrieves the Triplet of Throwables associated with this exceptional execution.
     *
     * @return the Triplet of Throwables
     */
    Triplet triplet();

    /**
     * Determines if the execution is considered to be OK.
     *
     * @param e       the Exceptions that occurred during execution (may be null)
     * @param triplet the triplet of Throwables (ultimate, penultimate, and antepenultimate) (must not be null)
     * @return true if the execution is considered to be OK, false otherwise
     * @throws SafeguardException if an exception occurs during the evaluation of the execution
     */
    boolean isOk(Exception e, Triplet triplet) throws SafeguardException;

    /**
     * Marks exceptional situation as resolved.
     */
    void markAsResolved();

    /**
     * @return true if exceptional situation is unresolved, false otherwise
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean unresolved();
}