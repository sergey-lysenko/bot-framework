package works.lysenko.util.apis.action;

/**
 * The ProvidesCode interface is used to define objects that provide a code value.
 * Classes implementing this interface must implement the getCode() method.
 */
@FunctionalInterface
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface ProvidesCode {

    /**
     * Returns the code value associated with this object.
     *
     * @return the code value of this object
     */
    int getCode();
}
