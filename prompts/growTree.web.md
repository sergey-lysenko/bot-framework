Create one-level sub-scenario stubs for the $SELECTION scenario that halted the latest test run:

# Scenario Expansion Request

## Step 1: Pre-Flight Target Resolution & Safeguards

> [!CAUTION]
> **Strict Execution Guard**: Do **NOT** run test execution commands (`mvn test`, `BotRunner`, `mvn exec:java`, or test suite runners) during scenario expansion and verification. Running tests triggers live browser sessions and pollutes/overwrites the latest target run directory while analyzing DOM snapshots. Verification is strictly restricted to read operations and compile-time checks (`mvn test-compile`). Do **NOT** run `mvn clean`, as deleting `../target` wipes all test run snapshots and logs.
> *(To restart tests after completing scenario expansion, adhere strictly to the Automatic Restart Policy in Step 4.4).*

Locate and verify the target scenario from the latest test execution before proceeding:

1. **Locate Latest Run, Snapshots & Failure Line**:
   Run the consolidated discovery command (trailing slash restricts search to directories, ignoring macOS `.DS_Store` files):
   ```bash
   LATEST_RUN=$(ls -td target/runs/*/*/ 2>/dev/null | head -1)
   echo "RUN: $LATEST_RUN"
   find "$LATEST_RUN" -maxdepth 1 \( -name "*EXIT*.*" -o -name "*[EXIT]*.*" \) 2>/dev/null
   grep -m 1 -E "\[FAILURE\] Not implemented.*in '.*'" "$LATEST_RUN"/*.run.log 2>/dev/null
   ```

2. **Resolve Target Source Path**:
   Extract and resolve the target scenario class and file path:
   ```bash
   TARGET=$(grep -h -m 1 -E "\[FAILURE\] Not implemented.*in '.*'" "$LATEST_RUN"/*.run.log 2>/dev/null | sed -E "s/.*in '([^']+)'.*/\1/")
   echo "Resolved target: $TARGET"
   TARGET_FILE=$(find src/main/java -path "*/tree/$(echo "$TARGET" | tr '.' '/').java" 2>/dev/null)
   echo "Resolved source: $TARGET_FILE"
   ```
   The target scenario is **always** dynamically derived from the latest failure line in `$LATEST_RUN/*.run.log`. Any `$SELECTION` snippet serves as initial context to verify against `$TARGET`.
   From `[FAILURE] Not implemented ... in '<packagePath>.<ClassName>'` in the execution log, the source file maps to:
   ```text
   src/main/java/**/tree/<packagePath>/<ClassName>.java
   ```
   *(If no `[FAILURE] Not implemented` entry is found in the latest run log or the target source file cannot be located, **HALT immediately** and report the test run status).*

3. **Pre-Flight Eligibility Safeguards**:
    - **Type Check**: Target must be a Java scenario class under `src/main/java/**/tree/` (not an interface, utility, or root runner) extending `Leaf`.
    - **Unexpanded Check**: Target must call `notImplemented();` inside `action()` and have no existing child scenario package (`src/main/java/**/tree/<packagePath>/<classNameWithFirstLetterLower>/`).
    - **Terminal Guard**: Target must **NOT** already contain the terminal marker:
      ```text
      TODO: Terminal Leaf - requires direct functional implementation
      ```
      *(If this mark is already present, **HALT immediately** and report that the scenario has already been analyzed and marked as a terminal leaf awaiting functional implementation).*
    - **Halt Rule**: If any check fails, **HALT immediately** and report the specific mismatch. Do **NOT** proceed with stub generation unless this pre-flight check passes.

---

## Step 2: Snapshot Analysis & Container Isolation

Examine the exit snapshot (HTML & screenshot) to determine whether the state reveals new combinatoric paths or terminates at a leaf:

- **Overlay & Container Priority**:
    - Prioritize inspecting active overlay containers (`.modal.show`, `.drawer.open`, `[role=\"dialog\"]`, `.swal2-container`, `.collapse.show`) before inspecting the background page.
    - For collapsible menus or expandable accordions, isolate interactive elements within the newly expanded container (`.collapse.show`, `[aria-expanded='true']`, or dropdown panels) rather than the background page.
- **Actionable Scope (Branching Elements)**:
    - Target domain action buttons, primary state-altering controls, and clickable navigation widgets:
        - Action buttons (e.g. `Add Profile`, `Save`, `Export`, `Apply Filters`, `Clear Filters`).
          > [!NOTE]
          > *Action Lifecycle*: Action buttons that remain on the same view without opening a sub-view (such as `Clear Filters`) are valid child stubs to generate when expanding the parent view (Workflow B). In subsequent test executions where such an in-place action stub itself halts as the `$TARGET`, it terminates via Workflow A.
        - Clickable navigation links/cards leading to sub-views.
        - Navigation tabs: Target inactive tab switches only (e.g. `<ul class="nav-tabs">` or `<ul class="nav-pills">` tabs with `aria-selected="false"` or lacking `.active`). Do **NOT** generate stubs for tabs that are already active on page load to avoid redundant cyclical loops.
        - Expandable accordions / collapsible panels.
        - Custom interactive pickers/widgets (e.g. date-range picker divs with calendar icons).
- **Exclude Inputs & Filter Bar Controls**:
    - Do **NOT** create stubs for standard form inputs and table filter bars:
        - Text inputs, textareas, password fields, hidden tokens (`input`, `textarea`).
        - Native `<select>` dropdowns (e.g. category, status, reseller filters).
        - Inline filter toggle switches / checkboxes (e.g. `Show inactive`).
        - Date / Time Picker Controls: Controls inside open date-range or calendar popovers/dialogs (preset interval buttons like `Today`, `This Week`, calendar day cells, day input ranges, and month/year steppers/selects). When an interactive date-picker stub halts as the `$TARGET`, its popover state terminates via **Workflow A** with a date-selection blueprint rather than branching into individual preset stubs.
    - *These belong to form-filling actions, not separate scenario nodes.*
- **Exclude Chrome, Pagination, Table Internals, Modal Teardown & Ancestors/Siblings**:
    - **Ancestor & Sibling Actions (Anti-Recursion)**: Do **NOT** create child stubs for action buttons, links, or widgets that already exist as parent, ancestor, or sibling scenario nodes on the same view (e.g., clicking `Clear Filters` on a view must not produce cyclical child stubs for `ClearFilters`, `ApplyFilters`, or `DateRange`).
    - **Global Chrome & Navigation**: Exclude top navbar, sidebar navigation already captured higher in the tree, breadcrumbs, and generic table pagination controls (`.page-link`, next/previous).
    - **Table Internals & Row Actions**: Exclude raw table rows (`<tr style="cursor: pointer;">`) and destructive row action buttons (e.g. trash icons, `Delete`, `Remove`) to protect staging test data from deletion during automated crawler runs. Only include explicit non-destructive row action buttons (e.g. `Details`, `Edit`).
    - **Modal Dismiss/Close Controls**: Do **NOT** create stubs for modal dismiss controls (`button.close`, `[aria-label="Close"]`, buttons labeled `Close`, `Cancel`, or `Dismiss` that purely close the overlay without leading to a sub-view).
      > [!NOTE]
      > *State Restoration & Overlay Leaks*: While dismiss controls do not become separate child stubs, if a terminal leaf scenario opened an overlay (e.g. an informational details modal or picker), that terminal leaf **must** perform the minimum operation (closing the opened dialogue via `waitThenClickOn(...)`) in its `action()` to restore the initial state for subsequent test execution (see Workflow A.2).
      >
      > *Distinguishing Leaked Overlays from Race Conditions*: If a subsequent test fails with `ElementClickInterceptedException` (e.g., clicking a navigation link like `FirstDelivery` where "Other element would receive the click: `<div class='modal fade show'...>`"), it is almost never a race condition in the failing navigation node. Instead, it is an **overlay leak** from a preceding leaf scenario (such as [`Details.java`](file:///Users/sergii/Repos/BiteHeist-test/src/main/java/biteheist/tree/signIn/correctLogin/auditLogs/Details.java)) that opened a modal without calling `waitThenClickOn(c(CLOSE))` or `waitThenClickOn(c(CANCEL))`.\n    - **Modal Form Submissions**: Do **NOT** create child stubs for modal form submit buttons (e.g. `Add`, `Create`, `Save`, `Submit` inside an active creation/edit modal dialog). Modal submission is tied to form data population and dismisses the dialog back to the parent view; it belongs to the terminal leaf functional implementation blueprint rather than an isolated branching action.

---

## Step 3: Branching Decision

Evaluate the elements discovered in Step 2:
- If no view-specific interactive elements or combinatoric branching paths are revealed (e.g. purely informational view, empty state with no actions, an in-place state mutation/reset such as an existing `Clear Filters` or `Refresh` scenario that remains on the same view without opening a new sub-view, or a modal/inline form/picker view where remaining interactions consist of form input population, date/interval selection, and dialog submission/dismissal), follow **Workflow A: Terminal Leaf Blueprint (End of Tree)**.
- If one or more view-specific interactive elements or sub-views are revealed (e.g. sub-navigation links, drawer items, action triggers like `Save`, `Add Profile`, `Clear Filters`), follow **Workflow B: Node Expansion (Combinatoric Branching)**.

---

### Workflow A: Terminal Leaf Blueprint (End of Tree)

Follow this workflow if the view is purely informational (e.g. details dialogs), an empty state, an in-place state reset/mutation, or a form/picker view with no further combinatoric sub-scenarios:
- Recognize that the scenario has reached the end of the combinatorial tree. It is a terminal `Leaf` that requires direct functional implementation (form inputs, data population, API validation, business assertions) rather than further scenario branching.

1. **Synthesize Functional Implementation Blueprint**:
   Conduct a thorough analysis of the exit snapshot DOM and screenshot, documenting:
    - **View Controls & Input Surface**: Enumerate available inputs, dropdowns, pickers, and toggles on the view (including selectors/classes and potential test data permutations). List view elements, data fields, badges, buttons, and tables with CSS/XPath selectors.
    - **Interaction Sequence**: Detail step-by-step action flow needed (data population, trigger clicks, asynchronous waits for spin-loaders or invisibility of dialogs, data extraction, dismissal/submission, teardown waits, snapshot triggers).
    - **Verification Scope**: Detail expected state assertions to implement in `verify()` (metadata field presence, diff validation, table content updates, toast/alert validations, URL changes, dialog invisibility, or empty-state feedback).

2. **Update Target File**:
    - Keep the class extending `Leaf` (do **NOT** convert to `Node` and do **NOT** create a child package).
    - **Initial State Restoration (Teardown Guard)**:
      If there is a clear indication that the scenario alters the view state in a way that blocks subsequent test execution (for example, opening an informational modal dialogue, drawer, or popover overlay that obstructs background controls and subsequent scenario discovery), add the minimum amount of operations in `action()` required to return the site to its initial state (e.g. closing the opened dialogue via dismiss button/icon or backdrop) after the triggering action and before `notImplemented(false);`. Any tokens or locators used for state restoration must strictly follow vocabulary (B.3) and locator registration (B.4) conventions without raw strings.
      > [!IMPORTANT]
      > **Use `waitThenClickOn(...)` for State Restoration Teardown**: Because newly opened dialogs, drawers, or popovers take time to render and complete transition animations (e.g. CSS fade-ins), dismiss controls (such as `c(CLOSE)`, `c(CANCEL)`, or close icons) are often not immediately interactable. Always use `waitThenClickOn(...)` rather than `clickOn(...)` for state restoration dismissals (as demonstrated in [`AddNew.java`](file:///Users/sergii/Repos/BiteHeist-test/src/main/java/biteheist/tree/signIn/correctLogin/settings/posConfig/AddNew.java) and [`Details.java`](file:///Users/sergii/Repos/BiteHeist-test/src/main/java/biteheist/tree/signIn/correctLogin/auditLogs/Details.java)).
      >
      > **Overlay Leak Warning**: Never exit a leaf that opens a modal without closing it. An unclosed modal remains in the DOM across subsequent tests in the session, throwing `ElementClickInterceptedException` on arbitrary background navigation targets.
    - Insert the structured comment directly above `notImplemented` inside `action()` :
      ```java
      /*
       * TODO: Terminal Leaf - requires direct functional implementation
       *
       * 1. View Controls & Input Surface:
       *    - <discovered controls, tables, and selectors>
       *
       * 2. Interaction Sequence:
       *    - <step-by-step actions, waits, and dialog dismissal>
       *
       * 3. Verification Scope:
       *    - <concrete assertions for verify()>
       */
      ```
    - **Switch to Non-Halting Assertion**: Change `notImplemented();` to:
      ```java
      notImplemented(false);
      ```
      *(This records the stub status without halting the run, enabling subsequent test executions to discover and unfold remaining sibling nodes).*

3. **Compile Verification**:
    - Run `mvn test-compile` to ensure the marked file compiles cleanly. Do not proceed to Workflow B (no child stubs or package creation).
    - Proceed directly to Step 4 for post-execution inspection and automatic test restart.

---

### Workflow B: Node Expansion (Combinatoric Branching)

Follow this workflow if one or more view-specific interactive elements or sub-views are revealed:

#### B.1. Convert & Unblock Parent
- Change the target class from extending `Leaf` to extending `Node`:
  ```java
  import works.lysenko.tree.base.Node;
  ```
- Remove the `notImplemented();` call from `action()`.
- Remove the unused import:
  ```java
  import static works.lysenko.util.func.core.Assertions.notImplemented;
  ```

#### B.2. String Utilities & Character Reference
- **Case**:
    - `c(...)` &rarr; Title Case. **IMPORTANT**: `c(Object s)` asserts that the input string changes when capitalized (`Assertions.assertNotEqualsSilent`). Input words **MUST** be strictly lowercase! Calling `c(...)` on an already-capitalized phrase will throw a runtime `AssertionError`.
    - `u(...)` &rarr; Upper Case. Use for acronyms and initialisms (e.g. `u(SMS)`, `u(CSV)`, `u(FD)`, `u(POS)`).
    - `l(...)` &rarr; Lower Case.
- **Bind**:
    - `b(...)` &rarr; Space-separated concatenation (e.g. `b(c(ADD), c(PROFILE))` &rarr; `"Add Profile"`).
- **Swap**:
    - `s(...)` &rarr; Direct string concatenation (e.g. `s(AD, D)` &rarr; `"add"`, `s(PR, OF, IL, E)` &rarr; `"profile"`).
- **Chunks & Symbols**:
    - `works.lysenko.util.chrs.__` (2-letter lowercase tokens, e.g. `AD`, `AP`, `CL`, `CO`, `DA`, `EA`, `ER`, `EX`, `FI`, `IG`, `IL`, `OF`, `PL`, `PO`, `PR`, `RT`, `SA`, `TE`, `TS`, `UC`, `UR`, `VE`).
    - `works.lysenko.util.chrs.___` (3-letter lowercase tokens, e.g. `APP`, `FOR`, `LOG`, `ORT`).
    - `works.lysenko.util.chrs.____` (4-letter lowercase tokens, e.g. `CONF`, `FILE`, `PROD`, `RULE`, `TEXT`).
    - `works.lysenko.util.spec.Symbols` (single-character constants, where letters `A` through `Z` evaluate to lowercase ASCII `'a'` through `'z'`, e.g. `D = 'd'`, `E = 'e'`).

#### B.3. Formulate Vocabulary (No Raw Strings)
Ensure no raw string literals are used in scenario code:
- **Vocabulary Hierarchy & Lookup**:
    1. **Upstream Framework**: Check `works.lysenko.util.lang.word.*` and `link.lang.word.*` first.
       > [!TIP]
       > Inspect upstream framework vocabulary via classpath:
       > ```bash
     > javap -cp $(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout) works.lysenko.util.lang.word.<Letter> 2>/dev/null
     > ```
    2. **Project Single Words & Acronyms**: If absent upstream, define in `interlink.lang.word.<Letter>` as:
       ```java
       package interlink.lang.word;
  
       @SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
       public record <Letter>() {
  
           // Must evaluate to lower-case string so c(<WORD>) and u(<WORD>) succeed:
           public static final String <WORD> = s(...);
           public static final String <ACRONYM> = s(...); // e.g. POS = s(PO, S);
     }
       ```
    3. **Multi-Word Phrases & Disambiguated Identifiers**: Define in `interlink.lang.<Letter>` as:
       ```java
       package interlink.lang;
  
       @SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
       public record <Letter>() {
  
           public static final String <PHRASE> = b(c(<WORD1>), c(<WORD2>));
           // For phrases with acronyms:
           public static final String <PHRASE_WITH_ACRONYM> = b(u(<ACRONYM>), c(<WORD>)); // e.g. POS_CONFIG = b(u(POS), c(CONFIG));
       }
       ```
- **Disambiguation Rule**: If a discovered item key conflicts with an existing locator description (e.g. `Dashboard` when `Dashboard==//a[@href='/admin']` already exists), disambiguate by prefixing with the parent context (e.g. `BILLING_DASHBOARD = b(c(BILLING), c(DASHBOARD))`).
- **Ordering**: Maintain strict alphabetical order for constants and static imports in `interlink.lang.*` and `interlink.lang.word.*`.

#### B.4. Register Locators
In `../src/main/resources/locators`, check and register entries (`Description==XPath/CSS`):
- **Duplicate & Reuse Check**: Inspect `../src/main/resources/locators` first. If an entry with the exact evaluated description key and a matching locator already exists (e.g. shared Date pickers or standard action buttons), **REUSE** it and do **NOT** append duplicate lines.
- **Description Key**: Must strictly match the exact evaluated string from Step B.3 (e.g. `Save`, `Add Profile`, `Billing Dashboard`, `CSV`, `Configure Products`, `POS Config`).
- **Selector Conventions**:
    - **Buttons**: `//button[@type='submit' and contains(., 'Label')]` or `//button[contains(@class, 'btn') and contains(., 'Label')]`
    - **Links**: `//a[@href='...' and contains(., 'Label')]`
    - **Custom Widgets**: `//div[contains(@class, '...') and .//i[contains(@class, 'fa-...')]]`
    - **Text Matching**: Always prefer `contains(., 'Label')` over `text()='Label'` to reliably match elements containing icons (e.g. `<i class=\"fa ...\">`) or whitespace.

#### B.5. Generate Child Stubs
In the child package (named after parent class with first letter lowercase, e.g. `<parentPkg>.<classNameWithFirstLetterLower>`):
- Create a `Leaf` stub class for each discovered element/action.
- **Class name**: PascalCase of the element/action (e.g. `AddProfile`, `Save`, `BillingRules`, `ConfigureProducts`, `PosConfig`). For disambiguated elements (e.g. `BILLING_DASHBOARD`), use the disambiguated phrase in PascalCase (e.g. `BillingDashboard`).
- **Annotations**: `@SuppressWarnings(\"unused\")`.
- **Token Reference**:
    - **Single-word tokens**: Import from `[inter]link.lang.word.<Letter>` and use `c(<WORD>)` in `fits()` and `action()`.
    - **Acronyms / All-Caps tokens**: Use `u(<WORD>)` in `fits()` and `action()` (e.g. `u(CSV)`, `u(SMS)`).
    - **Multi-word phrases**: Import from `interlink.lang.<Letter>` and use `<PHRASE>` directly.
      > [!CAUTION]
      > **Do NOT wrap multi-word phrases with `c(...)`**: Because `<PHRASE>` is already capitalized via `b(c(...), c(...))`, calling `c(<PHRASE>)` triggers an immediate runtime `AssertionError` (`assertNotEqualsSilent`). Omit `import static works.lysenko.util.data.strs.Case.c;` if only multi-word phrases are referenced.
- **Method Implementation** (preserve blank lines around method bodies):
    - `fits()`: `return isPresent(...);`
    - `action()`:\n        - Standard static controls: `clickOn(...);` followed by `notImplemented();`.
        - **Navigation links, animated controls, or delayed elements**: Prefer `waitThenClickOn(...);` instead of `clickOn(...);` whenever the target element involves view transitions, page loads, sidebar expansion, or dynamic DOM insertion (e.g. [`FirstDelivery.java`](file:///Users/sergii/Repos/BiteHeist-test/src/main/java/biteheist/tree/signIn/correctLogin/FirstDelivery.java)).
        - **Understanding `waitThenClickOn(...)` vs. Overlays**: `waitThenClickOn(...)` waits for `waitForVisibilityOf(...)` before clicking. While this prevents clicking elements before they render, it does **not** bypass an active modal backdrop blocking the click coordinates; overlay hygiene in preceding leaf nodes remains mandatory.\n    - `verify()`: `return true;`

**Template A: Single-Word Stub**
```java
package <parentPkg>.<classNameWithFirstLetterLower>;

import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.exception.checked.SafeguardException;

import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.func.core.Assertions.notImplemented;
import static <wordPackage>.<WORD>;

@SuppressWarnings("unused")
public class <ClassName> extends Leaf {\n\n    @Override
    public final boolean fits() {\n\n        return isPresent(c(<WORD>));
    }\n\n    @Override
    public final void action() throws SafeguardException {\n\n        clickOn(c(<WORD>));
        notImplemented();
    }\n\n    @Override
    public final boolean verify() throws SafeguardException {\n\n        return true;
    }
}
```

**Template B: Multi-Word Phrase Stub**
```java
package <parentPkg>.<classNameWithFirstLetterLower>;

import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.exception.checked.SafeguardException;

import static interlink.lang.<Letter>.<PHRASE>;
import static works.lysenko.util.func.core.Assertions.notImplemented;

@SuppressWarnings("unused")
public class <ClassName> extends Leaf {\n\n    @Override
    public final boolean fits() {\n\n        return isPresent(<PHRASE>);
    }\n\n    @Override
    public final void action() throws SafeguardException {\n\n        clickOn(<PHRASE>);
        notImplemented();
    }\n\n    @Override
    public final boolean verify() throws SafeguardException {\n\n        return true;
    }
}
```

#### B.6. Compile Verification
Run `mvn test-compile` to verify that all code compiles cleanly. Do **NOT** run `mvn clean` (to preserve `target/runs/`) and do **NOT** execute tests or launch the bot runner during verification.

---

## Step 4: Post-Execution Inspection & Prompt Evolution

After completing scenario stub generation or terminal leaf marking in each turn:

1. **Self-Review & Code Quality Verification**:
    - Check `git status` and `git diff` for unwanted changes, unused imports, or violated ordering/formatting conventions.
    - Verify that all locators are properly registered in `../src/main/resources/locators` and no raw strings exist in Java code.
    - Verify that multi-word phrases are passed directly without `c(...)`.
    - Run `mvn test-compile` to guarantee 0 compiler errors.

2. **Report Findings & Observations**:
    - Provide a concise summary of all changes made (converted nodes, new stubs, registered locators, vocabulary tokens).
    - Explicitly highlight any edge cases, design trade-offs, or runtime anomalies identified.

3. **Prompt Evolution (Insights & Feedback Loop)**:
    - Identify whether any unforeseen pattern, ambiguity, or friction point emerged during execution.
    - Formulate proposed refinements for `var/request.md` and present them to the user for confirmation before applying updates.

4. **Restarting Tests (Headless Mode Execution & Automatic Restart Policy)**:
    - **Restart Command**:
      ```bash
      CI=true mvn exec:java
      ```
      *(or `CI=true ./run.maven.bash`)*
    - **Execution Mechanics & Parameter Handling**:
      - Setting `CI=true` switches the framework into non-interactive/headless mode (`Routines.isInsideCI()`):
        - Bypasses interactive GUI parameter dialogues (`Gui.java`) and automatically loads settings from `../var/parameters` (or environment variables `DOMAIN`, `PLATFORM`, `POOL`, `TEST`).
        - Replaces the Swing GUI dashboard window with the file-based dashboard interface (`target/dash/`).
        - Suppresses automatic opening of the HTML run log in the desktop browser upon completion.
    - **Automatic Restart Policy**:
      - **Safe Automatic Restart Condition**:
        Automatically launch the restart command in headless mode (`CI=true mvn exec:java`) before ending the turn when both of the following criteria are met:
        1. The previous test failure was a regular, expected halt caused by:
           ```text
           [FAILURE] Not implemented {•Code-induced failure•} in '...' scenario
           ```
        2. There is **nothing to add or refine** in `var/request.md` in terms of prompt self-evolution / insights (Step 4.3).
      - **Irregular Failures & Prompt Evolution (Human Confirmation Required)**:
        In all other cases, do **NOT** restart automatically. Stop and ask the user how to proceed:
        - If new insights, ambiguities, or friction points warrant prompt evolution, ask whether to update `var/request.md` first.
        - If the previous halt was an irregular failure (e.g. `ElementClickInterceptedException`, `TimeoutException`, stale elements, authentication failure, network error, or unexpected assertion error), ask the user whether to diagnose and resolve the irregular failure, adjust test configuration in [`/Users/sergii/Repos/BiteHeist-test/var/parameters`](file:///Users/sergii/Repos/BiteHeist-test/var/parameters), or update the prompt.
