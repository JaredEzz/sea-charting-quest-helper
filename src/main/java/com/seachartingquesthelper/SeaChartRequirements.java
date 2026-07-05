package com.seachartingquesthelper;

import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * Level/quest gating for sea chart tasks. The per-type quest gate mirrors what the "Sailing"
 * plugin's own requirement check uses (LlemonDuck, https://github.com/LlemonDuck/sailing) --
 * each chart task type is unlocked by one specific quest, a game-design fact rather than
 * anything specific to that plugin's implementation.
 */
final class SeaChartRequirements
{
	private SeaChartRequirements()
	{
	}

	static Quest questFor(SeaChartTaskType type)
	{
		switch (type)
		{
			case CURRENT_DUCK:
				return Quest.CURRENT_AFFAIRS;
			case DRINK_CRATE:
				return Quest.PRYING_TIMES;
			case MERMAID_GUIDE:
				return Quest.RECIPE_FOR_DISASTER__PIRATE_PETE;
			case GENERIC:
			case SPYGLASS:
			case WEATHER:
			default:
				return Quest.PANDEMONIUM;
		}
	}

	/**
	 * Whether the local player's quest state and Sailing level clear this task's gate. Does not
	 * (and cannot) check for boat upgrades like the keel/helm/brazier -- see the README.
	 */
	static boolean meetsRequirement(Client client, SeaChartTask task)
	{
		Quest quest = questFor(task.getType());
		if (quest.getState(client) != QuestState.FINISHED)
		{
			return false;
		}
		return client.getRealSkillLevel(Skill.SAILING) >= task.getLevel();
	}
}
