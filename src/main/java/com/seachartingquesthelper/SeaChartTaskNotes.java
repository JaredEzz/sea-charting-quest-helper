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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-task informational asides, sourced directly from the {@code ''Note: ...''} annotation on
 * each task's own row in the OSRS Wiki's "Sea charting" page (the same 358-row {@code
 * {{SeaChartRow}}} template data {@link SeaChartRegion}/{@link SeaChartSea} source from). Unlike
 * {@link SeaChartGearRequirements}'s sea-hazard mapping (keyed by {@link SeaChartSea}, since a
 * hazard applies to every task in that sea), this is keyed by the task's stable {@link
 * SeaChartTask#getTaskId()}, since a note is specific to one individual task -- the same reason
 * {@code SeaChartGearRequirements}'s raft requirement is also task-id-keyed rather than sea-keyed.
 *
 * <p>10 of the 358 tasks carry a note. Two ("A raft is necessary...", ids 210/261) are the hard
 * requirements already surfaced as {@link SeaChartGearRequirement#REQUIRES_RAFT}; task 170's "A
 * skiff is necessary..." is a similar hard access requirement but for a different, single task
 * and a different vessel, so it's left as a note rather than invented as its own one-task filter
 * category. The rest are genuinely just useful context (a raft/skiff being merely recommended, a
 * quest-completion prerequisite, or a level-38 access workaround) with no filter of their own.
 */
final class SeaChartTaskNotes
{
	private SeaChartTaskNotes()
	{
	}

	private static final Map<Integer, String> BY_TASK_ID = build();

	private static final Map<Integer, String> MERMAID_GUIDE_ANSWERS = buildMermaidGuideAnswers();

	private static Map<Integer, String> build()
	{
		Map<Integer, String> map = new HashMap<>();
		map.put(39, "A raft is recommended but not required to reach this location.");
		map.put(81, "Accessing the Great Conch requires partial completion of Troubled Tortugans.");
		map.put(99, "Accessing the Great Conch requires partial completion of Troubled Tortugans.");
		map.put(126, "To do this task at 38 Sailing, you must board another player's boat -- one with an inoculation station, to survive the fetid waters.");
		map.put(168, "A raft or skiff is recommended but not required to reach this location.");
		map.put(170, "A skiff is necessary to reach this location.");
		map.put(210, "A raft is necessary to reach this location.");
		map.put(258, "A raft is recommended but not required to reach this location.");
		map.put(261, "A raft is necessary to reach this location.");
		map.put(346, "A raft is recommended but not required to reach this location.");
		return Collections.unmodifiableMap(map); }

	private static Map<Integer, String> buildMermaidGuideAnswers()
	{
		Map<Integer, String> map = new HashMap<>();
		map.put(12, "Riddle answer: Willow stock");
		map.put(44, "Riddle answer: Pie dish, pot of flour, and cooking apple");
		map.put(45, "Riddle answer: Iron med helm or rune med helm and bronze chainbody");
		map.put(46, "Riddle answer: 5 cabbage seeds");
		map.put(47, "Riddle answer: 10 watermelons");
		map.put(48, "Riddle answer: Vial, avantoe, snape grass, and caviar");
		map.put(49, "Riddle answer: Harralander potion (unf)");
		map.put(50, "Riddle answer: Papaya fruit");
		map.put(51, "Riddle answer: Ashes or any demonic ashes");
		map.put(52, "Riddle answer: Bucket of sap and raw slimy eel");
		map.put(53, "Riddle answer: Barley");
		map.put(125, "Riddle answer: Earth impling jar");
		map.put(126, "Riddle answer: Coal");
		map.put(127, "Riddle answer: Cabbage, onion, and tomato");
		map.put(128, "Riddle answer: Grimy kwuarm or kwuarm");
		map.put(129, "Riddle answer: Dwellberries"); map.put(152, "Riddle answer: Black flowers");
		map.put(153, "Riddle answer: Butterfly jar");
		map.put(154, "Riddle answer: 2 calquat kegs, ale yeast, oak roots, and 2 barley malt");
		map.put(155, "Riddle answer: Vial, coconut milk, toadflax, and yew roots");
		map.put(156, "Riddle answer: Soiled page");
		map.put(170, "Riddle answer: Thatch spar dense");
		map.put(171, "Riddle answer: Gold ore");
		map.put(172, "Riddle answer: 2 malicious ashes");
		map.put(192, "Riddle answer: Sandwich lady bottom");
		map.put(241, "Riddle answer: Kharyrll teleport (tablet)");
		map.put(242, "Riddle answer: Raw cod");
		map.put(243, "Riddle answer: Bronze limbs");
		map.put(244, "Riddle answer: Onion");
		map.put(245, "Riddle answer: Torstol");
		map.put(246, "Riddle answer: Needle");
		map.put(247, "Riddle answer: Clockwork");
		map.put(248, "Riddle answer: Shield left half");
		map.put(249, "Riddle answer: Vial of blood, cadantine, and Wine of Zamorak");
		map.put(250, "Riddle answer: Dragon bitter");
		map.put(251, "Riddle answer: Rain bow");
		map.put(252, "Riddle answer: Royal crown");
		map.put(271, "Riddle answer: Nose peg");
		map.put(272, "Riddle answer: Charcoal");
		map.put(273, "Riddle answer: 2 woad leaves and 2 onions");
		map.put(274, "Riddle answer: Swamp weed");
		map.put(307, "Riddle answer: Stripy feather");
		map.put(308, "Riddle answer: 2 tomatoes, equa leaves, batta tin, cheese, dwellberries, onion, cabbage, and gianne dough");
		map.put(309, "Riddle answer: Lime, lime slices, or lime chunks");
		map.put(310, "Riddle answer: Fedora");
		map.put(311, "Riddle answer: Common tench");
		map.put(312, "Riddle answer: Any plank");
		map.put(313, "Riddle answer: Ghrazi rapier");
		map.put(321, "Riddle answer: 3 air runes, silver ore, chisel, uncut jade, ring mould, and cosmic rune");
		map.put(322, "Riddle answer: Potato and potato cactus");
		map.put(323, "Riddle answer: Sandstone (2kg), sandstone (1kg), and sandstone (10kg)");
		map.put(324, "Riddle answer: Dark bow tie");
		map.put(325, "Riddle answer: Double eye patch");
		map.put(326, "Riddle answer: Bucket helm (g)");
		return Collections.unmodifiableMap(map);
	}

	/**
	 * @return this task's wiki note, or {@code null} if it doesn't have one.
	 */
	static String forTaskId(int taskId)
	{
		String mermaidAnswer = MERMAID_GUIDE_ANSWERS.get(taskId);
		String task = BY_TASK_ID.get(taskId);
		
		if (mermaidAnswer != null && task != null)
		{
			return task + "\n" + mermaidAnswer;
		}
		
		if (mermaidAnswer != null)
		{
			 return mermaidAnswer;
		}
		
		return task;
	}
}
