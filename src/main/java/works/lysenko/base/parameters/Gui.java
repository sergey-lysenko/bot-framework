package works.lysenko.base.parameters;

import works.lysenko.base.Parameters;
import works.lysenko.base.util.Platforms;
import works.lysenko.util.apis.parameters._GUI;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.data.enums.Platform;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static works.lysenko.base.Parameters.WIDTH;
import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.data.enums.ExecutionParameter.*;
import static works.lysenko.util.data.enums.ExitCode.CLOSED_THROUGH_GUI;
import static works.lysenko.util.data.enums.ExitCode.PLATFORMS_RESET;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.D.DELETE;
import static works.lysenko.util.lang.word.F.FIXED;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.P.PLATFORMS;
import static works.lysenko.util.lang.word.R.RESET;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.V.VERIFICATION;
import static works.lysenko.util.prop.core.Start.forcedPlatform;
import static works.lysenko.util.spec.Layout.Directories.ETC_;
import static works.lysenko.util.spec.Layout.Files.PLATFORMS_;
import static works.lysenko.util.spec.Layout.Parts.TEST_PROPERTIES_EXTENSION;
import static works.lysenko.util.spec.Layout.Parts.USERS_POOL_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._TESTS_;

/**
 * Represents a Graphical User Interface (GUI) for parameter input and user interaction.
 */
@SuppressWarnings({"UseOfConcreteClass", "AssignmentOrReturnOfFieldWithMutableType", "CallToSuspiciousStringMethod"})
public class Gui implements _GUI {

    private final Parameters parameters;
    private JTextField domain = null;
    private JComboBox<Object> platform = null;
    private JComboBox<Object> test = null;
    private JComboBox<Object> pool = null;
    private JButton reset = null;
    private List<JTextField> aParams = null;

    /**
     * Represents a graphical user interface for parameter input and user interaction.
     *
     * @param parameters the Parameters object that holds and manages various parameters for execution
     */
    public Gui(final Parameters parameters) {

        this.parameters = parameters;
    }

    /**
     * Adds a label and a component to a container.
     *
     * @param label     the label for the component
     * @param comp      the component to add
     * @param container the container to add the label and component to
     */
    private static void add(final String label, final Component comp, final Container container) {

        container.add(new JLabel(label));
        container.add(comp);
    }

    /**
     * Returns a sorted list of platform names.
     *
     * @return a list of platform names
     */
    private static List<String> platformNames() {

        final List<String> platformNames = new LinkedList<>();
        for (final Platform platform : Platforms.available())
            platformNames.add(platform.getString());
        platformNames.sort(null);
        return platformNames;
    }

    /**
     * Resets the platforms file by deleting it.
     *
     * @throws RuntimeException if unable to delete the platforms file
     */
    private static void reset() {

        if (!(new File(PLATFORMS_).delete()))
            throw new IllegalStateException(b(c(UNABLE), TO, DELETE, PLATFORMS, FILE, q(PLATFORMS_)));
    }

    /**
     * Displays the graphical user interface for parameter input and user interaction.
     * This method adds standard parameters, adds additional parameters, displays a dialogue box,
     * propagates user input, and allows the user to exit the program.
     */
    public final void display() {

        addStandardParameters();
        addAdditionalParameters();
        final int answer = JOptionPane.showConfirmDialog(
                null, dialogueBox(),
                b(c(PARAMETERS), VERIFICATION),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (2 == answer || -1 == answer) _Test.processCode(CLOSED_THROUGH_GUI);
        propagateUserInput();
    }

    /**
     * Adds additional parameters based on the given params and types arrays.
     * Parameters with type "t" will create a JTextField and parameters with type "p" will create a JPasswordField.
     *
     * @throws NullPointerException if params is null
     */
    @SuppressWarnings({"ObjectAllocationInLoop", "SwitchStatement"})
    private void addAdditionalParameters() {

        // Additional parameters
        if (isNotNull(parameters.getAdditionalValues())) {
            aParams = new LinkedList<>();
            int i = 0;
            for (final String param : parameters.getAdditionalValues())
                switch (parameters.getAdditionalTypes()[i++]) {
                    case "t" -> aParams.add(new JTextField(parameters.getProperty(param), WIDTH));
                    case "p" -> aParams.add(new JPasswordField(parameters.getProperty(param), WIDTH)); //NON-NLS
                    default -> {
                    }
                }
        }
    }

    /**
     * Returns a JComboBox populated with entity names obtained from files in a given directory that match a specified filter.
     *
     * @param type      the type of entity to retrieve
     * @param directory the directory to search for files
     * @param filter    the filter to match file names against
     * @return a JComboBox containing the entity names obtained from the files
     */
    private JComboBox<Object> addFromFiles(final String type, final String directory, final String filter) {

        final List<String> entityList = new LinkedList<>();
        if (!(parameters.getProperty(type)).isEmpty()) {
            entityList.add(parameters.getProperty(type));
        }

        final File dir = new File(directory);
        final File[] files = dir.listFiles((dir1, name) -> name.endsWith(filter));
        if (isNotNull(files)) {
            for (final File file : files) {
                final String entity = removeEnd(file.getName(), filter);
                if (!entityList.contains(entity)) entityList.add(entity);
            }
            entityList.sort(null);
        }

        final JComboBox<Object> comboBox = new JComboBox<>(entityList.toArray());
        comboBox.setSelectedItem(parameters.getProperty(type));
        return comboBox;
    }

    /**
     * Adds platforms to the combo box and initializes the reset button.
     */
    @SuppressWarnings("VariableNotUsedInsideIf")
    private void addPlatforms() {

        final List<String> platformNames = platformNames();
        // Creating Browsers ComboBox
        platform = new JComboBox<>(platformNames.toArray());
        platform.setSelectedItem(parameters.getProperty(PLATFORM.name()));
        reset = new JButton((null == forcedPlatform) ? RESET : b(FIXED, BY, CONFIGURATION));
        reset.setEnabled(null == forcedPlatform);
        reset.addActionListener(e -> {
            reset();
            _Test.processCode(PLATFORMS_RESET);
        });
    }

    /**
     * Adds the standard parameters to the Parameters object.
     */
    private void addStandardParameters() {
        // Standard parameters
        test = addFromFiles(TEST.name(), _TESTS_, TEST_PROPERTIES_EXTENSION);
        pool = addFromFiles(POOL.name(), ETC_, USERS_POOL_EXTENSION);
        addPlatforms();

        //noinspection StatementWithEmptyBody
        if (parameters.getProperty(PLATFORM.name()).equals(Platform.ANDROID.getString())) {
            /* Devices selection is commented out until implementation of automatic devices management
            List<String> devices = loadLinesFromFile(Paths.get(DEVICES_FILE));
            device = new JComboBox<>(devices.toArray());
            device.setSelectedItem(get("DEVICE")); */
        } else {
            domain = new JTextField(parameters.getProperty(DOMAIN.name()), WIDTH);
        }
    }

    /**
     * Builds a dialogue box panel with various input components.
     *
     * @return a JPanel containing the dialogue box
     */
    @SuppressWarnings({"StatementWithEmptyBody", "ForeachStatement", "ObjectAllocationInLoop",
            "ValueOfIncrementOrDecrementUsed"})
    private JPanel dialogueBox() {

        // Building dialogue box
        final JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(parameters.size() + 1, 2, 5, 5));
        add(TEST.name(), test, panel);
        add(POOL.name(), pool, panel);
        add(PLATFORM.name(), platform, panel);

        if (parameters.getProperty(PLATFORM.name()).equals(Platform.ANDROID.getString())) {
            // TODO: [framework] continue implementation of automatic emulators management
            // add("DEVICE", device.template, p);
        } else {
            add(DOMAIN.name(), domain, panel);
        }

        // Additional parameters
        if (isNotNull(aParams)) {
            int i = 0;
            for (final JTextField aParam : aParams) {
                panel.add(new JLabel(parameters.getAdditionalValues()[i++]));
                panel.add(aParam);
            }
        }

        add(b(EMPTY), reset, panel);
        return panel;
    }

    /**
     * Propagates user input to the parameters object.
     */
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    private void propagateUserInput() {
        // Propagation of the user input
        parameters.setProperty(TEST.name(), null == test.getSelectedItem() ? EMPTY : test.getSelectedItem().toString());
        parameters.setProperty(POOL.name(), null == pool.getSelectedItem() ? EMPTY : pool.getSelectedItem().toString());
        parameters.setProperty(PLATFORM.name(), null == platform.getSelectedItem() ? EMPTY :
                platform.getSelectedItem().toString());
        //noinspection StatementWithEmptyBody
        if (parameters.getProperty(PLATFORM.name()).equals(Platform.ANDROID.getString())) {
            /* Devices selection is commented out until implementation of automatic devices management
            put("DEVICE", device.getSelectedItem() == null ? StringUtils.EMPTY : device.getSelectedItem().toString()); */
        } else parameters.setProperty(DOMAIN.name(), domain.getText());

        // Additional parameters
        if (isNotNull(aParams)) {
            int i = 0;
            for (final JTextField aParam : aParams)
                parameters.setProperty(parameters.getAdditionalValues()[i++], aParam.getText());
        }
    }
}
