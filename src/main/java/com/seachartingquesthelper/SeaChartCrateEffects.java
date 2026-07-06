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
 * What sampling each named "Sealed crate" drink actually does, per each drink's own OSRS Wiki
 * page. Keyed by the task's {@code taskName} -- that name literally <em>is</em> the specific
 * drink's flavour name for every Sealed-crate task (e.g. "Marrow Wine", "Weiss Meltwater").
 *
 * <p><b>Danger classification:</b> {@code dangerous=true} means the effect actively harms the
 * player -- HP damage, poison/disease, a net-negative stat drain, forced combat with a hostile
 * spawn, an involuntary teleport/cutscene that can expose you to hazards, or destroyed/lost items
 * (e.g. active courier-task cargo). A temporary buff with no real drawback is not flagged, even if
 * cosmetic side effects (screen flash, transformation, dialogue) are involved.
 *
 * <p>6 of the 65 named drinks have no dedicated Wiki page or documented effect as of this writing
 * (Drunk Impling, Light Dark, Monkfish Stout, Prying Times, Sailing Cat, Wild Whisky) and are
 * simply absent from this map -- treat a task with no entry here as "effect not yet documented",
 * not "confirmed harmless".
 */
final class SeaChartCrateEffects
{
	private SeaChartCrateEffects()
	{
	}

	private static final Map<String, SeaChartCrateEffect> BY_TASK_NAME = build();

	private static Map<String, SeaChartCrateEffect> build()
	{
		Map<String, SeaChartCrateEffect> map = new HashMap<>();
		put(map, "Alco Sol", "Grants 75 Sailing XP; bright screen flash and burning-sensation flavour text.", false);
		put(map, "Alone At Sea", "Grants 75 Sailing XP but locks you into an unskippable 75-second meditation cutscene while the ship keeps moving.", true);
		put(map, "Banana Daiquiri", "Grants 75 Sailing XP; temporarily transforms you into a banana.", false);
		put(map, "Bankers Draught", "Deposits your inventory and equipment to the bank and grants 75 Sailing XP. Ultimate Ironmen take 10% max HP damage instead.", true);
		put(map, "Barracuda Brew", "Grants 75 Sailing XP but knocks you back and stuns you -- can knock you off a raft.", true);
		put(map, "Black Lobster", "First drink grants 75 Sailing XP, then fully drains Cooking and Fishing to 0 and gives a rare lobster.", true);
		put(map, "Blue Lagoon", "Grants 75 Sailing XP but spawns an aggressive level-100 blue dagannoth.", true);
		put(map, "Comp Kvass", "Grants 75 Sailing XP and a medium clue scroll or scroll box.", false);
		put(map, "Congratulation Wine", "Grants 75 Sailing XP and a silent level-99 fireworks display.", false);
		put(map, "Corpse Reviver", "Grants 75 Sailing XP but spawns an aggressive level-103 corpse that can regenerate and re-fight you.", true);
		put(map, "Creators Cocktail", "Grants 75 Sailing XP but deals 1/3 max HP damage -- can be lethal at low HP.", true);
		put(map, "Crocodile Tears", "Grants 75 Sailing XP but deals 3-13 desert-heat damage, worse without waterskins.", true);
		put(map, "Crystal Vodka", "Grants 75 Sailing XP but deals 20% max HP damage with a loud ringing sound.", true);
		put(map, "Crystal Water", "Grants 6 minutes of poison immunity.", false);
		put(map, "Destructors Cocktail", "Grants 75 Sailing XP but deals 1/3 max HP damage -- can be lethal (red soul cape negates it).", true);
		put(map, "Dognose Draught", "Grants 75 Sailing XP, +3 Cooking, but drains Farming by up to 3.", true);
		put(map, "Dwarvern Wizard", "Boosts Magic +4/Smithing +2/Mining +2 but fully drains Attack, Strength, and Defence to 0.", true);
		put(map, "Elven Wine", "Grants 75 Sailing XP; if Regicide is complete, involuntarily teleports you to Isafdar and can lose courier cargo.", true);
		put(map, "Endless Night", "Grants 75 Sailing XP but cuts Miscellania approval by 18-19% if the throne quest is done.", true);
		put(map, "Exiles Welcome", "Grants 75 Sailing XP and teleports you to a random safe lunar spellbook destination.", false);
		put(map, "Fishier Stout", "Grants Sailing XP but teleports you into an instanced arena to fight three trolls.", true);
		put(map, "Fish Stoutier", "Summons \"Dinky\" the drink troll, capable of hitting hard.", true);
		put(map, "Fishtongue Tonic", "Grants 75 Sailing XP, +3 Fishing, and garbled fish-speech flavour text.", false);
		put(map, "Goldless Ale", "Grants 75 Sailing XP but steals up to 100 coins from your inventory.", true);
		put(map, "Headless Unicornman", "Fills your inventory with ensouled heads; destroys any courier-task cargo you're carrying.", true);
		put(map, "Kgp Martini", "Grants 75 Sailing XP; a KGP agent knocks you out and teleports you to the Rellekka hunter area.", false);
		put(map, "Kharazi Cooler", "Grants 75 Sailing XP but fully drains your run energy.", true);
		put(map, "Life Water", "Restores HP and 50% prayer, cures disease, and grants 6 minutes of poison immunity.", false);
		put(map, "Lunarshine", "Grants 75 Sailing XP and changes your hairstyle to an afro.", false);
		put(map, "Mango Gin", "Grants 75 Sailing XP but causes several seconds of involuntary random movement.", true);
		put(map, "Marrow Wine", "Grants 75 Sailing XP and +5 Farming but drains Agility by 26%.", true);
		put(map, "Mystery Cider", "Grants 75 Sailing XP with no negative effects.", false);
		put(map, "Myths Mixer", "Grants 75 Sailing XP but deals 25% max HP damage -- can be lethal.", true);
		put(map, "Ogre Prayer", "Restores prayer points but deals 50% max HP damage.", true);
		put(map, "Ooglug", "Restores run energy and grants 75 Sailing XP.", false);
		put(map, "Platinum Rum", "Grants 75 Sailing XP and summons dwarves that drop food.", false);
		put(map, "Point Punch", "Grants 75 Sailing XP and 25 arrowtips but deals 5 damage.", true);
		put(map, "Portal Perry", "Grants 75 Sailing XP; may teleport you to the Abyss and lose courier-task cargo.", true);
		put(map, "Possible Albumen", "Grants 75 Sailing XP but spawns a hostile level-81 tortugan that attacks you.", true);
		put(map, "Puzzlers Poteen", "Grants 75 Sailing XP and a quiz -- wrong answers deal 10% max HP damage.", true);
		put(map, "Reddest Rum", "Grants 75 Sailing XP and randomly transforms your appearance.", false);
		put(map, "Roberts Port", "Grants 75 Sailing XP but deals 10% max HP damage, five times -- can be fatal.", true);
		put(map, "Sea Shandy", "Grants 75 Sailing XP but teleports you to Port Sarim and destroys any cargo aboard.", true);
		put(map, "Sea Spray", "Grants 75 Sailing XP; rocks the camera for 20 seconds.", false);
		put(map, "Slug Balm", "Grants 75 Sailing XP but drains Strength/Magic by 20%, teleports you to Witchaven, and capsizes your boat.", true);
		put(map, "Smuggled Rum", "Grants 75 Sailing XP and a temporary Strength boost but drains Attack.", true);
		put(map, "Snake Gravy", "Grants 75 Sailing XP but spawns an aggressive level-90 snakeling that can poison you.", true);
		put(map, "Sorodamin Bru", "A counterfeit Saradomin brew: deals 5 damage and drains all combat stats, no offsetting boost.", true);
		put(map, "Soul Bottle", "Grants 75 Sailing XP; deals 90% max HP damage if paired with Soul Juice -- can be lethal.", true);
		put(map, "Soul Juice", "Grants 75 Sailing XP; deals 90% max HP damage if paired with Soul Bottle -- can be lethal.", true);
		put(map, "Spinners Gasp", "Poisons you for 4 minutes.", true);
		put(map, "Suqah Cola", "Grants 75 Sailing XP, fully restores HP, and boosts every non-HP/Prayer skill by 1.", false);
		put(map, "Toad Cider", "Makes you say \"Ribbit\" and spawns collectible swamp toads, up to 15 times.", false);
		put(map, "Underground Milk", "Grants 75 Sailing XP but deals 2/3 max HP damage.", true);
		put(map, "Way Home", "Grants 75 Sailing XP and teleports you home; loses courier-task cargo if you're carrying any.", true);
		put(map, "Weiss Meltwater", "Grants 75 Sailing XP but deals 1/3 max HP damage unless you're wearing 4+ warm clothing pieces.", true);
		put(map, "Winter Sun", "Grants 75 Sailing XP and teleports you near Pollnivneach; destroys any courier cargo in your boat.", true);
		put(map, "Zogres Kiss", "Grants 75 Sailing XP but poisons (starting at 5 damage) and diseases you.", true);
		put(map, "Zul Rye", "Grants 75 Sailing XP but envenoms you, starting at 6 damage.", true);
		return Collections.unmodifiableMap(map);
	}

	private static void put(Map<String, SeaChartCrateEffect> map, String taskName, String description, boolean dangerous)
	{
		map.put(taskName, new SeaChartCrateEffect(description, dangerous));
	}

	/**
	 * @return the known effect of sampling this task's crate, or {@code null} if not documented
	 * (see class Javadoc for the 6 undocumented drinks).
	 */
	static SeaChartCrateEffect forTaskName(String taskName)
	{
		return BY_TASK_NAME.get(taskName);
	}
}
