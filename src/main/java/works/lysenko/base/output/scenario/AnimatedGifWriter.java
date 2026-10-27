package works.lysenko.base.output.scenario;

import org.w3c.dom.NodeList;
import works.lysenko.base.output.ProgressionFrames;
import works.lysenko.base.output.ProgressionSettings;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Utility for writing animated GIF files from image lists and SVG frame files.
 */
public final class AnimatedGifWriter {

    private AnimatedGifWriter() {
    }

    public static void writeAnimatedGif(
            final List<BufferedImage> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds) throws IOException {

        writeAnimatedGif(frames, outFile, delayCentiseconds, finalFrameDelayCentiseconds, percentage -> {
        });
    }

    public static void writeAnimatedGif(
            final List<BufferedImage> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds,
            final IntConsumer progress) throws IOException {

        if (frames.isEmpty())
            return;
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("gif");
        if (!writers.hasNext()) {
            throw new IOException("No GIF ImageWriter available");
        }
        final ImageWriter writer = writers.next();

        if (isNotNull(outFile.getParentFile())) {
            outFile.getParentFile().mkdirs();
        }

        try (final ImageOutputStream ios = ImageIO.createImageOutputStream(outFile)) {
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);

            final ImageWriteParam params = writer.getDefaultWriteParam();

            for (int i = 0; i < frames.size(); i++) {
                final BufferedImage frame = frames.get(i);
                final int delay = (i == frames.size() - 1) ? finalFrameDelayCentiseconds : delayCentiseconds;
                final IIOMetadata metadata = configureGifMetadata(writer, frame, delay, i == 0);
                writer.writeToSequence(new IIOImage(frame, null, metadata), params);
                progress.accept((int) (((i + 1L) * 100) / frames.size()));
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    public static void writeAnimatedGifFiles(
            final List<File> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds,
            final IntConsumer progress) throws IOException {
        writeAnimatedGifFiles(
                frames,
                outFile,
                delayCentiseconds,
                finalFrameDelayCentiseconds,
                ProgressionSettings.current().maxFramePixels(),
                progress);
    }

    public static void writeAnimatedGifFiles(
            final List<File> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds,
            final int maxFramePixels,
            final IntConsumer progress) throws IOException {
        if (frames.isEmpty())
            return;
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("gif");
        if (!writers.hasNext())
            throw new IOException("No GIF ImageWriter available");
        final ImageWriter writer = writers.next();
        if (isNotNull(outFile.getParentFile()) && !outFile.getParentFile().mkdirs()
                && !outFile.getParentFile().isDirectory()) {
            throw new IOException("Unable to create GIF output directory: " + outFile.getParent());
        }

        try (final ImageOutputStream ios = ImageIO.createImageOutputStream(outFile)) {
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);
            final ImageWriteParam params = writer.getDefaultWriteParam();
            for (int i = 0; i < frames.size(); i++) {
                final BufferedImage frame = ProgressionFrames.read(frames.get(i), maxFramePixels);
                final int delay = (i == frames.size() - 1) ? finalFrameDelayCentiseconds : delayCentiseconds;
                final IIOMetadata metadata = configureGifMetadata(writer, frame, delay, i == 0);
                writer.writeToSequence(new IIOImage(frame, null, metadata), params);
                progress.accept((int) (((i + 1L) * 100) / frames.size()));
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private static IIOMetadata configureGifMetadata(
            final ImageWriter writer,
            final BufferedImage image,
            final int delayCentiseconds,
            final boolean isFirst) throws IOException {

        final ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
        final IIOMetadata metadata = writer.getDefaultImageMetadata(type, null);
        final String nativeFormat = metadata.getNativeMetadataFormatName();
        final IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);

        IIOMetadataNode gceNode = null;
        final NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if ("GraphicControlExtension".equalsIgnoreCase(children.item(i).getNodeName())) {
                gceNode = (IIOMetadataNode) children.item(i);
                break;
            }
        }
        if (gceNode == null) {
            gceNode = new IIOMetadataNode("GraphicControlExtension");
            root.appendChild(gceNode);
        }
        gceNode.setAttribute("disposalMethod", "none");
        gceNode.setAttribute("userInputFlag", "FALSE");
        gceNode.setAttribute("transparentColorFlag", "FALSE");
        gceNode.setAttribute("delayTime", Integer.toString(delayCentiseconds));
        gceNode.setAttribute("transparentColorIndex", "0");

        if (isFirst) {
            final IIOMetadataNode appExtensions = new IIOMetadataNode("ApplicationExtensions");
            final IIOMetadataNode appNode = new IIOMetadataNode("ApplicationExtension");
            appNode.setAttribute("applicationID", "NETSCAPE");
            appNode.setAttribute("authenticationCode", "2.0");
            appNode.setUserObject(new byte[] { 0x01, 0x00, 0x00 });
            appExtensions.appendChild(appNode);
            root.appendChild(appExtensions);
        }

        metadata.setFromTree(nativeFormat, root);
        return metadata;
    }
}
