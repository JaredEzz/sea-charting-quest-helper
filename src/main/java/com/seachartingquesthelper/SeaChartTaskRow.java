package com.seachartingquesthelper;

import lombok.Value;

/**
 * One row's worth of computed, EDT-ready state for the panel: the task itself, its live distance
 * from the local player, whether its level/quest requirement is currently met, and whether the
 * task is in its second stage (see {@link SeaChartTwoStageTracker}) -- in which case
 * {@code distance} already measures to the task's secondary location.
 */
@Value
class SeaChartTaskRow
{
	SeaChartTask task;
	int distance;
	boolean eligible;
	boolean stageTwo;
}
