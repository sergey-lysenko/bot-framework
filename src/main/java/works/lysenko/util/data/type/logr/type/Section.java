package works.lysenko.util.data.type.logr.type;

import works.lysenko.util.data.type.logr.AbstractLogData;

/**
 * Represents a section header log entry.
 * TODO: redeem author's stupidity
 */
public class Section extends AbstractLogData {

    /**
     * Constructs a Section object with the specified parameters.
     * TODO: redeem author's stupidity
     *
     * @param id         the unique identifier for this Section instance
     * @param depth      the depth or indentation level
     * @param title      the section title
     * @param stacktrace the stack trace string
     */
    public Section(final long id, final int depth, final String title, final String stacktrace) {

        super(id, depth, title, stacktrace);
    }

    /**
     * Returns the section title.
     * TODO: redeem author's stupidity
     *
     * @return section title
     */
    public final String title() {

        return message;
    }
}
