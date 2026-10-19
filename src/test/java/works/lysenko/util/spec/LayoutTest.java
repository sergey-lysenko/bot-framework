package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;
import works.lysenko.base.output.loghtml.LogFormatting;

import java.io.File;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static works.lysenko.util.spec.Layout.Directories.DASH_;
import static works.lysenko.util.spec.Layout.Directories.RESULT_;
import static works.lysenko.util.spec.Layout.Files.APPIUM_LOG_;
import static works.lysenko.util.spec.Layout.Paths._DASH_;
import static works.lysenko.util.spec.Layout.Paths._RESULT_DASH_;
import static works.lysenko.util.spec.Layout.Paths._RUNS_;
import static works.lysenko.util.spec.Layout.Paths._TARGET_DASH_;

class LayoutTest {

    @Test
    void testTestResultStoragePaths() {
        final String s = File.separator;
        assertEquals("result" + s, RESULT_);
        assertEquals("result" + s + "runs" + s, _RUNS_);
        assertEquals("dash" + s, DASH_);
        assertEquals("dash" + s, _DASH_);
        assertEquals(_DASH_, _RESULT_DASH_);
        assertEquals(_DASH_, _TARGET_DASH_);
        assertEquals("result" + s + "appium.log", APPIUM_LOG_);
    }

    @Test
    void testArtifactRegexMatchesResultAndTargetRuns() {
        final String resultPath = "result/runs/Test/123/snapshots/screen.png";
        final String targetPath = "target/runs/Test/123/snapshots/screen.png";

        final java.lang.reflect.Field field;
        try {
            field = LogFormatting.class.getDeclaredField("ARTIFACT_PAT");
            field.setAccessible(true);
            final java.util.regex.Pattern pat = (java.util.regex.Pattern) field.get(null);

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
