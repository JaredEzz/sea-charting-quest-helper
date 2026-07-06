/*
 * Copyright (c) 2026, JaredEzz <me@jaredezz.tech>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.seachartingquesthelper;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps a sea-chart task to the special boat facility/facilities its sea requires, per the OSRS
 * Wiki's per-sea hazard pages.
 *
 * <p><b>Best-effort, name-based, not exhaustive.</b> The upstream task table (see {@link
 * SeaChartTask}) has no per-task hazard/gear field, so this maps by matching each task's {@code
 * taskName} against the wiki's confirmed hazard sea names below. That name literally <em>is</em>
 * the sea name for every Weather / Current Duck / Spyglass / Mermaid Guide task -- the vast
 * majority of geographically-anchored tasks -- so this covers those directly and exhaustively
 * (verified by cross-referencing every matching task in {@code SeaChartTask} against the wiki's
 * location lists below). Generic and Sealed-crate ("drink crate") tasks are usually named after a
 * flavour item instead (e.g. "Weiss Meltwater", "Crystal Water") rather than their sea, and the
 * upstream table has no field linking them back to a sea, so a handful of those sitting in an
 * otherwise-hazardous sea may not be individually flagged here. This is the same class of
 * approximation {@link SeaChartRegion} documents for ocean boundaries -- treat a task with no
 * flagged requirement as "not confirmed hazardous", not "confirmed safe".
 *
 * <p>Sources (OSRS Wiki, 2026):
 * <ul>
 * <li>"Crystal-flecked waters" -- Porth Gwenith, Porth Neigwl. Requires an adamant keel or better;
 * unprepared boats are slowed and dealt 20 damage.
 * <li>"Tangled kelp" -- near the Isle of Serpents / Sunbleak Island, i.e. this plugin's Rainbow
 * Reef and Southern Expanse chart tasks (both level-72-gated, matching Sunbleak Island's level-72
 * mooring requirement). Requires an adamant helm or better.
 * <li>"Icy seas" -- Weiss Melt, Everwinter Sea, Stoneheart Sea, Weissmere, Winter's Edge,
 * Shiverwake Expanse, Idestia Strait, Lunar Sea, Kannski Tides, V's Belt (all ten seas the wiki's
 * dedicated "Icy seas" page lists, confirmed against the Northern Ocean's full sea list -- an
 * earlier version of this class only had the first six). Requires an eternal brazier; otherwise
 * crew freeze and the ship takes increasing damage over time.
 * <li>"Fetid waters" -- Backwater, Breakbone Strait, Mythic Sea, Sea of Souls, Zul-Egil. Requires
 * an inoculation station; otherwise disease plus 5 damage per tick and a severe speed penalty.
 * </ul>
 */
final class SeaChartGearRequirements
{
	private SeaChartGearRequirements()
	{
	}

	private static final Map<String, Set<SeaChartGearRequirement>> BY_SEA_NAME = build();

	private static Map<String, Set<SeaChartGearRequirement>> build()
	{
		Map<String, Set<SeaChartGearRequirement>> map = new HashMap<>();
		tag(map, SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM, "Porth Gwenith", "Porth Neigwl", "Rainbow Reef", "Southern Expanse");
		tag(map, SeaChartGearRequirement.ETERNAL_BRAZIER, "Weiss Melt", "Everwinter Sea", "Stoneheart Sea", "Weissmere", "Winters Edge", "Shiverwake Expanse", "Idestia Strait", "Lunar Sea", "Kannski Tides", "Vs Belt");
		tag(map, SeaChartGearRequirement.INOCULATION_STATION, "Backwater", "Breakbone Strait", "Mythic Sea", "Sea Of Souls", "Zul Egil");
		return Collections.unmodifiableMap(map);
	}

	private static void tag(Map<String, Set<SeaChartGearRequirement>> map, SeaChartGearRequirement requirement, String... seaNames)
	{
		for (String seaName : seaNames)
		{
			map.computeIfAbsent(seaName, k -> EnumSet.noneOf(SeaChartGearRequirement.class)).add(requirement);
		}
	}

	/**
	 * @return the gear requirement(s) this task's sea is confirmed to need, or an empty set if
	 * none are confirmed (see class Javadoc caveat about drink-crate/generic tasks).
	 */
	static Set<SeaChartGearRequirement> forTaskName(String taskName)
	{
		Set<SeaChartGearRequirement> found = BY_SEA_NAME.get(taskName);
		return found == null ? Collections.emptySet() : Collections.unmodifiableSet(found);
	}
}
