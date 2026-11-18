package works.lysenko.base.ui;

import works.lysenko.base.Parameters;
import works.lysenko.base.parameters.Gui;
import works.lysenko.util.apis.parameters._GUI;
import works.lysenko.util.apis.util._Dashboard;
import works.lysenko.util.apis.util._UiProvider;

/**
 * Default Swing {@link _UiProvider} registered via {@code META-INF/services}.
 */
public final class DefaultUiProvider implements _UiProvider {

    @Override
    public _Dashboard createDashboard() {
        return ControlPanel.getInstance();
    }

    @Override
    public _GUI createSetupGui(final Parameters parameters) {
        return new Gui(parameters);
    }
}
