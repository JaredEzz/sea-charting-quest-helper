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
 * The named sea-charting oceans, purely so each task can point at the closest port with a
 * reasonably direct teleport -- not just its raw tile distance.
 *
 * <p><b>Sourced directly from the OSRS Wiki's "Sea charting" page</b> (the {@code {{SeaChartRow}}}
 * template instances backing its "All tasks" table), which tags every one of the 358 tasks with an
 * explicit {@code id=} parameter matching this plugin's {@link SeaChartTask#getTaskId()} /
 * WikiSync {@code sea_charting} numbering (0-357) one-for-one, plus explicit {@code sea=} and
 * {@code ocean=} parameters for that task. This superseded an earlier version of this enum that
 * guessed ocean boundaries from rough index ranges and got everything past roughly task 175 wrong
 * (see git history) -- e.g. the old "Western" range (194-249) was overwhelmingly Fremennik/Weiss
 * icy-sea coordinates (i.e. actually {@link #NORTHERN}), while the tasks matching Western Ocean's
 * real place names (Great Sound, Crabclaw Bay, Gulf of Kourend, Hosidian Sea, Pilgrims' Passage,
 * Crystal Sea, Litus Lucis, Moonshadow, Vagabonds Rest, Porth Neigwl, Piscatoris Sea, Porth
 * Gwenith, Tirannwn Bight) actually live scattered across ids 107-189 and 314-342.
 *
 * <p>The wiki's {@code ocean=} field also has a seventh value, "Bonus charts", covering the
 * account-wide Miscellaneous XP-bonus category (bonus dives/currents/weather/crates) -- but that's
 * an XP-bookkeeping category, not a geographic one: every "Bonus charts" task shares a {@code sea=}
 * with ordinary same-ocean tasks (e.g. a bonus current-duck task in "Kharidian Sea" sits right next
 * to Ardent Ocean's other Kharidian Sea tasks), and no {@code sea} name is ever used by both a
 * "Bonus charts" task and a task in a different real ocean. So {@link #forTaskId} resolves every
 * task's real geographic ocean via its {@code sea}, folding "Bonus charts" tasks into whichever
 * ocean their sea actually sits in -- giving every task a genuinely nearby port hint instead of a
 * one-size-fits-all "Miscellaneous" bucket.
 *
 * <p>The Western Ocean's 13 seas split into two geographically distinct clusters with different
 * best ports, so it's represented here as two enum constants ({@link #WESTERN_KOUREND} and
 * {@link #WESTERN_TIRANNWN}) sharing the same display label: the Great Kourend / Land's End /
 * Hosidius side (Great Sound, Crabclaw Bay, Gulf of Kourend, Hosidian Sea, Pilgrims' Passage,
 * Crystal Sea, Litus Lucis, Moonshadow, Vagabonds Rest), and the Tirannwn / Piscatoris side (Porth
 * Neigwl, Tirannwn Bight, Porth Gwenith, Piscatoris Sea).
 *
 * <p>Confidence is high: every task id in {@link SeaChartTask} was cross-referenced against the
 * wiki's per-task {@code ocean=}/{@code sea=} tags (not re-derived from coordinates). Before
 * folding "Bonus charts" into its real ocean, the raw wiki tags summed to the wiki's published
 * per-ocean totals exactly (Ardent 69, Unquiet 36, Shrouded 75, Sunset 13, Western 57, Northern 75,
 * plus 33 "Bonus charts" -- 358 total); after folding, the final per-{@link #forTaskId} tallies are
 * {@link #ARDENT} 87, {@link #UNQUIET} 38, {@link #SHROUDED} 78, {@link #SUNSET} 14,
 * {@link #WESTERN_KOUREND} 44, {@link #WESTERN_TIRANNWN} 22, {@link #NORTHERN} 75 (358 total). No
 * remaining boundary here is a guess.
 */
@Getter
@RequiredArgsConstructor
enum SeaChartRegion
{
	ARDENT("Ardent Ocean", "The Pandemonium (bank + shipwright)"),
	UNQUIET("Unquiet Ocean", "Dognose Island"),
	SHROUDED("Shrouded Ocean", "Deepfin Point (shipwright, bank)"),
	SUNSET("Sunset Ocean", "Aldarin tele (or Sunset Coast)"),
	WESTERN_KOUREND("Western Ocean", "Kourend Castle tele (or Xeric's talisman) -> Land's End dock (or Hosidius)"),
	WESTERN_TIRANNWN("Western Ocean", "Port Tyras tele (or Prifddinas) -> Piscatoris/Tirannwn coast"),
	NORTHERN("Northern Ocean", "Rellekka (full facilities)"),
	;

	private final String label;
	private final String nearestPort;

	/**
	 * Maps a task's stable {@link SeaChartTask#getTaskId()} (0-357) to its real ocean/region, per
	 * an authoritative per-task lookup table sourced from the OSRS Wiki -- see this enum's
	 * class-level Javadoc for the source and methodology.
	 */
	static SeaChartRegion forTaskId(int taskId)
	{
		return BY_TASK_ID[taskId];
	}

	// Index == SeaChartTask#getTaskId() (0-357). The trailing comment on each line is the task's
	// real-world "sea" name per the OSRS Wiki, included purely for auditability against the
	// source table -- it's not otherwise used at runtime.
	private static final SeaChartRegion[] BY_TASK_ID = {
		ARDENT, // 0: Bay of Sarim
		ARDENT, // 1: Bay of Sarim
		ARDENT, // 2: Bay of Sarim
		ARDENT, // 3: Bay of Sarim
		ARDENT, // 4: Bay of Sarim
		ARDENT, // 5: Bay of Sarim
		ARDENT, // 6: Mudskipper Sound
		ARDENT, // 7: Kharidian Sea
		ARDENT, // 8: Mudskipper Sound
		ARDENT, // 9: Mudskipper Sound
		ARDENT, // 10: Mudskipper Sound
		ARDENT, // 11: Kharidian Sea
		ARDENT, // 12: Kharidian Sea
		ARDENT, // 13: Kharidian Sea
		ARDENT, // 14: Kharidian Sea
		ARDENT, // 15: Kharidian Sea
		ARDENT, // 16: Kharidian Sea
		ARDENT, // 17: Lumbridge Basin
		ARDENT, // 18: Lumbridge Basin
		ARDENT, // 19: Lumbridge Basin
		ARDENT, // 20: Lumbridge Basin
		ARDENT, // 21: Rimmington Strait
		ARDENT, // 22: Rimmington Strait
		ARDENT, // 23: Rimmington Strait
		ARDENT, // 24: Rimmington Strait
		ARDENT, // 25: Catherby Bay
		ARDENT, // 26: Catherby Bay
		ARDENT, // 27: Catherby Bay
		ARDENT, // 28: Catherby Bay
		ARDENT, // 29: Brimhaven Passage
		ARDENT, // 30: Brimhaven Passage
		ARDENT, // 31: Brimhaven Passage
		ARDENT, // 32: Brimhaven Passage
		ARDENT, // 33: Strait of Khazard
		ARDENT, // 34: Strait of Khazard
		ARDENT, // 35: Strait of Khazard
		ARDENT, // 36: Strait of Khazard
		ARDENT, // 37: Strait of Khazard
		ARDENT, // 38: Mudskipper Sound
		ARDENT, // 39: Lumbridge Basin
		ARDENT, // 40: Catherby Bay
		ARDENT, // 41: Strait of Khazard
		ARDENT, // 42: Rimmington Strait
		ARDENT, // 43: Strait of Khazard
		ARDENT, // 44: Mudskipper Sound
		ARDENT, // 45: Catherby Bay
		ARDENT, // 46: Strait of Khazard
		UNQUIET, // 47: Red Reef
		SHROUDED, // 48: Barracuda Belt
		ARDENT, // 49: Arrow Passage
		UNQUIET, // 50: Turtle Belt
		UNQUIET, // 51: Tortugan Sea
		SHROUDED, // 52: Rainbow Reef
		UNQUIET, // 53: Anglerfish's Light
		SHROUDED, // 54: The Skullhorde
		SHROUDED, // 55: Barracuda Belt
		ARDENT, // 56: Arrow Passage
		ARDENT, // 57: The Storm Tempor
		UNQUIET, // 58: Pearl Bank
		UNQUIET, // 59: Anglerfish's Light
		ARDENT, // 60: The Simian Sea
		SHROUDED, // 61: Barracuda Belt
		ARDENT, // 62: Arrow Passage
		ARDENT, // 63: Kharazi Strait
		UNQUIET, // 64: Turtle Belt
		UNQUIET, // 65: Sea of Shells
		SHROUDED, // 66: Rainbow Reef
		UNQUIET, // 67: Anglerfish's Light
		ARDENT, // 68: The Simian Sea
		ARDENT, // 69: The Simian Sea
		ARDENT, // 70: Kharazi Strait
		UNQUIET, // 71: Red Reef
		ARDENT, // 72: The Storm Tempor
		UNQUIET, // 73: Sea of Shells
		SHROUDED, // 74: Rainbow Reef
		UNQUIET, // 75: The Lonely Sea
		ARDENT, // 76: The Simian Sea
		ARDENT, // 77: Arrow Passage
		ARDENT, // 78: Kharazi Strait
		UNQUIET, // 79: Turtle Belt
		ARDENT, // 80: The Storm Tempor
		UNQUIET, // 81: Sea of Shells
		UNQUIET, // 82: Bay of Elidinis
		UNQUIET, // 83: Anglerfish's Light
		ARDENT, // 84: The Simian Sea
		ARDENT, // 85: Oo'glog Channel
		UNQUIET, // 86: Red Reef
		SHROUDED, // 87: The Skullhorde
		SHROUDED, // 88: Barracuda Belt
		SHROUDED, // 89: Barracuda Belt
		ARDENT, // 90: Kharazi Strait
		ARDENT, // 91: Arrow Passage
		ARDENT, // 92: Kharazi Strait
		ARDENT, // 93: Kharazi Strait
		UNQUIET, // 94: Turtle Belt
		SHROUDED, // 95: Barracuda Belt
		ARDENT, // 96: The Storm Tempor
		ARDENT, // 97: The Storm Tempor
		UNQUIET, // 98: Tortugan Sea
		UNQUIET, // 99: Tortugan Sea
		SHROUDED, // 100: Rainbow Reef
		SHROUDED, // 101: Rainbow Reef
		UNQUIET, // 102: The Lonely Sea
		UNQUIET, // 103: Anglerfish's Light
		ARDENT, // 104: Gu'tanoth Bay
		SHROUDED, // 105: Breakbone Strait
		SHROUDED, // 106: Soul Bay
		WESTERN_TIRANNWN, // 107: Porth Neigwl
		ARDENT, // 108: Gu'tanoth Bay
		ARDENT, // 109: Oo'glog Channel
		SHROUDED, // 110: Breakbone Strait
		SHROUDED, // 111: Sea of Souls
		SHROUDED, // 112: Backwater
		SHROUDED, // 113: Zul-Egil
		SHROUDED, // 114: Zul-Egil
		ARDENT, // 115: Gu'tanoth Bay
		ARDENT, // 116: Feldip Gulf
		ARDENT, // 117: Oo'glog Channel
		SHROUDED, // 118: Mythic Sea
		SHROUDED, // 119: Breakbone Strait
		SHROUDED, // 120: Backwater
		SHROUDED, // 121: Backwater
		SHROUDED, // 122: Soul Bay
		SHROUDED, // 123: Sea of Souls
		SHROUDED, // 124: Zul-Egil
		ARDENT, // 125: Gu'tanoth Bay
		ARDENT, // 126: Oo'glog Channel
		SHROUDED, // 127: Mythic Sea
		SHROUDED, // 128: Backwater
		SHROUDED, // 129: Soul Bay
		ARDENT, // 130: Feldip Gulf
		SHROUDED, // 131: Mythic Sea
		SHROUDED, // 132: Breakbone Strait
		SHROUDED, // 133: Soul Bay
		SHROUDED, // 134: Backwater
		ARDENT, // 135: Gu'tanoth Bay
		ARDENT, // 136: Feldip Gulf
		ARDENT, // 137: Oo'glog Channel
		SHROUDED, // 138: Mythic Sea
		SHROUDED, // 139: Sea of Souls
		SHROUDED, // 140: Breakbone Strait
		SHROUDED, // 141: Backwater
		SHROUDED, // 142: Western Gate
		SHROUDED, // 143: Soul Bay
		SHROUDED, // 144: Zul-Egil
		WESTERN_TIRANNWN, // 145: Tirannwn Bight
		WESTERN_TIRANNWN, // 146: Porth Neigwl
		SHROUDED, // 147: Western Gate
		WESTERN_TIRANNWN, // 148: Porth Neigwl
		WESTERN_TIRANNWN, // 149: Tirannwn Bight
		WESTERN_TIRANNWN, // 150: Porth Neigwl
		WESTERN_TIRANNWN, // 151: Tirannwn Bight
		SHROUDED, // 152: Western Gate
		WESTERN_KOUREND, // 153: Crystal Sea
		WESTERN_TIRANNWN, // 154: Porth Neigwl
		SHROUDED, // 155: Sapphire Sea
		WESTERN_TIRANNWN, // 156: Tirannwn Bight
		SHROUDED, // 157: Western Gate
		WESTERN_KOUREND, // 158: Crystal Sea
		WESTERN_TIRANNWN, // 159: Porth Neigwl
		WESTERN_TIRANNWN, // 160: Tirannwn Bight
		WESTERN_KOUREND, // 161: Vagabonds Rest
		SHROUDED, // 162: Western Gate
		WESTERN_KOUREND, // 163: Crystal Sea
		WESTERN_TIRANNWN, // 164: Porth Neigwl
		WESTERN_TIRANNWN, // 165: Porth Gwenith
		WESTERN_TIRANNWN, // 166: Tirannwn Bight
		WESTERN_TIRANNWN, // 167: Porth Gwenith
		WESTERN_KOUREND, // 168: Pilgrims' Passage
		NORTHERN, // 169: Winter's Edge
		WESTERN_TIRANNWN, // 170: Porth Gwenith
		WESTERN_TIRANNWN, // 171: Piscatoris Sea
		WESTERN_KOUREND, // 172: Gulf of Kourend
		WESTERN_TIRANNWN, // 173: Porth Gwenith
		WESTERN_KOUREND, // 174: Pilgrims' Passage
		WESTERN_KOUREND, // 175: Gulf of Kourend
		NORTHERN, // 176: Winter's Edge
		WESTERN_TIRANNWN, // 177: Piscatoris Sea
		WESTERN_KOUREND, // 178: Hosidian Sea
		WESTERN_KOUREND, // 179: Gulf of Kourend
		NORTHERN, // 180: Everwinter Sea
		WESTERN_KOUREND, // 181: Vagabonds Rest
		WESTERN_KOUREND, // 182: Hosidian Sea
		WESTERN_KOUREND, // 183: Gulf of Kourend
		NORTHERN, // 184: Winter's Edge
		WESTERN_TIRANNWN, // 185: Porth Gwenith
		WESTERN_KOUREND, // 186: Vagabonds Rest
		WESTERN_TIRANNWN, // 187: Piscatoris Sea
		WESTERN_KOUREND, // 188: Hosidian Sea
		WESTERN_KOUREND, // 189: Gulf of Kourend
		NORTHERN, // 190: Everwinter Sea
		ARDENT, // 191: Menaphite Sea
		ARDENT, // 192: Menaphite Sea
		ARDENT, // 193: Menaphite Sea
		ARDENT, // 194: Menaphite Sea
		ARDENT, // 195: Menaphite Sea
		NORTHERN, // 196: Fremensund
		NORTHERN, // 197: Grandroot Bay
		NORTHERN, // 198: V's Belt
		NORTHERN, // 199: Fremennik Strait
		NORTHERN, // 200: Idestia Strait
		NORTHERN, // 201: Lunar Bay
		NORTHERN, // 202: Winter's Edge
		NORTHERN, // 203: Lunar Sea
		NORTHERN, // 204: Kannski Tides
		NORTHERN, // 205: Weissmere
		NORTHERN, // 206: Stoneheart Sea
		NORTHERN, // 207: Shiverwake Expanse
		NORTHERN, // 208: Weiss Melt
		NORTHERN, // 209: Fremensund
		NORTHERN, // 210: Grandroot Bay
		NORTHERN, // 211: V's Belt
		NORTHERN, // 212: Fremennik Strait
		NORTHERN, // 213: Idestia Strait
		NORTHERN, // 214: Lunar Bay
		NORTHERN, // 215: Everwinter Sea
		NORTHERN, // 216: Kannski Tides
		NORTHERN, // 217: Weissmere
		NORTHERN, // 218: Stoneheart Sea
		NORTHERN, // 219: Weiss Melt
		NORTHERN, // 220: Fremensund
		NORTHERN, // 221: Grandroot Bay
		NORTHERN, // 222: Fremennik Strait
		NORTHERN, // 223: Idestia Strait
		NORTHERN, // 224: Everwinter Sea
		NORTHERN, // 225: Stoneheart Sea
		NORTHERN, // 226: Shiverwake Expanse
		NORTHERN, // 227: Weiss Melt
		NORTHERN, // 228: Fremensund
		NORTHERN, // 229: Grandroot Bay
		NORTHERN, // 230: V's Belt
		NORTHERN, // 231: Fremennik Strait
		NORTHERN, // 232: Idestia Strait
		NORTHERN, // 233: Lunar Bay
		NORTHERN, // 234: Winter's Edge
		NORTHERN, // 235: Lunar Sea
		NORTHERN, // 236: Kannski Tides
		NORTHERN, // 237: Weissmere
		NORTHERN, // 238: Stoneheart Sea
		NORTHERN, // 239: Shiverwake Expanse
		NORTHERN, // 240: Weiss Melt
		NORTHERN, // 241: Fremensund
		NORTHERN, // 242: Grandroot Bay
		NORTHERN, // 243: V's Belt
		NORTHERN, // 244: Idestia Strait
		NORTHERN, // 245: Lunar Bay
		NORTHERN, // 246: Winter's Edge
		NORTHERN, // 247: Lunar Sea
		NORTHERN, // 248: Everwinter Sea
		NORTHERN, // 249: Kannski Tides
		NORTHERN, // 250: Weissmere
		NORTHERN, // 251: Stoneheart Sea
		NORTHERN, // 252: Shiverwake Expanse
		NORTHERN, // 253: Fremensund
		NORTHERN, // 254: Grandroot Bay
		NORTHERN, // 255: V's Belt
		NORTHERN, // 256: Fremennik Strait
		NORTHERN, // 257: Idestia Strait
		NORTHERN, // 258: Lunar Bay
		NORTHERN, // 259: Lunar Sea
		NORTHERN, // 260: Everwinter Sea
		NORTHERN, // 261: Kannski Tides
		NORTHERN, // 262: Weissmere
		NORTHERN, // 263: Stoneheart Sea
		NORTHERN, // 264: Shiverwake Expanse
		NORTHERN, // 265: Weiss Melt
		UNQUIET, // 266: Turtle Belt
		UNQUIET, // 267: Bay of Elidinis
		UNQUIET, // 268: Tortugan Sea
		UNQUIET, // 269: Pearl Bank
		UNQUIET, // 270: Tortugan Sea
		UNQUIET, // 271: Sea of Shells
		UNQUIET, // 272: Bay of Elidinis
		UNQUIET, // 273: Pearl Bank
		UNQUIET, // 274: The Lonely Sea
		UNQUIET, // 275: Sea of Shells
		UNQUIET, // 276: Bay of Elidinis
		UNQUIET, // 277: Pearl Bank
		UNQUIET, // 278: Red Reef
		UNQUIET, // 279: Bay of Elidinis
		UNQUIET, // 280: Pearl Bank
		UNQUIET, // 281: The Lonely Sea
		SHROUDED, // 282: Aureum Coast
		SHROUDED, // 283: The Everdeep
		SHROUDED, // 284: Southern Expanse
		SHROUDED, // 285: Fortis Bay
		SHROUDED, // 286: Aureum Coast
		SHROUDED, // 287: Wyrm's Waters
		SHROUDED, // 288: The Everdeep
		SHROUDED, // 289: Sapphire Sea
		SHROUDED, // 290: Fortis Bay
		SHROUDED, // 291: Aureum Coast
		SHROUDED, // 292: Wyrm's Waters
		SHROUDED, // 293: The Skullhorde
		SHROUDED, // 294: The Everdeep
		SHROUDED, // 295: Sapphire Sea
		SHROUDED, // 296: Southern Expanse
		SHROUDED, // 297: Fortis Bay
		SHROUDED, // 298: Aureum Coast
		SHROUDED, // 299: Wyrm's Waters
		SHROUDED, // 300: The Everdeep
		SHROUDED, // 301: Sapphire Sea
		SHROUDED, // 302: Southern Expanse
		SHROUDED, // 303: Fortis Bay
		SHROUDED, // 304: Wyrm's Waters
		SHROUDED, // 305: The Skullhorde
		SHROUDED, // 306: Sapphire Sea
		SHROUDED, // 307: Fortis Bay
		SHROUDED, // 308: Aureum Coast
		SHROUDED, // 309: Wyrm's Waters
		SHROUDED, // 310: The Skullhorde
		SHROUDED, // 311: Sea of Souls
		SHROUDED, // 312: The Everdeep
		SHROUDED, // 313: Southern Expanse
		WESTERN_KOUREND, // 314: Crabclaw Bay
		WESTERN_KOUREND, // 315: Litus Lucis
		WESTERN_KOUREND, // 316: Great Sound
		WESTERN_KOUREND, // 317: Crabclaw Bay
		WESTERN_KOUREND, // 318: Crabclaw Bay
		WESTERN_KOUREND, // 319: Crystal Sea
		WESTERN_KOUREND, // 320: Vagabonds Rest
		WESTERN_KOUREND, // 321: Crabclaw Bay
		WESTERN_KOUREND, // 322: Hosidian Sea
		WESTERN_KOUREND, // 323: Pilgrims' Passage
		WESTERN_KOUREND, // 324: Litus Lucis
		WESTERN_KOUREND, // 325: Vagabonds Rest
		WESTERN_KOUREND, // 326: Moonshadow
		WESTERN_KOUREND, // 327: Great Sound
		WESTERN_KOUREND, // 328: Litus Lucis
		WESTERN_KOUREND, // 329: Crystal Sea
		WESTERN_KOUREND, // 330: Moonshadow
		WESTERN_KOUREND, // 331: Great Sound
		WESTERN_KOUREND, // 332: Pilgrims' Passage
		WESTERN_KOUREND, // 333: Litus Lucis
		WESTERN_TIRANNWN, // 334: Piscatoris Sea
		WESTERN_KOUREND, // 335: Moonshadow
		WESTERN_KOUREND, // 336: Great Sound
		WESTERN_KOUREND, // 337: Great Sound
		WESTERN_KOUREND, // 338: Crabclaw Bay
		WESTERN_KOUREND, // 339: Pilgrims' Passage
		WESTERN_KOUREND, // 340: Litus Lucis
		WESTERN_KOUREND, // 341: Crystal Sea
		WESTERN_KOUREND, // 342: Moonshadow
		SUNSET, // 343: Sunset Bay
		SUNSET, // 344: Misty Sea
		SUNSET, // 345: Dusk's Maw
		SUNSET, // 346: Sunset Bay
		SUNSET, // 347: Dusk's Maw
		SUNSET, // 348: Sunset Bay
		SUNSET, // 349: Misty Sea
		SUNSET, // 350: Dusk's Maw
		SUNSET, // 351: Sunset Bay
		SUNSET, // 352: Misty Sea
		SUNSET, // 353: Dusk's Maw
		SUNSET, // 354: Sunset Bay
		SUNSET, // 355: Misty Sea
		SUNSET, // 356: Dusk's Maw
		ARDENT, // 357: Kharidian Sea
	};
}
