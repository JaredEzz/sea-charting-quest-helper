package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * Sanity-checks the per-sea "(x/y)" completion counters against known task counts. Expected
 * totals are derived from {@link SeaChartTask#getRegion()} rather than hardcoded per-region
 * numbers, so these tests keep validating the aggregation itself even if the region boundaries
 * are corrected later.
 */
public class SeaChartRegionProgressTest
{
	@Test
	public void totalsCoverAll358TasksAndMatchPerRegionCounts()
	{
		Map<SeaChartRegion, SeaChartRegionProgress> progress =
			SeaChartRegionProgress.compute(EnumSet.noneOf(SeaChartTask.class));

		int totalAcrossRegions = 0;
		for (SeaChartRegionProgress p : progress.values())
		{
			totalAcrossRegions += p.getTotal();
		}
		assertEquals(SeaChartTask.values().length, totalAcrossRegions);
		assertEquals(358, totalAcrossRegions);

		// Each region's total must equal an independent count over the task table, and exactly
		// the regions that actually own tasks may appear.
		Map<SeaChartRegion, Integer> expected = new EnumMap<>(SeaChartRegion.class);
		for (SeaChartTask task : SeaChartTask.values())
		{
			expected.merge(task.getRegion(), 1, Integer::sum);
		}
		assertEquals(expected.keySet(), progress.keySet());
		expected.forEach((region, count) ->
			assertEquals(region + " total", (int) count, progress.get(region).getTotal()));
	}

	@Test
	public void nothingCompletedMeansZeroCompleteEverywhere()
	{
		Map<SeaChartRegion, SeaChartRegionProgress> progress =
			SeaChartRegionProgress.compute(EnumSet.noneOf(SeaChartTask.class));

		progress.forEach((region, p) ->
		{
			assertEquals(region + " complete", 0, p.getComplete());
			assertEquals(region + " remaining", p.getTotal(), p.getRemaining());
		});
	}

	@Test
	public void completedTasksAreCountedInTheirOwnRegion()
	{
		Set<SeaChartTask> completed = EnumSet.of(
			SeaChartTask.TASK_0, SeaChartTask.TASK_1, SeaChartTask.TASK_2, SeaChartTask.TASK_100);
		Map<SeaChartRegion, SeaChartRegionProgress> progress = SeaChartRegionProgress.compute(completed);

		for (SeaChartRegion region : progress.keySet())
		{
			long expectedComplete = completed.stream().filter(t -> t.getRegion() == region).count();
			SeaChartRegionProgress p = progress.get(region);
			assertEquals(region + " complete", expectedComplete, p.getComplete());
			assertEquals(region + " remaining", p.getTotal() - p.getComplete(), p.getRemaining());
		}

		int completeAcrossRegions = progress.values().stream()
			.mapToInt(SeaChartRegionProgress::getComplete)
			.sum();
		assertEquals(completed.size(), completeAcrossRegions);
	}

	@Test
	public void allTasksCompletedMeansEveryRegionFullyCharted()
	{
		Set<SeaChartTask> all = EnumSet.copyOf(Arrays.asList(SeaChartTask.values()));
		Map<SeaChartRegion, SeaChartRegionProgress> progress = SeaChartRegionProgress.compute(all);

		progress.forEach((region, p) ->
		{
			assertEquals(region + " should be fully charted", p.getTotal(), p.getComplete());
			assertEquals(region + " remaining", 0, p.getRemaining());
		});
	}
}
