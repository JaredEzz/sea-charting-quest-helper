package com.seachartingquesthelper;

import lombok.Value;

/**
 * One row's worth of computed, EDT-ready state for the panel: the task itself, its live distance
 * from the local player, whether its level/quest requirement is currently met, whether the task is
 * in its second stage (see {@link SeaChartTwoStageTracker}) -- in which case {@code distance}
 * already measures to the task's secondary location -- and whether it's already been charted (the
 * panel only shows completed rows when the "Show completed" toggle is on).
 */
@Value
class SeaChartTaskRow
{
	SeaChartTask task;
	int distance;
	boolean eligible;
	boolean stageTwo;
	boolean completed;
}
