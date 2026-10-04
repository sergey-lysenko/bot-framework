package works.lysenko.base.output;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

final class ProgressionMp4 {

    private ProgressionMp4() {
    }

    static void write(
            final List<File> frames,
            final File outputFile,
            final String ffmpeg,
            final int frameRate,
            final int maxFramePixels,
            final IntConsumer progress) throws IOException, InterruptedException {
        write(frames, outputFile, ffmpeg, frameRate, maxFramePixels, null, progress);
    }

    static void write(
            final List<File> frames,
            final File outputFile,
            final String ffmpeg,
            final int frameRate,
            final int maxFramePixels,
            final File audioFile,
            final IntConsumer progress) throws IOException, InterruptedException {
        if (frames.isEmpty()) throw new IllegalArgumentException("At least one MP4 frame is required");
        final BufferedImage first = ProgressionFrames.read(frames.get(0), maxFramePixels);
        final int width = first.getWidth();
        final int height = first.getHeight();
        validateDimensions(width, height);
        final Path parent = outputFile.getAbsoluteFile().getParentFile().toPath();
        Files.createDirectories(parent);
        final Path encodedOutput = Files.createTempFile(parent, "progression-video-", ".mp4");
        final String executable = (null == ffmpeg || ffmpeg.isBlank()) ? "ffmpeg" : ffmpeg;
        final Process process;
        try {
            final List<String> command = new ArrayList<>(List.of(
                    executable,
                    "-hide_banner",
                    "-loglevel", "error",
                    "-y",
                    "-f", "image2pipe",
                    "-vcodec", "png",
                    "-framerate", String.valueOf(Math.max(1, frameRate)),
                    "-i", "pipe:0"));
            if (null != audioFile) {
                command.addAll(List.of(
                        "-i", audioFile.getAbsolutePath(),
                        "-map", "0:v:0",
                        "-map", "1:a:0",
                        "-c:a", "aac",
                        "-b:a", "96k"));
            } else {
                command.add("-an");
            }
            command.addAll(List.of(
                    "-c:v", "libx264",
                    "-pix_fmt", "yuv420p",
                    "-movflags", "+faststart",
                    encodedOutput.toString()));
            process = new ProcessBuilder(command).start();
        } catch (final IOException e) {
            Files.deleteIfExists(encodedOutput);
            throw new IOException("Unable to start FFmpeg executable '" + executable + "'", e);
        }

        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        final AtomicReference<IOException> stderrFailure = new AtomicReference<>();
        final Thread stderrReader = new Thread(
                () -> copyLimited(process.getErrorStream(), stderr, stderrFailure),
                "progression-ffmpeg-stderr");
        stderrReader.setDaemon(true);
        stderrReader.start();
        try {
            try (OutputStream input = process.getOutputStream()) {
                for (int i = 0; i < frames.size(); i++) {
                    final BufferedImage image = (i == 0)
                            ? first
                            : ProgressionFrames.read(frames.get(i), maxFramePixels);
                    if (image.getWidth() != width || image.getHeight() != height) {
                        throw new IOException("All MP4 frames must have identical dimensions");
                    }
                    validateDimensions(image.getWidth(), image.getHeight());
                    if (!javax.imageio.ImageIO.write(image, "png", input)) {
                        throw new IOException("No PNG ImageIO writer is available for MP4 input");
                    }
                    input.flush();
                    progress.accept((int) (((i + 1L) * 100) / frames.size()));
                }
            }

            final boolean exited = process.waitFor(10, TimeUnit.MINUTES);
            if (!exited) {
                process.destroyForcibly();
                throw new IOException("FFmpeg exceeded the 10 minute encoding limit");
            }
            stderrReader.join(5000);
            if (stderrReader.isAlive()) throw new IOException("Unable to collect FFmpeg diagnostic output");
            if (process.exitValue() != 0) {
                final String details;
                synchronized (stderr) {
                    details = stderr.toString(StandardCharsets.UTF_8).trim();
                }
                throw new IOException("FFmpeg exited with status " + process.exitValue() + ": " + details);
            }
            if (null != stderrFailure.get()) throw stderrFailure.get();
            Files.move(encodedOutput, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException | InterruptedException | RuntimeException e) {
            process.destroyForcibly();
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw e;
        } finally {
            Files.deleteIfExists(encodedOutput);
        }
    }

    private static void validateDimensions(final int width, final int height) throws IOException {
        if ((width & 1) != 0 || (height & 1) != 0) {
            throw new IOException("MP4 frames must have even dimensions; got " + width + "x" + height);
        }
    }

    private static void copyLimited(
            final java.io.InputStream input,
            final ByteArrayOutputStream output,
            final AtomicReference<IOException> failure) {
        try (input) {
            final byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                synchronized (output) {
                    final int remaining = 16_384 - output.size();
                    if (remaining > 0) output.write(buffer, 0, Math.min(count, remaining));
                }
            }
        } catch (final IOException e) {
            failure.set(e);
        }
    }
}
