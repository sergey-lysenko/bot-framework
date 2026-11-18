package works.lysenko.base.output.loghtml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogFormattingTest {

    @Test
    void testLinkifyArtifactsWithSubstringsInScenarioNames() {
        final String rawLine = "••• Writing full screenshot 'snapshots/1794528002702.5.000122227.biteheist.tree.signIn.login.billing.modules.addModule.AllEmpty.png' [329]";
        final String linkified = LogFormatting.linkifyArtifacts(rawLine);

        assertTrue(linkified.contains("href=\"snapshots/1794528002702.5.000122227.biteheist.tree.signIn.login.billing.modules.addModule.AllEmpty.png\""),
                "Link should point to the full PNG artifact and not cut off at .log inside .login");
        assertTrue(linkified.contains(">snapshots/1794528002702.5.000122227.biteheist.tree.signIn.login.billing.modules.addModule.AllEmpty.png</a>"),
                "Link label should be the complete PNG filename");
    }

    @Test
    void testLinkifyArtifactsStandardLog() {
        final String rawLine = "Writing log 'snapshots/12345.run.log'";
        final String linkified = LogFormatting.linkifyArtifacts(rawLine);

        assertTrue(linkified.contains("href=\"snapshots/12345.run.log\""));
    }
}
