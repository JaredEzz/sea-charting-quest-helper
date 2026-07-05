package com.seachartingquesthelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

/**
 * Regression coverage for the "12,000+ tiles" bug: while aboard a boat, the local player's
 * {@code WorldView} is not {@link WorldView#TOPLEVEL} and {@code getWorldLocation()} is
 * deck-local, not a real overworld coordinate. {@link BoatLocationResolver} must detect that and
 * resolve the boat's true overworld position instead of naively trusting the raw location.
 */
public class BoatLocationResolverTest
{
	@Test
	public void noLocalPlayerResolvesToNull()
	{
		Client client = mock(Client.class);
		when(client.getLocalPlayer()).thenReturn(null);

		assertNull(BoatLocationResolver.resolveEffectivePlayerLocation(client));
	}

	@Test
	public void topLevelWorldViewUsesRawWorldLocationUnchanged()
	{
		Client client = mock(Client.class);
		Player player = mock(Player.class);
		WorldView topLevelWorldView = mock(WorldView.class);
		when(topLevelWorldView.isTopLevel()).thenReturn(true);

		WorldPoint dockLocation = new WorldPoint(3000, 3000, 0);
		when(player.getWorldView()).thenReturn(topLevelWorldView);
		when(player.getWorldLocation()).thenReturn(dockLocation);
		when(client.getLocalPlayer()).thenReturn(player);

		assertEquals(dockLocation, BoatLocationResolver.resolveEffectivePlayerLocation(client));
	}

	@Test
	public void missingWorldViewFallsBackToRawWorldLocation()
	{
		// Defensive case: getWorldView() somehow returns null -- treat like top-level rather
		// than crash or silently misbehave.
		Client client = mock(Client.class);
		Player player = mock(Player.class);

		WorldPoint location = new WorldPoint(3000, 3000, 0);
		when(player.getWorldView()).thenReturn(null);
		when(player.getWorldLocation()).thenReturn(location);
		when(client.getLocalPlayer()).thenReturn(player);

		assertEquals(location, BoatLocationResolver.resolveEffectivePlayerLocation(client));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void onBoatTransformsDeckPositionToRealOverworldCoordinates()
	{
		Client client = mock(Client.class);
		Player player = mock(Player.class);

		final int boatWorldViewId = 7;
		WorldView boatWorldView = mock(WorldView.class);
		when(boatWorldView.isTopLevel()).thenReturn(false);
		when(boatWorldView.getId()).thenReturn(boatWorldViewId);
		when(player.getWorldView()).thenReturn(boatWorldView);

		LocalPoint deckLocal = new LocalPoint(50, 60, boatWorldViewId);
		when(player.getLocalLocation()).thenReturn(deckLocal);
		when(client.getLocalPlayer()).thenReturn(player);

		// The boat itself is a WorldEntity discoverable via the top-level WorldView's
		// worldEntities(), matched by its owned (nested) WorldView id.
		WorldEntity boat = mock(WorldEntity.class);
		when(boat.getWorldView()).thenReturn(boatWorldView);

		WorldView topLevelWorldView = mock(WorldView.class);
		IndexedObjectSet<WorldEntity> worldEntities = mock(IndexedObjectSet.class);
		when(worldEntities.iterator()).thenReturn(Collections.singletonList(boat).iterator());
		doReturn(worldEntities).when(topLevelWorldView).worldEntities();
		when(client.getTopLevelWorldView()).thenReturn(topLevelWorldView);

		// transformToMainWorld() converts the player's on-deck position into a LocalPoint
		// scoped to the top-level world (local coords are tile-granular: >> 7 == tile offset).
		LocalPoint mainWorldLocal = new LocalPoint(30 << 7, 40 << 7, WorldView.TOPLEVEL);
		when(boat.transformToMainWorld(deckLocal)).thenReturn(mainWorldLocal);

		WorldView mainWorldViewForLookup = mock(WorldView.class);
		when(mainWorldViewForLookup.getBaseX()).thenReturn(100);
		when(mainWorldViewForLookup.getBaseY()).thenReturn(200);
		when(mainWorldViewForLookup.getPlane()).thenReturn(1);
		when(client.getWorldView(WorldView.TOPLEVEL)).thenReturn(mainWorldViewForLookup);

		WorldPoint expected = new WorldPoint(30 + 100, 40 + 200, 1);
		assertEquals(expected, BoatLocationResolver.resolveEffectivePlayerLocation(client));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void onBoatWithNoResolvableWorldEntityFailsGracefully()
	{
		Client client = mock(Client.class);
		Player player = mock(Player.class);

		WorldView boatWorldView = mock(WorldView.class);
		when(boatWorldView.isTopLevel()).thenReturn(false);
		when(boatWorldView.getId()).thenReturn(9);
		when(player.getWorldView()).thenReturn(boatWorldView);
		when(client.getLocalPlayer()).thenReturn(player);

		// No WorldEntity owns worldview 9 -- e.g. it despawned/desynced this tick.
		WorldView topLevelWorldView = mock(WorldView.class);
		IndexedObjectSet<WorldEntity> worldEntities = mock(IndexedObjectSet.class);
		when(worldEntities.iterator()).thenReturn(Collections.<WorldEntity>emptyList().iterator());
		doReturn(worldEntities).when(topLevelWorldView).worldEntities();
		when(client.getTopLevelWorldView()).thenReturn(topLevelWorldView);

		assertNull(BoatLocationResolver.resolveEffectivePlayerLocation(client));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nullTransformResultFailsGracefullyInsteadOfGarbageDistance()
	{
		Client client = mock(Client.class);
		Player player = mock(Player.class);

		final int boatWorldViewId = 3;
		WorldView boatWorldView = mock(WorldView.class);
		when(boatWorldView.isTopLevel()).thenReturn(false);
		when(boatWorldView.getId()).thenReturn(boatWorldViewId);
		when(player.getWorldView()).thenReturn(boatWorldView);
		when(player.getLocalLocation()).thenReturn(new LocalPoint(1, 1, boatWorldViewId));
		when(client.getLocalPlayer()).thenReturn(player);

		WorldEntity boat = mock(WorldEntity.class);
		when(boat.getWorldView()).thenReturn(boatWorldView);
		when(boat.transformToMainWorld(any())).thenReturn(null);

		WorldView topLevelWorldView = mock(WorldView.class);
		IndexedObjectSet<WorldEntity> worldEntities = mock(IndexedObjectSet.class);
		when(worldEntities.iterator()).thenReturn(Collections.singletonList(boat).iterator());
		doReturn(worldEntities).when(topLevelWorldView).worldEntities();
		when(client.getTopLevelWorldView()).thenReturn(topLevelWorldView);

		assertNull(BoatLocationResolver.resolveEffectivePlayerLocation(client));
	}
}
