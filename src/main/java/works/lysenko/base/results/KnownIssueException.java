package works.lysenko.base.results;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ClassIndependentOfModule", "UncheckedExceptionClass",
        "WeakerAccess", "ClassUnconnectedToPackage", "serial", "unused", "ClassWithoutNoArgConstructor"})
public class KnownIssueException extends RuntimeException {

    /**
     * @param message message to identify the known issue
     */
    @SuppressWarnings({"unused", "PublicConstructor", "LocalCanBeFinal"})
    public KnownIssueException(String message) {

        super(message);
    }

}
