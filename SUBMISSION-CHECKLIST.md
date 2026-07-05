# Plugin Hub Submission Readiness Checklist

Audit date: 2026-07-05. Reviewed against the current `runelite/plugin-hub` README
(https://github.com/runelite/plugin-hub, fetched raw from `master`) and Jagex's Third Party
Client Guidelines (https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1),
plus the actual plugin source under `src/main/java/com/seachartingquesthelper/` as it stands on
`main` at commit `eae1fdc` (after the region fix, sorter, config toggles, progress-marker, and
dangerous-water gear-requirement-filter passes).

## Checklist

1. ✅ **Submission mechanism** — Confirmed current: fork `runelite/plugin-hub`, add a
   `plugins/sea-charting-quest-helper` file with `repository=` (this repo's `.git` URL) and
   `commit=` (40-char hash), open a PR. No other mechanism exists today.

2. ✅ **Automation / input-simulation rules** — The plugin only reads game state
   (`client.getVarbitValue`/`VarbitChanged`, quest state, Sailing level, player `WorldPoint`) and
   renders a Swing side panel. It performs zero mouse/keyboard simulation, no chatbox injection, no
   `Robot`/`KeyEvent`/`MouseEvent` synthesis anywhere in `src/main/`. The one "click" handler
   (`SeaChartingQuestHelperPanel.java`) is a normal Swing listener on the plugin's own list rows,
   not game input automation. This is squarely in the well-established "read state, draw overlay/
   panel" category the Plugin Hub already hosts (Quest Helper, Sailing, etc.).

3. ✅ **Optional Shortest Path integration** — `SeaChartingQuestHelperPlugin.onTaskClicked()` posts a
   `PluginMessage` to the `"shortestpath"` namespace with a `target` `WorldPoint`, using Shortest
   Path's own documented API. It's gated behind a config toggle (`shortestPathIntegration()`,
   defaults to `true` but user-controllable), and if Shortest Path isn't installed the message is
   simply never consumed — no compile-time dependency, no fallback behavior that walks/
   auto-navigates the player itself. Shortest Path itself only draws a route; it doesn't auto-walk.
   No rule crossed.

4. ✅ **`@PluginDependency`** — Grepped all of `src/`, `build.gradle`, and
   `runelite-plugin.properties`: zero matches. The only cross-plugin interaction is the soft,
   runtime-only `PluginMessage` described above. Confirmed still true after all recent agent
   passes (region fix, sorter, config toggles, gear-requirements work in the separate worktrees
   have not touched this).

5. ✅ **`mockito-core` scope** — `build.gradle` line 26: `testImplementation
   'org.mockito:mockito-core:5.11.0'`. Test-only scope, does not leak into the runtime artifact.
   Additionally, `runelite-plugin.properties` sets `build=standard`, which per the Plugin Hub
   README means the submitted `build.gradle`/`settings.gradle` are **replaced** by the hub's own
   template during the hub's build — so this dependency (and the whole custom `build.gradle`) is
   moot for the published artifact either way.

6. ⚠️ **License/attribution (OSRS Wiki data source)** — Two files now source small factual tables
   from the OSRS Wiki: `SeaChartRegion.java` (per-task sea/ocean classification via the wiki's
   `{{SeaChartRow}}` `sea=`/`ocean=` tags) and `SeaChartGearRequirements.java` (per-sea special
   boat-facility requirements via the wiki's per-sea hazard pages, added in the most recent commit
   on `main`). Both already carry thorough Javadoc explaining their own wiki sourcing and
   methodology, but the top-level `LICENSE` file — which is where this project's existing
   convention puts formal attribution (it already has a dedicated section for the LlemonDuck/
   sailing data) — didn't mention the Wiki at all. OSRS Wiki text is CC BY-NC-SA 3.0; a small
   factual "task N belongs to sea/ocean X" or "sea X needs facility Y" mapping is arguably an
   uncopyrightable fact rather than the wiki's creative prose, so this isn't a hard legal
   requirement, but crediting it in `LICENSE` is good practice and consistent with how this
   project already treats the LlemonDuck data. **Fixed**: added a second `---`-delimited section
   to `LICENSE` crediting the OSRS Wiki for both tables, mirroring the existing style. See
   "Fixes applied" below.

7. ✅ **Name collision** — GitHub search for `sea-charting-quest-helper` returns only this repo.
   Scanned the `plugin-hub/plugins` manifest directory for anything sailing/chart/quest-adjacent:
   existing entries are `sailing` (LlemonDuck), `chart-plotter` (Dazuzi), `deepseatrawling`
   (CarelessEsper), `bruhsailor` (Bobakanoosh), `quest-helper` (Zoinkwiz), `shortest-path`
   (Skretzo), `level-up-quest-requirements` (Hoffi-Coffi) — none share this plugin's name, Java
   package (`com.seachartingquesthelper`), or main class
   (`SeaChartingQuestHelperPlugin`). No collision.

8. ✅ **`tags=` convention** — Current line:
   `tags=sailing,charting,chart,sea,quest,helper,horizon,lure,checklist` — comma-separated,
   lowercase, no spaces. Compared against three real hub plugins' `runelite-plugin.properties`
   (`deepseatrawling`, `bruhsailor`, `level-up-quest-requirements`): lowercase/comma-separated is
   the norm (one existing plugin uses `, ` with a space after each comma, so formatting isn't
   strictly enforced, but this repo's no-space style matches the majority and is fine as-is).

9. ✅ **Icon** — `icon.png` added at the repo root (48x48, Sailing cape + Captain's log badge,
   composited from real OSRS item sprites, same technique as the Farming Profit plugin's icon).

10. ✅ **Required files present** — `LICENSE` (BSD 2-Clause, now with two attribution addenda),
    `README.md` (feature list, how-it-works, testing instructions, submission note),
    `runelite-plugin.properties` (all required fields populated), `build.gradle`
    (`build=standard`, JDK 11 target matching RuneLite's).

## Overall verdict

**Ready to submit.** Every item above is now ✅.

No blockers. No rule under either the Plugin Hub README or Jagex's Third Party Client Guidelines
is crossed — the plugin is a pure read-state-and-display panel with a graceful, opt-in,
no-dependency integration with an already-approved plugin (Shortest Path). This audit's fixes
were a documentation/attribution nicety and the Hub-listing icon — nothing functional or policy-related.

## Fixes applied in this pass

- `LICENSE`: added a second `---`-delimited attribution section crediting the OSRS Wiki
  (CC BY-NC-SA 3.0) as the source of the per-task sea/ocean classification
  (`SeaChartRegion.java`) and per-sea gear/hazard requirements (`SeaChartGearRequirements.java`),
  mirroring the existing LlemonDuck/sailing attribution section's style. No code changes; both
  files' own headers already have their own detailed sourcing Javadoc and were left untouched to
  avoid duplicating that explanation.
