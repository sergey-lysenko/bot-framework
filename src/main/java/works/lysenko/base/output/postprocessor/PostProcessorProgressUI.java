package works.lysenko.base.output.postprocessor;

import works.lysenko.base.output.ProcessingProgress;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages Swing GUI progress window dialogs during post-processing task execution.
 */
public final class PostProcessorProgressUI implements ProcessingProgress {

    private final JFrame frame;
    private final Map<String, JProgressBar> bars;

    private PostProcessorProgressUI(final JFrame frame, final Map<String, JProgressBar> bars) {
        this.frame = frame;
        this.bars = bars;
    }

    public static ProcessingProgress create(final boolean enabled, final List<String> tasks) {
        if (!enabled || GraphicsEnvironment.isHeadless()) {
            return ProcessingProgress.NONE;
        }

        final JFrame[] frameRef = new JFrame[1];
        final Map<String, JProgressBar> bars = new LinkedHashMap<>();

        try {
            SwingUtilities.invokeAndWait(() -> {
                final JFrame frame = new JFrame("Post-Processing Progress");
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                final JPanel panel = new JPanel(new GridLayout(tasks.size(), 1, 8, 8));
                panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

                for (final String task : tasks) {
                    addTask(panel, bars, task);
                }

                frame.add(panel);
                frame.pack();
                final Dimension pref = frame.getPreferredSize();
                frame.setSize(new Dimension(Math.max(420, pref.width), pref.height));
                frame.setLocationRelativeTo(null);
                frame.setAlwaysOnTop(true);
                frame.setVisible(true);
                frameRef[0] = frame;
            });
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessingProgress.NONE;
        } catch (final InvocationTargetException e) {
            return ProcessingProgress.NONE;
        }

        return new PostProcessorProgressUI(frameRef[0], bars);
    }

    private static void addTask(
            final JPanel panel,
            final Map<String, JProgressBar> bars,
            final String name) {
        final JPanel row = new JPanel(new GridLayout(2, 1, 2, 2));
        row.add(new JLabel(name));
        final JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setString("Waiting...");
        bars.put(name, bar);
        row.add(bar);
        panel.add(row);
    }

    @Override
    public void update(final String task, final int percentage) {
        SwingUtilities.invokeLater(() -> {
            final JProgressBar bar = bars.get(task);
            if (null != bar) {
                bar.setValue(percentage);
                bar.setString(percentage + "%");
            }
        });
    }

    @Override
    public void complete(final String task) {
        SwingUtilities.invokeLater(() -> {
            final JProgressBar bar = bars.get(task);
            if (null != bar) {
                bar.setValue(100);
                bar.setString("Complete");
            }
        });
    }

    @Override
    public void skipped(final String task) {
        SwingUtilities.invokeLater(() -> {
            final JProgressBar bar = bars.get(task);
            if (null != bar) {
                bar.setValue(0);
                bar.setString("Skipped");
            }
        });
    }

    @Override
    public void failed(final String task, final Exception e) {
        SwingUtilities.invokeLater(() -> {
            final JProgressBar bar = bars.get(task);
            if (null != bar) {
                bar.setValue(0);
                bar.setString("Failed: " + e.getMessage());
            }
        });
    }

    @Override
    public void close() {
        SwingUtilities.invokeLater(() -> {
            if (null != frame) {
                frame.dispose();
            }
        });
    }
}
