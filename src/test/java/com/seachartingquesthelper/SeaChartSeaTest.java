package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * Data-integrity checks for {@link SeaChartSea}: every task must resolve to exactly one of the 70
 * real seas, and each sea's {@link SeaChartSea#getRegion()} must agree with the independently
 * verified {@link SeaChartTask#getRegion()} (sourced from {@link SeaChartRegion}, which folds the
 * wiki's "Bonus charts" tag and splits the Western Ocean's two geographic clusters) -- confirming
 * the two data sets, extracted from the same raw wiki table by two different passes, agree
 * task-by-task rather than just at the aggregate-count level.
 */
public class SeaChartSeaTest
{
	@Test
	public void everyTaskResolvesToASea()
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			assertNotNull(task + " has no sea", task.getSea());
		}
	}

	@Test
	public void thereAreExactly70RealSeas()
	{
		assertEquals(70, SeaChartSea.values().length);
	}

	@Test
	public void everySeasRegionAgreesWithItsTasksIndependentlyComputedRegion()
	{
		// Cross-checks SeaChartSea's per-sea region assignment against SeaChartTask#getRegion()
		// (SeaChartRegion's already-verified per-task resolution) for every single task, not just
		// a spot check -- both ultimately derive from the same wiki table but were built as
		// separate passes, so this confirms they didn't diverge anywhere.
		for (SeaChartTask task : SeaChartTask.values())
		{
			assertEquals(task + " (" + task.getSea() + ")'s region should match its own getRegion()",
				task.getRegion(), task.getSea().getRegion());
		}
	}

	@Test
	public void knownSeasHaveTheExpectedRegion()
	{
		// A handful of well-known place-name seas, cross-checked directly against the wiki.
		assertEquals(SeaChartRegion.NORTHERN, SeaChartSea.SHIVERWAKE_EXPANSE.getRegion());
		assertEquals(SeaChartRegion.SHROUDED, SeaChartSea.BACKWATER.getRegion());
		assertEquals(SeaChartRegion.WESTERN_TIRANNWN, SeaChartSea.PORTH_GWENITH.getRegion());
		assertEquals(SeaChartRegion.WESTERN_KOUREND, SeaChartSea.GREAT_SOUND.getRegion());
		assertEquals(SeaChartRegion.SUNSET, SeaChartSea.SUNSET_BAY.getRegion());
		assertEquals(SeaChartRegion.ARDENT, SeaChartSea.KHARAZI_STRAIT.getRegion());
	}
}
