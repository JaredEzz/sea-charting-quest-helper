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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Orders remaining tasks by a blend of raw proximity and how close each task's individual sea
 * (see {@link SeaChartSea} -- e.g. "Shiverwake Expanse", not the whole "Northern Ocean" it sits
 * in) is to full completion, so the player naturally closes out nearly-finished seas instead of
 * leaving one stray task behind in each and paying a dedicated return sail for it later.
 *
 * <p><b>Fixed a real bug:</b> an earlier version keyed the "remaining" count off {@link
 * SeaChartRegion} (the 7 broad oceans) instead of the true sea. Since an ocean can hold well over
 * a dozen distinct seas (Northern Ocean alone spans 75 tasks across ~14 seas), "remaining in
 * region" almost never dropped low enough to trigger a meaningful bonus -- the completion-priority
 * feature was correct in design but nearly inert in practice. This class's own Javadoc already
 * described the bonus in terms of "a typical centre-to-centre sail between adjacent seas", which
 * only makes sense at the true sea granularity -- the region-keyed implementation didn't match its
 * own documented intent.
 *
 * <p><b>Design notes -- why "distance minus (bonus / remaining)":</b>
 *
 * <p>The quantity actually being minimised is total travel over the whole checklist. Leaving a
 * sea 90% charted means a dedicated return trip later, costing roughly one inter-sea sail
 * (several hundred tiles on this chart). Whether finishing a sea <i>now</i> is worth a detour
 * therefore hinges on two observations:
 *
 * <ol>
 * <li>The avoided return trip has a bounded size. If the last task in a sea is 800 tiles away,
 * sailing there now costs about what the future return trip would have -- nothing is gained. So
 * the completion incentive must be capped at roughly one inter-sea leg; it can never be an
 * unconditional "always finish the sea first" rule, which is exactly what would send the player
 * across the map for a percentage.</li>
 * <li>With k tasks remaining in a sea, completing one of them finishes nothing by itself -- the
 * return trip is only avoided once all k are done, and the other k-1 tasks will pull the player
 * back through that sea anyway. So the completion credit any single task can claim is ~1/k of
 * the trip saving.</li>
 * </ol>
 *
 * <p>Both fall out of scoring each task with an <i>effective distance</i> (lower = better):
 *
 * <pre>score = tileDistance - COMPLETION_BONUS_TILES / remainingInSea</pre>
 *
 * <p>remaining=1 claims the full bonus: the last task in a sea jumps anything up to ~600 tiles
 * closer -- near-guaranteed priority locally, but it still loses to a rival 600+ tiles nearer
 * (the huge-detour guard). remaining=2 claims half, and remaining=40 claims a negligible ~15
 * tiles, so the order degrades gracefully to plain nearest-first exactly where completion
 * pressure is meaningless.
 *
 * <p>A two-tier scheme ("seas with &le;2 remaining always sort first") was considered and
 * rejected: it has a hard cliff (3 remaining behaves nothing like 2) and no notion of "too far
 * to be worth it" -- both of which this smooth formula handles with a single tunable constant.
 * A fixed 50/50 blend of normalized distance and completion fraction was also rejected: it makes
 * a nearby task in a large half-done sea outrank based on percentage alone, with no travel-cost
 * meaning behind the number.
 *
 * <p>{@link #COMPLETION_BONUS_TILES} is the one knob. It approximates the tile cost of a
 * dedicated return trip to a sea; 600 is a typical centre-to-centre sail between adjacent seas
 * on the current chart (the mainland chart spans ~1000-3300 x, ~2100-3800 y, with six regions).
 * Raise it to chase sea completion harder, lower it toward pure proximity.
 */
final class SeaChartTaskSorter
{
	/**
	 * Approximate tile cost of a dedicated return sail to a sea -- the maximum detour worth
	 * taking to finish one off. See the class Javadoc for how this value was chosen.
	 */
	static final double COMPLETION_BONUS_TILES = 600.0;

	private SeaChartTaskSorter()
	{
	}

	/**
	 * The blended score: raw tile distance minus this task's share of the avoided-return-trip
	 * bonus. Lower is better.
	 */
	static double effectiveDistance(int distance, int remainingInSea)
	{
		// Guard against a nonsensical count; an incomplete task always leaves >= 1 remaining.
		int remaining = Math.max(1, remainingInSea);
		return distance - COMPLETION_BONUS_TILES / remaining;
	}

	/**
	 * Sorts rows in place by {@link #effectiveDistance}, tie-broken by raw distance and then by
	 * stable task id for determinism.
	 *
	 * @param progress per-sea live progress; a sea missing from the map is treated as having many
	 *                 tasks remaining (no completion bonus)
	 */
	static void sort(List<SeaChartTaskRow> rows, Map<SeaChartSea, SeaChartSeaProgress> progress)
	{
		rows.sort(Comparator
			.comparingDouble((SeaChartTaskRow row) -> effectiveDistance(row.getDistance(), remainingFor(row, progress)))
			.thenComparingInt(SeaChartTaskRow::getDistance)
			.thenComparingInt(row -> row.getTask().getTaskId()));
	}

	private static int remainingFor(SeaChartTaskRow row, Map<SeaChartSea, SeaChartSeaProgress> progress)
	{
		SeaChartSeaProgress seaProgress = progress.get(row.getTask().getSea());
		return seaProgress == null ? Integer.MAX_VALUE : seaProgress.getRemaining();
	}
}
