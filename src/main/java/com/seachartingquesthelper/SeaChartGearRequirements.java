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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps a sea-chart task to the special boat facility/facilities its sea requires.
 *
 * <p><b>Keyed by {@link SeaChartSea}, not task name.</b> An earlier version matched by {@code
 * taskName} instead, on the assumption that a task's name always equals its real sea for
 * Weather/Current Duck/Spyglass/Mermaid Guide tasks -- true for the vast majority, but not
 * universal: task 107 is a Weather task literally named "Zul Egil" whose real sea (per the wiki's
 * own {@code sea=} tag) is actually Porth Neigwl, so name-matching flagged it with the wrong
 * hazard (inoculation station instead of adamant keel/helm). Keying by {@link SeaChartSea} --
 * already independently cross-checked against {@link SeaChartTask#getRegion()} for all 358 tasks
 * -- fixes that task and, as a side effect, now also covers every Generic/Sealed-crate task with a
 * known sea (previously "not confirmed hazardous" simply because those types are usually named
 * after a flavour item rather than their sea, e.g. "Weiss Meltwater").
 *
 * <p>Sourced directly from the OSRS Wiki's "Sea charting" page's own per-task {@code
 * {{SeaChartRow}}} template data (the same 358-row table {@link SeaChartRegion}/{@link
 * SeaChartSea} use), which carries an explicit {@code hazard=} field per task. Grouping the raw
 * table's 358 rows by their {@code hazard=} value and then by {@code sea=} gives the exhaustive,
 * authoritative list below -- not derived from any hub page's prose (an earlier version did that
 * and, as a result, missed an entire hazard category, "Stormy seas", and two of four
 * Crystal-flecked-waters seas, Piscatoris Sea and Tirannwn Bight, that the individual hub pages
 * didn't make obvious).
 *
 * <p>The full authoritative breakdown, straight from the wiki's own {@code hazard=} tags (358
 * tasks total; 262 have no hazard tag at all):
 * <ul>
 * <li>{@code hazard=Crystal-flecked waters} (4 seas) -- Piscatoris Sea, Porth Gwenith, Porth
 * Neigwl, Tirannwn Bight. Requires an adamant keel or better; unprepared boats are slowed and dealt
 * 20 damage.
 * <li>{@code hazard=Tangled kelp} (2 seas) -- Rainbow Reef, Southern Expanse (both level-72-gated,
 * matching Sunbleak Island's level-72 mooring requirement). Requires an adamant helm or better.
 * <li>{@code hazard=Icy seas} (10 seas) -- Everwinter Sea, Idestia Strait, Kannski Tides, Lunar
 * Sea, Shiverwake Expanse, Stoneheart Sea, V's Belt, Weiss Melt, Weissmere, Winter's Edge. Requires
 * an eternal brazier; otherwise crew freeze and the ship takes increasing damage over time.
 * <li>{@code hazard=Fetid waters} (5 seas) -- Backwater, Breakbone Strait, Mythic Sea, Sea of
 * Souls, Zul-Egil. Requires an inoculation station; otherwise disease plus 5 damage per tick and a
 * severe speed penalty.
 * <li>{@code hazard=Stormy seas} (2 seas) -- Kharazi Strait, The Storm Tempor. Requires a boat mast
 * upgrade for storm resistance (Oak/Teak/Mahogany reduce lightning-cloud damage and slow;
 * Camphor/Ironwood/Rosewood prevent it entirely).
 * </ul>
 *
 * <p><b>Raft access is genuinely per-task, not per-sea</b>, so it's kept separate from the
 * sea-keyed map above and looked up by {@link SeaChartTask#getTaskId()} instead -- tagging it by
 * sea would have over-flagged, since e.g. "Grandroot Bay" is the name of four different tasks
 * (Current duck, Weather, Spyglass, Mermaid guide all in that sea) and only the Current duck one
 * actually needs a raft. Sourced from each task's own wiki description text (not a structured
 * field): scanning all 358 descriptions for "raft" finds 6 real hits (a 7th, task 21's "Crafting
 * Guild", is a substring false-positive) -- 4 read "a raft is recommended but not required to
 * reach this location" (or "a raft or skiff"), informational since the big boat can still get
 * there, so those aren't flagged. Only 2 use the harder "a raft is necessary to reach this
 * location" wording: task 210 (Grandroot Bay, Current duck) and task 261 ("Black Lobster", Sealed
 * crate). Those two are tagged {@link SeaChartGearRequirement#REQUIRES_RAFT}.
 */
final class SeaChartGearRequirements
{
	private SeaChartGearRequirements()
	{
	}

	private static final Map<SeaChartSea, Set<SeaChartGearRequirement>> BY_SEA = build();
	private static final Set<Integer> RAFT_REQUIRED_TASK_IDS = new HashSet<>(java.util.Arrays.asList(210, 261));

	private static Map<SeaChartSea, Set<SeaChartGearRequirement>> build()
	{
		Map<SeaChartSea, Set<SeaChartGearRequirement>> map = new EnumMap<>(SeaChartSea.class);
		tag(map, SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM,
			SeaChartSea.PISCATORIS_SEA, SeaChartSea.PORTH_GWENITH, SeaChartSea.PORTH_NEIGWL, SeaChartSea.TIRANNWN_BIGHT,
			SeaChartSea.RAINBOW_REEF, SeaChartSea.SOUTHERN_EXPANSE);
		tag(map, SeaChartGearRequirement.ETERNAL_BRAZIER,
			SeaChartSea.WEISS_MELT, SeaChartSea.EVERWINTER_SEA, SeaChartSea.STONEHEART_SEA, SeaChartSea.WEISSMERE,
			SeaChartSea.WINTERS_EDGE, SeaChartSea.SHIVERWAKE_EXPANSE, SeaChartSea.IDESTIA_STRAIT, SeaChartSea.LUNAR_SEA,
			SeaChartSea.KANNSKI_TIDES, SeaChartSea.VS_BELT);
		tag(map, SeaChartGearRequirement.INOCULATION_STATION,
			SeaChartSea.BACKWATER, SeaChartSea.BREAKBONE_STRAIT, SeaChartSea.MYTHIC_SEA, SeaChartSea.SEA_OF_SOULS,
			SeaChartSea.ZUL_EGIL);
		tag(map, SeaChartGearRequirement.MAST_UPGRADE, SeaChartSea.KHARAZI_STRAIT, SeaChartSea.THE_STORM_TEMPOR);
		return Collections.unmodifiableMap(map);
	}

	private static void tag(Map<SeaChartSea, Set<SeaChartGearRequirement>> map, SeaChartGearRequirement requirement, SeaChartSea... seas)
	{
		for (SeaChartSea sea : seas)
		{
			map.computeIfAbsent(sea, s -> EnumSet.noneOf(SeaChartGearRequirement.class)).add(requirement);
		}
	}

	/**
	 * @return the gear requirement(s) this task needs -- its sea's hazard (if any) plus
	 * {@link SeaChartGearRequirement#REQUIRES_RAFT} if this specific task is one of the two that
	 * needs one -- or an empty set if none apply.
	 */
	static Set<SeaChartGearRequirement> forTask(SeaChartTask task)
	{
		Set<SeaChartGearRequirement> seaRequirements = BY_SEA.get(task.getSea());
		if (!RAFT_REQUIRED_TASK_IDS.contains(task.getTaskId()))
		{
			return seaRequirements == null ? Collections.emptySet() : Collections.unmodifiableSet(seaRequirements);
		}

		Set<SeaChartGearRequirement> combined = seaRequirements == null
			? EnumSet.noneOf(SeaChartGearRequirement.class) : EnumSet.copyOf(seaRequirements);
		combined.add(SeaChartGearRequirement.REQUIRES_RAFT);
		return Collections.unmodifiableSet(combined);
	}
}
