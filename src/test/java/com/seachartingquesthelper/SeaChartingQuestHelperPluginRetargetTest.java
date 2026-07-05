package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * End-to-end coverage of the automatic stage-two route re-target through the real plugin event
 * handlers: click a Weather task (route -> primary location), receive the verbatim in-game
 * success message as a {@link ChatMessage}, and verify the plugin posts a second Shortest Path
 * {@link PluginMessage} carrying the task's <em>secondary</em> location -- with no manual
 * re-click. Collaborators are mocked; the decision logic under test is real.
 */
public class SeaChartingQuestHelperPluginRetargetTest
{
	private static final String WEATHER_MESSAGE =
		"You find a spot where the winds have dropped. You fill the station's log and your"
			+ " charts with interesting data. You should now return to Meaty Aura Logist where"
			+ " she gave you the weather station.";

	private static final String DUCK_MESSAGE = "Your current duck comes to a stop.";

	private SeaChartingQuestHelperPlugin plugin;
	private Client client;
	private EventBus eventBus;
	private Player player;

	@Before
	public void setUp() throws Exception
	{
		plugin = new SeaChartingQuestHelperPlugin();
		client = mock(Client.class);
		eventBus = mock(EventBus.class);

		ClientThread clientThread = mock(ClientThread.class);
		doAnswer(invocation ->
		{
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(clientThread).invoke(any(Runnable.class));

		SeaChartingQuestHelperConfig config = mock(SeaChartingQuestHelperConfig.class);
		when(config.shortestPathIntegration()).thenReturn(true);

		// Note: the mocked client returns 0 for every varbit, so every task reads incomplete.

		// Player stands in the top-level world; position is set per test.
		player = mock(Player.class);
		WorldView topLevel = mock(WorldView.class);
		when(topLevel.isTopLevel()).thenReturn(true);
		when(player.getWorldView()).thenReturn(topLevel);
		when(client.getLocalPlayer()).thenReturn(player);

		inject("client", client);
		inject("clientThread", clientThread);
		inject("eventBus", eventBus);
		inject("config", config);
	}

	@Test
	public void weatherMessageAutoRetargetsClickedRouteToSecondaryLocation() throws Exception
	{
		SeaChartTask weather = firstTaskOfType(SeaChartTaskType.WEATHER);
		standNear(weather.getLocation());

		clickTask(weather);
		plugin.onChatMessage(gameMessage(WEATHER_MESSAGE));

		List<WorldPoint> targets = capturePostedTargets(2);
		assertEquals("click routes to the primary location", weather.getLocation(), targets.get(0));
		assertEquals("stage-two signal re-routes to the secondary location, hands-free",
			weather.getSecondaryLocation(), targets.get(1));
	}

	@Test
	public void duckStopMessageAutoRetargetsClickedRouteToEndPoint() throws Exception
	{
		SeaChartTask duck = firstTaskOfType(SeaChartTaskType.CURRENT_DUCK);
		standNear(duck.getLocation());

		clickTask(duck);
		plugin.onChatMessage(gameMessage(DUCK_MESSAGE));

		List<WorldPoint> targets = capturePostedTargets(2);
		assertEquals(duck.getLocation(), targets.get(0));
		assertEquals(duck.getSecondaryLocation(), targets.get(1));
	}

	@Test
	public void weatherMessageNeverRedirectsARouteAimedAtAnUnrelatedTask() throws Exception
	{
		SeaChartTask weather = firstTaskOfType(SeaChartTaskType.WEATHER);
		SeaChartTask unrelated = firstTaskOfType(SeaChartTaskType.SPYGLASS);
		standNear(weather.getLocation());

		clickTask(unrelated);
		plugin.onChatMessage(gameMessage(WEATHER_MESSAGE));

		// Only the original click's post; the stage-two signal must not touch this route.
		List<WorldPoint> targets = capturePostedTargets(1);
		assertEquals(unrelated.getLocation(), targets.get(0));
	}

	@Test
	public void clickingATaskAlreadyInStageTwoRoutesStraightToItsSecondaryLocation() throws Exception
	{
		SeaChartTask weather = firstTaskOfType(SeaChartTaskType.WEATHER);
		standNear(weather.getLocation());

		// Signal arrives with no route active: nothing posted, but stage two is remembered...
		plugin.onChatMessage(gameMessage(WEATHER_MESSAGE));
		// ...so a click afterwards routes to where the task actually continues.
		clickTask(weather);

		List<WorldPoint> targets = capturePostedTargets(1);
		assertEquals(weather.getSecondaryLocation(), targets.get(0));
	}

	private void standNear(WorldPoint location)
	{
		when(player.getWorldLocation()).thenReturn(new WorldPoint(location.getX() + 1, location.getY() + 1, 0));
	}

	private List<WorldPoint> capturePostedTargets(int expectedPosts)
	{
		ArgumentCaptor<PluginMessage> captor = ArgumentCaptor.forClass(PluginMessage.class);
		verify(eventBus, times(expectedPosts)).post(captor.capture());

		java.util.List<WorldPoint> targets = new java.util.ArrayList<>();
		for (PluginMessage message : captor.getAllValues())
		{
			assertEquals("shortestpath", message.getNamespace());
			assertEquals("path", message.getName());
			targets.add((WorldPoint) message.getData().get("target"));
		}
		return targets;
	}

	private static ChatMessage gameMessage(String message)
	{
		return new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", message, null, 0);
	}

	/** Drives the panel's click callback (private on the plugin) reflectively. */
	private void clickTask(SeaChartTask task) throws Exception
	{
		Method onTaskClicked = SeaChartingQuestHelperPlugin.class
			.getDeclaredMethod("onTaskClicked", SeaChartTask.class);
		onTaskClicked.setAccessible(true);
		onTaskClicked.invoke(plugin, task);
	}

	private void inject(String fieldName, Object value) throws Exception
	{
		Field field = SeaChartingQuestHelperPlugin.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(plugin, value);
	}

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
