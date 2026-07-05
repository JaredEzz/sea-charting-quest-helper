package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the chat-message-triggered stage-two re-targeting rules in
 * {@link SeaChartTwoStageTracker}: the verbatim Weather success message and the wiki-verified
 * Current duck stop message must re-target the active route to the task's <em>secondary</em>
 * location -- and nothing else may (wrong message, wrong chat type, spoofable player chat, a
 * route pointing at an unrelated task).
 */
public class SeaChartTwoStageTrackerTest
{
	/**
	 * Verbatim Weather stage-two success message, captured from a real play session's client
	 * log. The NPC name ("Meaty Aura Logist") varies per weather-troll task.
	 */
	private static final String WEATHER_MESSAGE =
		"You find a spot where the winds have dropped. You fill the station's log and your"
			+ " charts with interesting data. You should now return to Meaty Aura Logist where"
			+ " she gave you the weather station.";

	/** Wiki-verified Current duck arrival message. */
	private static final String DUCK_MESSAGE = "Your current duck comes to a stop.";

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
	public void weatherSuccessMessageRetargetsRouteToSecondaryNotPrimary()
	{
		RecordingRetarget retarget = new RecordingRetarget();

		boolean retargeted = tracker.handleTrigger(
			gameMessage(WEATHER_MESSAGE), weatherTask, weatherTask, retarget);

		assertTrue(retargeted);
		assertEquals(1, retarget.targets.size());
		assertEquals(weatherTask.getSecondaryLocation(), retarget.targets.get(0));
		assertFalse(weatherTask.getLocation().equals(retarget.targets.get(0)));
		assertTrue(tracker.isStageTwo(weatherTask));
	}

	@Test
	public void weatherMessageMatchesRegardlessOfWhichNpcIssuedTheStation()
	{
		// The NPC name is per-task; the match must key on the stable template text only.
		String otherNpc = WEATHER_MESSAGE.replace("Meaty Aura Logist", "Gusty Cloud Watcher");
		RecordingRetarget retarget = new RecordingRetarget();

		assertTrue(tracker.handleTrigger(gameMessage(otherNpc), weatherTask, weatherTask, retarget));
		assertEquals(weatherTask.getSecondaryLocation(), retarget.targets.get(0));
	}

	@Test
	public void weatherMessageWithColourTagsStillMatches()
	{
		String tagged = "<col=ef1020>" + WEATHER_MESSAGE + "</col>";
		RecordingRetarget retarget = new RecordingRetarget();

		assertTrue(tracker.handleTrigger(gameMessage(tagged), weatherTask, weatherTask, retarget));
		assertEquals(weatherTask.getSecondaryLocation(), retarget.targets.get(0));
	}

	@Test
	public void duckStopMessageRetargetsRouteToDuckEndPoint()
	{
		RecordingRetarget retarget = new RecordingRetarget();

		boolean retargeted = tracker.handleTrigger(
			gameMessage(DUCK_MESSAGE), duckTask, duckTask, retarget);

		assertTrue(retargeted);
		assertEquals(duckTask.getSecondaryLocation(), retarget.targets.get(0));
		assertFalse(duckTask.getLocation().equals(retarget.targets.get(0)));
		assertTrue(tracker.isStageTwo(duckTask));
	}

	@Test
	public void routePointedAtUnrelatedTaskIsNeverSilentlyRedirected()
	{
		// The player routed to some other task; the weather signal must still flip the weather
		// task's own effective location (panel consistency) but must NOT touch the route.
		SeaChartTask unrelatedRouteTask = firstTaskOfType(SeaChartTaskType.SPYGLASS);
		RecordingRetarget retarget = new RecordingRetarget();

		boolean retargeted = tracker.handleTrigger(
			gameMessage(WEATHER_MESSAGE), weatherTask, unrelatedRouteTask, retarget);

		assertFalse(retargeted);
		assertTrue(retarget.targets.isEmpty());
		assertTrue(tracker.isStageTwo(weatherTask));
		assertFalse(tracker.isStageTwo(unrelatedRouteTask));
	}

	@Test
	public void noActiveRouteMarksStageTwoWithoutRetargeting()
	{
		RecordingRetarget retarget = new RecordingRetarget();

		assertFalse(tracker.handleTrigger(gameMessage(WEATHER_MESSAGE), weatherTask, null, retarget));
		assertTrue(retarget.targets.isEmpty());
		assertTrue(tracker.isStageTwo(weatherTask));
	}

	@Test
	public void playerTypedChatCannotSpoofARetarget()
	{
		ChatMessage spoofed = new ChatMessage(null, ChatMessageType.PUBLICCHAT, "SomePlayer",
			WEATHER_MESSAGE, null, 0);
		RecordingRetarget retarget = new RecordingRetarget();

		assertNull(SeaChartTwoStageTracker.triggerType(spoofed));
		assertFalse(tracker.handleTrigger(spoofed, weatherTask, weatherTask, retarget));
		assertTrue(retarget.targets.isEmpty());
		assertFalse(tracker.isStageTwo(weatherTask));
	}

	@Test
	public void unrelatedGameMessagesDoNothing()
	{
		RecordingRetarget retarget = new RecordingRetarget();

		assertFalse(tracker.handleTrigger(gameMessage("You should now return to the bank."),
			weatherTask, weatherTask, retarget));
		assertFalse(tracker.handleTrigger(gameMessage("Welcome to Old School RuneScape."),
			weatherTask, weatherTask, retarget));
		assertTrue(retarget.targets.isEmpty());
		assertFalse(tracker.isStageTwo(weatherTask));
	}

	@Test
	public void triggerTypeMustMatchTheActiveTasksType()
	{
		// A weather signal while the nearest candidate of that type resolution handed us a duck
		// task (or vice versa) must be ignored -- a category mix-up should never re-target.
		RecordingRetarget retarget = new RecordingRetarget();

		assertFalse(tracker.handleTrigger(gameMessage(WEATHER_MESSAGE), duckTask, duckTask, retarget));
		assertFalse(tracker.handleTrigger(gameMessage(DUCK_MESSAGE), weatherTask, weatherTask, retarget));
		assertTrue(retarget.targets.isEmpty());
	}

	@Test
	public void effectiveLocationFollowsStageAndCompletionResetsIt()
	{
		assertEquals(weatherTask.getLocation(), tracker.effectiveLocation(weatherTask));

		tracker.handleTrigger(gameMessage(WEATHER_MESSAGE), weatherTask, null, new RecordingRetarget());
		assertEquals(weatherTask.getSecondaryLocation(), tracker.effectiveLocation(weatherTask));

		tracker.onTaskCompleted(weatherTask);
		assertFalse(tracker.isStageTwo(weatherTask));
		assertEquals(weatherTask.getLocation(), tracker.effectiveLocation(weatherTask));
	}

	@Test
	public void triggerTypeClassifiesBothSignals()
	{
		assertEquals(SeaChartTaskType.WEATHER, SeaChartTwoStageTracker.triggerType(gameMessage(WEATHER_MESSAGE)));
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

	private static final class RecordingRetarget implements Consumer<WorldPoint>
	{
		private final List<WorldPoint> targets = new ArrayList<>();

		@Override
		public void accept(WorldPoint target)
		{
			targets.add(target);
		}
	}
}
