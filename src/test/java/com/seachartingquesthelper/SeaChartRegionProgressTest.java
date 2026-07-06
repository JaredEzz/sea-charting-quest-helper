package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
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

	// -- projectedCompleteCounts: the display-only "(x/y)" running-count projection --

	@Test
	public void consecutiveSameSeaRowsIncrementOneByOne()
	{
		SeaChartRegion regionA = SeaChartTask.values()[0].getRegion();
		List<SeaChartTask> aTasks = tasksInRegion(regionA, 3);

		Map<SeaChartRegion, SeaChartRegionProgress> regionProgress = new EnumMap<>(SeaChartRegion.class);
		regionProgress.put(regionA, new SeaChartRegionProgress(25, 75));

		List<SeaChartTaskRow> sortedRows = Arrays.asList(
			new SeaChartTaskRow(aTasks.get(0), 10, true, false, false),
			new SeaChartTaskRow(aTasks.get(1), 20, true, false, false),
			new SeaChartTaskRow(aTasks.get(2), 30, true, false, false));

		List<Integer> projected = SeaChartRegionProgress.projectedCompleteCounts(sortedRows, regionProgress);

		// 1st Northern-style row shown: real current count. Each subsequent row in the same sea
		// climbs by one, as if the earlier-listed rows of that sea get charted first.
		assertEquals(Arrays.asList(25, 26, 27), projected);
	}

	@Test
	public void interspersedSameSeaRowsStillIncrementCorrectly()
	{
		SeaChartRegion regionA = SeaChartTask.values()[0].getRegion();
		SeaChartRegion regionB = differentRegion(regionA);
		List<SeaChartTask> aTasks = tasksInRegion(regionA, 3);
		List<SeaChartTask> bTasks = tasksInRegion(regionB, 2);

		Map<SeaChartRegion, SeaChartRegionProgress> regionProgress = new EnumMap<>(SeaChartRegion.class);
		regionProgress.put(regionA, new SeaChartRegionProgress(25, 75));
		regionProgress.put(regionB, new SeaChartRegionProgress(10, 50));

		// Interleaved: A, B, A, B, A -- other seas' rows in between must not disturb each sea's
		// own running count.
		List<SeaChartTaskRow> sortedRows = Arrays.asList(
			new SeaChartTaskRow(aTasks.get(0), 10, true, false, false),
			new SeaChartTaskRow(bTasks.get(0), 15, true, false, false),
			new SeaChartTaskRow(aTasks.get(1), 20, true, false, false),
			new SeaChartTaskRow(bTasks.get(1), 25, true, false, false),
			new SeaChartTaskRow(aTasks.get(2), 30, true, false, false));

		List<Integer> projected = SeaChartRegionProgress.projectedCompleteCounts(sortedRows, regionProgress);

		assertEquals(Arrays.asList(25, 10, 26, 11, 27), projected);
	}

	@Test
	public void projectionNeverMutatesTheRegionProgressUsedForSortScoring()
	{
		SeaChartRegion regionA = SeaChartTask.values()[0].getRegion();
		SeaChartRegion regionB = differentRegion(regionA);
		List<SeaChartTask> aTasks = tasksInRegion(regionA, 3);

		Map<SeaChartRegion, SeaChartRegionProgress> regionProgress = new EnumMap<>(SeaChartRegion.class);
		regionProgress.put(regionA, new SeaChartRegionProgress(25, 75));
		regionProgress.put(regionB, new SeaChartRegionProgress(10, 50));

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(
			new SeaChartTaskRow(aTasks.get(0), 500, true, false, false),
			new SeaChartTaskRow(aTasks.get(1), 450, true, false, false),
			new SeaChartTaskRow(aTasks.get(2), 400, true, false, false)));

		// A real, stable sort (plain distance -- SeaChartTaskSorter's own smart-sort scoring is
		// keyed by SeaChartSea, not SeaChartRegion, and is covered separately by
		// SeaChartTaskSorterTest; this test only needs some real, reproducible order to check
		// projection immutability against).
		rows.sort(Comparator.comparingInt(SeaChartTaskRow::getDistance));
		List<SeaChartTask> orderBeforeProjection = new ArrayList<>();
		for (SeaChartTaskRow row : rows)
		{
			orderBeforeProjection.add(row.getTask());
		}

		// Running the display-only projection afterwards must not perturb the region progress
		// map (used for scoring) or the already-sorted row order.
		SeaChartRegionProgress.projectedCompleteCounts(rows, regionProgress);

		assertEquals(25, regionProgress.get(regionA).getComplete());
		assertEquals(75, regionProgress.get(regionA).getTotal());
		assertEquals(10, regionProgress.get(regionB).getComplete());
		assertEquals(50, regionProgress.get(regionB).getTotal());

		List<SeaChartTask> orderAfterProjection = new ArrayList<>();
		for (SeaChartTaskRow row : rows)
		{
			orderAfterProjection.add(row.getTask());
		}
		assertEquals(orderBeforeProjection, orderAfterProjection);

		// Re-sorting a fresh copy of the same rows reproduces the identical order -- a sanity check
		// that the earlier projection call didn't leave any hidden state behind anywhere.
		List<SeaChartTaskRow> freshRows = new ArrayList<>(Arrays.asList(
			new SeaChartTaskRow(aTasks.get(2), 400, true, false, false),
			new SeaChartTaskRow(aTasks.get(0), 500, true, false, false),
			new SeaChartTaskRow(aTasks.get(1), 450, true, false, false)));
		freshRows.sort(Comparator.comparingInt(SeaChartTaskRow::getDistance));
		List<SeaChartTask> orderAfterFreshSort = new ArrayList<>();
		for (SeaChartTaskRow row : freshRows)
		{
			orderAfterFreshSort.add(row.getTask());
		}
		assertEquals(orderBeforeProjection, orderAfterFreshSort);
	}

	private static List<SeaChartTask> tasksInRegion(SeaChartRegion region, int count)
	{
		List<SeaChartTask> found = new ArrayList<>();
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task.getRegion() == region)
			{
				found.add(task);
				if (found.size() == count)
				{
					break;
				}
			}
		}
		assertTrue("not enough tasks in " + region + " for this test", found.size() == count);
		return found;
	}

	private static SeaChartRegion differentRegion(SeaChartRegion other)
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task.getRegion() != other)
			{
				return task.getRegion();
			}
		}
		throw new AssertionError("all tasks share one region -- test setup impossible");
	}
}
