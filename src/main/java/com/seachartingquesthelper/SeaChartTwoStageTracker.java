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

import java.util.EnumSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

/**
 * Tracks the two-stage sea chart task types -- {@link SeaChartTaskType#WEATHER} and
 * {@link SeaChartTaskType#CURRENT_DUCK} -- whose relevant map target <em>moves</em> partway
 * through the task, and decides when the plugin should automatically re-target its Shortest Path
 * route from the task's primary {@link SeaChartTask#getLocation()} to its
 * {@link SeaChartTask#getSecondaryLocation()}.
 *
 * <p><b>Weather tasks:</b> the player borrows a portable weather station from a weather troll,
 * hunts down the calm wind spot near the task's primary point using in-game directional clues
 * (not automated here -- the clues are the gameplay), and on success the game prints, verbatim
 * (captured from a real client log):
 *
 * <pre>You find a spot where the winds have dropped. You fill the station's log and your charts
 * with interesting data. You should now return to Meaty Aura Logist where she gave you the
 * weather station.</pre>
 *
 * <p>The NPC name varies per task, so matching keys on the stable template text: the
 * {@code "You should now return to"} phrase plus the {@code "weather station"} suffix. Once seen,
 * the task's live target becomes the secondary point -- where the weather troll who issued the
 * station is.
 *
 * <p><b>Current duck tasks:</b> the player places the current duck at the rippled start point
 * (the primary location) and it drifts along the current to a predetermined end point (the
 * secondary location, already known at compile time -- it's vendored task data, not something
 * the game reveals mid-task the way Weather's return-to-NPC destination is). Per the OSRS Wiki
 * (<a href="https://oldschool.runescape.wiki/w/Current_duck">Current duck</a>, <a
 * href="https://oldschool.runescape.wiki/w/Sea_charting/Current">Sea charting/Current</a>),
 * escorting the duck is optional -- "you do not need to escort the duck, you can go to the end
 * location directly" -- so the moment the destination is worth routing to is release, not
 * arrival: re-targeting on arrival ("Your current duck comes to a stop") would only fire once the
 * player is already there, too late to be useful for routing. The release message, confirmed
 * verbatim from a real client log (with LlemonDuck's Sailing plugin logging {@code
 * CurrentDuckTaskTracker - beginning duck task} at the same moment, confirming this is the
 * task-start event):
 *
 * <pre>You release your current duck and he begins tracking the currents...</pre>
 *
 * <p>is therefore the trigger: it fires right as the player is standing at the primary location
 * about to sail, and the known secondary location is exactly where they need to head next.
 *
 * <p>Which task the signal belongs to is inferred by the caller as the nearest incomplete task
 * of the triggering type (the message only fires while the player is actually out doing that
 * task, so the nearest candidate is the active one).
 *
 * <p><b>Re-targeting is unconditional, not gated on a prior manual selection.</b> An earlier
 * version only re-targeted when the resolved task was already the Shortest Path route's current
 * target -- i.e. only if the player had previously clicked that exact task's panel row. In real
 * play a player just sails up to a duck or troll directly; they essentially never do that click
 * first, so the gate made the feature nearly dead in practice (confirmed live: the release
 * message fired, LlemonDuck's own Sailing plugin logged the same event, but nothing here acted
 * on it). Per direct player confirmation, the correct behaviour is to act exactly like a manual
 * panel click the moment the signal fires -- automatically, regardless of whatever the route was
 * previously pointed at -- just aimed at the secondary location instead of the primary one.
 *
 * <p>Not thread-safe by design: mutated on the client thread only, mirroring the plugin's other
 * state.
 */
@Slf4j
class SeaChartTwoStageTracker
{
	/**
	 * Stable template fragment of the weather success message; the surrounding NPC name varies
	 * per weather-troll task. See the class Javadoc for the full verbatim message.
	 */
	static final String WEATHER_RETURN_PHRASE = "You should now return to";

	/** Second weather key, guarding against unrelated "return to" texts. */
	static final String WEATHER_STATION_PHRASE = "weather station";

	/**
	 * Message printed the moment the current duck is released and begins tracking the currents
	 * -- task start, not arrival. Confirmed verbatim from a real client log. See the class
	 * Javadoc for why release, not arrival, is the correct re-target moment.
	 */
	static final String CURRENT_DUCK_RELEASE_PHRASE = "You release your current duck";

	/**
	 * Incomplete tasks whose stage-two signal has fired: their live target is now the secondary
	 * location.
	 */
	private final Set<SeaChartTask> stageTwo = EnumSet.noneOf(SeaChartTask.class);

	/**
	 * Classifies a chat message as a stage-two trigger, or returns {@code null} for anything
	 * else. Only server-originated message types are honoured so a player typing the phrase in
	 * public chat can't spoof a re-target.
	 */
	static SeaChartTaskType triggerType(ChatMessage event)
	{
		if (event == null || event.getMessage() == null || !isServerMessage(event.getType()))
		{
			return null;
		}

		final String message = Text.removeTags(event.getMessage());
		if (message.contains(WEATHER_RETURN_PHRASE) && message.contains(WEATHER_STATION_PHRASE))
		{
			log.debug("Chat message matched the Weather stage-two trigger phrase: \"{}\"", message);
			return SeaChartTaskType.WEATHER;
		}
		if (message.contains(CURRENT_DUCK_RELEASE_PHRASE))
		{
			log.debug("Chat message matched the Current duck stage-two trigger phrase: \"{}\"", message);
			return SeaChartTaskType.CURRENT_DUCK;
		}
		return null;
	}

	private static boolean isServerMessage(ChatMessageType type)
	{
		return type == ChatMessageType.GAMEMESSAGE
			|| type == ChatMessageType.SPAM
			|| type == ChatMessageType.MESBOX;
	}

	/**
	 * Processes a possible stage-two trigger.
	 *
	 * <p>If {@code event} is a stage-two signal matching {@code activeTask}'s type, the task is
	 * marked stage-two -- {@link #effectiveLocation} and therefore the panel's distances now
	 * point at the secondary location -- and this returns that secondary location so the caller
	 * can re-target the Shortest Path route with it, unconditionally, exactly as if the player
	 * had just clicked this task's panel row. There is deliberately no gate requiring a prior
	 * manual selection of this task (see the class Javadoc for why that gate was removed).
	 *
	 * @param event      the chat message just received
	 * @param activeTask the nearest incomplete task of the trigger's type -- the caller's best
	 *                   identification of the task the player is actually doing; may be null
	 * @return the secondary location to re-target the route to, or {@code null} if this message
	 * wasn't a trigger, didn't resolve to a task, or that task has no secondary location
	 */
	WorldPoint handleTrigger(ChatMessage event, SeaChartTask activeTask)
	{
		final SeaChartTaskType type = triggerType(event);
		if (type == null)
		{
			// Not a trigger message at all -- the overwhelmingly common case for ordinary chat
			// traffic, so this is intentionally silent rather than logged at debug on every line.
			return null;
		}

		if (activeTask == null)
		{
			log.debug("{} stage-two trigger fired but no nearby incomplete {} task could be"
				+ " resolved -- skipping", type, type);
			return null;
		}

		if (activeTask.getType() != type)
		{
			log.debug("{} stage-two trigger fired but the resolved active task {} is a {} task"
				+ " -- type mismatch, skipping", type, activeTask, activeTask.getType());
			return null;
		}

		final WorldPoint secondary = activeTask.getSecondaryLocation();
		if (secondary == null)
		{
			log.debug("{} stage-two trigger resolved to task {} but it has no secondary"
				+ " location -- skipping (data gap)", type, activeTask);
			return null;
		}

		final boolean alreadyStageTwo = stageTwo.contains(activeTask);
		stageTwo.add(activeTask);
		log.debug("{} stage-two trigger resolved to task {} (already stage-two: {}) --"
			+ " re-targeting route to secondary location {}, unconditionally (same as a manual"
			+ " panel click)", type, activeTask, alreadyStageTwo, secondary);
		return secondary;
	}

	/**
	 * The location this task should currently be measured against and routed to: the secondary
	 * location once its stage-two signal has fired, the primary location otherwise.
	 */
	WorldPoint effectiveLocation(SeaChartTask task)
	{
		if (stageTwo.contains(task) && task.getSecondaryLocation() != null)
		{
			return task.getSecondaryLocation();
		}
		return task.getLocation();
	}

	boolean isStageTwo(SeaChartTask task)
	{
		return stageTwo.contains(task);
	}

	/** Completing (or re-scanning as complete) a task retires its stage-two state. */
	void onTaskCompleted(SeaChartTask task)
	{
		stageTwo.remove(task);
	}

	void reset()
	{
		stageTwo.clear();
	}
}
