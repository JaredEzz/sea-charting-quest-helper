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

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/**
 * Per-sea/ocean charting progress: how many of a region's tasks are complete out of how many
 * exist there. Recomputed from the live completed-task set each refresh, so the panel's
 * "(3/12)" markers advance the moment a completion varbit flips.
 */
@Value
class SeaChartRegionProgress
{
	int complete;
	int total;

	int getRemaining()
	{
		return total - complete;
	}

	/**
	 * Aggregates the given completed set into per-region counters. Region membership comes from
	 * {@link SeaChartTask#getRegion()}, so any future correction to the region boundaries flows
	 * through here automatically.
	 */
	static Map<SeaChartRegion, SeaChartRegionProgress> compute(Set<SeaChartTask> completed)
	{
		Map<SeaChartRegion, int[]> counts = new EnumMap<>(SeaChartRegion.class);
		for (SeaChartTask task : SeaChartTask.values())
		{
			int[] c = counts.computeIfAbsent(task.getRegion(), r -> new int[2]);
			c[1]++;
			if (completed.contains(task))
			{
				c[0]++;
			}
		}

		Map<SeaChartRegion, SeaChartRegionProgress> progress = new EnumMap<>(SeaChartRegion.class);
		counts.forEach((region, c) -> progress.put(region, new SeaChartRegionProgress(c[0], c[1])));
		return progress;
	}
}
