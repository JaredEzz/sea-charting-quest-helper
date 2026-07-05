package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
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
}
