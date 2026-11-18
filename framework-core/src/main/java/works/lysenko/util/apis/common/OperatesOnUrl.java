package works.lysenko.util.apis.common;

/**
 * OperatesOnUrl is an interface that provides methods to open URLs.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface OperatesOnUrl {

    /**
     * Opens a given URL.
     *
     * @param u The URL to be opened.
     */
    void open(String u);

    /**
     * Open a page defined by protocol and domain. Protocol parameter could be
     * overridden by directly specifying protocol in the domain parameter value
     *
     * @param p default protocol
     * @param u domain or full url
     */
    void open(String p, String u);


}
