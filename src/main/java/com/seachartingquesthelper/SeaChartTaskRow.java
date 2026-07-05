package com.seachartingquesthelper;

import lombok.Value;

/**
 * One row's worth of computed, EDT-ready state for the panel: the task itself, its live distance
 * from the local player, and whether its level/quest requirement is currently met.
 */
@Value
class SeaChartTaskRow
{
	SeaChartTask task;
	int distance;
	boolean eligible;
}
