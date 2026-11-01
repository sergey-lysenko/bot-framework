package works.lysenko.base.test;

import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Ctrl;
import works.lysenko.util.apis.scenario._Pool;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Exec;

import java.util.Set;

import static works.lysenko.Base.logEmptyLine;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Manages and executes the root scenario controller ({@link _Ctrl}) for a test session.
 */
public class Exec implements _Exec {

    private _Ctrl ctrl = null;

    /**
     * Retrieves the root scenario controller managed by this executor.
     *
     * @return the configured {@link _Ctrl} instance, or {@code null} if not yet assigned
     */
    @Override
    public final _Ctrl ctrl() {

        return ctrl;
    }

    /**
     * Assigns the root scenario controller to be managed and executed by this executor.
     *
     * @param ctrl the {@link _Ctrl} instance to set
     */
    @Override
    public final void ctrl(final _Ctrl ctrl) {

        this.ctrl = ctrl;
    }

    /**
     * Executes a scenario selection and execution cycle via the configured {@link _Ctrl}
     * and logs a trailing empty line.
     *
     * @throws SafeguardException if a safeguard violation occurs during scenario execution
     */
    @Override
    public final void exec() throws SafeguardException {

        ctrl.exec();
        logEmptyLine();
    }

    /**
     * Retrieves the sorted set of root scenarios from the configured controller's scenario pool.
     *
     * @return the sorted {@link Set} of root {@link _Scenario} instances, or an empty set if unavailable
     */
    @Override
    public final Set<_Scenario> getRootScenariosList() {

        final _Pool pool = isNotNull(ctrl) ? ctrl.getPool() : null;
        final Set<_Scenario> scenarios = isNotNull(pool) ? pool.getSortedSet() : null;
        return isNotNull(scenarios) ? scenarios : Set.of();
    }
}
