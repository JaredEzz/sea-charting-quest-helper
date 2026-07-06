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
import net.runelite.api.events.ChatMessage;
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
	name = "Braindead Sea Charting",
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

	// Two-stage (Weather / Current duck) tracking; client thread only.
	private final SeaChartTwoStageTracker twoStageTracker = new SeaChartTwoStageTracker();
	/** The task whose location was last sent to Shortest Path, i.e. the active route target. */
	private SeaChartTask routeTask;

	@Override
	protected void startUp()
	{
		panel = new SeaChartingQuestHelperPanel();
		panel.setCallbacks(this::onPanelToggle, this::onTaskClicked);
		panel.setGearHideCallback(this::onGearHideToggle);
		panel.initOptions(config.hideNotYetReachable(), config.showSeaCompletion(),
			config.smartSort(), config.showNearestPort());
		panel.initGearFilters(initiallyHiddenGearRequirements());

		final Map<SeaChartTaskType, BufferedImage> icons = new EnumMap<>(SeaChartTaskType.class);
		for (SeaChartTaskType type : SeaChartTaskType.values())
		{
			icons.put(type, itemManager.getImage(type.getIconItemId()));
		}
		panel.setIcons(icons);

		final BufferedImage icon = new SkillIconManager().getSkillImage(Skill.SAILING, false);
		navButton = NavigationButton.builder()
			.tooltip("Braindead Sea Charting")
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
		twoStageTracker.reset();
		routeTask = null;
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
		completed.forEach(twoStageTracker::onTaskCompleted);
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
			twoStageTracker.onTaskCompleted(task);
			if (task == routeTask)
			{
				routeTask = null;
			}
		}
		else
		{
			completed.remove(task);
		}
	}

	/**
	 * Watches for the two-stage "stage 2 is now relevant" signals and re-targets the active
	 * Shortest Path route to the task's secondary location when they fire:
	 *
	 * <ul>
	 * <li><b>Weather</b>: the "You find a spot where the winds have dropped. ... You should now
	 * return to &lt;NPC&gt; where she gave you the weather station." success message -- the
	 * relevant target becomes the weather troll who issued the station.</li>
	 * <li><b>Current duck</b>: the "You release your current duck and he begins tracking the
	 * currents..." message, fired at task <em>start</em> (confirmed via a real client log) -- the
	 * duck's end point is static, known task data, so re-targeting to it is useful the moment the
	 * player releases the duck and needs to start sailing, not once they've already arrived.</li>
	 * </ul>
	 *
	 * <p>Re-targeting is <b>unconditional</b> -- it fires the moment the signal resolves to a
	 * task, exactly like a manual panel click on that task's row, regardless of whatever the
	 * route was previously pointed at (see {@link SeaChartTwoStageTracker} for why a prior
	 * "must already be the clicked route target" gate was removed -- it made this feature almost
	 * never fire in real play, since players don't pre-click a task before sailing up to it).
	 *
	 * <p>See {@link SeaChartTwoStageTracker} for the exact matching rules and sourcing. Fires on
	 * the client thread (event bus), so state access here is safe.
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		final SeaChartTaskType triggerType = SeaChartTwoStageTracker.triggerType(event);
		if (triggerType == null)
		{
			return;
		}

		final SeaChartTask activeTask = nearestIncompleteTaskOfType(triggerType);
		log.debug("Stage-two {} trigger message received; resolved active task = {}, current"
			+ " routeTask = {} (routeTask is informational only -- not required to match)",
			triggerType, activeTask, routeTask);

		final WorldPoint target = twoStageTracker.handleTrigger(event, activeTask);
		if (target == null)
		{
			log.debug("Stage-two {} trigger did not result in a re-target (see"
				+ " SeaChartTwoStageTracker's debug line above for the reason)", triggerType);
			return;
		}

		routeTask = activeTask;
		postRouteTarget(target);
		log.debug("Stage-two {} trigger auto re-targeted the route to task {}'s new location {}"
			+ " (see SeaChartTwoStageTracker's debug line above for which leg/direction), with no"
			+ " manual click required", triggerType, activeTask, target);
	}

	/**
	 * The nearest incomplete task of the given type -- our identification of which task a
	 * stage-two chat signal belongs to, since the signal only fires while the player is out doing
	 * that task. Falls back to the current route target (if it matches the type) when the
	 * player's overworld position can't be resolved this tick.
	 */
	private SeaChartTask nearestIncompleteTaskOfType(SeaChartTaskType type)
	{
		final WorldPoint playerLocation = BoatLocationResolver.resolveEffectivePlayerLocation(client);
		if (playerLocation == null)
		{
			log.debug("Can't resolve player location this tick -- falling back to routeTask for"
				+ " {} identification", type);
			return routeTask != null && routeTask.getType() == type && !completed.contains(routeTask)
				? routeTask : null;
		}

		SeaChartTask nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task.getType() != type || completed.contains(task))
			{
				continue;
			}
			final int distance = playerLocation.distanceTo2D(task.getLocation());
			if (distance < nearestDistance)
			{
				nearest = task;
				nearestDistance = distance;
			}
		}
		log.debug("Nearest incomplete {} task to player location {} is {} (distance {})",
			type, playerLocation, nearest, nearestDistance == Integer.MAX_VALUE ? "n/a" : nearestDistance);
		return nearest;
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
			// Two-stage tasks (Weather / Current duck) measure to their secondary location once
			// their stage-two signal has fired, keeping the shown distance consistent with where
			// the route actually points.
			int distance = playerLocation.distanceTo2D(twoStageTracker.effectiveLocation(task));
			boolean eligible = SeaChartRequirements.meetsRequirement(client, task);
			rows.add(new SeaChartTaskRow(task, distance, eligible, twoStageTracker.isStageTwo(task)));
		}

		// Per-sea completion counts feed both the panel's "(x/y)" markers and the smart sort's
		// remaining-in-sea weighting; `completed` is kept live by onVarbitChanged, so these
		// advance the moment a task is charted.
		final Map<SeaChartRegion, SeaChartRegionProgress> regionProgress = SeaChartRegionProgress.compute(completed);
		if (config.smartSort())
		{
			SeaChartTaskSorter.sort(rows, regionProgress);
		}
		else
		{
			rows.sort(Comparator.comparingInt(SeaChartTaskRow::getDistance));
		}

		final int overallComplete = completed.size();
		final int overallTotal = SeaChartTask.values().length;
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.setRows(rows, regionProgress, overallComplete, overallTotal);
			}
		});
	}

	/** Persists a panel checkbox toggle back into this plugin's config group. */
	private void onPanelToggle(String configKey, boolean value)
	{
		configManager.setConfiguration(SeaChartingQuestHelperConfig.GROUP, configKey, value);
	}

	private Set<SeaChartGearRequirement> initiallyHiddenGearRequirements()
	{
		Set<SeaChartGearRequirement> hidden = EnumSet.noneOf(SeaChartGearRequirement.class);
		if (config.hideNeedsAdamantKeelOrHelm())
		{
			hidden.add(SeaChartGearRequirement.ADAMANT_KEEL_OR_HELM);
		}
		if (config.hideNeedsEternalBrazier())
		{
			hidden.add(SeaChartGearRequirement.ETERNAL_BRAZIER);
		}
		if (config.hideNeedsInoculationStation())
		{
			hidden.add(SeaChartGearRequirement.INOCULATION_STATION);
		}
		return hidden;
	}

	private void onGearHideToggle(SeaChartGearRequirement requirement, boolean hide)
	{
		String keyName;
		switch (requirement)
		{
			case ADAMANT_KEEL_OR_HELM:
				keyName = SeaChartingQuestHelperConfig.KEY_HIDE_NEEDS_ADAMANT_KEEL_OR_HELM;
				break;
			case ETERNAL_BRAZIER:
				keyName = SeaChartingQuestHelperConfig.KEY_HIDE_NEEDS_ETERNAL_BRAZIER;
				break;
			case INOCULATION_STATION:
				keyName = SeaChartingQuestHelperConfig.KEY_HIDE_NEEDS_INOCULATION_STATION;
				break;
			default:
				throw new IllegalArgumentException("Unhandled gear requirement: " + requirement);
		}
		configManager.setConfiguration(SeaChartingQuestHelperConfig.GROUP, keyName, hide);
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
			// Remember which task the route points at -- informational only (e.g. debug logging,
			// and clearing on completion in onVarbitChanged); onChatMessage's stage-two re-target
			// no longer requires this to match. Clicking a task already in stage two routes
			// straight to its secondary location.
			routeTask = task;
			postRouteTarget(twoStageTracker.effectiveLocation(task));
		});
	}

	/** Posts a route target to Shortest Path. Client thread only. */
	private void postRouteTarget(WorldPoint target)
	{
		if (!config.shortestPathIntegration())
		{
			return;
		}

		Map<String, Object> data = new HashMap<>();
		data.put(SHORTEST_PATH_TARGET_KEY, target);
		eventBus.post(new PluginMessage(SHORTEST_PATH_NAMESPACE, SHORTEST_PATH_PATH_ACTION, data));
	}
}
