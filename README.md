# Braindead Sea Charting

A [RuneLite](https://runelite.net) plugin for **Sailing**: a Quest Helper-style side panel for the
358-task sea-charting grind. Nearest incomplete task first, auto-advancing as you chart.

<img src="docs/panel.png" alt="Panel showing progress, filters, and the task list" width="280">

## Why

Sailing's sea charting has **358 individual chart tasks** spread across every ocean, each gated by
its own Sailing level (and sometimes a quest). The existing Plugin Hub **Sailing** plugin (by
LlemonDuck) highlights nearby chartable locations on the map/minimap once you're standing next to
one, but it doesn't show which of the 358 you're missing from anywhere, or what's fastest to do
next.

quest-helper already ships a working sea-charting sidebar
([`ChartingHelper`](https://github.com/Zoinkwiz/quest-helper/pull/2431), merged Nov 2025): per-sea
sections, a sorted/proximity toggle, auto-hiding finished seas, fading tasks you don't meet
requirements for. Two earlier Plugin Hub submissions covering similar ground
([#9556](https://github.com/runelite/plugin-hub/pull/9556),
[#9597](https://github.com/runelite/plugin-hub/pull/9597)) were closed citing it.

### Comparison to quest-helper's ChartingHelper

| | quest-helper's `ChartingHelper` | This plugin |
|---|---|---|
| Per-sea progress | Plain text, no exact count | Numeric **"(x/y)"** counter for both the specific sea and its broader ocean, live, each independently toggleable |
| Sort | Binary toggle: alphabetical or proximity | Weighted blend: proximity, but a sea's last 1-2 tasks get a bounded priority bump so you finish it before moving on (reasoning in `SeaChartTaskSorter`'s Javadoc); degrades to pure proximity for seas with lots left |
| Gear hazards | Not covered | Filters for adamant keel/helm, eternal brazier, inoculation station, mast upgrade, and raft access — sourced directly from the wiki's own per-task hazard data, not a hub-page summary |
| Sealed crate contents | Not covered | Hover a crate task for its actual in-game effect, colour-coded red if it's dangerous |
| Routing | None | Optional [Shortest Path](https://github.com/Skretzo/shortest-path) integration: click a row (or a two-stage task auto-fires it) and it draws a route |
| Two-stage tasks (Weather / Current duck) | Not specifically handled | Auto re-targets the route through every leg (troll → search area → back to troll for Weather; drop point → end point for Current duck) as the game reveals/starts each one |
| Review completed tasks | Not shown | Optional "Show completed" toggle lists already-charted tasks alongside the active ones |
| Overall progress | Not shown | "x/358" + a live percentage bar |

The smart-sort weighting, gear filters, routing integration, and two-stage auto-retargeting aren't
in `ChartingHelper` today. That's the case for this being a separate plugin rather than a
quest-helper PR.

## Features

- Side panel listing incomplete sea chart tasks, sorted by distance from your current position,
  recomputed every game tick while the panel is open.
- Each row shows the task's type icon, name, distance in tiles, and (if you haven't met the
  requirement yet) a grayed-out "Requires N Sailing" label, matching Quest Helper's locked-step
  styling.
- Auto-advance: completing a task flips its RuneLite varbit, picked up instantly via
  `VarbitChanged`. The task drops off the list and the panel re-sorts. No manual refresh.
- Type filter (Oddity / Spyglass / Sealed crate / Current duck / Diving / Weather checkboxes) and a
  "hide not-yet-reachable" toggle, persisted in config, for tasks whose level/quest gate you
  haven't cleared.
- Dangerous-water gear filters: toggles for "Hide needs adamant keel/helm+", "Hide needs eternal
  brazier", "Hide needs inoculation station", "Hide needs mast upgrade", and "Hide needs raft".
  Some seas' hazards (crystal-flecked/tangled-kelp waters, icy seas, fetid/disease waters, stormy
  seas) damage or slow an unprepared boat unless it has the matching facility built, and two tasks
  are only physically reachable with a raft; see "Known caveats" below for why these are manual
  toggles rather than an automatic check. Any task with a known requirement shows a "Needs: ..."
  line regardless of the filter state, so it's visible even with the filter off.
- Sealed crate tasks show an ⓘ icon on hover with the drink's real effect (colour-coded red if
  it's actively harmful), and any task with a wiki note worth knowing (raft/skiff access, a quest
  prerequisite, a low-level workaround) shows the same icon with that note.
- Independent "sea completion" and "ocean completion" toggles: each row can show its specific
  sea's count (e.g. "Shiverwake Expanse (2/5)") and/or its broader ocean's count (e.g. "Northern
  Ocean (68/75)") — two different granularities, shown or hidden separately.
- "Show completed" toggle: also lists already-charted tasks (marked with a checkmark) alongside
  the active ones, for reviewing what you've finished without digging through a separate log.
- Optional routing: clicking a task sends its location to the
  [Shortest Path](https://github.com/Skretzo/shortest-path) plugin, if installed, via its
  documented `PluginMessage` API. No compile-time dependency; if it's not installed, the message is
  never picked up.
- Two-stage task auto re-target (Weather / Current duck): these task types move partway through.
  Weather is three-phase — troll (collect the station) → search area (find the calm wind spot) →
  back to the troll (return it) — and the plugin re-targets on both legs: forward to the search
  area the moment "The troll hands you a portable weather station" prints, then back to the troll
  the moment "...You should now return to &lt;NPC&gt; where she gave you the weather station"
  prints. Current duck is two-phase and stays put: the destination is static task data known from
  the start, so the plugin re-targets right away on "You release your current duck and he begins
  tracking the currents..." rather than waiting for arrival, since escorting the duck to its
  endpoint is optional ([wiki](https://oldschool.runescape.wiki/w/Current_duck)). Each signal
  re-targets the Shortest Path route automatically, same as clicking that row manually — fixed
  after live testing turned up two real bugs: an earlier version only re-targeted if the task was
  already the clicked route target (almost never true in practice), and a later version treated
  Weather as two-phase and pointed the return leg at the search area instead of back at the troll.
- The rendered list caps at the nearest 40 matching tasks — re-rendering hundreds of Swing rows
  every tick isn't worth it, and "what's my next task" only needs the nearby few.

## How it works

- **Data source:** all 358 tasks' `WorldPoint` locations, completion `VarbitID`s, `ObjectID`/`NpcID`
  targets, and Sailing level requirements are `net.runelite.api.gameval` constants: official
  RuneLite game-data constants, not proprietary to any one plugin, so completion state
  (`client.getVarbitValue(...)`) is readable regardless of dependency. `SeaChartTask.java`
  mechanically compiles this table into its own enum, independent of any other plugin's code at
  compile time or runtime.
- **Auto-advance:** a `Map<Integer, SeaChartTask>` keyed by completion varbit lets
  `onVarbitChanged` update a `Set<SeaChartTask>` of completed tasks in O(1); the panel filters
  against that set and re-sorts by distance on every `GameTick` while visible.
- **Requirement gating:** each task type maps to one governing quest (Current Duck → Current
  Affairs, Sealed Crate → Prying Times, Diving → Recipe for Disaster: Pirate Pete, everything else
  → Pandemonium), checked against `QuestState.FINISHED` plus
  `client.getRealSkillLevel(Skill.SAILING)`.
- **Gear requirement mapping:** `SeaChartGearRequirements` maps each task's real sea
  (`task.getSea()`) to the boat facility it needs, sourced directly from the wiki's own per-task
  `hazard=` field (grouping all 358 rows by that tag, not a hub-page summary — see the class
  Javadoc for why that distinction matters): Crystal-flecked waters (Piscatoris Sea, Porth Gwenith,
  Porth Neigwl, Tirannwn Bight → adamant keel+), Tangled kelp (Rainbow Reef, Southern Expanse →
  adamant helm+), Icy seas (10 seas → eternal brazier), Fetid waters (5 seas → inoculation
  station), Stormy seas (Kharazi Strait, The Storm Tempor → mast upgrade). Keying by real sea
  rather than task name means it applies to every task in a hazardous sea regardless of what that
  specific task happens to be named — including one real case, task 107, a Weather task literally
  named "Zul Egil" whose actual sea is Porth Neigwl (see the class Javadoc). Raft access is a
  separate, genuinely per-task lookup (`SeaChartTaskNotes`-style, by task id) rather than per-sea,
  since e.g. "Grandroot Bay" is the name of four different tasks and only one of them needs a
  raft.
- **Nearest teleport:** `SeaChartSea` carries a real, per-sea nearest-teleport hint (70 individual
  seas, not 7 broad oceans), computed from each sea's actual wiki map coordinate against every
  standard teleport (spellbooks, jewelry, diary/quest items, fairy rings), excluding the two
  sailing-locked "Teleport to Boat" options since those only work once you already have a boat.
- **Per-task notes:** `SeaChartTaskNotes` surfaces the handful of tasks with a wiki annotation
  worth knowing (a raft/skiff being merely recommended vs. genuinely necessary, a quest-completion
  prerequisite, a low-level access workaround), shown in the same ⓘ tooltip as the crate effects.
- **Sealed crate effects:** `SeaChartCrateEffects` maps each of the 65 named Sealed-crate drinks to
  its real in-game effect (59 documented, 6 with no wiki page and left undocumented rather than
  guessed), shown on hover and colour-coded red if the effect is actively harmful.

### Credit

The sea chart task table (locations, completion varbits, object/npc ids, level requirements) is
mechanically compiled from the public data in the **Sailing** plugin by
[LlemonDuck](https://github.com/LlemonDuck/sailing) (BSD-2-Clause). Those are Jagex's public
`gameval` game-data constants, not that plugin's creative expression, but the compiled table itself
was real work and is credited in the LICENSE and in `SeaChartTask.java`'s header. No source code
from that project is reused; this plugin does not depend on it at compile time or runtime, and
only vendors a fresh copy of the data table as its own enum.

### Known caveats

- The panel checks Sailing level and quest state, but can't verify you physically have the boat
  upgrades (keel/helm/brazier/inoculation station/mast) needed to survive far oceans — RuneLite's
  client API has no "does my boat have an eternal brazier" getter, the same limitation Quest Helper
  has with F2P/members gates it can't detect. That's why the gear filters are manual toggles rather
  than an automatic eligibility check like "hide not-yet-reachable": the plugin can't know your
  boat's loadout.
- Sea-hazard gear requirements (keel/helm, brazier, inoculation station, mast) are keyed by each
  task's actual sea (`SeaChartTask#getSea()`), not its name, so they apply exhaustively regardless
  of whether a task happens to be named after its sea or after a flavour item; see
  `SeaChartGearRequirements`'s Javadoc for the one real task this fix corrected.
- Raft access: two tasks — Grandroot Bay (Current duck) and the "Black Lobster" Sealed crate —
  have a hard "a raft is necessary to reach this location" note in their own wiki description and
  are flagged with a "Requires raft" filter. This is looked up per task, not per sea, since
  "Grandroot Bay" is also the name of three other tasks (Weather/Spyglass/Mermaid guide) that don't
  need one. Four other tasks mention a raft too, but only as "a raft is recommended but not
  required" (or "a raft or skiff") — informational, not a real requirement, since the big boat can
  still get there, so those aren't flagged.

## Building

Requires JDK 11 (RuneLite's build target).

```sh
./gradlew build      # compiles + runs the unit tests
./gradlew run        # launches RuneLite in dev mode with this plugin loaded
```

## Testing it

- Sailing is live, so this is fully solo-testable. Open the panel near the sea, confirm the
  nearest-task sort matches what you'd expect standing where you are, sail to one, chart it, and
  confirm it drops off the list and the panel re-sorts without a manual reload.
- Cross-check a level-gated task, e.g. `TASK_52` (Rainbow Reef mermaid guide, level 72), shows as
  locked with "Requires 72 Sailing" until your Sailing level clears it.
- Toggle the type filter checkboxes and the "hide not-yet-reachable" box and confirm the list
  updates accordingly.
- Cross-check a gear-hazard task, e.g. `TASK_219` (Weiss Melt, icy sea), shows a "Needs: Eternal
  brazier" line, and toggling "Hide needs eternal brazier" removes it (and any other icy-sea task)
  from the list.
- If you run [Shortest Path](https://github.com/Skretzo/shortest-path), click a task row and
  confirm it draws a route to that location.

## Submitting to the Plugin Hub

Push to a public repo, then add a `plugins/sea-charting-quest-helper` file (`repository=...`,
`commit=...`) to a [`runelite/plugin-hub`](https://github.com/runelite/plugin-hub) fork and open a
PR. See the Plugin Hub README for the current rules.
