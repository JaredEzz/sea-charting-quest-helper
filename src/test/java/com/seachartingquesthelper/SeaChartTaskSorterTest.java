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
 * <p>Tasks are picked from the real enum by sea <i>dynamically</i> (never by hardcoded sea
 * constants), so these tests survive a future correction of {@link SeaChartSea}'s data.
 *
 * <p>Keyed by {@link SeaChartSea} (the true ~70 individual seas), not {@link SeaChartRegion} (the
 * 7 broad oceans) -- an earlier version of {@link SeaChartTaskSorter} used the region, which meant
 * "remaining" almost never dropped low enough within an entire ocean to trigger a meaningful
 * bonus. See {@link SeaChartTaskSorter}'s class Javadoc.
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
		SeaChartTask inBusySea = taskInDifferentSea(lastInSea);

		// The busy-sea task is even slightly CLOSER, but the last-remaining task should win.
		SeaChartTaskRow lastRow = new SeaChartTaskRow(lastInSea, 500, true, false, false);
		SeaChartTaskRow busyRow = new SeaChartTaskRow(inBusySea, 450, true, false, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(busyRow, lastRow));
		SeaChartTaskSorter.sort(rows, progress(lastInSea.getSea(), 1, inBusySea.getSea(), 30));

		assertEquals(lastInSea, rows.get(0).getTask());
	}

	@Test
	public void proximityIsTheTiebreakWhenCompletionStatusIsEqual()
	{
		SeaChartTask near = SeaChartTask.values()[0];
		SeaChartTask far = taskInSameSea(near);

		SeaChartTaskRow nearRow = new SeaChartTaskRow(near, 200, true, false, false);
		SeaChartTaskRow farRow = new SeaChartTaskRow(far, 300, true, false, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(farRow, nearRow));
		SeaChartTaskSorter.sort(rows, progress(near.getSea(), 10, near.getSea(), 10));

		assertEquals(near, rows.get(0).getTask());
	}

	@Test
	public void proximityDominatesBetweenTwoBusySeas()
	{
		SeaChartTask near = SeaChartTask.values()[0];
		SeaChartTask far = taskInDifferentSea(near);

		// Both seas have plenty remaining; the small difference in remaining counts (25 vs 30)
		// must not overcome a 200-tile proximity edge -- this is where nearest-first should hold.
		SeaChartTaskRow nearRow = new SeaChartTaskRow(near, 100, true, false, false);
		SeaChartTaskRow farRow = new SeaChartTaskRow(far, 300, true, false, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(farRow, nearRow));
		SeaChartTaskSorter.sort(rows, progress(near.getSea(), 30, far.getSea(), 25));

		assertEquals(near, rows.get(0).getTask());
	}

	@Test
	public void completionBonusIsBoundedSoLastTaskNeverForcesAHugeDetour()
	{
		SeaChartTask lastInSea = SeaChartTask.values()[0];
		SeaChartTask inBusySea = taskInDifferentSea(lastInSea);

		// The last task in its sea is a 2000-tile cross-map sail; a busy-sea task is right here.
		// The bonus is capped at ~one inter-sea leg, so the nearby task must still win.
		SeaChartTaskRow lastRow = new SeaChartTaskRow(lastInSea, 2000, true, false, false);
		SeaChartTaskRow busyRow = new SeaChartTaskRow(inBusySea, 100, true, false, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(lastRow, busyRow));
		SeaChartTaskSorter.sort(rows, progress(lastInSea.getSea(), 1, inBusySea.getSea(), 30));

		assertEquals(inBusySea, rows.get(0).getTask());
	}

	@Test
	public void exactTiesFallBackToStableTaskIdOrder()
	{
		SeaChartTask first = SeaChartTask.values()[0];
		SeaChartTask second = taskInSameSea(first);
		assertTrue(first.getTaskId() < second.getTaskId());

		SeaChartTaskRow firstRow = new SeaChartTaskRow(first, 250, true, false, false);
		SeaChartTaskRow secondRow = new SeaChartTaskRow(second, 250, true, false, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(secondRow, firstRow));
		SeaChartTaskSorter.sort(rows, progress(first.getSea(), 5, first.getSea(), 5));

		assertEquals(first, rows.get(0).getTask());
		assertEquals(second, rows.get(1).getTask());
	}

	@Test
	public void missingSeaProgressMeansNoBonusPureProximity()
	{
		SeaChartTask a = SeaChartTask.values()[0];
		SeaChartTask b = taskInDifferentSea(a);

		SeaChartTaskRow aRow = new SeaChartTaskRow(a, 400, true, false, false);
		SeaChartTaskRow bRow = new SeaChartTaskRow(b, 300, true, false, false);

		List<SeaChartTaskRow> rows = new ArrayList<>(Arrays.asList(aRow, bRow));
		SeaChartTaskSorter.sort(rows, new EnumMap<>(SeaChartSea.class));

		assertEquals(b, rows.get(0).getTask());
		assertEquals(a, rows.get(1).getTask());
	}

	private static Map<SeaChartSea, SeaChartSeaProgress> progress(
		SeaChartSea seaA, int remainingA, SeaChartSea seaB, int remainingB)
	{
		Map<SeaChartSea, SeaChartSeaProgress> progress = new EnumMap<>(SeaChartSea.class);
		// complete=0, total=remaining -> getRemaining() == remaining; only remaining matters here.
		progress.put(seaA, new SeaChartSeaProgress(0, remainingA));
		progress.put(seaB, new SeaChartSeaProgress(0, remainingB));
		return progress;
	}

	private static SeaChartTask taskInDifferentSea(SeaChartTask other)
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task.getSea() != other.getSea())
			{
				return task;
			}
		}
		throw new AssertionError("all tasks share one sea -- test setup impossible");
	}

	private static SeaChartTask taskInSameSea(SeaChartTask other)
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task != other && task.getSea() == other.getSea())
			{
				return task;
			}
		}
		throw new AssertionError("no second task in " + other.getSea() + " -- test setup impossible");
	}
}
