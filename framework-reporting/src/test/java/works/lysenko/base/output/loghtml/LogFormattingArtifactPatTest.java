package works.lysenko.base.output.loghtml;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogFormattingArtifactPatTest {

    @Test
    void testArtifactRegexMatchesResultAndTargetRuns() {
        final String resultPath = "result/runs/Test/123/snapshots/screen.png";
        final String targetPath = "target/runs/Test/123/snapshots/screen.png";

        try {
            final Field field = LogFormatting.class.getDeclaredField("ARTIFACT_PAT");
            field.setAccessible(true);
            final Pattern pat = (Pattern) field.get(null);

            final Matcher mResult = pat.matcher(resultPath);
            assertTrue(mResult.find(), "ARTIFACT_PAT should match result/runs path");
            assertEquals("snapshots/screen.png", mResult.group(2));

            final Matcher mTarget = pat.matcher(targetPath);
            assertTrue(mTarget.find(), "ARTIFACT_PAT should match target/runs path");
            assertEquals("snapshots/screen.png", mTarget.group(2));
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
