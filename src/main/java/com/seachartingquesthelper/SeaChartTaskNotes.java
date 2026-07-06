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
import java.util.HashMap;
import java.util.Map;

/**
 * Per-task informational asides, sourced directly from the {@code ''Note: ...''} annotation on
 * each task's own row in the OSRS Wiki's "Sea charting" page (the same 358-row {@code
 * {{SeaChartRow}}} template data {@link SeaChartRegion}/{@link SeaChartSea} source from). Unlike
 * {@link SeaChartGearRequirements}'s sea-hazard mapping (keyed by {@link SeaChartSea}, since a
 * hazard applies to every task in that sea), this is keyed by the task's stable {@link
 * SeaChartTask#getTaskId()}, since a note is specific to one individual task -- the same reason
 * {@code SeaChartGearRequirements}'s raft requirement is also task-id-keyed rather than sea-keyed.
 *
 * <p>10 of the 358 tasks carry a note. Two ("A raft is necessary...", ids 210/261) are the hard
 * requirements already surfaced as {@link SeaChartGearRequirement#REQUIRES_RAFT}; task 170's "A
 * skiff is necessary..." is a similar hard access requirement but for a different, single task
 * and a different vessel, so it's left as a note rather than invented as its own one-task filter
 * category. The rest are genuinely just useful context (a raft/skiff being merely recommended, a
 * quest-completion prerequisite, or a level-38 access workaround) with no filter of their own.
 */
final class SeaChartTaskNotes
{
	private SeaChartTaskNotes()
	{
	}

	private static final Map<Integer, String> BY_TASK_ID = build();

	private static Map<Integer, String> build()
	{
		Map<Integer, String> map = new HashMap<>();
		map.put(39, "A raft is recommended but not required to reach this location.");
		map.put(81, "Accessing the Great Conch requires partial completion of Troubled Tortugans.");
		map.put(99, "Accessing the Great Conch requires partial completion of Troubled Tortugans.");
		map.put(126, "To do this task at 38 Sailing, you must board another player's boat -- one with an inoculation station, to survive the fetid waters.");
		map.put(168, "A raft or skiff is recommended but not required to reach this location.");
		map.put(170, "A skiff is necessary to reach this location.");
		map.put(210, "A raft is necessary to reach this location.");
		map.put(258, "A raft is recommended but not required to reach this location.");
		map.put(261, "A raft is necessary to reach this location.");
		map.put(346, "A raft is recommended but not required to reach this location.");
		return Collections.unmodifiableMap(map);
	}

	/**
	 * @return this task's wiki note, or {@code null} if it doesn't have one.
	 */
	static String forTaskId(int taskId)
	{
		return BY_TASK_ID.get(taskId);
	}
}
