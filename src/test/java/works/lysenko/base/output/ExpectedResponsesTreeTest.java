package works.lysenko.base.output;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.base.output.tree.SvgTreeRenderer;
import works.lysenko.base.output.tree.TreeTheme;
import works.lysenko.base.output.tree.VisualNode;
import works.lysenko.base.output.tree.VisualNodeResolver;
import works.lysenko.base.responses.ExpectedResponse;
import works.lysenko.base.responses.ExpectedResponsesLoader;
import works.lysenko.base.responses.ExpectedResponsesStore;
import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.data.type.logr.type.Event;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExpectedResponsesTreeTest {

    @AfterEach
    void tearDown() {
        TreeHtml.setExpectedResponsesStore(null);
    }

    private static _Scenario createScenario(final String fqn, final String shortName, final String simpleName) {
        return new Leaf(Fraction.ONE) {
            @Override
            public String getName() {
                return fqn;
            }

            @Override
            public String getShortName() {
                return shortName;
            }

            @Override
            public String getSimpleName() {
                return simpleName;
            }

            @Override
            public ScenarioType type() {
                return ScenarioType.LEAF;
            }
        };
    }

    @Test
    @DisplayName("Expected browser 404 response suppresses amber WARNING outline, dot, progress bar, and edge on matching nodes")
    void testExpectedResponseSuppressesWarningOnNodeAndEdge() {
        final String json = """
                {
                  "$schema": "./responses.schema.json",
                  "responses": [
                    {
                      "scenario": "signIn.WrongLogin",
                      "pattern": "api/auth/local.*404",
                      "reason": "Expected HTTP 404 response on invalid login credentials"
                    },
                    {
                      "scenario": "signIn.WrongPassword",
                      "pattern": "api/auth/local.*404",
                      "reason": "Expected HTTP 404 response on incorrect password"
                    }
                  ]
                }
                """;
        final List<ExpectedResponse> loaded = ExpectedResponsesLoader.loadFromStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(2, loaded.size());
        TreeHtml.setExpectedResponsesStore(new ExpectedResponsesStore(loaded));

        final _Scenario signInScenario = createScenario("biteheist.scenarios.SignIn", "SignIn", "SignIn");
        final Result signInResult = new Result(signInScenario);
        signInResult.tick();

        final _Scenario wrongLoginScenario = createScenario("biteheist.scenarios.signIn.WrongLogin", "signIn.WrongLogin", "WrongLogin");
        final Result wrongLoginResult = new Result(wrongLoginScenario);
        wrongLoginResult.tick();
        wrongLoginResult.addEvent(new LogRecord(34, 295730L, new Event(1, EventType.WARNING, 2,
                "[6] [BROWSER] [2026-11-03T14:36:57.059Z] [SEVERE] https://stg-dashboard.myordering.online/api/auth/local - Failed to load resource: the server responded with a status of 404 ()", "")));
        wrongLoginResult.addEvent(new LogRecord(34, 295730L, new Event(2, EventType.WARNING, 2,
                "[6] [BROWSER] [2026-11-03T14:36:57.059Z] [SEVERE] https://stg-dashboard.myordering.online/static/js/main.6dc89438.js 1:3360947 o", "")));

        assertEquals(ExecutionStatus.PASSED, wrongLoginResult.status(), "Result status must remain PASSED when browser event matches expected response");
        assertEquals(0, wrongLoginResult.getEffectiveEventsCount(), "Effective event count must be 0 when events are neutralized");
        assertFalse(wrongLoginResult.toString().contains("event"), "Result.toString() must not print (N events) when all events are expected responses");

        final TreeHtml.NodeData parentNode = new TreeHtml.NodeData("n_signin", "SignIn ▷", "Tests", 0, 1.0, signInResult, signInScenario);
        final TreeHtml.NodeData wrongLoginNode = new TreeHtml.NodeData("n_wronglogin", "WrongLogin ◼", "SignIn ▷", 1, 1.0, wrongLoginResult, wrongLoginScenario);
        wrongLoginNode.setParent(parentNode);
        parentNode.children().add(wrongLoginNode);
        final TreeHtml.Edge edge = new TreeHtml.Edge(parentNode, wrongLoginNode);

        assertFalse(TreeHtml.hasWarning(wrongLoginNode), "Node matching expected response must not have warning state");
        assertEquals(0, TreeHtml.getUnclassifiedEventCount(wrongLoginNode), "Unclassified event count must be 0");

        final VisualNodeResolver resolver = new VisualNodeResolver(TreeTheme.DEFAULT, Set.of(signInScenario, wrongLoginScenario), true);
        final VisualNode vn = resolver.resolve(wrongLoginNode);
        assertEquals("PASSED", vn.statusName());
        assertEquals(TreeTheme.DEFAULT.passedBorder(), vn.borderColor(), "Border must be green passedBorder, not warningBorder");
        assertNull(vn.indicatorColor(), "Indicator circle dot must be null (Model A: outlines only)");

        final String html = TreeHtml.renderHtml(List.of(parentNode, wrongLoginNode), List.of(edge));
        assertTrue(html.contains("class=\"connector visited\""), "Incoming edge must be connector visited (green)");
        assertFalse(html.contains("class=\"connector warning\""), "Incoming edge must not be connector warning (amber)");

        final String svg = SvgTreeRenderer.renderTreeProgressionSvg(
                new TreeHtml.TreeLayout(List.of(parentNode, wrongLoginNode), List.of(edge)),
                1, "Test #34", Set.of(), Set.of(signInScenario, wrongLoginScenario), null, null, null, 800, 400);
        assertFalse(svg.contains("stroke=\"#F59E0B\""), "Progression SVG edge must not be amber #F59E0B");
    }

    @Test
    @DisplayName("Unexpected browser warning marks node as WARNING with amber border and no indicator pip (Model A)")
    void testUnexpectedBrowserErrorMarksNodeAsWarning() {
        final ExpectedResponse er = new ExpectedResponse("signIn.WrongLogin", "api/auth/local.*404", "Expected 404");
        TreeHtml.setExpectedResponsesStore(new ExpectedResponsesStore(List.of(er)));

        final _Scenario dashboardScenario = createScenario("biteheist.scenarios.signIn.login.Dashboard", "signIn.login.Dashboard", "Dashboard");
        final Result dashboardResult = new Result(dashboardScenario);
        dashboardResult.tick();
        dashboardResult.addEvent(new LogRecord(10, 10000L, new Event(1, EventType.WARNING, 2,
                "[BROWSER] [2026-11-03T14:36:57.059Z] Deprecation warning in dashboard widget", "")));

        final TreeHtml.NodeData dashboardNode = new TreeHtml.NodeData("n_dash", "Dashboard ◆", "Login ▷", 1, 1.0, dashboardResult, dashboardScenario);
        assertTrue(TreeHtml.hasWarning(dashboardNode));
        assertEquals(1, TreeHtml.getUnclassifiedEventCount(dashboardNode));

        final VisualNodeResolver resolver = new VisualNodeResolver(TreeTheme.DEFAULT, Set.of(dashboardScenario), true);
        final VisualNode vn = resolver.resolve(dashboardNode);
        assertEquals("WARNING", vn.statusName());
        assertEquals(TreeTheme.DEFAULT.warningBorder(), vn.borderColor());
        assertNull(vn.indicatorColor(), "Model A: tree cards use outlines only, no circle indicator pips");
    }

    @Test
    @DisplayName("Unclassified browser INFO (S3 / NOTICE) marks node as NOTICE with cyan border")
    void testBrowserInfoMarksNodeAsNoticeWithCyanBorder() {
        TreeHtml.setExpectedResponsesStore(new ExpectedResponsesStore(List.of()));

        final _Scenario dashboardScenario = createScenario("biteheist.scenarios.signIn.login.Dashboard", "signIn.login.Dashboard", "Dashboard");
        final Result dashboardResult = new Result(dashboardScenario);
        dashboardResult.tick();
        dashboardResult.addEvent(new LogRecord(10, 10000L, new Event(1, EventType.NOTICE, 2,
                "[6] [BROWSER] [2026-11-03T14:36:57.059Z] Console info log message", "")));

        final TreeHtml.NodeData dashboardNode = new TreeHtml.NodeData("n_dash", "Dashboard ◆", "Login ▷", 1, 1.0, dashboardResult, dashboardScenario);
        assertFalse(TreeHtml.hasFailure(dashboardNode));
        assertFalse(TreeHtml.hasWarning(dashboardNode));
        assertTrue(TreeHtml.hasNotice(dashboardNode));

        final VisualNodeResolver resolver = new VisualNodeResolver(TreeTheme.DEFAULT, Set.of(dashboardScenario), true);
        final VisualNode vn = resolver.resolve(dashboardNode);
        assertEquals("NOTICE", vn.statusName());
        assertEquals(TreeTheme.DEFAULT.noticeBorder(), vn.borderColor());
        assertNull(vn.indicatorColor());
    }
}
