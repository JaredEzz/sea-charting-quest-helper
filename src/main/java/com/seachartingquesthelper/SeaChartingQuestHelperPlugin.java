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

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Sea Charting Quest Helper",
	description = "Quest Helper-style side panel for Sailing chart tasks: nearest incomplete task"
		+ " first, auto-advancing as you complete each one",
	tags = {"sailing", "charting", "chart", "sea", "quest", "helper", "horizon", "lure"}
)
@Slf4j
public class SeaChartingQuestHelperPlugin extends Plugin
{
	private static final String SHORTEST_PATH_NAMESPACE = "shortestpath";
	private static final String SHORTEST_PATH_PATH_ACTION = "path";
	private static final String SHORTEST_PATH_TARGET_KEY = "target";

	@Inject
	private Client client;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ItemManager itemManager;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ConfigManager configManager;
	@Inject
	private EventBus eventBus;
	@Inject
	private SeaChartingQuestHelperConfig config;

	@Provides
	SeaChartingQuestHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SeaChartingQuestHelperConfig.class);
	}

	private SeaChartingQuestHelperPanel panel;
	private NavigationButton navButton;

	// Mutated on the client thread only.
	private final Set<SeaChartTask> completed = EnumSet.noneOf(SeaChartTask.class);
	private boolean scanned = false;

	@Override
	protected void startUp()
	{
		panel = new SeaChartingQuestHelperPanel();
		panel.setCallbacks(this::onHideToggle, this::onTaskClicked);
		panel.initHideNotReachable(config.hideNotYetReachable());

		final Map<SeaChartTaskType, BufferedImage> icons = new EnumMap<>(SeaChartTaskType.class);
		for (SeaChartTaskType type : SeaChartTaskType.values())
		{
			icons.put(type, itemManager.getImage(type.getIconItemId()));
		}
		panel.setIcons(icons);

		final BufferedImage icon = new SkillIconManager().getSkillImage(Skill.SAILING, false);
		navButton = NavigationButton.builder()
			.tooltip("Sea Charting Quest Helper")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		completed.clear();
		scanned = false;
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invoke(this::scanCompletedTasks);
		}
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		panel = null;
		completed.clear();
		scanned = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invoke(this::scanCompletedTasks);
		}
	}

	/**
	 * One-time full scan of all 358 completion varbits, run at startup/login. After this,
	 * {@link #onVarbitChanged} keeps {@link #completed} in sync incrementally.
	 */
	private void scanCompletedTasks()
	{
		completed.clear();
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task.isComplete(client))
			{
				completed.add(task);
			}
		}
		scanned = true;
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		SeaChartTask task = SeaChartTask.byVarbit(event.getVarbitId());
		if (task == null)
		{
			return;
		}

		if (task.isComplete(client))
		{
			completed.add(task);
		}
		else
		{
			completed.remove(task);
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (panel == null || !panel.isShowing() || !scanned)
		{
			return;
		}

		if (client.getLocalPlayer() == null)
		{
			return;
		}

		final WorldPoint playerLocation = BoatLocationResolver.resolveEffectivePlayerLocation(client);
		if (playerLocation == null)
		{
			// Couldn't resolve a real overworld position this tick (e.g. aboard a boat whose
			// owning WorldEntity we failed to find) -- skip this tick rather than compute
			// nonsense distances against a stale/local coordinate. The panel just keeps
			// showing its last-known-good rows until this resolves.
			log.debug("Skipping tile-distance update this tick -- no resolvable overworld player location");
			return;
		}

		List<SeaChartTaskRow> rows = new ArrayList<>(SeaChartTask.values().length - completed.size());
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (completed.contains(task))
			{
				continue;
			}
			int distance = playerLocation.distanceTo2D(task.getLocation());
			boolean eligible = SeaChartRequirements.meetsRequirement(client, task);
			rows.add(new SeaChartTaskRow(task, distance, eligible));
		}
		rows.sort(Comparator.comparingInt(SeaChartTaskRow::getDistance));

		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.setRows(rows);
			}
		});
	}

	private void onHideToggle(boolean hide)
	{
		configManager.setConfiguration(SeaChartingQuestHelperConfig.GROUP, "hideNotYetReachable", hide);
	}

	/**
	 * Sends the clicked task's location to the Shortest Path plugin (Skretzo) via its documented
	 * {@link PluginMessage} API, if installed and enabled -- no compile-time dependency needed.
	 * If Shortest Path isn't running, this message is simply never picked up by anyone.
	 */
	private void onTaskClicked(SeaChartTask task)
	{
		if (!config.shortestPathIntegration())
		{
			return;
		}

		clientThread.invoke(() ->
		{
			Map<String, Object> data = new HashMap<>();
			data.put(SHORTEST_PATH_TARGET_KEY, task.getLocation());
			eventBus.post(new PluginMessage(SHORTEST_PATH_NAMESPACE, SHORTEST_PATH_PATH_ACTION, data));
		});
	}
}
