package com.seachartingquesthelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * The six categories of Sailing chart task. Labels and representative icon items mirror the
 * public "Sailing" plugin by LlemonDuck (https://github.com/LlemonDuck/sailing, BSD-2-Clause) --
 * these item ids are Jagex's public gameval constants, not that plugin's creative expression.
 */
@Getter
@RequiredArgsConstructor
public enum SeaChartTaskType
{
	GENERIC("Oddity", ItemID.SAILING_LOG_INITIAL),
	SPYGLASS("Spyglass", ItemID.SAILING_CHARTING_SPYGLASS),
	DRINK_CRATE("Sealed crate", ItemID.SAILING_CHARTING_CROWBAR),
	CURRENT_DUCK("Current duck", ItemID.SAILING_CHARTING_CURRENT_DUCK),
	MERMAID_GUIDE("Diving", ItemID.HUNDRED_PIRATE_DIVING_HELMET),
	WEATHER("Weather", ItemID.SAILING_CHARTING_WEATHER_STATION_EMPTY),
	;

	private final String label;
	private final int iconItemId;
}
