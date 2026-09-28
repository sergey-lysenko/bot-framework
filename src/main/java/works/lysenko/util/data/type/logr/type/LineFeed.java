package works.lysenko.util.data.type.logr.type;

import works.lysenko.util.data.type.logr.AbstractLogData;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.____.FEED;
import static works.lysenko.util.chrs.____.LINE;
import static works.lysenko.util.data.enums.Brackets.CURLY;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Wrap.e;

/**
 * @author Sergii Lysenko
 */
public class LineFeed extends AbstractLogData {

    /**
     * Constructs a LineFeed object with the specified ID.
     *
     * @param id the unique identifier for this LineFeed instance
     */
    public LineFeed(final long id) {

        super(id, 0, EMPTY);
    }

    @Override
    public final String render() {

        return message;
    }

    @Override
    public final String toString() {

        return e(CURLY, b(c(LINE), c(FEED)));
    }
}
