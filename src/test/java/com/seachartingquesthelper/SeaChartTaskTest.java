package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import org.junit.Test;

public class SeaChartTaskTest
{
	@Test
	public void has358Tasks()
	{
		// The whole point of this plugin -- confirm we mirrored the full task table.
		assertEquals(358, SeaChartTask.values().length);
	}

	@Test
	public void taskIdsAreSequentialAndMatchOrdinal()
	{
		int expected = 0;
		for (SeaChartTask task : SeaChartTask.values())
		{
			assertEquals(expected, task.getTaskId());
			assertEquals(expected, task.ordinal());
			expected++;
		}
	}

	@Test
	public void varbitsAreUnique()
	{
		Set<Integer> seen = new HashSet<>();
		for (SeaChartTask task : SeaChartTask.values())
		{
			assertTrue("duplicate completion varbit for " + task, seen.add(task.getVarbit()));
		}
	}

	@Test
	public void byVarbitLooksUpTheOwningTask()
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			assertEquals(task, SeaChartTask.byVarbit(task.getVarbit()));
		}
	}

	@Test
	public void everyTaskHasEitherAnObjectOrNpcTarget()
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			assertTrue(task + " has neither an object nor an npc id",
				task.getObjectId() != -1 || task.getNpcId() != -1);
		}
	}

	@Test
	public void rainbowReefMermaidGuideIsLevel72()
	{
		// Spot-check named in the design notes: Rainbow Reef's mermaid guide task requires 72
		// Sailing, one of the highest level gates in the table.
		SeaChartTask task = SeaChartTask.TASK_52;
		assertEquals(SeaChartTaskType.MERMAID_GUIDE, task.getType());
		assertEquals(72, task.getLevel());
		assertEquals("Rainbow Reef", task.getTaskName());
	}

	@Test
	public void questForCoversEveryTaskType()
	{
		for (SeaChartTaskType type : SeaChartTaskType.values())
		{
			assertNotNull(SeaChartRequirements.questFor(type));
		}
	}

	@Test
	public void levelsAreNonNegative()
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			assertTrue(task + " has a negative level requirement", task.getLevel() >= 0);
		}
	}

	@Test
	public void everyTaskHasARegionAndPortHint()
	{
		// Region assignment is sourced directly from the OSRS Wiki's per-task ocean/sea tags (see
		// SeaChartRegion's Javadoc), but every one of the 358 tasks must still resolve to some
		// region with a non-blank port hint -- the panel always renders a "Nearest: ..." line.
		for (SeaChartTask task : SeaChartTask.values())
		{
			SeaChartRegion region = task.getRegion();
			assertNotNull(task + " has no region", region);
			assertTrue(task + "'s region has a blank port hint", !region.getNearestPort().trim().isEmpty());
		}
	}

	@Test
	public void regionCountsMatchWikiOceanTotals()
	{
		// Every task's region comes from a 358-entry lookup table built from the wiki's per-task
		// ocean/sea tags (not guessed index ranges) -- these per-region tallies are a strong
		// regression check: any future edit that nudges even one task into the wrong region will
		// change one of these counts.
		Map<SeaChartRegion, Integer> counts = new EnumMap<>(SeaChartRegion.class);
		for (SeaChartTask task : SeaChartTask.values())
		{
			counts.merge(task.getRegion(), 1, Integer::sum);
		}

		assertEquals(Integer.valueOf(87), counts.get(SeaChartRegion.ARDENT));
		assertEquals(Integer.valueOf(38), counts.get(SeaChartRegion.UNQUIET));
		assertEquals(Integer.valueOf(78), counts.get(SeaChartRegion.SHROUDED));
		assertEquals(Integer.valueOf(14), counts.get(SeaChartRegion.SUNSET));
		assertEquals(Integer.valueOf(44), counts.get(SeaChartRegion.WESTERN_KOUREND));
		assertEquals(Integer.valueOf(22), counts.get(SeaChartRegion.WESTERN_TIRANNWN));
		assertEquals(Integer.valueOf(75), counts.get(SeaChartRegion.NORTHERN));
	}

	@Test
	public void westernOceanTasksAreNotMislabeledNorthern()
	{
		// Regression test for the original bug: an earlier version of SeaChartRegion guessed ocean
		// boundaries from index ranges and put every real Western Ocean place name (Great Sound,
		// Crabclaw Bay, Gulf of Kourend, Hosidian Sea, Pilgrims' Passage, Crystal Sea, Litus Lucis,
		// Moonshadow, Vagabonds Rest, Porth Neigwl, Piscatoris Sea, Porth Gwenith, Tirannwn Bight)
		// into "Northern+Misc" instead. Spot-check tasks from both real Western clusters.

		// Kourend-side cluster (Great Kourend / Land's End / Hosidius).
		assertEquals(SeaChartRegion.WESTERN_KOUREND, SeaChartTask.TASK_153.getRegion()); // Crystal Sea
		assertEquals(SeaChartRegion.WESTERN_KOUREND, SeaChartTask.TASK_175.getRegion()); // Gulf of Kourend
		assertEquals(SeaChartRegion.WESTERN_KOUREND, SeaChartTask.TASK_314.getRegion()); // Crabclaw Bay
		assertEquals(SeaChartRegion.WESTERN_KOUREND, SeaChartTask.TASK_326.getRegion()); // Moonshadow
		assertEquals("Western Ocean", SeaChartTask.TASK_314.getRegion().getLabel());

		// Tirannwn-side cluster (Port Tyras / Prifddinas / Piscatoris).
		assertEquals(SeaChartRegion.WESTERN_TIRANNWN, SeaChartTask.TASK_107.getRegion()); // Porth Neigwl
		assertEquals(SeaChartRegion.WESTERN_TIRANNWN, SeaChartTask.TASK_145.getRegion()); // Tirannwn Bight
		assertEquals(SeaChartRegion.WESTERN_TIRANNWN, SeaChartTask.TASK_334.getRegion()); // Piscatoris Sea
		assertEquals("Western Ocean", SeaChartTask.TASK_334.getRegion().getLabel());

		// These specific ids used to fall inside the old bogus "Western" range (194-249) or the old
		// "Northern+Misc" range (250-357) -- confirm they land in their real oceans now.
		assertEquals(SeaChartRegion.ARDENT, SeaChartTask.TASK_194.getRegion()); // Menaphite Sea
		assertEquals(SeaChartRegion.NORTHERN, SeaChartTask.TASK_249.getRegion()); // Kannski Tides
		assertEquals(SeaChartRegion.NORTHERN, SeaChartTask.TASK_190.getRegion()); // Everwinter Sea
	}

	@Test
	public void knownAnchorTasksResolveToTheirRealOcean()
	{
		// A handful of well-known place-name tasks, cross-checked directly against the wiki.
		assertEquals(SeaChartRegion.ARDENT, SeaChartTask.TASK_0.getRegion()); // Bay of Sarim
		assertEquals(SeaChartRegion.ARDENT, SeaChartTask.TASK_357.getRegion()); // Kharidian Sea (Chartin' Charles)
		assertEquals(SeaChartRegion.UNQUIET, SeaChartTask.TASK_47.getRegion()); // Red Reef
		assertEquals(SeaChartRegion.ARDENT, SeaChartTask.TASK_104.getRegion()); // Gu'tanoth Bay
		assertEquals(SeaChartRegion.SHROUDED, SeaChartTask.TASK_105.getRegion()); // Breakbone Strait
		assertEquals(SeaChartRegion.SUNSET, SeaChartTask.TASK_343.getRegion()); // Sunset Bay
	}
}
