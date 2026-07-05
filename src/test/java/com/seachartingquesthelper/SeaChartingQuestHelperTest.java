package com.seachartingquesthelper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SeaChartingQuestHelperTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SeaChartingQuestHelperPlugin.class);
		RuneLite.main(args);
	}
}
