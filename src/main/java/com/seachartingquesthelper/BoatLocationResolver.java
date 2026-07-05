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

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Resolves the local player's real, top-level-world (overworld) position.
 *
 * <p>Sailing boats are implemented as nested {@link WorldView}s. While standing on a moving
 * boat, {@code client.getLocalPlayer().getWorldView()} is <em>not</em> {@link
 * WorldView#TOPLEVEL} -- it's a separate WorldView tied to the boat's own small deck area, and
 * {@code getWorldLocation()} in that state returns coordinates local to the deck, not real
 * overworld coordinates. Naively comparing that against static overworld {@link SeaChartTask}
 * locations produces nonsense distances (e.g. "12,000+ tiles"), and since the deck-local
 * coordinate barely changes tick to tick, it also looks frozen even as the boat actually sails.
 *
 * <p>This class detects that case, finds the {@link WorldEntity} (boat) the player is currently
 * standing on, and transforms the player's on-deck position into real overworld coordinates via
 * {@link WorldEntity#transformToMainWorld(LocalPoint)} -- which reflects the boat's true, moving
 * position every tick.
 */
@Slf4j
final class BoatLocationResolver
{
	private BoatLocationResolver()
	{
	}

	/**
	 * @return the player's real overworld {@link WorldPoint}, or {@code null} if it can't be
	 * resolved this tick (no local player, or aboard a boat whose position couldn't be
	 * determined) -- callers should fail gracefully (e.g. skip the update) rather than fall back
	 * to a raw, possibly boat-local, coordinate.
	 */
	static WorldPoint resolveEffectivePlayerLocation(Client client)
	{
		final Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		final WorldView playerWorldView = localPlayer.getWorldView();
		if (playerWorldView == null || playerWorldView.isTopLevel())
		{
			return localPlayer.getWorldLocation();
		}

		final WorldEntity boat = findWorldEntityByWorldViewId(client, playerWorldView.getId());
		if (boat == null)
		{
			log.debug("Player is in non-toplevel worldview {} but no owning WorldEntity (boat)"
				+ " could be found -- can't compute a real distance this tick", playerWorldView.getId());
			return null;
		}

		final LocalPoint deckLocal = localPlayer.getLocalLocation();
		if (deckLocal == null)
		{
			log.debug("Player has no local location while aboard boat in worldview {}", playerWorldView.getId());
			return null;
		}

		final LocalPoint mainWorldLocal = boat.transformToMainWorld(deckLocal);
		if (mainWorldLocal == null)
		{
			log.debug("WorldEntity.transformToMainWorld returned null for boat in worldview {}",
				playerWorldView.getId());
			return null;
		}

		return WorldPoint.fromLocal(client, mainWorldLocal);
	}

	/** Finds the {@link WorldEntity} (boat) that owns the given nested {@link WorldView} id. */
	static WorldEntity findWorldEntityByWorldViewId(Client client, int worldViewId)
	{
		for (WorldEntity entity : client.getTopLevelWorldView().worldEntities())
		{
			final WorldView entityWorldView = entity.getWorldView();
			if (entityWorldView != null && entityWorldView.getId() == worldViewId)
			{
				return entity;
			}
		}
		return null;
	}
}
