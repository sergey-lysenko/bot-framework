# Grow the Test Tree (generic)

Create one-level sub-scenario stubs for the scenario that halted the latest test run.

This prompt is platform-neutral. A platform prompt (`growTree.android.md`, `growTree.web.md`) must be read together with it
and **overrides** the items listed under "Platform hooks". Anything not overridden applies as written here.

> [!CAUTION]
> **Strict Execution Guard**: Do **NOT** run tests (`mvn test`, `mvn exec:java`, `BotRunner`, `run.maven.bash`) while expanding the tree or verifying it,
> except through the Restart Policy in Step 4. Running tests overwrites the latest run directory you are analysing.
> Verification is limited to read operations and `mvn test-compile`. Do **NOT** run `mvn clean` (it wipes `target/runs`).

## Platform hooks (defined by the platform prompt)

| Hook | Meaning |
|------|---------|
| `SNAPSHOT` | Which exit-snapshot files of the run to analyse and how to read them |
| `BRANCHING` | Which elements are scenario-worthy (create a child stub) |
| `EXCLUDED` | Which elements never become scenarios |
| `LOCATOR` | Locator syntax, where locators are registered, naming and reuse rules |
| `INTERACT` | Names of the click/wait/presence methods used in stubs |
| `TEARDOWN` | How a leaf restores the initial state (dialogs, overlays, navigation) |
| `BASES` | Project base classes that stubs should extend instead of plain `Leaf` |
| `RESTART` | The exact restart command and parameter source |

## Step 1: Pre-Flight Target Resolution

1. Find the latest run: `LATEST_RUN=$(ls -td target/runs/*/*/ | head -1)`.
2. Read the last halt from `$LATEST_RUN/*.run.log`: `[FAILURE] Not implemented ... in '<package.path>.<ClassName>'`.
   The target is **always** derived from this line; a user selection is only context to cross-check.
3. Map it to `src/main/java/**/tree/<package/path>/<ClassName>.java`.
4. **Resolve the real owner of `notImplemented()`**. Scenario trees are not always plain trees: a scenario may inherit its behaviour from a
   shared base class, be reused in several places, or be a thin subclass (`class X extends CommonX {}`). The log names the scenario that was
   *executed*, not necessarily the class that *contains* the call.
    - Follow `extends` upwards until you find the class whose `action()` calls `notImplemented()`. That class is the **owner**.
    - If the owner is a shared base class, expanding it affects **every** subclass. Child scenarios then belong to the **owner's** package
      (the base class's constructor may point there explicitly, for example `super("some.package.path")`), not to the subclass's package.
      Before editing, list the subclasses (`grep -rn "extends <Owner>"`) and tell the user which scenarios will be affected.
    - If the owner already has children, the halt may be stale (the run predates them). Say so in the report. Remove the stale `notImplemented()`
      only if the existing children make the owner complete, and continue with the missing children.
    - A scenario may also reach its children through a `BASES` helper class rather than a child package; look for it before concluding that none exist.
5. Eligibility (all must hold, otherwise **HALT** and report the mismatch):
    - The owner extends `Leaf` (directly or through a `BASES` class) or is a `Node` whose own `action()` still calls `notImplemented();`.
    - No child package (own or declared by the owner) already covers the element you are about to add.
    - It does not already contain `TODO: Terminal Leaf - requires direct functional implementation`.
6. If no `Not implemented` failure exists or the file cannot be found, **HALT** and report the run status.

## Step 2: Snapshot Analysis

Analyse the exit snapshot per `SNAPSHOT`. Isolate the topmost interactive layer first (open dialog, sheet, drawer, expanded panel),
then the underlying screen. Collect `BRANCHING` elements and drop `EXCLUDED` ones.

Always exclude, regardless of platform:
- Inputs and value pickers (text fields, selects, toggles, date or time choices): they are form filling, not scenarios.
- Controls that already exist as an ancestor or sibling scenario (anti-recursion).
- Global navigation already covered higher in the tree.
- Destructive actions (delete, remove) and purely dismissing controls (close, cancel).
- Submit buttons of dialogs or forms that return to the parent view: they belong to the terminal leaf's functional implementation.

## Step 3: Branching Decision

- Nothing branching found (informational view, empty state, in-place mutation, form or picker with only input and submit left): **Workflow A**.
- One or more branching elements found: **Workflow B**.

### Workflow A: Terminal Leaf

The scenario is the end of the combinatorial tree. Keep it a `Leaf` and create no child package.

1. Write a functional-implementation blueprint from the snapshot: controls and input surface, interaction sequence, verification scope.
2. Add `TEARDOWN` operations to `action()` if the scenario leaves the application in a state that blocks later scenarios.
   Never leave an overlay or dialog open.
3. Insert above `notImplemented`:
   ```java
   /*
    * TODO: Terminal Leaf - requires direct functional implementation
    *
    * 1. View Controls & Input Surface:
    *    - <controls and locators>
    *
    * 2. Interaction Sequence:
    *    - <steps, waits, teardown>
    *
    * 3. Verification Scope:
    *    - <assertions for verify()>
    */
   ```
4. Change `notImplemented();` to `notImplemented(false);` so the run continues and discovers siblings.
5. Run `mvn test-compile`, then go to Step 4.

### Workflow B: Node Expansion

1. **Convert the parent (the owner from Step 1)**: extend `Node` instead of `Leaf`, remove the `notImplemented();` call and its import (and any other imports that became unused).
2. **Vocabulary, no raw strings in scenario code**:
    - Look upstream first (`works.lysenko.util.lang.word.*`; inspect with `javap` on the Maven classpath).
    - Otherwise add single words and acronyms to the project's word records, and multi-word phrases to the project's phrase records (paths are in the platform prompt).
    - Keep constants and imports in strict alphabetical order.
    - Helpers: `c(x)` Title Case (input **must** be lowercase, otherwise an `AssertionError` is thrown), `u(x)` UPPER, `l(x)` lower,
      `b(...)` space-join, `s(...)` plain concatenation.
    - Never wrap an already capitalised phrase constant in `c(...)`.
    - On a name clash with an existing locator description, disambiguate with the parent context (`BILLING_DASHBOARD = b(c(BILLING), c(DASHBOARD))`).
3. **Register locators** per `LOCATOR`. Reuse an existing entry with the same key and locator; never append duplicates.
4. **Generate stubs** in the child package (owner class name, first letter lowercase, or the package the owner declares). One class per element:
    - Name: PascalCase of the element or action; extend a `BASES` class where one fits, otherwise `Leaf`.
    - `@SuppressWarnings("unused")`.
    - `fits()` returns presence of the element, `action()` interacts with it (use the waiting variant for transitions, animations and delayed elements)
      and ends with `notImplemented();`, `verify()` returns `true`.
    - Preserve a blank line after each method signature line.
5. Run `mvn test-compile`.

## Step 4: Post-Execution

1. **Self-review**: `git status` and `git diff` show no unwanted changes, unused imports or ordering violations; every locator is registered; no raw strings; `mvn test-compile` is clean.
2. **Report**: converted nodes, new stubs, registered locators, vocabulary additions, plus edge cases and trade-offs.
3. **Prompt evolution**: if you met an ambiguity or friction point, propose a refinement to the relevant prompt and ask the user before editing it.
4. **Restart Policy**: run the `RESTART` command before ending the turn only when **both** hold:
    - the previous halt was the regular `[FAILURE] Not implemented {•Code-induced failure•} in '...' scenario`;
    - there is nothing to propose under prompt evolution.

   Otherwise do **not** restart. For irregular failures (click intercepted, timeouts, stale elements, authentication or network errors, unexpected assertions)
   or pending prompt changes, stop and ask how to proceed. A leaked overlay in a preceding leaf is the usual cause of "click intercepted" failures on unrelated scenarios.
