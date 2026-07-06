package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.runelite.api.ChatMessageType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the chat-message-triggered stage-two re-targeting rules in
 * {@link SeaChartTwoStageTracker}.
 *
 * <p>Weather is three-phase: collecting the station moves the target from primary (troll) to
 * secondary (search area); the return-success message moves it back to primary. Current duck is
 * two-phase: release moves the target from primary to secondary and it stays there until the task
 * completes -- there's no return leg.
 *
 * <p>Nothing else may trigger a re-target: wrong message, wrong chat type, spoofable player chat,
 * or a type mismatch between the signal and the resolved active task.
 *
 * <p>Current duck fires on <em>release</em> ("You release your current duck..."), not on
 * arrival ("...comes to a stop"): the destination is static, known task data, so it's useful to
 * route to the moment the player lets the duck go and needs to start sailing -- waiting for
 * arrival would fire only once the player is already there.
 *
 * <p><b>Re-targeting is unconditional</b> -- confirmed via a live bug report: an earlier version
 * gated the re-target on the resolved task already being the Shortest Path route's current
 * target (i.e. the player must have previously clicked that exact task's panel row). In real
 * play a player just sails up to a duck or troll directly without ever pre-clicking its row, so
 * that gate made the feature almost never fire. {@link SeaChartTwoStageTracker#handleTrigger}
 * therefore takes no "current route target" parameter at all any more -- it always returns the
 * relevant location once a task is resolved, and the caller re-targets exactly as it would for
 * a manual click.
 */
public class SeaChartTwoStageTrackerTest
{
	/**
	 * Verbatim message printed when the weather troll hands over the portable station,
	 * captured from a real play session's client log.
	 */
	private static final String WEATHER_COLLECTED_MESSAGE = "The troll hands you a portable weather station.";

	/**
	 * Verbatim Weather stage-two success (return) message, captured from a real play session's
	 * client log. The NPC name ("Meaty Aura Logist") varies per weather-troll task.
	 */
	private static final String WEATHER_RETURN_MESSAGE =
		"You find a spot where the winds have dropped. You fill the station's log and your"
			+ " charts with interesting data. You should now return to Meaty Aura Logist where"
			+ " she gave you the weather station.";

	/**
	 * Verbatim Current duck release message, captured from a real play session's client log
	 * (alongside LlemonDuck's Sailing plugin logging "beginning duck task" at the same moment,
	 * confirming this is the task-start event, not the arrival/stop event).
	 */
	private static final String DUCK_MESSAGE = "You release your current duck and he begins tracking the currents...";

	private SeaChartTask weatherTask;
	private SeaChartTask duckTask;
	private SeaChartTwoStageTracker tracker;

	@Before
	public void setUp()
	{
		weatherTask = firstTaskOfType(SeaChartTaskType.WEATHER);
		duckTask = firstTaskOfType(SeaChartTaskType.CURRENT_DUCK);
		tracker = new SeaChartTwoStageTracker();

		// The feature is meaningless without vendored secondary-location data; guard it here so
		// a future data regression fails loudly.
		assertNotNull("Weather tasks must carry a secondary location", weatherTask.getSecondaryLocation());
		assertNotNull("Current duck tasks must carry a secondary location", duckTask.getSecondaryLocation());
	}

	@Test
	public void weatherCollectedMessageResolvesToSecondary()
	{
		WorldPoint target = tracker.handleTrigger(gameMessage(WEATHER_COLLECTED_MESSAGE), weatherTask);

		assertEquals(weatherTask.getSecondaryLocation(), target);
		assertFalse(weatherTask.getLocation().equals(target));
		assertTrue(tracker.isStageTwo(weatherTask));
	}

	@Test
	public void weatherReturnMessageResolvesBackToPrimaryNotSecondary()
	{
		tracker.handleTrigger(gameMessage(WEATHER_COLLECTED_MESSAGE), weatherTask);
		assertTrue(tracker.isStageTwo(weatherTask));

		WorldPoint target = tracker.handleTrigger(gameMessage(WEATHER_RETURN_MESSAGE), weatherTask);

		assertEquals(weatherTask.getLocation(), target);
		assertFalse(weatherTask.getSecondaryLocation().equals(target));
		assertFalse(tracker.isStageTwo(weatherTask));
	}

	@Test
	public void weatherReturnMessageResolvesToPrimaryEvenWithoutAPriorCollectedSignal()
	{
		// Re-targeting is unconditional -- the return message alone must still resolve to the
		// troll's location, matching how a player could join the plugin mid-task.
		WorldPoint target = tracker.handleTrigger(gameMessage(WEATHER_RETURN_MESSAGE), weatherTask);

		assertEquals(weatherTask.getLocation(), target);
		assertFalse(tracker.isStageTwo(weatherTask));
	}

	@Test
	public void weatherMessageMatchesRegardlessOfWhichNpcIssuedTheStation()
	{
		// The NPC name is per-task; the match must key on the stable template text only.
		String otherNpc = WEATHER_RETURN_MESSAGE.replace("Meaty Aura Logist", "Gusty Cloud Watcher");

		assertEquals(weatherTask.getLocation(), tracker.handleTrigger(gameMessage(otherNpc), weatherTask));
	}

	@Test
	public void weatherMessageWithColourTagsStillMatches()
	{
		String tagged = "<col=ef1020>" + WEATHER_RETURN_MESSAGE + "</col>";

		assertEquals(weatherTask.getLocation(), tracker.handleTrigger(gameMessage(tagged), weatherTask));
	}

	@Test
	public void duckReleaseMessageResolvesToDuckEndPointImmediately()
	{
		WorldPoint target = tracker.handleTrigger(gameMessage(DUCK_MESSAGE), duckTask);

		assertEquals(duckTask.getSecondaryLocation(), target);
		assertFalse(duckTask.getLocation().equals(target));
		assertTrue(tracker.isStageTwo(duckTask));
	}

	@Test
	public void duckArrivalMessageIsNotTheTrigger()
	{
		// Regression guard for the original (wrong) implementation: arrival is too late to be
		// useful for routing purposes, since by then the player is already at the destination.
		String arrivalMessage = "Your current duck comes to a stop.";

		assertNull(SeaChartTwoStageTracker.triggerType(gameMessage(arrivalMessage)));
		assertNull(tracker.handleTrigger(gameMessage(arrivalMessage), duckTask));
		assertFalse(tracker.isStageTwo(duckTask));
	}

	@Test
	public void releaseMessageResolvesUnconditionallyEvenWithoutAnyPriorPanelClick()
	{
		// The real-world bug: the player never clicked this duck task's panel row before sailing
		// up and releasing the duck in-game -- there was never a "current route target" at all.
		// handleTrigger no longer needs or accepts one; the resolved task alone is enough.
		WorldPoint target = tracker.handleTrigger(gameMessage(DUCK_MESSAGE), duckTask);

		assertEquals(duckTask.getSecondaryLocation(), target);
		assertTrue(tracker.isStageTwo(duckTask));
	}

	@Test
	public void noResolvedActiveTaskMeansNoRetarget()
	{
		assertNull(tracker.handleTrigger(gameMessage(WEATHER_COLLECTED_MESSAGE), null));
	}

	@Test
	public void playerTypedChatCannotSpoofARetarget()
	{
		ChatMessage spoofed = new ChatMessage(null, ChatMessageType.PUBLICCHAT, "SomePlayer",
			WEATHER_COLLECTED_MESSAGE, null, 0);

		assertNull(SeaChartTwoStageTracker.triggerType(spoofed));
		assertNull(tracker.handleTrigger(spoofed, weatherTask));
		assertFalse(tracker.isStageTwo(weatherTask));
	}

	@Test
	public void unrelatedGameMessagesDoNothing()
	{
		assertNull(tracker.handleTrigger(gameMessage("You should now return to the bank."), weatherTask));
		assertNull(tracker.handleTrigger(gameMessage("Welcome to Old School RuneScape."), weatherTask));
		assertFalse(tracker.isStageTwo(weatherTask));
	}

	@Test
	public void triggerTypeMustMatchTheActiveTasksType()
	{
		// A weather signal while the nearest candidate of that type resolution handed us a duck
		// task (or vice versa) must be ignored -- a category mix-up should never re-target.
		assertNull(tracker.handleTrigger(gameMessage(WEATHER_COLLECTED_MESSAGE), duckTask));
		assertNull(tracker.handleTrigger(gameMessage(DUCK_MESSAGE), weatherTask));
	}

	@Test
	public void effectiveLocationFollowsStageAndCompletionResetsIt()
	{
		assertEquals(weatherTask.getLocation(), tracker.effectiveLocation(weatherTask));

		tracker.handleTrigger(gameMessage(WEATHER_COLLECTED_MESSAGE), weatherTask);
		assertEquals(weatherTask.getSecondaryLocation(), tracker.effectiveLocation(weatherTask));

		tracker.onTaskCompleted(weatherTask);
		assertFalse(tracker.isStageTwo(weatherTask));
		assertEquals(weatherTask.getLocation(), tracker.effectiveLocation(weatherTask));
	}

	@Test
	public void effectiveLocationReturnsToPrimaryAfterTheReturnMessageWithoutWaitingForCompletion()
	{
		tracker.handleTrigger(gameMessage(WEATHER_COLLECTED_MESSAGE), weatherTask);
		assertEquals(weatherTask.getSecondaryLocation(), tracker.effectiveLocation(weatherTask));

		tracker.handleTrigger(gameMessage(WEATHER_RETURN_MESSAGE), weatherTask);
		assertEquals(weatherTask.getLocation(), tracker.effectiveLocation(weatherTask));
	}

	@Test
	public void triggerTypeClassifiesAllThreeSignals()
	{
		assertEquals(SeaChartTaskType.WEATHER, SeaChartTwoStageTracker.triggerType(gameMessage(WEATHER_COLLECTED_MESSAGE)));
		assertEquals(SeaChartTaskType.WEATHER, SeaChartTwoStageTracker.triggerType(gameMessage(WEATHER_RETURN_MESSAGE)));
		assertEquals(SeaChartTaskType.CURRENT_DUCK, SeaChartTwoStageTracker.triggerType(gameMessage(DUCK_MESSAGE)));
		assertNull(SeaChartTwoStageTracker.triggerType(gameMessage("Your bird's nest falls to the ground.")));
		assertNull(SeaChartTwoStageTracker.triggerType(null));
	}

	private static ChatMessage gameMessage(String message)
	{
		return new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", message, null, 0);
	}

	/** Picked dynamically from the real enum so the tests survive future data corrections. */
	private static SeaChartTask firstTaskOfType(SeaChartTaskType type)
	{
		for (SeaChartTask task : SeaChartTask.values())
		{
			if (task.getType() == type)
			{
				return task;
			}
		}
		throw new AssertionError("No task of type " + type);
	}
}
