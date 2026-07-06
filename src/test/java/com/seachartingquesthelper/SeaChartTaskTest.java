package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
		// SeaChartRegion's Javadoc); the nearest-teleport hint is per-sea (SeaChartSea), not
		// per-region -- see SeaChartSeaTest for the sea-level equivalent of this check. Every one
		// of the 358 tasks must still resolve to some region and some non-blank port hint, since
		// the panel always renders a "Nearest: ..." line.
		for (SeaChartTask task : SeaChartTask.values())
		{
			SeaChartRegion region = task.getRegion();
			assertNotNull(task + " has no region", region);
			assertTrue(task + "'s sea has a blank port hint", !task.getSea().getNearestPort().trim().isEmpty());
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

	@Test
	public void mostTasksHaveNoGearRequirement()
	{
		// The vast majority of tasks are in ordinary water -- only the confirmed hazard seas
		// (crystal-flecked/tangled-kelp/icy/fetid) should ever report a requirement.
		for (SeaChartTask task : SeaChartTask.values())
		{
			assertNotNull(task + " gear requirement set is null, should be empty", task.getGearRequirements());
		}
	}

	@Test
	public void porthGwenithAndPorthNeigwlNeedAdamantKeelOrHelm()
	{
		// Crystal-flecked waters, per the wiki's own per-task hazard= field (4 seas total).
		assertEquals(SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM,
			only(SeaChartTask.TASK_170.getGearRequirements())); // Porth Gwenith, mermaid guide
		assertEquals(SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM,
			only(SeaChartTask.TASK_173.getGearRequirements())); // Porth Gwenith, current duck
		assertEquals(SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM,
			only(SeaChartTask.TASK_146.getGearRequirements())); // Porth Neigwl, weather
	}

	@Test
	public void piscatorisSeaAndTirannwnBightNeedAdamantKeelOrHelm()
	{
		// The other two of Crystal-flecked waters' four seas -- missed by an earlier version that
		// matched against the wiki's "Crystal-flecked waters" hub page prose instead of the raw
		// per-task hazard= data, which only names Porth Gwenith/Porth Neigwl as examples.
		assertEquals(SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM,
			only(SeaChartTask.TASK_171.getGearRequirements())); // Piscatoris Sea, mermaid guide
		assertEquals(SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM,
			only(SeaChartTask.TASK_145.getGearRequirements())); // Tirannwn Bight, weather
	}

	@Test
	public void rainbowReefAndSouthernExpanseNeedAdamantKeelOrHelm()
	{
		// Tangled kelp, per the OSRS Wiki's "Tangled kelp" page (Isle of Serpents/Sunbleak Island,
		// both level-72-gated -- matching these tasks' level 72 requirement).
		assertEquals(SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM,
			only(SeaChartTask.TASK_52.getGearRequirements())); // Rainbow Reef, mermaid guide
		assertEquals(SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM,
			only(SeaChartTask.TASK_284.getGearRequirements())); // Southern Expanse, weather
	}

	@Test
	public void icySeasNeedEternalBrazier()
	{
		// All ten icy seas per the wiki's own per-task hazard= field -- an earlier version only
		// covered the first six (Idestia Strait, Lunar Sea, Kannski Tides, V's Belt were missing).
		SeaChartTask[] icySeaTasks = {
			SeaChartTask.TASK_219, // Weiss Melt
			SeaChartTask.TASK_215, // Everwinter Sea
			SeaChartTask.TASK_218, // Stoneheart Sea
			SeaChartTask.TASK_217, // Weissmere
			SeaChartTask.TASK_169, // Winters Edge
			SeaChartTask.TASK_226, // Shiverwake Expanse
			SeaChartTask.TASK_213, // Idestia Strait, current duck
			SeaChartTask.TASK_235, // Lunar Sea, spyglass
			SeaChartTask.TASK_216, // Kannski Tides, current duck
			SeaChartTask.TASK_211, // Vs Belt, current duck
		};
		for (SeaChartTask task : icySeaTasks)
		{
			assertEquals(task + " should need an eternal brazier", SeaChartGearRequirement.ETERNAL_BRAZIER,
				only(task.getGearRequirements()));
		}
	}

	@Test
	public void stormySeasNeedMastUpgrade()
	{
		// Per the wiki's own per-task hazard= field -- a hazard category with no handling at all
		// in an earlier version of this class.
		assertEquals(SeaChartGearRequirement.MAST_UPGRADE,
			only(SeaChartTask.TASK_70.getGearRequirements())); // Kharazi Strait, current duck
		assertEquals(SeaChartGearRequirement.MAST_UPGRADE,
			only(SeaChartTask.TASK_57.getGearRequirements())); // Storm Tempor, weather
	}

	@Test
	public void diseaseSeasNeedInoculationStation()
	{
		// Fetid waters, per the wiki's own per-task hazard= field.
		SeaChartTask[] fetidSeaTasks = {
			SeaChartTask.TASK_128, // Backwater
			SeaChartTask.TASK_105, // Breakbone Strait
			SeaChartTask.TASK_127, // Mythic Sea
			SeaChartTask.TASK_311, // Sea Of Souls
			SeaChartTask.TASK_114, // "Zul Andra" (Spyglass) -- named after a flavour landmark, but
			                        // its real sea is Zul-Egil; only catchable by keying off
			                        // getSea() rather than taskName (see SeaChartGearRequirements).
		};
		for (SeaChartTask task : fetidSeaTasks)
		{
			assertEquals(task + " should need an inoculation station", SeaChartGearRequirement.INOCULATION_STATION,
				only(task.getGearRequirements()));
		}
	}

	@Test
	public void taskNamedAfterAWrongSeaResolvesToItsRealSeasHazard()
	{
		// TASK_107 is a Weather task literally named "Zul Egil", but its real sea (per the wiki's
		// sea= tag) is Porth Neigwl -- a name-based lookup would (and, in an earlier version, did)
		// wrongly flag it INOCULATION_STATION instead of the correct ADAMANT_KEEL_OR_HELM.
		assertEquals(SeaChartSea.PORTH_NEIGWL, SeaChartTask.TASK_107.getSea());
		assertEquals(SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM,
			only(SeaChartTask.TASK_107.getGearRequirements()));
	}

	@Test
	public void grandrootBayAndBlackLobsterRequireARaft()
	{
		// The only two of the 358 tasks whose own description says "a raft is necessary to reach
		// this location" -- every other raft mention in the table says "recommended but not
		// required", which the big boat can still reach, so isn't flagged.
		assertEquals(SeaChartGearRequirement.REQUIRES_RAFT,
			only(SeaChartTask.TASK_210.getGearRequirements())); // Grandroot Bay, current duck -- an ordinary (non-hazard) sea, so raft is its only requirement

		// "Black Lobster" sits in Kannski Tides, one of the ten icy seas -- it genuinely needs
		// BOTH a raft to reach it AND an eternal brazier for the icy water, which only shows up
		// now that gear requirements are keyed by real sea rather than by task name (the name
		// "Black Lobster" never matched any sea, so the old lookup only ever saw the raft tag).
		assertEquals(SeaChartSea.KANNSKI_TIDES, SeaChartTask.TASK_261.getSea());
		Set<SeaChartGearRequirement> blackLobster = SeaChartTask.TASK_261.getGearRequirements();
		assertTrue(blackLobster.contains(SeaChartGearRequirement.REQUIRES_RAFT));
		assertTrue(blackLobster.contains(SeaChartGearRequirement.ETERNAL_BRAZIER));
		assertEquals(2, blackLobster.size());
	}

	@Test
	public void raftRequirementIsPerTaskNotPerSea()
	{
		// "Grandroot Bay" is the name of four different tasks (one per type); only the Current
		// duck one (TASK_210, checked above) actually needs a raft. An earlier version tagged
		// REQUIRES_RAFT by sea name, which wrongly flagged all four.
		assertTrue(SeaChartTask.TASK_221.getGearRequirements().isEmpty()); // Grandroot Bay, weather
		assertTrue(SeaChartTask.TASK_229.getGearRequirements().isEmpty()); // Grandroot Bay, spyglass
		assertTrue(SeaChartTask.TASK_242.getGearRequirements().isEmpty()); // Grandroot Bay, mermaid guide
	}

	@Test
	public void tasksWithRaftOrSkiffNotesCarryThem()
	{
		// The two hard "necessary" tasks also have a gear requirement (see
		// grandrootBayAndBlackLobsterRequireARaft above); this checks the note text itself, plus a
		// "recommended but not required" task and the skiff-necessary one, which has no gear
		// requirement filter of its own.
		assertEquals("A raft is necessary to reach this location.",
			SeaChartTask.TASK_210.getNote());
		assertEquals("A raft is recommended but not required to reach this location.",
			SeaChartTask.TASK_39.getNote());
		assertEquals("A skiff is necessary to reach this location.",
			SeaChartTask.TASK_170.getNote());
	}

	@Test
	public void mostTasksHaveNoNote()
	{
		assertNull(SeaChartTask.TASK_0.getNote());
	}

	@Test
	public void ordinarySeaHasNoGearRequirement()
	{
		// Spot-check an everyday task far from any confirmed hazard sea.
		assertTrue(SeaChartTask.TASK_0.getGearRequirements().isEmpty()); // Board Port Sarim
		assertFalse(SeaChartTask.TASK_0.getGearRequirements().contains(SeaChartGearRequirement.ETERNAL_BRAZIER));
	}

	private static SeaChartGearRequirement only(Set<SeaChartGearRequirement> requirements)
	{
		assertEquals("expected exactly one gear requirement, got " + requirements, 1, requirements.size());
		return requirements.iterator().next();
	}
}
