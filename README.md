# Braindead Sea Charting

A [RuneLite](https://runelite.net) plugin for **Sailing** that does the one thing no existing
plugin does — turns the 358-task sea-charting grind into a **Quest Helper-style side panel**:
nearest incomplete task first, auto-advancing the instant you chart something.

## Why

Sailing's sea charting has **358 individual chart tasks** spread across every ocean, each gated
by its own Sailing level (and sometimes a quest). The existing Plugin Hub **Sailing** plugin (by
LlemonDuck) highlights nearby chartable locations on the map/minimap once you're already standing
next to one — but there's no way to see, from anywhere, which of the 358 you're missing, which
one is nearest, or what's next once you finish one. That's the gap Quest Helper fills for quests:
a running list, sorted and auto-advancing, instead of "sail around and hope."

## Features

- **Side panel** listing incomplete sea chart tasks, sorted by distance from your current
  position — recomputed every game tick while the panel is open.
- Each row shows the task's **type icon**, name, **distance in tiles**, and (if you haven't met
  the requirement yet) a grayed-out **"Requires N Sailing"** label, mirroring Quest Helper's
  locked-step styling.
- **Auto-advance:** completing a task flips its RuneLite varbit, which the plugin picks up
  instantly via `VarbitChanged` — the task drops off the list and the panel re-sorts. No manual
  refresh.
- **Type filter** (Oddity / Spyglass / Sealed crate / Current duck / Diving / Weather checkboxes)
  and a **"hide not-yet-reachable"** toggle (persisted in config) for tasks whose level/quest gate
  you haven't cleared.
- **Dangerous-water gear filters:** three more toggles (persisted in config), one each for
  "Hide needs adamant keel/helm+", "Hide needs eternal brazier", "Hide needs inoculation station".
  Some seas' hazards (crystal-flecked/tangled-kelp waters, icy seas, fetid/disease waters) damage
  or slow an unprepared boat unless it has the matching facility built — see "Known caveats" below
  for why these are manual toggles rather than an automatic check. Any task with a known
  requirement also shows a "Needs: ..." line in its row, all-the-time, regardless of the filter
  state, so you can see it coming even with the filter off.
- **Optional routing:** clicking a task sends its location to the
  [Shortest Path](https://github.com/Skretzo/shortest-path) plugin (if installed and enabled) via
  its documented `PluginMessage` API, so it draws a route. No compile-time dependency on that
  plugin — if it's not installed, the message is simply never picked up.
- **Two-stage task auto re-target (Weather / Current duck):** these two task types move partway
  through. Weather: after finding the calm wind spot, the game prints "…You should now return to
  &lt;NPC&gt; where she gave you the weather station." and the relevant target becomes the weather
  troll. Current duck: the placed duck drifts to a predetermined end point, signalled by "Your
  current duck comes to a stop." ([wiki](https://oldschool.runescape.wiki/w/Current_duck)) — and
  you can sail straight to the end point without escorting it. When either signal fires, the
  plugin marks the task as stage-two: its panel distance switches to the **secondary location**,
  the row gains a "return the weather station" / "retrieve it at the end point" hint, and — if
  that task is the one you routed to — the Shortest Path route is **re-targeted automatically**,
  no re-click needed. A signal never redirects a route pointed at some unrelated task.
- The rendered list is capped to the nearest 40 matching tasks. Nobody wants to scroll a 358-row
  list, and re-rendering hundreds of Swing rows every tick would be wasteful — "what's my next
  task" only ever needs the nearby few.

## How it works (for the curious)

- **Data source:** all 358 tasks' `WorldPoint` locations, completion `VarbitID`s,
  `ObjectID`/`NpcID` targets and Sailing level requirements are `net.runelite.api.gameval`
  constants — official RuneLite game-data constants, not proprietary to any one plugin, so
  completion state (`client.getVarbitValue(...)`) is readable regardless of dependency.
  `SeaChartTask.java` mechanically compiles this table (see credit below) into its own enum,
  independent of any other plugin's code at compile time or runtime.
- **Auto-advance:** a `Map<Integer, SeaChartTask>` keyed by completion varbit lets
  `onVarbitChanged` update a `Set<SeaChartTask>` of completed tasks in O(1); the panel filters
  against that set and re-sorts by distance on every `GameTick` while visible.
- **Requirement gating:** each task type maps to one governing quest (mirroring the actual game
  design — Current Duck → Current Affairs, Sealed Crate → Prying Times, Diving → Recipe for
  Disaster: Pirate Pete, everything else → Pandemonium), checked against `QuestState.FINISHED`
  plus `client.getRealSkillLevel(Skill.SAILING)`.
- **Gear requirement mapping:** `SeaChartGearRequirements` maps each task's sea to the special
  boat facility it needs, per the OSRS Wiki's hazard pages — "Crystal-flecked waters" (Porth
  Gwenith, Porth Neigwl → adamant keel or better), "Tangled kelp" (Rainbow Reef, Southern Expanse
  → adamant helm or better), "Icy seas" (Weiss Melt, Everwinter Sea, Stoneheart Sea, Weissmere,
  Winter's Edge, Shiverwake Expanse → eternal brazier), "Fetid waters" (Backwater, Breakbone
  Strait, Mythic Sea, Sea of Souls, Zul-Egil → inoculation station). It matches by task name,
  which is the literal sea name for every Weather/Current Duck/Spyglass/Mermaid Guide task — the
  geographically-anchored majority. Sealed-crate/Oddity tasks are usually named after a flavour
  item instead (e.g. "Weiss Meltwater"), and the upstream table has no field linking them back to
  a sea, so a handful sitting in an otherwise-hazardous sea may not be individually flagged; treat
  an unflagged task as "not confirmed hazardous", not "confirmed safe" — the same caveat
  `SeaChartRegion` documents for ocean boundaries.

### Credit

The sea chart task table (locations, completion varbits, object/npc ids, level requirements) is
mechanically compiled from the public data in the **Sailing** plugin by
[LlemonDuck](https://github.com/LlemonDuck/sailing) (BSD-2-Clause). Those are Jagex's public
`gameval` game-data constants, not that plugin's creative expression — but the compiled table
itself was real work, so it's credited in the LICENSE and in `SeaChartTask.java`'s header. No
source code from that project is reused; this plugin does not depend on it at compile time or
runtime, and only vendors a fresh copy of the data table as its own enum.

### Known caveats

- The panel checks Sailing level and quest state, but **can't verify you physically have the boat
  upgrades** (keel/helm/brazier/inoculation station) needed to actually survive far oceans —
  RuneLite's client API has no "does my boat have an eternal brazier" getter, the same limitation
  Quest Helper has with F2P/members gates it can't detect. That's why the three gear filters
  (above) are manual toggles the player flips themselves once they know they're missing a
  facility, rather than an automatic eligibility check like "hide not-yet-reachable" — the plugin
  literally cannot know your boat's loadout.
- The gear-requirement mapping is name-based and not exhaustive for Sealed-crate/Oddity tasks —
  see `SeaChartGearRequirements`'s Javadoc.
- **Raft/skiff vs. big-boat access is not implemented — it isn't really a per-task mechanic.**
  Researched via the OSRS Wiki: Sailing does have three boat hull sizes (raft/skiff/sloop) with a
  real maneuverability difference (a sloop can't fit through some narrow channels a raft/skiff
  can), and this matters for general Sailing content (e.g. Barracuda Trial couriers squeezing
  through rapids). But scanning the *entire* 358-row sea-charting task table for any raft/skiff
  mention turned up exactly **one** task with any note at all, and that note reads "a raft is
  recommended but not required to reach this location." That's not a real per-task hard
  requirement worth building a filter/indicator around, so this plugin doesn't invent one — hiding
  or flagging tasks by a distinction that's essentially never load-bearing here would just be
  noise.

## Building

Requires JDK 11 (RuneLite's build target).

```sh
./gradlew build      # compiles + runs the unit tests
./gradlew run        # launches RuneLite in dev mode with this plugin loaded
```

## Testing it

- **Solo, fully testable right now:** Sailing is live. Open the panel near the sea, confirm the
  nearest-task sort matches what you'd expect standing where you are, sail to one, chart it, and
  confirm it drops off the list and the panel re-sorts without a manual reload.
- Cross-check a level-gated task — e.g. `TASK_52` (Rainbow Reef mermaid guide, level 72) — shows
  as locked with "Requires 72 Sailing" until your Sailing level actually clears it.
- Toggle the type filter checkboxes and the "hide not-yet-reachable" box and confirm the list
  updates accordingly.
- Cross-check a gear-hazard task — e.g. `TASK_219` (Weiss Melt, icy sea) — shows a "Needs: Eternal
  brazier" line, and toggling "Hide needs eternal brazier" removes it (and any other icy-sea task)
  from the list.
- If you run [Shortest Path](https://github.com/Skretzo/shortest-path), click a task row and
  confirm it draws a route to that location.

## Submitting to the Plugin Hub

Push to a public repo, then add a `plugins/sea-charting-quest-helper` file
(`repository=…`, `commit=…`) to a [`runelite/plugin-hub`](https://github.com/runelite/plugin-hub)
fork and open a PR. See the Plugin Hub README for the current rules.
