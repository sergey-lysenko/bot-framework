package works.lysenko.base.ui;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.t._Geometry;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.gsrc.ColourSearchRequest;
import works.lysenko.util.grid.record.gsrc.ColoursSearchResult;
import works.lysenko.util.grid.record.gsrc.Geometry;
import works.lysenko.util.grid.record.gsrc.Resolution;
import works.lysenko.util.grid.record.misc.DrawGridResult;
import works.lysenko.util.lang.word.M;
import works.lysenko.util.prop.grid.Defaults;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static works.lysenko.Base.log;
import static works.lysenko.util.apis.grid.d._Resolution.readResolution;
import static works.lysenko.util.chrs.__.AN;
import static works.lysenko.util.chrs.___.PNG;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.func.imgs.Painter.drawArea;
import static works.lysenko.util.func.imgs.Painter.drawGrid;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.grid.record.gsrc.ColourSearchRequest.csr;
import static works.lysenko.util.grid.record.gsrc.Region.search;
import static works.lysenko.util.grid.record.gsrc.Routines.getRequestedGridLocation;
import static works.lysenko.util.grid.validation.Check.byGrid;
import static works.lysenko.util.lang.word.A.AREA;
import static works.lysenko.util.lang.word.C.CENTER;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.C.COORDINATE;
import static works.lysenko.util.lang.word.D.DENSITY;
import static works.lysenko.util.lang.word.E.EXCEPTION;
import static works.lysenko.util.lang.word.G.GRIDS;
import static works.lysenko.util.lang.word.H.HELPER;
import static works.lysenko.util.lang.word.H.HORIZONTAL;
import static works.lysenko.util.lang.word.O.OCCURED;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.P.PROBLEM;
import static works.lysenko.util.lang.word.R.RADIUS;
import static works.lysenko.util.lang.word.R.RECALCULATE;
import static works.lysenko.util.lang.word.R.RESOLUTION;
import static works.lysenko.util.lang.word.S.*;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.lang.word.V.VERTICAL;
import static works.lysenko.util.spec.Layout.Paths._GRIDS_;
import static works.lysenko.util.spec.Layout.Paths._RUNS_;
import static works.lysenko.util.spec.Numbers.HUNDRED;
import static works.lysenko.util.spec.Numbers.TEN;
import static works.lysenko.util.spec.Symbols.X;
import static works.lysenko.util.spec.Symbols.Y;
import static works.lysenko.util.spec.Symbols._COLON_;

/**
 * This class is a helper class that extends {@code JFrame}.
 * It provides a user interface to manipulate and visualize image transformation parameters.
 */
@SuppressWarnings("ClassWithTooManyFields")
final class Helper extends JFrame {

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private final ImagePanel imagePanel;
    private final JScrollPane imageScrollPane;
    private final JTextField horizontal;
    private final JTextField vertical;
    private final JTextField scale;
    private final JTextField resolution;
    private final JTextField mask;
    private final JTextField centerX;
    private final JTextField centerY;
    private final JTextField radius;
    private final JTextField samples;
    private final JTextField area;
    private final JTextField density;
    private final JMenuItem openGridItem;
    private final JPanel controlPanel = new JPanel();
    private final JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 1, 1000, 100);
    private Image sourceImg = null;
    private Image targetImg = null;
    private int major = 1000;

    /**
     * Constructs a new instance of the Helper class. Initializes the graphical user interface components and sets up event
     * listeners.
     */
    @SuppressWarnings({"OverlyLongMethod", "AbsoluteAlignmentInUserInterface"})
    private Helper() {

        setTitle(b(c(GRID), c(PARAMETERS), c(HELPER)));
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setLocationRelativeTo(null);

        imagePanel = new ImagePanel();
        imagePanel.setScale(1.0F);
        imageScrollPane = new JScrollPane(imagePanel);

        add(controlPanel, BorderLayout.WEST);
        add(imageScrollPane, BorderLayout.CENTER);

        configureSlider();
        controlPanel.add(slider);

        controlPanel.setPreferredSize(new Dimension(BUTTON_WIDTH, getHeight()));
        controlPanel.setMinimumSize(new Dimension(BUTTON_WIDTH, getHeight()));
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        centerX = addReadOnlyField(b(c(CENTER), c(X), s(c(COORDINATE), _COLON_)));
        centerY = addReadOnlyField(b(c(CENTER), c(Y), s(c(COORDINATE), _COLON_)));
        radius = addReadOnlyField(s(c(RADIUS), _COLON_));
        samples = addReadOnlyField(s(c(SAMPLES), _COLON_));
        area = addReadOnlyField(s(c(AREA), _COLON_));
        density = addReadOnlyField(s(c(DENSITY), _COLON_));
        horizontal = addField(s(c(HORIZONTAL), _COLON_), Defaults.horizontal);
        vertical = addField(s(c(VERTICAL), _COLON_), Defaults.vertical);
        scale = addField(s(c(SCALE), _COLON_), Defaults.scale);
        resolution = addField(s(c(RESOLUTION), _COLON_), Defaults.resolution);
        mask = addField(s(c(M.MASK), _COLON_), StringUtils.EMPTY);

        final JButton recalculate = new JButton(c(RECALCULATE));
        recalculate.addActionListener(this::actionPerformed);
        controlPanel.add(recalculate);

        final JMenuBar menuBar = new JMenuBar();
        final JMenu fileMenu = new JMenu(c(FILE));

        final JMenuItem openMenuItem = new JMenuItem(b(c(OPEN), c(SCREENSHOT)));
        openMenuItem.addActionListener(e -> openImage());

        openGridItem = new JMenuItem(b(c(LOAD), c(GRID)));
        openGridItem.setEnabled(false);
        openGridItem.addActionListener(e -> openGrid());

        fileMenu.add(openMenuItem);
        fileMenu.add(openGridItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    /**
     * The entry point of the application.
     * Initializes the graphical user interface components and sets up event listeners.
     *
     * @param args The command line arguments.
     */
    public static void main(final String[] args) {

        SwingUtilities.invokeLater(() -> {
            final Helper frame = new Helper();
            frame.setVisible(true);
        });
    }

    private void actionPerformed(final ActionEvent e) {

        recalculate();
    }

    private JTextField addField(final String text, final String initialValue) {

        final JLabel label = new JLabel(text);
        final JTextField field = new JTextField();
        field.setText(initialValue);
        field.setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));

        // Adding KeyListener
        field.addKeyListener(new KeyAdapter() {
            public void keyPressed(final KeyEvent e) {

                if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                    recalculate();
                }
            }
        });

        controlPanel.add(label);
        controlPanel.add(field);
        return field;
    }

    private JTextField addReadOnlyField(final String text) {

        final JLabel label = new JLabel(text);
        final JTextField field = new JTextField();
        field.setEditable(false);
        field.setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        controlPanel.add(label);
        controlPanel.add(field);
        return field;
    }

    private void configureSlider() {

        slider.setPaintTicks(true);
        slider.setPaintLabels(false);
        slider.addChangeListener(this::stateChanged);
        updateSlider();

    }

    private BufferedImage copySource() {

        final BufferedImage copy = new BufferedImage(((RenderedImage) sourceImg).getWidth(),
                ((RenderedImage) sourceImg).getHeight(), ((BufferedImage) sourceImg).getType());
        final Graphics2D g = copy.createGraphics();
        try {
            // Draw the original image onto the copy image
            g.drawImage(sourceImg, 0, 0, null);
        } finally {
            // Dispose the Graphics context to free up resources
            g.dispose();
        }
        return copy;
    }

    private BufferedImage drawAndUpdateImage(final BufferedImage image, final _Geometry geometry,
                                             final _Resolution resolution, final Iterable<? extends _Region> exclude) {

        final DrawGridResult result = drawGrid(image, geometry, resolution, exclude);
        samples.setText(s(result.samples()));
        area.setText(s(result.area()));
        density.setText(percentString(result.samples(), result.area()));
        density.repaint();
        return result.image();
    }

    @SuppressWarnings("OverlyStrongTypeCast")
    private void openGrid() {

        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(_GRIDS_));
        final FileNameExtensionFilter filter = new FileNameExtensionFilter(c(GRIDS), GRID);
        fileChooser.setFileFilter(filter);
        final int returnValue = fileChooser.showOpenDialog(null);
        if (JFileChooser.APPROVE_OPTION == returnValue) {
            final File selectedFile = fileChooser.getSelectedFile();
            try {
                final Request vr = byGrid(selectedFile.getAbsolutePath());
                final Geometry at = getRequestedGridLocation(vr, (BufferedImage) sourceImg);
                final ColourSearchRequest request = null;
                if (isNotNull(at)) updateValuesAndRepaint(request, copySource(), at, vr.resolution());
                horizontal.setText(ts(vr.position().horizontal(), false));
                vertical.setText(ts(vr.position().vertical(), false));
                scale.setText(ts(vr.scale(), false));
                resolution.setText(vr.resolution().text());
            } catch (final RuntimeException e) {
                JOptionPane.showMessageDialog(this,
                        b(c(AN), EXCEPTION, s(OCCURED, _COLON_), b(e.getMessage())),
                        c(EXCEPTION),
                        JOptionPane.ERROR_MESSAGE);
                log(Arrays.toLines(e.getStackTrace()));
            }
        }
    }

    /**
     * Opens an image file using a file chooser dialog. Allows the user to select a screenshot file in PNG format.
     * Once a file is selected, the method reads the image file using ImageIO and assigns it to the sourceImg variable.
     * It then creates a copy of the source image using the copySource() method and assigns it to the targetImg variable.
     * The copy image is set as the image to be displayed on the imagePanel component.
     * The scroll pane containing the imagePanel is then revalidated and repainted to reflect the changes.
     * Finally, it enables the openGridItem menu item.
     */
    private void openImage() {

        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(_RUNS_));
        final FileNameExtensionFilter filter = new FileNameExtensionFilter(c(SCREENSHOTS), PNG);
        fileChooser.setFileFilter(filter);

        final int returnValue = fileChooser.showOpenDialog(null);
        if (JFileChooser.APPROVE_OPTION == returnValue) {
            final File selectedFile = fileChooser.getSelectedFile();
            try {
                sourceImg = ImageIO.read(selectedFile);
                major = (TEN * HUNDRED * HUNDRED) / sourceImg.getWidth(null);
                updateSlider();
                targetImg = copySource();
                imagePanel.setImage(targetImg);
                imageScrollPane.revalidate();
                imageScrollPane.repaint();
                openGridItem.setEnabled(true);
            } catch (final IOException e) {
                JOptionPane.showMessageDialog(this,
                        b(c(AN), EXCEPTION, s(OCCURED, _COLON_), e.getMessage()),
                        c(EXCEPTION),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Recalculates the grid location and updates the values and repaints the UI.
     */
    @SuppressWarnings("OverlyBroadCatchBlock")
    private void recalculate() {

        if (isNotNull(sourceImg)) {
            try {
                final Resolution to = readResolution(resolution.getText());
                final Geometry at = getRequestedGridLocation(
                        ((RenderedImage) sourceImg).getWidth(), ((RenderedImage) sourceImg).getHeight(),
                        fr(horizontal.getText()).doubleValue(),
                        fr(vertical.getText()).doubleValue(),
                        fr(scale.getText()).doubleValue(),
                        to.ratio());
                final ColourSearchRequest request = csr(mask.getText());
                if (isNotNull(request) && !request.getUnknownColours().isEmpty())
                    JOptionPane.showMessageDialog(this, b(s1(request.getUnknownColours().size(), b(UNKNOWN, COLOUR)),
                            java.util.Arrays.toString(request.getUnknownColours().toArray())), c(PROBLEM),
                            JOptionPane.ERROR_MESSAGE);
                else updateValuesAndRepaint(request, copySource(), at, to);
            } catch (final RuntimeException e) {
                JOptionPane.showMessageDialog(this,
                        b(c(AN), EXCEPTION, s(OCCURED, _COLON_), e.getMessage(), Arrays.toLines(e.getStackTrace())),
                        c(EXCEPTION),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void stateChanged(final ChangeEvent e) {

        final JSlider source = (JSlider) e.getSource();
        if (!source.getValueIsAdjusting()) {
            final int value = source.getValue();
            final float scale = value / 100.0f;
            imagePanel.setScale(scale);
            imageScrollPane.revalidate();
            imageScrollPane.repaint();
        }
    }

    private void updateSlider() {

        final int minor = major >> 2;
        major = minor << 2;
        final int max = major * 5;
        slider.setValue(100);
        slider.setMajorTickSpacing(major);
        slider.setMinorTickSpacing(minor);
        slider.setMinimum(1);
        slider.setMaximum(max);
    }

    @SuppressWarnings({"NumericCastThatLosesPrecision", "AssignmentToMethodParameter"})
    private void updateValuesAndRepaint(final ColourSearchRequest request, BufferedImage image, final _Geometry geometry,
                                        final _Resolution resolutionValue) {

        centerX.setText(s(geometry.center().X()));
        centerY.setText(s(geometry.center().Y()));
        radius.setText(s((int) geometry.radius()));
        final Paint colour = new Color(255, 0, 0, 100);
        final ColoursSearchResult result = search(request, image);
        final Collection<_Region> exclude = isNotNull(result) ? result.regions() : null;
        if (isNotNull(exclude)) for (final _Region region : exclude) image = drawArea(image, colour, region);
        targetImg = drawAndUpdateImage(image, geometry, resolutionValue, exclude);
        imagePanel.setImage(targetImg);
        imageScrollPane.repaint();
    }
}
