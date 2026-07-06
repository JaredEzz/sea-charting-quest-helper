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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SeaChartingQuestHelperConfig.GROUP)
public interface SeaChartingQuestHelperConfig extends Config
{
	String GROUP = "seaChartingQuestHelper";

	// Key names shared with the panel's config-backed checkboxes, which persist through these
	// same keys so the sidebar toggles and the plugin config panel stay one source of truth.
	String KEY_HIDE_NOT_YET_REACHABLE = "hideNotYetReachable";
	/** Renamed from {@code showSeaCompletion}: this one is ocean-level (7 broad oceans). */
	String KEY_SHOW_OCEAN_COMPLETION = "showOceanCompletion";
	/** True per-sea completion (70 individually-named seas) -- see {@link SeaChartSea}. */
	String KEY_SHOW_SEA_COMPLETION = "showSeaCompletion";
	String KEY_SHOW_COMPLETED = "showCompleted";
	String KEY_SMART_SORT = "smartSort";
	String KEY_SHOW_NEAREST_PORT = "showNearestPort";
	String KEY_HIDE_NEEDS_ADAMANT_KEEL_OR_HELM = "hideNeedsAdamantKeelOrHelm";
	String KEY_HIDE_NEEDS_ETERNAL_BRAZIER = "hideNeedsEternalBrazier";
	String KEY_HIDE_NEEDS_INOCULATION_STATION = "hideNeedsInoculationStation";
	String KEY_HIDE_NEEDS_MAST_UPGRADE = "hideNeedsMastUpgrade";
	String KEY_HIDE_NEEDS_RAFT = "hideNeedsRaft";

	@ConfigItem(
		keyName = "hideNotYetReachable",
		name = "Hide not-yet-reachable",
		description = "Hide tasks whose Sailing level or quest requirement you haven't met yet",
		position = 0
	)
	default boolean hideNotYetReachable()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shortestPathIntegration",
		name = "Route with Shortest Path",
		description = "Clicking the nearest task sends its location to the Shortest Path plugin (if installed) to draw a route",
		position = 1
	)
	default boolean shortestPathIntegration()
	{
		return true;
	}

	@ConfigItem(
		keyName = KEY_SHOW_SEA_COMPLETION,
		name = "Show sea completion",
		description = "Show each task's specific sea and how many of its tasks you've charted, e.g. Shiverwake Expanse (2/5)",
		position = 2
	)
	default boolean showSeaCompletion()
	{
		return true;
	}

	@ConfigItem(
		keyName = KEY_SHOW_OCEAN_COMPLETION,
		name = "Show ocean completion",
		description = "Show each task's broader ocean and how many of its tasks you've charted, e.g. Northern Ocean (68/75)",
		position = 3
	)
	default boolean showOceanCompletion()
	{
		return true;
	}

	@ConfigItem(
		keyName = KEY_SHOW_COMPLETED,
		name = "Show completed",
		description = "Also list already-charted tasks (marked done), so you can review what you've finished",
		position = 4
	)
	default boolean showCompleted()
	{
		return false;
	}

	@ConfigItem(
		keyName = KEY_SMART_SORT,
		name = "Prioritise nearly-done seas",
		description = "Blend distance with per-sea completion so nearly-finished seas get closed out first; off = pure nearest-first",
		position = 5
	)
	default boolean smartSort()
	{
		return true;
	}

	@ConfigItem(
		keyName = KEY_SHOW_NEAREST_PORT,
		name = "Show nearest port hint",
		description = "Show the nearest port/teleport hint line for each task's sea",
		position = 6
	)
	default boolean showNearestPort()
	{
		return true;
	}

	@ConfigItem(
		keyName = KEY_HIDE_NEEDS_ADAMANT_KEEL_OR_HELM,
		name = "Hide needs adamant keel/helm+",
		description = "Hide tasks in crystal-flecked or tangled-kelp seas (e.g. Porth Gwenith, Rainbow Reef) if you don't have an adamant-tier keel/helm yet",
		position = 7
	)
	default boolean hideNeedsAdamantKeelOrHelm()
	{
		return false;
	}

	@ConfigItem(
		keyName = KEY_HIDE_NEEDS_ETERNAL_BRAZIER,
		name = "Hide needs eternal brazier",
		description = "Hide tasks in Northern icy seas (e.g. Weiss Melt, Everwinter Sea) if you don't have an eternal brazier yet",
		position = 8
	)
	default boolean hideNeedsEternalBrazier()
	{
		return false;
	}

	@ConfigItem(
		keyName = KEY_HIDE_NEEDS_INOCULATION_STATION,
		name = "Hide needs inoculation station",
		description = "Hide tasks in Shrouded disease seas (e.g. Backwater, Mythic Sea) if you don't have an inoculation station yet",
		position = 9
	)
	default boolean hideNeedsInoculationStation()
	{
		return false;
	}

	@ConfigItem(
		keyName = KEY_HIDE_NEEDS_MAST_UPGRADE,
		name = "Hide needs mast upgrade",
		description = "Hide tasks in stormy seas (Kharazi Strait, The Storm Tempor) if your boat doesn't have a mast upgrade yet",
		position = 10
	)
	default boolean hideNeedsMastUpgrade()
	{
		return false;
	}

	@ConfigItem(
		keyName = KEY_HIDE_NEEDS_RAFT,
		name = "Hide needs raft",
		description = "Hide the two tasks (Grandroot Bay, \"Black Lobster\") that need a raft to physically reach",
		position = 11
	)
	default boolean hideNeedsRaft()
	{
		return false;
	}
}
