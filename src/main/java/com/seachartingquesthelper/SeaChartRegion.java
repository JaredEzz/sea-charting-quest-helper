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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The named sea-charting oceans, purely so each task can point at the closest port with a
 * reasonably direct teleport -- not just its raw tile distance.
 *
 * <p><b>Boundaries are approximate.</b> {@link #forTaskId} carries forward an index-range-to-ocean
 * breakdown compiled from account-side WikiSync progress notes, not an authoritative per-task
 * ocean field (the upstream task table this plugin mechanically compiles from has no such field --
 * see {@link SeaChartTask}). Spot-checking {@code WorldPoint}s against real geography during
 * development showed the ranges hold up well through roughly task 175 (Ardent/Unquiet/Shrouded),
 * but get noticeably less reliable past that: the "Sunset" range (180-192) mixes Piscarilius/
 * Kourend coordinates with Menaphos-desert coordinates, the "Western" range (194-249) is
 * overwhelmingly Fremennik/Weiss/icy-sea coordinates (i.e. actually Northern Ocean geography), and
 * the tasks that actually match Western Ocean's real place names (Great Sound, Crabclaw Bay,
 * Hosidian Sea, Pilgrims' Passage, Crystal Sea, Litus Lucis, Moonshadow, Vagabonds Rest) mostly
 * live around index 314-330, inside what this enum labels {@link #NORTHERN_MISC}. Treat the port
 * hint as a directional nudge, not a precise ocean boundary.
 */
@Getter
@RequiredArgsConstructor
enum SeaChartRegion
{
	ARDENT("Ardent Ocean", "The Pandemonium (bank + shipwright)"),
	UNQUIET("Unquiet Ocean", "Dognose Island"),
	SHROUDED("Shrouded Ocean", "Deepfin Point (shipwright, bank)"),
	SUNSET("Sunset Ocean", "Aldarin tele (or Sunset Coast)"),
	WESTERN("Western Ocean", "Kourend Castle tele -> Land's End dock"),
	NORTHERN_MISC("Northern Ocean", "Rellekka (full facilities)"),
	;

	private final String label;
	private final String nearestPort;

	/**
	 * Maps a task's stable {@link SeaChartTask#getTaskId()} (0-357) to its approximate ocean, per
	 * the boundaries documented on this enum's class-level Javadoc.
	 */
	static SeaChartRegion forTaskId(int taskId)
	{
		if (taskId <= 67)
		{
			return ARDENT;
		}
		if (taskId <= 103)
		{
			return UNQUIET;
		}
		if (taskId <= 177)
		{
			return SHROUDED;
		}
		if (taskId <= 192)
		{
			return SUNSET;
		}
		if (taskId <= 249)
		{
			return WESTERN;
		}
		return NORTHERN_MISC;
	}
}
