package works.lysenko.util.apis.common;

/**
 * The SendsKeys interface provides methods to send keys to elements.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "unused"})
public interface SendsKeys {

    /**
     * Send keys as Action
     *
     * @param keys CharSequence to send
     */
    void sendKeys(CharSequence keys);

    /**
     * Send keys as Action defined number of times
     *
     * @param keys CharSequence to send
     * @param i    times to repeat
     */
    void sendKeys(CharSequence keys, int i);

    /**
     * Send keys to the element defined by string locator
     *
     * @param locator    string locator of target input element
     * @param keysToSend CharSequence to send
     */
    void sendKeys(String locator, CharSequence keysToSend);

    /**
     * @param uploader locator of upload input
     * @param name     filename of file to upload
     */
    void upload(String uploader, String name);
}
