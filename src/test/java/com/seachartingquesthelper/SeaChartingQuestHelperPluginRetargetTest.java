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
 * handlers: receive the verbatim in-game trigger message as a {@link ChatMessage}, and verify the
 * plugin posts a Shortest Path {@link PluginMessage} carrying the task's <em>secondary</em>
 * location -- with no manual re-click. Collaborators are mocked; the decision logic under test is
 * real.
 *
 * <p>Current duck's trigger is the release message ("You release your current duck..."), fired
 * at task start -- not the arrival message ("...comes to a stop") -- since the destination is
 * static, known task data and useful to route to immediately, before the player has already
 * arrived.
 *
 * <p><b>{@link #duckReleaseMessageAutoRetargetsRouteWithNoPriorPanelClickAtAll} is the live-bug
 * regression test:</b> an earlier version only re-targeted when the resolved task was already
 * the Shortest Path route's current target -- requiring the player to have clicked that exact
 * task's panel row first. Confirmed via a real play session, this almost never happens: a player
 * just sails up to a duck directly. That test exercises exactly that scenario (no
 * {@code onTaskClicked} call at all) and would have caught the bug before it shipped.
 */
public class SeaChartingQuestHelperPluginRetargetTest
{
	private static final String WEATHER_MESSAGE =
		"You find a spot where the winds have dropped. You fill the station's log and your"
			+ " charts with interesting data. You should now return to Meaty Aura Logist where"
			+ " she gave you the weather station.";

	private static final String DUCK_MESSAGE =
		"You release your current duck and he begins tracking the currents...";

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
	public void duckReleaseMessageAutoRetargetsClickedRouteToEndPoint() throws Exception
	{
		SeaChartTask duck = firstTaskOfType(SeaChartTaskType.CURRENT_DUCK);
		standNear(duck.getLocation());

		clickTask(duck);
		plugin.onChatMessage(gameMessage(DUCK_MESSAGE));

		List<WorldPoint> targets = capturePostedTargets(2);
		assertEquals(duck.getLocation(), targets.get(0));
		assertEquals(duck.getSecondaryLocation(), targets.get(1));
	}

	/**
	 * The live-bug scenario: the player never clicked ANY panel row -- they just sailed up to the
	 * duck in-game and released it. There is no prior Shortest Path route at all when the release
	 * message fires. The plugin must still post the route target automatically, exactly as if the
	 * player had clicked this task's row -- just aimed at the secondary (destination) location.
	 */
	@Test
	public void duckReleaseMessageAutoRetargetsRouteWithNoPriorPanelClickAtAll() throws Exception
	{
		SeaChartTask duck = firstTaskOfType(SeaChartTaskType.CURRENT_DUCK);
		standNear(duck.getLocation());

		// No clickTask(...) call anywhere -- the route has never been set.
		plugin.onChatMessage(gameMessage(DUCK_MESSAGE));

		List<WorldPoint> targets = capturePostedTargets(1);
		assertEquals(duck.getSecondaryLocation(), targets.get(0));
	}

	/** Same live-bug scenario, for Weather. */
	@Test
	public void weatherMessageAutoRetargetsRouteWithNoPriorPanelClickAtAll() throws Exception
	{
		SeaChartTask weather = firstTaskOfType(SeaChartTaskType.WEATHER);
		standNear(weather.getLocation());

		plugin.onChatMessage(gameMessage(WEATHER_MESSAGE));

		List<WorldPoint> targets = capturePostedTargets(1);
		assertEquals(weather.getSecondaryLocation(), targets.get(0));
	}

	/**
	 * Re-targeting is unconditional: even if the route currently points at a completely
	 * unrelated task (clicked earlier, or left over from a previous session), a stage-two signal
	 * for a different task still re-targets the route -- exactly like clicking that task's row
	 * would. There is no "must already be the active route target" gate any more (see class
	 * Javadoc: that gate was the bug).
	 */
	@Test
	public void weatherMessageRedirectsARouteEvenIfPreviouslyAimedAtAnUnrelatedTask() throws Exception
	{
		SeaChartTask weather = firstTaskOfType(SeaChartTaskType.WEATHER);
		SeaChartTask unrelated = firstTaskOfType(SeaChartTaskType.SPYGLASS);
		standNear(weather.getLocation());

		clickTask(unrelated);
		plugin.onChatMessage(gameMessage(WEATHER_MESSAGE));

		List<WorldPoint> targets = capturePostedTargets(2);
		assertEquals(unrelated.getLocation(), targets.get(0));
		assertEquals(weather.getSecondaryLocation(), targets.get(1));
	}

	@Test
	public void clickingATaskAlreadyInStageTwoRoutesStraightToItsSecondaryLocation() throws Exception
	{
		SeaChartTask weather = firstTaskOfType(SeaChartTaskType.WEATHER);
		standNear(weather.getLocation());

		// Signal arrives first (auto-posts to the secondary location -- see the no-prior-click
		// test above), then a later click on the same row should route there too, not the primary.
		plugin.onChatMessage(gameMessage(WEATHER_MESSAGE));
		clickTask(weather);

		List<WorldPoint> targets = capturePostedTargets(2);
		assertEquals(weather.getSecondaryLocation(), targets.get(0));
		assertEquals(weather.getSecondaryLocation(), targets.get(1));
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
