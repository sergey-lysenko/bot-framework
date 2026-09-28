package works.lysenko.base.test;


import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Ctrl;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._Exec;

import java.util.Set;

import static works.lysenko.Base.exec;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * The Exec class represents an object that executes scenarios. It implements the _Exec interface.
 */
public class Exec implements _Exec {

    private _Ctrl ctrl = null;

    public final _Ctrl ctrl() {

        return ctrl;
    }

    public final void ctrl(final _Ctrl ctrl) {

        this.ctrl = ctrl;
    }

    /**
     * Executes the scenarios by calling the execute() method of the Scenarios object
     * and logs an empty line using the getLogger().logEmptyLine () method.
     */
    public final void exec() throws SafeguardException {

        ctrl.exec();
        exec.logEmptyLine();
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public final Set<_Scenario> getRootScenariosList() {

        if (isNotNull(ctrl))
            if (isNotNull(ctrl.getPool()))
                if (isNotNull(ctrl.getPool().getSortedSet()))
                    return ctrl.getPool().getSortedSet();
        return Set.of();
    }
}
