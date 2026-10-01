Create one-level sub-scenario stubs for the $SELECTION scenario that halted the latest test run:

# Scenario Expansion Request

## Step 1: Pre-Flight Target Resolution & Safeguards

Locate and verify the target scenario from the latest test execution before proceeding:

1. **Locate Latest Run, Snapshots & Failure Line**:
   Run the consolidated discovery command:
   ```bash
   LATEST_RUN=$(ls -td target/runs/*/* 2>/dev/null | head -1)
   echo "RUN: $LATEST_RUN"
   find "$LATEST_RUN" -maxdepth 1 \( -name "*EXIT*.*" -o -name "*[EXIT]*.*" \) 2>/dev/null
   grep -m 1 -E "\[FAILURE\] Not implemented.*in '.*'" "$LATEST_RUN"/*.run.log 2>/dev/null
   ```

2. **Resolve Target Source Path**:
   The target scenario is **always** dynamically derived from the latest failure line in `$LATEST_RUN/*.run.log`. Any `$SELECTION` snippet serves solely as optional initial context.
   From `[FAILURE] Not implemented ... in '<packagePath>.<ClassName>'` in the execution log, resolve the source file:
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
  - Prioritize inspecting active overlay containers (`.modal.show`, `.drawer.open`, `[role="dialog"]`, `.swal2-container`, `.collapse.show`) before inspecting the background page.
  - For collapsible menus or expandable accordions, isolate interactive elements within the newly expanded container (`.collapse.show`, `[aria-expanded='true']`, or dropdown panels) rather than the background page.
- **Actionable Scope (Branching Elements)**:
  - Target domain action buttons, primary state-altering controls, and clickable navigation widgets:
    - Action buttons (e.g. `Add Profile`, `Save`, `Export`, `Apply Filters`, `Clear Filters`).
    - Clickable navigation links/cards leading to sub-views.
    - Navigation tabs (e.g. `<ul class="nav-tabs">` tab switches).
    - Expandable accordions / collapsible panels.
    - Custom interactive pickers/widgets (e.g. date-range picker divs with calendar icons).
- **Exclude Inputs & Filter Bar Controls**:
  - Do **NOT** create stubs for standard form inputs and table filter bars:
    - Text inputs, textareas, password fields, hidden tokens (`input`, `textarea`).
    - Native `<select>` dropdowns (e.g. category, status, reseller filters).
    - Inline filter toggle switches / checkboxes (e.g. `Show inactive`).
  - *These belong to form-filling actions, not separate scenario nodes.*
- **Exclude Chrome, Pagination, Table Internals, Modal Teardown & Ancestors/Siblings**:
  - **Ancestor & Sibling Actions (Anti-Recursion)**: Do **NOT** create child stubs for action buttons, links, or widgets that already exist as parent, ancestor, or sibling scenario nodes on the same view (e.g., clicking `Clear Filters` on a view must not produce cyclical child stubs for `ClearFilters`, `ApplyFilters`, or `DateRange`).
  - **Global Chrome & Navigation**: Exclude top navbar, sidebar navigation already captured higher in the tree, breadcrumbs, and generic table pagination controls (`.page-link`, next/previous).
  - **Table Internals**: Exclude raw table rows (`<tr style="cursor: pointer;">`) unless they contain explicit named action buttons (e.g. `Details`).
  - **Modal Dismiss/Close Controls**: Do **NOT** create stubs for modal dismiss controls (`button.close`, `[aria-label="Close"]`, buttons labeled `Close`, `Cancel`, or `Dismiss` that purely close the overlay without leading to a sub-view).

---

## Step 3: Branching Decision

Evaluate the elements discovered in Step 2:
- If no view-specific interactive elements or combinatoric branching paths are revealed (e.g. purely informational view, empty state with no actions, an in-place state mutation/reset such as `Clear Filters` or `Refresh` that remains on the same view without opening a new sub-view, or a leaf form view where all remaining interactions are raw data inputs/submissions), follow **Workflow A: Terminal Leaf Blueprint (End of Tree)**.
- If one or more view-specific interactive elements or sub-views are revealed, follow **Workflow B: Node Expansion (Combinatoric Branching)**.

---

### Workflow A: Terminal Leaf Blueprint (End of Tree)

Follow this workflow if the view is purely informational (e.g. details dialogs), an empty state, an in-place state reset/mutation, or a form view with no further combinatoric sub-scenarios:
- Recognize that the scenario has reached the end of the combinatorial tree. It is a terminal `Leaf` that requires direct functional implementation (form inputs, data population, API validation, business assertions) rather than further scenario branching.

1. **Synthesize Functional Implementation Blueprint**:
   Conduct a thorough analysis of the exit snapshot DOM and screenshot, documenting:
   - **View Controls & Input Surface**: Enumerate available inputs, dropdowns, pickers, and toggles on the view (including selectors/classes and potential test data permutations). List view elements, data fields, badges, buttons, and tables with CSS/XPath selectors.
   - **Interaction Sequence**: Detail step-by-step action flow needed (data population, trigger clicks, asynchronous waits for spin-loaders or invisibility of dialogs, data extraction, dismissal/submission, teardown waits, snapshot triggers).
   - **Verification Scope**: Detail expected state assertions to implement in `verify()` (metadata field presence, diff validation, table content updates, toast/alert validations, URL changes, dialog invisibility, or empty-state feedback).

2. **Update Target File**:
   - Keep the class extending `Leaf` (do **NOT** convert to `Node` and do **NOT** create a child package).
   - Insert the structured comment directly above `notImplemented` inside `action()`:
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

3. **Compile & Halt**:
   - Run `mvn test-compile` to ensure the marked file compiles cleanly.
   - **HALT immediately** and report that the combinatorial branch has terminated at a leaf, that the file has been marked with the terminal `TODO` blueprint, and present the analysis summary.

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
  - `c(...)` &rarr; Title Case. **IMPORTANT**: `c(Object s)` asserts that the input string changes when capitalized (`Assertions.assertNotEqualsSilent`). Input words **MUST** be strictly lowercase!
  - `u(...)` &rarr; Upper Case. Use for acronyms and initialisms (e.g. `u(SMS)`, `u(CSV)`, `u(FD)`).
  - `l(...)` &rarr; Lower Case.
- **Bind**:
  - `b(...)` &rarr; Space-separated concatenation (e.g. `b(c(ADD), c(PROFILE))` &rarr; `"Add Profile"`).
- **Swap**:
  - `s(...)` &rarr; Direct string concatenation (e.g. `s(AD, D)` &rarr; `"add"`, `s(PR, OF, IL, E)` &rarr; `"profile"`).
- **Chunks & Symbols**:
  - `works.lysenko.util.chrs.__` (2-letter lowercase tokens, e.g. `AD`, `AP`, `PL`, `PR`, `OF`, `IL`, `CL`, `EA`, `DA`, `TE`, `SA`, `VE`, `EX`, `PO`, `RT`).
  - `works.lysenko.util.chrs.___` (3-letter lowercase tokens, e.g. `APP`, `LOG`, `FOR`, `ORT`).
  - `works.lysenko.util.chrs.____` (4-letter lowercase tokens, e.g. `RULE`, `TEXT`, `FILE`).
  - `works.lysenko.util.spec.Symbols` (single-character constants, where letters `A` through `Z` evaluate to lowercase ASCII `'a'` through `'z'`, e.g. `D = 'd'`, `E = 'e'`).

#### B.3. Formulate Vocabulary (No Raw Strings)
Ensure no raw string literals are used in scenario code:
- **Vocabulary Hierarchy & Lookup**:
  1. **Upstream Framework**: Check `works.lysenko.util.lang.word.*` and `link.lang.word.*` first.
  2. **Project Single Words**: If absent upstream, define in `interlink.lang.word.<Letter>` as:
     ```java
     package interlink.lang.word;

     @SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
     public record <Letter>() {

         // Must evaluate to lower-case string so c(<WORD>) succeeds:
         public static final String <WORD> = s(...);
     }
     ```
  3. **Multi-Word Phrases & Disambiguated Identifiers**: Define in `interlink.lang.<Letter>` as:
     ```java
     package interlink.lang;

     @SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "AutoBoxing", "WeakerAccess"})
     public record <Letter>() {

         public static final String <PHRASE> = b(c(<WORD1>), c(<WORD2>));
     }
     ```
- **Disambiguation Rule**: If a discovered item key conflicts with an existing locator description (e.g. `Dashboard` when `Dashboard==//a[@href='/admin']` already exists), disambiguate by prefixing with the parent context (e.g. `BILLING_DASHBOARD = b(c(BILLING), c(DASHBOARD))`).
- **Ordering**: Maintain strict alphabetical order for constants and static imports in `interlink.lang.*` and `interlink.lang.word.*`.

#### B.4. Register Locators
In `src/main/resources/locators`, check and register entries (`Description==XPath/CSS`):
- **Duplicate & Reuse Check**: Inspect `src/main/resources/locators` first. If an entry with the exact evaluated description key and a matching locator already exists (e.g. shared Date pickers or standard action buttons), **REUSE** it and do **NOT** append duplicate lines.
- **Description Key**: Must strictly match the exact evaluated string from Step B.3 (e.g. `Save`, `Add Profile`, `Billing Dashboard`, `CSV`).
- **Selector Conventions**:
  - **Buttons**: `//button[@type='submit' and contains(., 'Label')]` or `//button[contains(@class, 'btn') and contains(., 'Label')]`
  - **Links**: `//a[@href='...' and contains(., 'Label')]`
  - **Custom Widgets**: `//div[contains(@class, '...') and .//i[contains(@class, 'fa-...')]]`
  - **Text Matching**: Always prefer `contains(., 'Label')` over `text()='Label'` to reliably match elements containing icons (e.g. `<i class="fa ...">`) or whitespace.

#### B.5. Generate Child Stubs
In the child package (named after parent class with first letter lowercase, e.g. `<parentPkg>.<classNameWithFirstLetterLower>`):
- Create a `Leaf` stub class for each discovered element/action.
- **Class name**: PascalCase of the element/action (e.g. `AddProfile`, `Save`, `BillingRules`). For disambiguated elements (e.g. `BILLING_DASHBOARD`), use the disambiguated phrase in PascalCase (e.g. `BillingDashboard`).
- **Annotations**: `@SuppressWarnings("unused")`.
- **Token Reference**:
  - **Single-word tokens**: Import from `[inter]link.lang.word.<Letter>` and use `c(<WORD>)` in `fits()` and `action()`.
  - **Acronyms / All-Caps tokens**: Use `u(<WORD>)` in `fits()` and `action()` (e.g. `u(CSV)`, `u(SMS)`).
  - **Multi-word phrases**: Import from `interlink.lang.<Letter>` and use `<PHRASE>` directly.
- **Method Implementation** (preserve blank lines around method bodies):
  - `fits()`: `return isPresent(...);`
  - `action()`: `clickOn(...);` followed by `notImplemented();`
  - `verify()`: `return true;`

Example stub template:

```java
package <parentPkg>.<classNameWithFirstLetterLower>;

import works.lysenko.tree.base.Leaf;
import works.lysenko.util.apis.exception.checked.SafeguardException;

import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.func.core.Assertions.notImplemented;
// Vocabulary imports ...

@SuppressWarnings("unused")
public class <ClassName> extends Leaf {

    @Override
    public final boolean fits() {

        return isPresent(c(<WORD>));
    }

    @Override
    public final void action() throws SafeguardException {

        clickOn(c(<WORD>));
        notImplemented();
    }

    @Override
    public final boolean verify() throws SafeguardException {

        return true;
    }
}
```

#### B.6. Verify & Clean Build
Run `mvn clean test-compile` to verify a completely clean build.
