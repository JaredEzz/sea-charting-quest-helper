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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/**
 * Per-individual-sea charting progress: how many of a specific sea's tasks (e.g. "Shiverwake
 * Expanse", not the whole "Northern Ocean") are complete out of how many exist there. Recomputed
 * from the live completed-task set each refresh, so the panel's "(3/5)" markers advance the
 * moment a completion varbit flips.
 *
 * <p>This is the true-sea-granularity counterpart to {@link SeaChartRegionProgress} (which
 * operates at the coarser 7-ocean level) -- see {@link SeaChartSea}'s class Javadoc for why both
 * exist separately. {@link SeaChartTaskSorter} uses this one for its "almost-done sea" bonus.
 */
@Value
class SeaChartSeaProgress
{
	int complete;
	int total;

	int getRemaining()
	{
		return total - complete;
	}

	/**
	 * Aggregates the given completed set into per-sea counters. Sea membership comes from
	 * {@link SeaChartTask#getSea()}, so any future correction to the sea data flows through here
	 * automatically.
	 */
	static Map<SeaChartSea, SeaChartSeaProgress> compute(Set<SeaChartTask> completed)
	{
		Map<SeaChartSea, int[]> counts = new EnumMap<>(SeaChartSea.class);
		for (SeaChartTask task : SeaChartTask.values())
		{
			int[] c = counts.computeIfAbsent(task.getSea(), s -> new int[2]);
			c[1]++;
			if (completed.contains(task))
			{
				c[0]++;
			}
		}

		Map<SeaChartSea, SeaChartSeaProgress> progress = new EnumMap<>(SeaChartSea.class);
		counts.forEach((sea, c) -> progress.put(sea, new SeaChartSeaProgress(c[0], c[1])));
		return progress;
	}

	/**
	 * DISPLAY-ONLY: projects, for each row in {@code sortedRows} (already sorted <b>and</b>
	 * filtered to whatever the panel currently shows), what that sea's "complete" count would be
	 * if every same-sea row earlier in this exact list order were charted first. See {@link
	 * SeaChartRegionProgress#projectedCompleteCounts} for the full rationale -- identical logic,
	 * just keyed by the true sea instead of the ocean.
	 *
	 * @return a list the same size and order as {@code sortedRows}, each entry being the
	 * projected "complete" count to display for that row's sea (its {@code total} is unchanged --
	 * read it straight from {@code seaProgress})
	 */
	static List<Integer> projectedCompleteCounts(List<SeaChartTaskRow> sortedRows,
		Map<SeaChartSea, SeaChartSeaProgress> seaProgress)
	{
		Map<SeaChartSea, Integer> runningComplete = new EnumMap<>(SeaChartSea.class);
		List<Integer> projected = new ArrayList<>(sortedRows.size());
		for (SeaChartTaskRow row : sortedRows)
		{
			SeaChartSea sea = row.getTask().getSea();
			int baseline = runningComplete.computeIfAbsent(sea, s ->
			{
				SeaChartSeaProgress p = seaProgress.get(s);
				return p == null ? 0 : p.getComplete();
			});
			projected.add(baseline);
			runningComplete.put(sea, baseline + 1);
		}
		return projected;
	}
}
