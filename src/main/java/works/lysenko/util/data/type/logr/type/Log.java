package works.lysenko.util.data.type.logr.type;

import works.lysenko.util.apis.log._Indentation;
import works.lysenko.util.data.type.logr.AbstractLogData;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.util.chrs.___.LOG;
import static works.lysenko.util.chrs.____.TEXT;
import static works.lysenko.util.data.enums.Brackets.CURLY;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.lang.word.D.DEPTH;
import static works.lysenko.util.lang.word.L.LEVEL;
import static works.lysenko.util.spec.Symbols._BULLT_;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * @author Sergii Lysenko
 */
public class Log extends AbstractLogData implements _Indentation {

    private final int level;

    /**
     * Constructs a Log object with the specified parameters.
     *
     * @param id      the unique identifier for this Log instance
     * @param depth   the depth or indentation level of the log entry
     * @param level   the level or severity of the log entry
     * @param message the message or description of the log entry
     */
    public Log(final long id, final int depth, final int level, final String message) {

        super(id, depth, message);
        this.level = level;
    }

    public final int level() {

        return level;
    }

    @Override
    public final String message() {

        return message;
    }

    @Override
    public final String render() {

        return s(SPACE.repeat(depth), (0 == level) ?
                message : b(s(_BULLT_).repeat(level), message));
    }

    @Override
    public final String toString() {

        return
                b(
                        c(LOG),
                        e(CURLY,
                                b(
                                        a(DEPTH, depth, _COMMA_),
                                        a(LEVEL, level, _COMMA_),
                                        a(TEXT, message)
                                )
                        )
                );
    }
}