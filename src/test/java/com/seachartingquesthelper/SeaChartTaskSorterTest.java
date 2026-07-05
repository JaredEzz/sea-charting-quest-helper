package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Covers the smart-sort design decisions in {@link SeaChartTaskSorter}: the last task in a
 * nearly-finished sea should jump the queue locally, but never at the cost of a cross-map
 * detour, and busy seas must degrade to plain nearest-first.
 *
 * <p>Tasks are picked from the real enum by region <i>dynamically</i> (never by hardcoded region
 * constants), so these tests survive a future correction of {@link SeaChartRegion}'s boundaries.
 */
public class SeaChartTaskSorterTest
{
	@Test
	public void lastTaskClaimsFullBonusAndBusySeaClaimsAlmostNone()
	{
		// remaining=1 -> the whole avoided-return-trip bonus; remaining=40 -> a negligible share.
		assertEquals(500 - SeaChartTaskSorter.COMPLETION_BONUS_TILES,
			SeaChartTaskSorter.effectiveDistance(500, 1), 0.001);
		assertEquals(500 - SeaChartTaskSorter.COMPLETION_BONUS_TILES / 40,
			SeaChartTaskSorter.effectiveDistance(500, 40), 0.001);
		// A nonsense remaining count of 0 must not divide by zero or over-reward.
		assertEquals(SeaChartTaskSorter.effectiveDistance(500, 1),
			SeaChartTaskSorter.effectiveDistance(500, 0), 0.001);
	}

	@Test
	public void lastTaskInSeaOutranksSimilarlyDistantTaskInBusySea()
	{
		SeaChartTask lastInSea = SeaChartTask.values()[0];
		SeaChartTask inBusySea = taskInDifferentRegion(lastInSea);

		// The busy-sea task is even slightly CLOSER, but the last-remaining task should win.
		SeaChartTaskRow lastRow = new SeaChartTaskRow(lastInSea, 500, true, false);
		SeaChartTaskRow busyRow = new SeaChartTaskRow(inBusySea, 450, true, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(busyRow, lastRow));
		SeaChartTaskSorter.sort(rows, progress(lastInSea.getRegion(), 1, inBusySea.getRegion(), 30));

		assertEquals(lastInSea, rows.get(0).getTask());
	}

	@Test
	public void proximityIsTheTiebreakWhenCompletionStatusIsEqual()
	{
		SeaChartTask near = SeaChartTask.values()[0];
		SeaChartTask far = taskInSameRegion(near);

		SeaChartTaskRow nearRow = new SeaChartTaskRow(near, 200, true, false);
		SeaChartTaskRow farRow = new SeaChartTaskRow(far, 300, true, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(farRow, nearRow));
		SeaChartTaskSorter.sort(rows, progress(near.getRegion(), 10, near.getRegion(), 10));

		assertEquals(near, rows.get(0).getTask());
	}

	@Test
	public void proximityDominatesBetweenTwoBusySeas()
	{
		SeaChartTask near = SeaChartTask.values()[0];
		SeaChartTask far = taskInDifferentRegion(near);

		// Both seas have plenty remaining; the small difference in remaining counts (25 vs 30)
		// must not overcome a 200-tile proximity edge -- this is where nearest-first should hold.
		SeaChartTaskRow nearRow = new SeaChartTaskRow(near, 100, true, false);
		SeaChartTaskRow farRow = new SeaChartTaskRow(far, 300, true, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(farRow, nearRow));
		SeaChartTaskSorter.sort(rows, progress(near.getRegion(), 30, far.getRegion(), 25));

		assertEquals(near, rows.get(0).getTask());
	}

	@Test
	public void completionBonusIsBoundedSoLastTaskNeverForcesAHugeDetour()
	{
		SeaChartTask lastInSea = SeaChartTask.values()[0];
		SeaChartTask inBusySea = taskInDifferentRegion(lastInSea);

		// The last task in its sea is a 2000-tile cross-map sail; a busy-sea task is right here.
		// The bonus is capped at ~one inter-sea leg, so the nearby task must still win.
		SeaChartTaskRow lastRow = new SeaChartTaskRow(lastInSea, 2000, true, false);
		SeaChartTaskRow busyRow = new SeaChartTaskRow(inBusySea, 100, true, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(lastRow, busyRow));
		SeaChartTaskSorter.sort(rows, progress(lastInSea.getRegion(), 1, inBusySea.getRegion(), 30));

		assertEquals(inBusySea, rows.get(0).getTask());
	}

	@Test
	public void exactTiesFallBackToStableTaskIdOrder()
	{
		SeaChartTask first = SeaChartTask.values()[0];
		SeaChartTask second = taskInSameRegion(first);
		assertTrue(first.getTaskId() < second.getTaskId());

		SeaChartTaskRow firstRow = new SeaChartTaskRow(first, 250, true, false);
		SeaChartTaskRow secondRow = new SeaChartTaskRow(second, 250, true, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(secondRow, firstRow));
		SeaChartTaskSorter.sort(rows, progress(first.getRegion(), 5, first.getRegion(), 5));

		assertEquals(first, rows.get(0).getTask());
		assertEquals(second, rows.get(1).getTask());
	}

	@Test
	public void missingRegionProgressMeansNoBonusPureProximity()
	{
		SeaChartTask a = SeaChartTask.values()[0];
		SeaChartTask b = taskInDifferentRegion(a);

		SeaChartTaskRow aRow = new SeaChartTaskRow(a, 400, true, false);
		SeaChartTaskRow bRow = new SeaChartTaskRow(b, 300, true, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(aRow, bRow));
		SeaChartTaskSorter.sort(rows, new EnumMap<>(SeaChartRegion.class));

		assertEquals(b, rows.get(0).getTask());
		assertEquals(a, rows.get(1).getTask());
	}

	private static Map<SeaChartRegion, SeaChartRegionProgress> progress(
		SeaChartRegion regionA, int remainingA, SeaChartRegion regionB, int remainingB)
	{
		Map<SeaChartRegion, SeaChartRegionProgress> progress = new EnumMap<>(SeaChartRegion.class);
		// complete=0, total=remaining -> getRemaining() == remaining; only remaining matters here.
		progress.put(regionA, new SeaChartRegionProgress(0, remainingA));
		progress.put(regionB, new SeaChartRegionProgress(0, remainingB));
		return progress;
	}

	private static SeaChartTask taskInDifferentRegion(SeaChartTask other)
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task.getRegion() != other.getRegion())
			{
				return task;
			}
		}
		throw new AssertionError("all tasks share one region -- test setup impossible");
	}

	private static SeaChartTask taskInSameRegion(SeaChartTask other)
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task != other && task.getRegion() == other.getRegion())
			{
				return task;
			}
		}
		throw new AssertionError("no second task in " + other.getRegion() + " -- test setup impossible");
	}
}
