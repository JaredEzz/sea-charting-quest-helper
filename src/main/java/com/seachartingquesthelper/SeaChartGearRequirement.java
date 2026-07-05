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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Special boat facility a task's sea may require, beyond plain Sailing level/quest gating, per the
 * OSRS Wiki's per-sea hazard pages ("Crystal-flecked waters", "Tangled kelp", "Icy seas", "Fetid
 * waters"). See {@link SeaChartGearRequirements} for the sea-to-requirement mapping.
 *
 * <p>Unlike {@link SeaChartRequirements} (Sailing level / quest state), RuneLite's client API has
 * no way to read whether the player's boat actually has a given facility built -- there's no
 * "does my boat have an eternal brazier" getter. So this can't be an automatic eligibility check
 * the way "hide not-yet-reachable" is. Instead each requirement gets its own manual "hide tasks
 * that need this" toggle in the panel that the player flips themselves once they know they don't
 * have the facility yet -- see {@code SeaChartingQuestHelperPanel}'s gear-filter checkboxes.
 */
@Getter
@RequiredArgsConstructor
public enum SeaChartGearRequirement
{
	/**
	 * Crystal-flecked waters (Porth Gwenith, Porth Neigwl) and tangled kelp (Rainbow Reef,
	 * Southern Expanse) both damage/slow an unprepared boat unless it has at least an adamant-tier
	 * keel or helm respectively -- grouped as one requirement since both gate on the same boat-part
	 * tier and the player asked for a single filter covering "dangerous water needing adamant gear".
	 */
	ADAMANT_KEEL_OR_HELM("Adamant keel/helm+"),
	/** Icy seas (Weiss Melt, Everwinter Sea, Stoneheart Sea, Weissmere, Winter's Edge, Shiverwake Expanse) freeze crew/damage the ship without one. */
	ETERNAL_BRAZIER("Eternal brazier"),
	/** Fetid, disease-inducing waters (Backwater, Breakbone Strait, Mythic Sea, Sea of Souls, Zul-Egil) damage/slow the boat without one. */
	INOCULATION_STATION("Inoculation station"),
	;

	private final String label;
}
