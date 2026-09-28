package works.lysenko.util.data.type.logr;

import works.lysenko.util.apis.log._LogData;

/**
 * AbstractLogData is an abstract class that provides common functionalities for log data objects.
 * It implements the ProvidesLogData interface.
 */
@SuppressWarnings("ProtectedField")
public abstract class AbstractLogData implements _LogData { // TODO: port to Record

    protected final String message;
    protected final int depth;
    protected final long id;

    /**
     * @param depth   indentation
     * @param message text
     */
    protected AbstractLogData(final long id, final int depth, final String message) {

        this.id = id;
        this.depth = depth;
        this.message = message;
    }

    public final int depth() {

        return depth;
    }

    public final long id() {

        return id;
    }

    @SuppressWarnings("DesignForExtension")
    public String message() {

        return message;
    }
}
