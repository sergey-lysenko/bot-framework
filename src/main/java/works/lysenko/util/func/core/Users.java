package works.lysenko.util.func.core;

import com.github.javafaker.Faker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static works.lysenko.Base.logEvent;
import static works.lysenko.Base.parameters;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.NO;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Collector.selectOneOf;
import static works.lysenko.util.func.type.Files.loadLinesFromFile;
import static works.lysenko.util.lang.T.THERE_ARE;
import static works.lysenko.util.lang.word.U.USERS;
import static works.lysenko.util.spec.Layout.Templates.USERS_POOL_;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * This class provides utility methods for generating and managing users.
 */
@SuppressWarnings("unused")
public record Users() {

    /**
     * Generates a random business credit card number.
     *
     * @return the generated credit card number as a string
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String code() {

        return new Faker().business().creditCardNumber();
    }

    /**
     * Returns a list of users.
     *
     * @return a list of users
     */
    public static List<String> list() {

        final Path poolFile = Paths.get(USERS_POOL_);
        final String poolName = parameters.getPool();
        final Path path = Paths.get(poolFile.getParent().toString(), s(poolName, _DOT_, USERS));
        final List<String> list = loadLinesFromFile(path);
        if (list.isEmpty()) logEvent(S1, b(THERE_ARE, NO, USERS, IN, q(path.toString()), FILE));
        return list;
    }

    /**
     * Returns a randomly selected object from a list of objects.
     *
     * @return a randomly selected object from a list
     */
    public static String pick() {

        return selectOneOf(list());
    }
}
