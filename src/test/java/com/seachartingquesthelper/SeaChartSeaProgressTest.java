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
 * Sanity-checks the true per-individual-sea "(x/y)" completion counters against known task
 * counts. Expected totals are derived from {@link SeaChartTask#getSea()} rather than hardcoded
 * per-sea numbers, so these tests keep validating the aggregation itself even if the sea data is
 * corrected later. Mirrors {@link SeaChartRegionProgressTest}'s structure, one level more
 * granular -- see {@link SeaChartSea}'s class Javadoc for why both exist separately.
 */
public class SeaChartSeaProgressTest
{
	@Test
	public void totalsCoverAll358TasksAndMatchPerSeaCounts()
	{
		Map<SeaChartSea, SeaChartSeaProgress> progress =
			SeaChartSeaProgress.compute(EnumSet.noneOf(SeaChartTask.class));

		int totalAcrossSeas = 0;
		for (SeaChartSeaProgress p : progress.values())
		{
			totalAcrossSeas += p.getTotal();
		}
		assertEquals(SeaChartTask.values().length, totalAcrossSeas);
		assertEquals(358, totalAcrossSeas);

		Map<SeaChartSea, Integer> expected = new EnumMap<>(SeaChartSea.class);
		for (SeaChartTask task : SeaChartTask.values())
		{
			expected.merge(task.getSea(), 1, Integer::sum);
		}
		assertEquals(expected.keySet(), progress.keySet());
		expected.forEach((sea, count) ->
			assertEquals(sea + " total", (int) count, progress.get(sea).getTotal()));
	}

	@Test
	public void nothingCompletedMeansZeroCompleteEverywhere()
	{
		Map<SeaChartSea, SeaChartSeaProgress> progress =
			SeaChartSeaProgress.compute(EnumSet.noneOf(SeaChartTask.class));

		progress.forEach((sea, p) ->
		{
			assertEquals(sea + " complete", 0, p.getComplete());
			assertEquals(sea + " remaining", p.getTotal(), p.getRemaining());
		});
	}

	@Test
	public void completedTasksAreCountedInTheirOwnSea()
	{
		Set<SeaChartTask> completed = EnumSet.of(
			SeaChartTask.TASK_0, SeaChartTask.TASK_1, SeaChartTask.TASK_2, SeaChartTask.TASK_100);
		Map<SeaChartSea, SeaChartSeaProgress> progress = SeaChartSeaProgress.compute(completed);

		for (SeaChartSea sea : progress.keySet())
		{
			long expectedComplete = completed.stream().filter(t -> t.getSea() == sea).count();
			SeaChartSeaProgress p = progress.get(sea);
			assertEquals(sea + " complete", expectedComplete, p.getComplete());
			assertEquals(sea + " remaining", p.getTotal() - p.getComplete(), p.getRemaining());
		}

		int completeAcrossSeas = progress.values().stream()
			.mapToInt(SeaChartSeaProgress::getComplete)
			.sum();
		assertEquals(completed.size(), completeAcrossSeas);
	}

	@Test
	public void allTasksCompletedMeansEverySeaFullyCharted()
	{
		Set<SeaChartTask> all = EnumSet.copyOf(Arrays.asList(SeaChartTask.values()));
		Map<SeaChartSea, SeaChartSeaProgress> progress = SeaChartSeaProgress.compute(all);

		progress.forEach((sea, p) ->
		{
			assertEquals(sea + " should be fully charted", p.getTotal(), p.getComplete());
			assertEquals(sea + " remaining", 0, p.getRemaining());
		});
	}

	// -- projectedCompleteCounts: the display-only "(x/y)" running-count projection --

	@Test
	public void consecutiveSameSeaRowsIncrementOneByOne()
	{
		SeaChartSea seaA = SeaChartTask.values()[0].getSea();
		List<SeaChartTask> aTasks = tasksInSea(seaA, 3);

		Map<SeaChartSea, SeaChartSeaProgress> seaProgress = new EnumMap<>(SeaChartSea.class);
		seaProgress.put(seaA, new SeaChartSeaProgress(2, 5));

		List<SeaChartTaskRow> sortedRows = Arrays.asList(
			new SeaChartTaskRow(aTasks.get(0), 10, true, false, false),
			new SeaChartTaskRow(aTasks.get(1), 20, true, false, false),
			new SeaChartTaskRow(aTasks.get(2), 30, true, false, false));

		List<Integer> projected = SeaChartSeaProgress.projectedCompleteCounts(sortedRows, seaProgress);

		// 1st row shown: real current count. Each subsequent row in the same sea climbs by one, as
		// if the earlier-listed rows of that sea get charted first.
		assertEquals(Arrays.asList(2, 3, 4), projected);
	}

	@Test
	public void interspersedSameSeaRowsStillIncrementCorrectly()
	{
		SeaChartSea seaA = SeaChartTask.values()[0].getSea();
		SeaChartSea seaB = differentSea(seaA);
		List<SeaChartTask> aTasks = tasksInSea(seaA, 3);
		List<SeaChartTask> bTasks = tasksInSea(seaB, 2);

		Map<SeaChartSea, SeaChartSeaProgress> seaProgress = new EnumMap<>(SeaChartSea.class);
		seaProgress.put(seaA, new SeaChartSeaProgress(2, 5));
		seaProgress.put(seaB, new SeaChartSeaProgress(1, 4));

		// Interleaved: A, B, A, B, A -- other seas' rows in between must not disturb each sea's
		// own running count.
		List<SeaChartTaskRow> sortedRows = Arrays.asList(
			new SeaChartTaskRow(aTasks.get(0), 10, true, false, false),
			new SeaChartTaskRow(bTasks.get(0), 15, true, false, false),
			new SeaChartTaskRow(aTasks.get(1), 20, true, false, false),
			new SeaChartTaskRow(bTasks.get(1), 25, true, false, false),
			new SeaChartTaskRow(aTasks.get(2), 30, true, false, false));

		List<Integer> projected = SeaChartSeaProgress.projectedCompleteCounts(sortedRows, seaProgress);

		assertEquals(Arrays.asList(2, 1, 3, 2, 4), projected);
	}

	@Test
	public void projectionNeverMutatesTheSeaProgressMap()
	{
		SeaChartSea seaA = SeaChartTask.values()[0].getSea();
		SeaChartSea seaB = differentSea(seaA);
		List<SeaChartTask> aTasks = tasksInSea(seaA, 3);

		Map<SeaChartSea, SeaChartSeaProgress> seaProgress = new EnumMap<>(SeaChartSea.class);
		seaProgress.put(seaA, new SeaChartSeaProgress(2, 5));
		seaProgress.put(seaB, new SeaChartSeaProgress(1, 4));

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(
			new SeaChartTaskRow(aTasks.get(0), 500, true, false, false),
			new SeaChartTaskRow(aTasks.get(1), 450, true, false, false),
			new SeaChartTaskRow(aTasks.get(2), 400, true, false, false)));
		rows.sort(Comparator.comparingInt(SeaChartTaskRow::getDistance));

		SeaChartSeaProgress.projectedCompleteCounts(rows, seaProgress);

		assertEquals(2, seaProgress.get(seaA).getComplete());
		assertEquals(5, seaProgress.get(seaA).getTotal());
		assertEquals(1, seaProgress.get(seaB).getComplete());
		assertEquals(4, seaProgress.get(seaB).getTotal());
	}

	private static List<SeaChartTask> tasksInSea(SeaChartSea sea, int count)
	{
		List<SeaChartTask> found = new ArrayList<>();
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task.getSea() == sea)
			{
				found.add(task);
				if (found.size() == count)
				{
					break;
				}
			}
		}
		assertTrue("not enough tasks in " + sea + " for this test", found.size() == count);
		return found;
	}

	private static SeaChartSea differentSea(SeaChartSea other)
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task.getSea() != other)
			{
				return task.getSea();
			}
		}
		throw new AssertionError("all tasks share one sea -- test setup impossible");
	}
}
