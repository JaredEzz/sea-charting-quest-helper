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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The 70 real, individually-named seas sea-charting tasks sit in -- one level more granular than
 * {@link SeaChartRegion} (the 7 broad oceans). A "sea" here is the wiki's own {@code sea=} tag on
 * each task, e.g. "Shiverwake Expanse" or "Kharidian Sea" -- the thing a player actually finishes
 * before moving to a neighbouring one, as opposed to an entire ocean (which can hold a dozen-plus
 * distinct seas and rarely gets meaningfully "almost done").
 *
 * <p><b>Sourced directly from the OSRS Wiki's "Sea charting" page</b> (the same 358-row {@code
 * {{SeaChartRow}}} template data {@link SeaChartRegion} uses), grouping by {@code sea=} instead of
 * folding straight to {@code ocean=}. Each sea's {@link SeaChartRegion} is cross-checked against
 * {@link SeaChartRegion#forTaskId}'s own already-verified per-task resolution (which already
 * folds the wiki's "Bonus charts" ocean tag and splits the Western Ocean's two geographic
 * clusters) -- every one of the 70 seas maps to exactly one region unambiguously, confirmed
 * programmatically rather than assumed.
 *
 * <p><b>Why this exists as its own enum, not just reusing {@link SeaChartRegion}:</b> two real
 * bugs were traced to conflating "sea" and "region/ocean" under a single 7-bucket model:
 * <ul>
 * <li>{@link SeaChartTaskSorter}'s "almost-done sea gets a sort bonus" logic was keyed off {@link
 * SeaChartRegion} -- meaning a task only got the completion bonus once its entire *ocean* (up to
 * 87 tasks across a dozen-plus seas) was nearly finished, not its actual sea (as few as 3 tasks).
 * The bonus was correct in spirit but virtually inert in practice for any ocean with more than a
 * couple of seas in it.
 * <li>The panel's "sea completion" toggle and label actually displayed *ocean*-level progress
 * (e.g. "Northern Ocean (68/75)"), not the specific sea the task is in -- misleading despite being
 * named correctly for what a player would expect ("Shiverwake Expanse (2/5)").
 * </ul>
 * Both now key off this enum instead; {@link SeaChartRegion} still exists for the broader "which
 * ocean is this" grouping.
 *
 * <p><b>Nearest teleport ({@link #getNearestPort()}):</b> a real, per-sea nearest-teleport
 * recommendation, replacing the old one-per-ocean hint (e.g. every one of the Northern Ocean's 14
 * seas used to show the same "Rellekka" hint regardless of how far that actually was). Computed
 * by taking each sea's real wiki map coordinate and ranking every standard teleport (spellbooks,
 * jewelry, diary/quest items, fairy rings) by tile distance, excluding the two sailing-locked
 * teleports (the "Teleport to Boat" spell/tablet, which only work once you already have a boat).
 * Near-ties were broken by practicality (mainland/dock access over an obscure item). A handful of
 * seas are in genuinely remote ocean with nothing within a few hundred tiles -- the listed
 * teleport there is the true nearest, not a good one. One entry (Turtle Belt/Sea of
 * Shells/Tortugan Sea/Pearl Bank -- fairy ring CJQ, The Great Conch) only works after visiting
 * that fairy ring's destination once by boat; there's no closer alternative for those seas, but
 * that circularity is worth knowing about the first time through.
 */
@Getter
@RequiredArgsConstructor
enum SeaChartSea
{
	BAY_OF_SARIM("Bay of Sarim", SeaChartRegion.ARDENT, "Necklace of passage -> Wizards' Tower (Amulet of glory -> Draynor Village)"),
	MUDSKIPPER_SOUND("Mudskipper Sound", SeaChartRegion.ARDENT, "Fairy ring AIQ -> Mudskipper Point (Amulet of glory -> Karamja)"),
	KHARIDIAN_SEA("Kharidian Sea", SeaChartRegion.ARDENT, "Camulet -> Enakhra's Temple (fairy ring AIQ -> Mudskipper Point)"),
	LUMBRIDGE_BASIN("Lumbridge Basin", SeaChartRegion.ARDENT, "Fairy ring BIQ -> Kalphite Hive (Lumbridge home teleport)"),
	RIMMINGTON_STRAIT("Rimmington Strait", SeaChartRegion.ARDENT, "Skills necklace -> Crafting Guild (Amulet of glory -> Karamja/Musa Point)"),
	CATHERBY_BAY("Catherby Bay", SeaChartRegion.ARDENT, "Catherby teleport (Lunar) (Camelot teleport)"),
	BRIMHAVEN_PASSAGE("Brimhaven Passage", SeaChartRegion.ARDENT, "Fairy ring AIR -> island SE of Ardougne (Ardougne teleport)"),
	STRAIT_OF_KHAZARD("Strait of Khazard", SeaChartRegion.ARDENT, "Fairy ring CLS -> Yanille Chain island (Khazard teleport [Lunar] -> Port Khazard dock)"),
	RED_REEF("Red Reef", SeaChartRegion.UNQUIET, "Minigame teleport -> Pest Control (Ape Atoll teleport)"),
	BARRACUDA_BELT("Barracuda Belt", SeaChartRegion.SHROUDED, "Minigame teleport -> Soul Wars"),
	ARROW_PASSAGE("Arrow Passage", SeaChartRegion.ARDENT, "Ape Atoll teleport (fairy ring CLR)"),
	TURTLE_BELT("Turtle Belt", SeaChartRegion.UNQUIET, "Fairy ring CJQ -> The Great Conch (fairy ring AKP -> Necropolis)"),
	TORTUGAN_SEA("Tortugan Sea", SeaChartRegion.UNQUIET, "Fairy ring CJQ -> The Great Conch (Pharaoh's sceptre -> Jaltevas)"),
	RAINBOW_REEF("Rainbow Reef", SeaChartRegion.SHROUDED, "Minigame teleport -> Pest Control"),
	ANGLERFISHS_LIGHT("Anglerfish's Light", SeaChartRegion.UNQUIET, "Minigame teleport -> Pest Control"),
	THE_SKULLHORDE("The Skullhorde", SeaChartRegion.SHROUDED, "Minigame teleport -> Pest Control (Mythical cape)"),
	THE_STORM_TEMPOR("The Storm Tempor", SeaChartRegion.ARDENT, "Camulet -> Enakhra's Temple (Pharaoh's sceptre -> Jalsavrah)"),
	PEARL_BANK("Pearl Bank", SeaChartRegion.UNQUIET, "Fairy ring CJQ -> The Great Conch (Pharaoh's sceptre -> Jaltevas)"),
	THE_SIMIAN_SEA("The Simian Sea", SeaChartRegion.ARDENT, "Minigame teleport -> Pest Control (Void Knights' Outpost) (fairy ring CLR)"),
	KHARAZI_STRAIT("Kharazi Strait", SeaChartRegion.ARDENT, "Ape Atoll teleport (fairy ring CLR -> Ape Atoll west)"),
	SEA_OF_SHELLS("Sea of Shells", SeaChartRegion.UNQUIET, "Fairy ring CJQ -> The Great Conch (nothing else within 400)"),
	THE_LONELY_SEA("The Lonely Sea", SeaChartRegion.UNQUIET, "Minigame teleport -> Pest Control"),
	BAY_OF_ELIDINIS("Bay of Elidinis", SeaChartRegion.UNQUIET, "Fairy ring AKP -> Necropolis (Pharaoh's sceptre -> Jaltevas)"),
	OOGLOG_CHANNEL("Oo'glog Channel", SeaChartRegion.ARDENT, "Feldip hills teleport scroll (fairy ring AKS)"),
	GUTANOTH_BAY("Gu'tanoth Bay", SeaChartRegion.ARDENT, "Watchtower teleport (Yanille) (fairy ring CLS)"),
	BREAKBONE_STRAIT("Breakbone Strait", SeaChartRegion.SHROUDED, "Mythical cape -> Myths' Guild (fairy ring BJP -> Isle of Souls)"),
	SOUL_BAY("Soul Bay", SeaChartRegion.SHROUDED, "Minigame teleport -> Soul Wars (fairy ring BJP)"),
	PORTH_NEIGWL("Porth Neigwl", SeaChartRegion.WESTERN_TIRANNWN, "Iorwerth camp teleport scroll (fairy ring BJS)"),
	SEA_OF_SOULS("Sea of Souls", SeaChartRegion.SHROUDED, "Minigame teleport -> Soul Wars (Isle of Souls) (fairy ring BJP)"),
	BACKWATER("Backwater", SeaChartRegion.SHROUDED, "Fairy ring BKP -> south of Castle Wars (Ring of dueling -> Castle Wars)"),
	ZUL_EGIL("Zul-Egil", SeaChartRegion.SHROUDED, "Zul-andra teleport scroll (fairy ring BJS -> near Zul-Andra)"),
	FELDIP_GULF("Feldip Gulf", SeaChartRegion.ARDENT, "Fairy ring CKR -> south of Tai Bwo Wannai (fairy ring AKS -> Feldip Hunter area)"),
	MYTHIC_SEA("Mythic Sea", SeaChartRegion.SHROUDED, "Mythical cape -> Myths' Guild (minigame teleport -> Pest Control)"),
	WESTERN_GATE("Western Gate", SeaChartRegion.SHROUDED, "Fairy ring BJS -> near Zul-Andra (Zul-andra teleport scroll)"),
	TIRANNWN_BIGHT("Tirannwn Bight", SeaChartRegion.WESTERN_TIRANNWN, "Iorwerth camp teleport scroll (teleport crystal -> Prifddinas)"),
	CRYSTAL_SEA("Crystal Sea", SeaChartRegion.WESTERN_KOUREND, "Iorwerth camp teleport scroll (fairy ring BJS)"),
	SAPPHIRE_SEA("Sapphire Sea", SeaChartRegion.SHROUDED, "Fairy ring AJP -> Avium Savannah"),
	VAGABONDS_REST("Vagabonds Rest", SeaChartRegion.WESTERN_KOUREND, "Fairy ring AKQ -> Piscatoris Hunter area (Piscatoris teleport scroll / Western banner 4)"),
	PORTH_GWENITH("Porth Gwenith", SeaChartRegion.WESTERN_TIRANNWN, "Teleport crystal -> Prifddinas (fairy ring AKQ)"),
	PILGRIMS_PASSAGE("Pilgrims' Passage", SeaChartRegion.WESTERN_KOUREND, "Fairy ring AKR -> Hosidius Vinery (Xeric's talisman -> Xeric's Glade)"),
	WINTERS_EDGE("Winter's Edge", SeaChartRegion.NORTHERN, "Moonclan teleport (Lunar)"),
	PISCATORIS_SEA("Piscatoris Sea", SeaChartRegion.WESTERN_TIRANNWN, "Fairy ring AKQ -> Piscatoris Hunter area (Piscatoris scroll / Western banner 4)"),
	GULF_OF_KOUREND("Gulf of Kourend", SeaChartRegion.WESTERN_KOUREND, "Kharedst's memoirs -> The Fisher's Flute (Port Piscarilius) (fairy ring AKR -> Hosidius Vinery)"),
	HOSIDIAN_SEA("Hosidian Sea", SeaChartRegion.WESTERN_KOUREND, "Fairy ring AKR -> Hosidius Vinery (Xeric's talisman -> Xeric's Glade)"),
	EVERWINTER_SEA("Everwinter Sea", SeaChartRegion.NORTHERN, "Xeric's talisman -> Xeric's Honour (Wintertodt) (Games necklace -> Wintertodt Camp)"),
	MENAPHITE_SEA("Menaphite Sea", SeaChartRegion.ARDENT, "Fairy ring AKP -> Necropolis (Pharaoh's sceptre -> Jalsavrah/Sophanem)"),
	FREMENSUND("Fremensund", SeaChartRegion.NORTHERN, "Waterbirth teleport (Lunar) (enchanted lyre / Fremennik sea boots -> Rellekka)"),
	GRANDROOT_BAY("Grandroot Bay", SeaChartRegion.NORTHERN, "Games necklace -> Barbarian Outpost (fairy ring ALP -> Lighthouse)"),
	VS_BELT("V's Belt", SeaChartRegion.NORTHERN, "Ring of wealth -> Miscellania (fairy ring CIP -> Miscellania)"),
	FREMENNIK_STRAIT("Fremennik Strait", SeaChartRegion.NORTHERN, "Piscatoris teleport scroll / Western banner 4 (fairy ring AKQ)"),
	IDESTIA_STRAIT("Idestia Strait", SeaChartRegion.NORTHERN, "Ring of wealth -> Miscellania (Waterbirth teleport)"),
	LUNAR_BAY("Lunar Bay", SeaChartRegion.NORTHERN, "Moonclan teleport (Lunar)"),
	LUNAR_SEA("Lunar Sea", SeaChartRegion.NORTHERN, "Moonclan teleport (Lunar)"),
	KANNSKI_TIDES("Kannski Tides", SeaChartRegion.NORTHERN, "Fairy ring AJS -> Penguins near Miscellania (fairy ring CIP -> Miscellania)"),
	WEISSMERE("Weissmere", SeaChartRegion.NORTHERN, "Icy basalt -> Weiss"),
	STONEHEART_SEA("Stoneheart Sea", SeaChartRegion.NORTHERN, "Fairy ring AJS -> Penguins (fairy ring CIP -> Miscellania)"),
	SHIVERWAKE_EXPANSE("Shiverwake Expanse", SeaChartRegion.NORTHERN, "Icy basalt -> Weiss"),
	WEISS_MELT("Weiss Melt", SeaChartRegion.NORTHERN, "Icy basalt -> Weiss"),
	AUREUM_COAST("Aureum Coast", SeaChartRegion.SHROUDED, "Fairy ring AJP -> Avium Savannah (Civitas illa Fortis Teleport)"),
	THE_EVERDEEP("The Everdeep", SeaChartRegion.SHROUDED, "Minigame teleport -> Soul Wars (fairy ring AJP)"),
	SOUTHERN_EXPANSE("Southern Expanse", SeaChartRegion.SHROUDED, "Minigame teleport -> Soul Wars"),
	FORTIS_BAY("Fortis Bay", SeaChartRegion.SHROUDED, "Civitas illa Fortis Teleport (fairy ring AJP -> Avium Savannah)"),
	WYRMS_WATERS("Wyrm's Waters", SeaChartRegion.SHROUDED, "Fairy ring AJP -> Avium Savannah (quetzal whistle -> Hunter Guild)"),
	CRABCLAW_BAY("Crabclaw Bay", SeaChartRegion.WESTERN_KOUREND, "Skills necklace -> Woodcutting Guild (Rada's blessing -> Kourend Woodland)"),
	LITUS_LUCIS("Litus Lucis", SeaChartRegion.WESTERN_KOUREND, "Civitas illa Fortis Teleport"),
	GREAT_SOUND("Great Sound", SeaChartRegion.WESTERN_KOUREND, "Rada's blessing -> Kourend Woodland (fairy ring AIS -> Auburn Valley)"),
	MOONSHADOW("Moonshadow", SeaChartRegion.WESTERN_KOUREND, "Moonclan teleport (Lunar)"),
	SUNSET_BAY("Sunset Bay", SeaChartRegion.SUNSET, "Quetzal whistle -> Hunter Guild (fairy ring AJP)"),
	MISTY_SEA("Misty Sea", SeaChartRegion.SUNSET, "Fairy ring CKQ -> Aldarin (quetzal whistle -> Hunter Guild)"),
	DUSKS_MAW("Dusk's Maw", SeaChartRegion.SUNSET, "Fairy ring CKQ -> Aldarin"),
	;

	private final String label;
	private final SeaChartRegion region;
	private final String nearestPort;

	/**
	 * Maps a task's stable {@link SeaChartTask#getTaskId()} (0-357) to its real individual sea, per
	 * the same authoritative per-task table {@link SeaChartRegion} sources from -- see this enum's
	 * class-level Javadoc for the source and methodology.
	 */
	static SeaChartSea forTaskId(int taskId)
	{
		return BY_TASK_ID[taskId];
	}

	// Index == SeaChartTask#getTaskId() (0-357). The trailing comment on each line is the task's
	// real-world sea name, included purely for auditability against the source table -- it's not
	// otherwise used at runtime.
	private static final SeaChartSea[] BY_TASK_ID = {
		BAY_OF_SARIM, // 0: Bay of Sarim
		BAY_OF_SARIM, // 1: Bay of Sarim
		BAY_OF_SARIM, // 2: Bay of Sarim
		BAY_OF_SARIM, // 3: Bay of Sarim
		BAY_OF_SARIM, // 4: Bay of Sarim
		BAY_OF_SARIM, // 5: Bay of Sarim
		MUDSKIPPER_SOUND, // 6: Mudskipper Sound
		KHARIDIAN_SEA, // 7: Kharidian Sea
		MUDSKIPPER_SOUND, // 8: Mudskipper Sound
		MUDSKIPPER_SOUND, // 9: Mudskipper Sound
		MUDSKIPPER_SOUND, // 10: Mudskipper Sound
		KHARIDIAN_SEA, // 11: Kharidian Sea
		KHARIDIAN_SEA, // 12: Kharidian Sea
		KHARIDIAN_SEA, // 13: Kharidian Sea
		KHARIDIAN_SEA, // 14: Kharidian Sea
		KHARIDIAN_SEA, // 15: Kharidian Sea
		KHARIDIAN_SEA, // 16: Kharidian Sea
		LUMBRIDGE_BASIN, // 17: Lumbridge Basin
		LUMBRIDGE_BASIN, // 18: Lumbridge Basin
		LUMBRIDGE_BASIN, // 19: Lumbridge Basin
		LUMBRIDGE_BASIN, // 20: Lumbridge Basin
		RIMMINGTON_STRAIT, // 21: Rimmington Strait
		RIMMINGTON_STRAIT, // 22: Rimmington Strait
		RIMMINGTON_STRAIT, // 23: Rimmington Strait
		RIMMINGTON_STRAIT, // 24: Rimmington Strait
		CATHERBY_BAY, // 25: Catherby Bay
		CATHERBY_BAY, // 26: Catherby Bay
		CATHERBY_BAY, // 27: Catherby Bay
		CATHERBY_BAY, // 28: Catherby Bay
		BRIMHAVEN_PASSAGE, // 29: Brimhaven Passage
		BRIMHAVEN_PASSAGE, // 30: Brimhaven Passage
		BRIMHAVEN_PASSAGE, // 31: Brimhaven Passage
		BRIMHAVEN_PASSAGE, // 32: Brimhaven Passage
		STRAIT_OF_KHAZARD, // 33: Strait of Khazard
		STRAIT_OF_KHAZARD, // 34: Strait of Khazard
		STRAIT_OF_KHAZARD, // 35: Strait of Khazard
		STRAIT_OF_KHAZARD, // 36: Strait of Khazard
		STRAIT_OF_KHAZARD, // 37: Strait of Khazard
		MUDSKIPPER_SOUND, // 38: Mudskipper Sound
		LUMBRIDGE_BASIN, // 39: Lumbridge Basin
		CATHERBY_BAY, // 40: Catherby Bay
		STRAIT_OF_KHAZARD, // 41: Strait of Khazard
		RIMMINGTON_STRAIT, // 42: Rimmington Strait
		STRAIT_OF_KHAZARD, // 43: Strait of Khazard
		MUDSKIPPER_SOUND, // 44: Mudskipper Sound
		CATHERBY_BAY, // 45: Catherby Bay
		STRAIT_OF_KHAZARD, // 46: Strait of Khazard
		RED_REEF, // 47: Red Reef
		BARRACUDA_BELT, // 48: Barracuda Belt
		ARROW_PASSAGE, // 49: Arrow Passage
		TURTLE_BELT, // 50: Turtle Belt
		TORTUGAN_SEA, // 51: Tortugan Sea
		RAINBOW_REEF, // 52: Rainbow Reef
		ANGLERFISHS_LIGHT, // 53: Anglerfish's Light
		THE_SKULLHORDE, // 54: The Skullhorde
		BARRACUDA_BELT, // 55: Barracuda Belt
		ARROW_PASSAGE, // 56: Arrow Passage
		THE_STORM_TEMPOR, // 57: The Storm Tempor
		PEARL_BANK, // 58: Pearl Bank
		ANGLERFISHS_LIGHT, // 59: Anglerfish's Light
		THE_SIMIAN_SEA, // 60: The Simian Sea
		BARRACUDA_BELT, // 61: Barracuda Belt
		ARROW_PASSAGE, // 62: Arrow Passage
		KHARAZI_STRAIT, // 63: Kharazi Strait
		TURTLE_BELT, // 64: Turtle Belt
		SEA_OF_SHELLS, // 65: Sea of Shells
		RAINBOW_REEF, // 66: Rainbow Reef
		ANGLERFISHS_LIGHT, // 67: Anglerfish's Light
		THE_SIMIAN_SEA, // 68: The Simian Sea
		THE_SIMIAN_SEA, // 69: The Simian Sea
		KHARAZI_STRAIT, // 70: Kharazi Strait
		RED_REEF, // 71: Red Reef
		THE_STORM_TEMPOR, // 72: The Storm Tempor
		SEA_OF_SHELLS, // 73: Sea of Shells
		RAINBOW_REEF, // 74: Rainbow Reef
		THE_LONELY_SEA, // 75: The Lonely Sea
		THE_SIMIAN_SEA, // 76: The Simian Sea
		ARROW_PASSAGE, // 77: Arrow Passage
		KHARAZI_STRAIT, // 78: Kharazi Strait
		TURTLE_BELT, // 79: Turtle Belt
		THE_STORM_TEMPOR, // 80: The Storm Tempor
		SEA_OF_SHELLS, // 81: Sea of Shells
		BAY_OF_ELIDINIS, // 82: Bay of Elidinis
		ANGLERFISHS_LIGHT, // 83: Anglerfish's Light
		THE_SIMIAN_SEA, // 84: The Simian Sea
		OOGLOG_CHANNEL, // 85: Oo'glog Channel
		RED_REEF, // 86: Red Reef
		THE_SKULLHORDE, // 87: The Skullhorde
		BARRACUDA_BELT, // 88: Barracuda Belt
		BARRACUDA_BELT, // 89: Barracuda Belt
		KHARAZI_STRAIT, // 90: Kharazi Strait
		ARROW_PASSAGE, // 91: Arrow Passage
		KHARAZI_STRAIT, // 92: Kharazi Strait
		KHARAZI_STRAIT, // 93: Kharazi Strait
		TURTLE_BELT, // 94: Turtle Belt
		BARRACUDA_BELT, // 95: Barracuda Belt
		THE_STORM_TEMPOR, // 96: The Storm Tempor
		THE_STORM_TEMPOR, // 97: The Storm Tempor
		TORTUGAN_SEA, // 98: Tortugan Sea
		TORTUGAN_SEA, // 99: Tortugan Sea
		RAINBOW_REEF, // 100: Rainbow Reef
		RAINBOW_REEF, // 101: Rainbow Reef
		THE_LONELY_SEA, // 102: The Lonely Sea
		ANGLERFISHS_LIGHT, // 103: Anglerfish's Light
		GUTANOTH_BAY, // 104: Gu'tanoth Bay
		BREAKBONE_STRAIT, // 105: Breakbone Strait
		SOUL_BAY, // 106: Soul Bay
		PORTH_NEIGWL, // 107: Porth Neigwl
		GUTANOTH_BAY, // 108: Gu'tanoth Bay
		OOGLOG_CHANNEL, // 109: Oo'glog Channel
		BREAKBONE_STRAIT, // 110: Breakbone Strait
		SEA_OF_SOULS, // 111: Sea of Souls
		BACKWATER, // 112: Backwater
		ZUL_EGIL, // 113: Zul-Egil
		ZUL_EGIL, // 114: Zul-Egil
		GUTANOTH_BAY, // 115: Gu'tanoth Bay
		FELDIP_GULF, // 116: Feldip Gulf
		OOGLOG_CHANNEL, // 117: Oo'glog Channel
		MYTHIC_SEA, // 118: Mythic Sea
		BREAKBONE_STRAIT, // 119: Breakbone Strait
		BACKWATER, // 120: Backwater
		BACKWATER, // 121: Backwater
		SOUL_BAY, // 122: Soul Bay
		SEA_OF_SOULS, // 123: Sea of Souls
		ZUL_EGIL, // 124: Zul-Egil
		GUTANOTH_BAY, // 125: Gu'tanoth Bay
		OOGLOG_CHANNEL, // 126: Oo'glog Channel
		MYTHIC_SEA, // 127: Mythic Sea
		BACKWATER, // 128: Backwater
		SOUL_BAY, // 129: Soul Bay
		FELDIP_GULF, // 130: Feldip Gulf
		MYTHIC_SEA, // 131: Mythic Sea
		BREAKBONE_STRAIT, // 132: Breakbone Strait
		SOUL_BAY, // 133: Soul Bay
		BACKWATER, // 134: Backwater
		GUTANOTH_BAY, // 135: Gu'tanoth Bay
		FELDIP_GULF, // 136: Feldip Gulf
		OOGLOG_CHANNEL, // 137: Oo'glog Channel
		MYTHIC_SEA, // 138: Mythic Sea
		SEA_OF_SOULS, // 139: Sea of Souls
		BREAKBONE_STRAIT, // 140: Breakbone Strait
		BACKWATER, // 141: Backwater
		WESTERN_GATE, // 142: Western Gate
		SOUL_BAY, // 143: Soul Bay
		ZUL_EGIL, // 144: Zul-Egil
		TIRANNWN_BIGHT, // 145: Tirannwn Bight
		PORTH_NEIGWL, // 146: Porth Neigwl
		WESTERN_GATE, // 147: Western Gate
		PORTH_NEIGWL, // 148: Porth Neigwl
		TIRANNWN_BIGHT, // 149: Tirannwn Bight
		PORTH_NEIGWL, // 150: Porth Neigwl
		TIRANNWN_BIGHT, // 151: Tirannwn Bight
		WESTERN_GATE, // 152: Western Gate
		CRYSTAL_SEA, // 153: Crystal Sea
		PORTH_NEIGWL, // 154: Porth Neigwl
		SAPPHIRE_SEA, // 155: Sapphire Sea
		TIRANNWN_BIGHT, // 156: Tirannwn Bight
		WESTERN_GATE, // 157: Western Gate
		CRYSTAL_SEA, // 158: Crystal Sea
		PORTH_NEIGWL, // 159: Porth Neigwl
		TIRANNWN_BIGHT, // 160: Tirannwn Bight
		VAGABONDS_REST, // 161: Vagabonds Rest
		WESTERN_GATE, // 162: Western Gate
		CRYSTAL_SEA, // 163: Crystal Sea
		PORTH_NEIGWL, // 164: Porth Neigwl
		PORTH_GWENITH, // 165: Porth Gwenith
		TIRANNWN_BIGHT, // 166: Tirannwn Bight
		PORTH_GWENITH, // 167: Porth Gwenith
		PILGRIMS_PASSAGE, // 168: Pilgrims' Passage
		WINTERS_EDGE, // 169: Winter's Edge
		PORTH_GWENITH, // 170: Porth Gwenith
		PISCATORIS_SEA, // 171: Piscatoris Sea
		GULF_OF_KOUREND, // 172: Gulf of Kourend
		PORTH_GWENITH, // 173: Porth Gwenith
		PILGRIMS_PASSAGE, // 174: Pilgrims' Passage
		GULF_OF_KOUREND, // 175: Gulf of Kourend
		WINTERS_EDGE, // 176: Winter's Edge
		PISCATORIS_SEA, // 177: Piscatoris Sea
		HOSIDIAN_SEA, // 178: Hosidian Sea
		GULF_OF_KOUREND, // 179: Gulf of Kourend
		EVERWINTER_SEA, // 180: Everwinter Sea
		VAGABONDS_REST, // 181: Vagabonds Rest
		HOSIDIAN_SEA, // 182: Hosidian Sea
		GULF_OF_KOUREND, // 183: Gulf of Kourend
		WINTERS_EDGE, // 184: Winter's Edge
		PORTH_GWENITH, // 185: Porth Gwenith
		VAGABONDS_REST, // 186: Vagabonds Rest
		PISCATORIS_SEA, // 187: Piscatoris Sea
		HOSIDIAN_SEA, // 188: Hosidian Sea
		GULF_OF_KOUREND, // 189: Gulf of Kourend
		EVERWINTER_SEA, // 190: Everwinter Sea
		MENAPHITE_SEA, // 191: Menaphite Sea
		MENAPHITE_SEA, // 192: Menaphite Sea
		MENAPHITE_SEA, // 193: Menaphite Sea
		MENAPHITE_SEA, // 194: Menaphite Sea
		MENAPHITE_SEA, // 195: Menaphite Sea
		FREMENSUND, // 196: Fremensund
		GRANDROOT_BAY, // 197: Grandroot Bay
		VS_BELT, // 198: V's Belt
		FREMENNIK_STRAIT, // 199: Fremennik Strait
		IDESTIA_STRAIT, // 200: Idestia Strait
		LUNAR_BAY, // 201: Lunar Bay
		WINTERS_EDGE, // 202: Winter's Edge
		LUNAR_SEA, // 203: Lunar Sea
		KANNSKI_TIDES, // 204: Kannski Tides
		WEISSMERE, // 205: Weissmere
		STONEHEART_SEA, // 206: Stoneheart Sea
		SHIVERWAKE_EXPANSE, // 207: Shiverwake Expanse
		WEISS_MELT, // 208: Weiss Melt
		FREMENSUND, // 209: Fremensund
		GRANDROOT_BAY, // 210: Grandroot Bay
		VS_BELT, // 211: V's Belt
		FREMENNIK_STRAIT, // 212: Fremennik Strait
		IDESTIA_STRAIT, // 213: Idestia Strait
		LUNAR_BAY, // 214: Lunar Bay
		EVERWINTER_SEA, // 215: Everwinter Sea
		KANNSKI_TIDES, // 216: Kannski Tides
		WEISSMERE, // 217: Weissmere
		STONEHEART_SEA, // 218: Stoneheart Sea
		WEISS_MELT, // 219: Weiss Melt
		FREMENSUND, // 220: Fremensund
		GRANDROOT_BAY, // 221: Grandroot Bay
		FREMENNIK_STRAIT, // 222: Fremennik Strait
		IDESTIA_STRAIT, // 223: Idestia Strait
		EVERWINTER_SEA, // 224: Everwinter Sea
		STONEHEART_SEA, // 225: Stoneheart Sea
		SHIVERWAKE_EXPANSE, // 226: Shiverwake Expanse
		WEISS_MELT, // 227: Weiss Melt
		FREMENSUND, // 228: Fremensund
		GRANDROOT_BAY, // 229: Grandroot Bay
		VS_BELT, // 230: V's Belt
		FREMENNIK_STRAIT, // 231: Fremennik Strait
		IDESTIA_STRAIT, // 232: Idestia Strait
		LUNAR_BAY, // 233: Lunar Bay
		WINTERS_EDGE, // 234: Winter's Edge
		LUNAR_SEA, // 235: Lunar Sea
		KANNSKI_TIDES, // 236: Kannski Tides
		WEISSMERE, // 237: Weissmere
		STONEHEART_SEA, // 238: Stoneheart Sea
		SHIVERWAKE_EXPANSE, // 239: Shiverwake Expanse
		WEISS_MELT, // 240: Weiss Melt
		FREMENSUND, // 241: Fremensund
		GRANDROOT_BAY, // 242: Grandroot Bay
		VS_BELT, // 243: V's Belt
		IDESTIA_STRAIT, // 244: Idestia Strait
		LUNAR_BAY, // 245: Lunar Bay
		WINTERS_EDGE, // 246: Winter's Edge
		LUNAR_SEA, // 247: Lunar Sea
		EVERWINTER_SEA, // 248: Everwinter Sea
		KANNSKI_TIDES, // 249: Kannski Tides
		WEISSMERE, // 250: Weissmere
		STONEHEART_SEA, // 251: Stoneheart Sea
		SHIVERWAKE_EXPANSE, // 252: Shiverwake Expanse
		FREMENSUND, // 253: Fremensund
		GRANDROOT_BAY, // 254: Grandroot Bay
		VS_BELT, // 255: V's Belt
		FREMENNIK_STRAIT, // 256: Fremennik Strait
		IDESTIA_STRAIT, // 257: Idestia Strait
		LUNAR_BAY, // 258: Lunar Bay
		LUNAR_SEA, // 259: Lunar Sea
		EVERWINTER_SEA, // 260: Everwinter Sea
		KANNSKI_TIDES, // 261: Kannski Tides
		WEISSMERE, // 262: Weissmere
		STONEHEART_SEA, // 263: Stoneheart Sea
		SHIVERWAKE_EXPANSE, // 264: Shiverwake Expanse
		WEISS_MELT, // 265: Weiss Melt
		TURTLE_BELT, // 266: Turtle Belt
		BAY_OF_ELIDINIS, // 267: Bay of Elidinis
		TORTUGAN_SEA, // 268: Tortugan Sea
		PEARL_BANK, // 269: Pearl Bank
		TORTUGAN_SEA, // 270: Tortugan Sea
		SEA_OF_SHELLS, // 271: Sea of Shells
		BAY_OF_ELIDINIS, // 272: Bay of Elidinis
		PEARL_BANK, // 273: Pearl Bank
		THE_LONELY_SEA, // 274: The Lonely Sea
		SEA_OF_SHELLS, // 275: Sea of Shells
		BAY_OF_ELIDINIS, // 276: Bay of Elidinis
		PEARL_BANK, // 277: Pearl Bank
		RED_REEF, // 278: Red Reef
		BAY_OF_ELIDINIS, // 279: Bay of Elidinis
		PEARL_BANK, // 280: Pearl Bank
		THE_LONELY_SEA, // 281: The Lonely Sea
		AUREUM_COAST, // 282: Aureum Coast
		THE_EVERDEEP, // 283: The Everdeep
		SOUTHERN_EXPANSE, // 284: Southern Expanse
		FORTIS_BAY, // 285: Fortis Bay
		AUREUM_COAST, // 286: Aureum Coast
		WYRMS_WATERS, // 287: Wyrm's Waters
		THE_EVERDEEP, // 288: The Everdeep
		SAPPHIRE_SEA, // 289: Sapphire Sea
		FORTIS_BAY, // 290: Fortis Bay
		AUREUM_COAST, // 291: Aureum Coast
		WYRMS_WATERS, // 292: Wyrm's Waters
		THE_SKULLHORDE, // 293: The Skullhorde
		THE_EVERDEEP, // 294: The Everdeep
		SAPPHIRE_SEA, // 295: Sapphire Sea
		SOUTHERN_EXPANSE, // 296: Southern Expanse
		FORTIS_BAY, // 297: Fortis Bay
		AUREUM_COAST, // 298: Aureum Coast
		WYRMS_WATERS, // 299: Wyrm's Waters
		THE_EVERDEEP, // 300: The Everdeep
		SAPPHIRE_SEA, // 301: Sapphire Sea
		SOUTHERN_EXPANSE, // 302: Southern Expanse
		FORTIS_BAY, // 303: Fortis Bay
		WYRMS_WATERS, // 304: Wyrm's Waters
		THE_SKULLHORDE, // 305: The Skullhorde
		SAPPHIRE_SEA, // 306: Sapphire Sea
		FORTIS_BAY, // 307: Fortis Bay
		AUREUM_COAST, // 308: Aureum Coast
		WYRMS_WATERS, // 309: Wyrm's Waters
		THE_SKULLHORDE, // 310: The Skullhorde
		SEA_OF_SOULS, // 311: Sea of Souls
		THE_EVERDEEP, // 312: The Everdeep
		SOUTHERN_EXPANSE, // 313: Southern Expanse
		CRABCLAW_BAY, // 314: Crabclaw Bay
		LITUS_LUCIS, // 315: Litus Lucis
		GREAT_SOUND, // 316: Great Sound
		CRABCLAW_BAY, // 317: Crabclaw Bay
		CRABCLAW_BAY, // 318: Crabclaw Bay
		CRYSTAL_SEA, // 319: Crystal Sea
		VAGABONDS_REST, // 320: Vagabonds Rest
		CRABCLAW_BAY, // 321: Crabclaw Bay
		HOSIDIAN_SEA, // 322: Hosidian Sea
		PILGRIMS_PASSAGE, // 323: Pilgrims' Passage
		LITUS_LUCIS, // 324: Litus Lucis
		VAGABONDS_REST, // 325: Vagabonds Rest
		MOONSHADOW, // 326: Moonshadow
		GREAT_SOUND, // 327: Great Sound
		LITUS_LUCIS, // 328: Litus Lucis
		CRYSTAL_SEA, // 329: Crystal Sea
		MOONSHADOW, // 330: Moonshadow
		GREAT_SOUND, // 331: Great Sound
		PILGRIMS_PASSAGE, // 332: Pilgrims' Passage
		LITUS_LUCIS, // 333: Litus Lucis
		PISCATORIS_SEA, // 334: Piscatoris Sea
		MOONSHADOW, // 335: Moonshadow
		GREAT_SOUND, // 336: Great Sound
		GREAT_SOUND, // 337: Great Sound
		CRABCLAW_BAY, // 338: Crabclaw Bay
		PILGRIMS_PASSAGE, // 339: Pilgrims' Passage
		LITUS_LUCIS, // 340: Litus Lucis
		CRYSTAL_SEA, // 341: Crystal Sea
		MOONSHADOW, // 342: Moonshadow
		SUNSET_BAY, // 343: Sunset Bay
		MISTY_SEA, // 344: Misty Sea
		DUSKS_MAW, // 345: Dusk's Maw
		SUNSET_BAY, // 346: Sunset Bay
		DUSKS_MAW, // 347: Dusk's Maw
		SUNSET_BAY, // 348: Sunset Bay
		MISTY_SEA, // 349: Misty Sea
		DUSKS_MAW, // 350: Dusk's Maw
		SUNSET_BAY, // 351: Sunset Bay
		MISTY_SEA, // 352: Misty Sea
		DUSKS_MAW, // 353: Dusk's Maw
		SUNSET_BAY, // 354: Sunset Bay
		MISTY_SEA, // 355: Misty Sea
		DUSKS_MAW, // 356: Dusk's Maw
		KHARIDIAN_SEA, // 357: Kharidian Sea
	};
}
