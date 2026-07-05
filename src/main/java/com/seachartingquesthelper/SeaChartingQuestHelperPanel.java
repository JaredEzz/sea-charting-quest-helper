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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;

/**
 * Side panel: nearest incomplete sea chart tasks first, with a type filter and a "hide
 * not-yet-reachable" toggle. Rendering is capped to {@link #MAX_ROWS} entries -- the nearest
 * ones -- since re-rendering hundreds of Swing rows every game tick would be wasteful and nobody
 * wants to scroll a 358-row list anyway; "what's my next task" only needs the nearby few.
 */
class SeaChartingQuestHelperPanel extends PluginPanel
{
	private static final int MAX_ROWS = 40;

	private final JLabel statusLabel = new JLabel();
	private final ProgressBar overallBar = new ProgressBar();
	private final JPanel filterSection = new JPanel();
	private final JPanel gearFilterSection = new JPanel();
	private final JPanel listSection = new JPanel();
	private final JCheckBox hideNotReachableBox;
	private final JCheckBox seaCompletionBox;
	private final JCheckBox smartSortBox;
	private final JCheckBox nearestPortBox;
	private final Map<SeaChartTaskType, JCheckBox> typeBoxes = new EnumMap<>(SeaChartTaskType.class);
	private final Set<SeaChartTaskType> visibleTypes = EnumSet.allOf(SeaChartTaskType.class);
	private final Map<SeaChartGearRequirement, JCheckBox> gearHideBoxes = new EnumMap<>(SeaChartGearRequirement.class);
	private final Set<SeaChartGearRequirement> hiddenGearRequirements = EnumSet.noneOf(SeaChartGearRequirement.class);

	private Map<SeaChartTaskType, BufferedImage> icons = Collections.emptyMap();
	private List<SeaChartTaskRow> rows = Collections.emptyList();
	private Map<SeaChartRegion, SeaChartRegionProgress> regionProgress = Collections.emptyMap();
	private int overallComplete;
	private int overallTotal;
	private boolean hideNotReachable = false;
	private boolean showSeaCompletion = true;
	private boolean smartSort = true;
	private boolean showNearestPort = true;

	/** (config key, new value) -- the plugin persists panel toggles back into plugin config. */
	private BiConsumer<String, Boolean> onToggle = (key, value) -> { };
	private Consumer<SeaChartTask> onTaskClicked = task -> { };
	private BiConsumer<SeaChartGearRequirement, Boolean> onGearHideToggle = (requirement, hide) -> { };

	SeaChartingQuestHelperPanel()
	{
		super(true);
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JLabel title = new JLabel("Braindead Sea Charting");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setBorder(new EmptyBorder(0, 0, 6, 0));
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		title.setHorizontalAlignment(SwingConstants.LEFT);

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setBorder(new EmptyBorder(0, 0, 6, 0));
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Overall charting progress, XP-goal style: green fill plus a "x/358 · 58.38%" label.
		overallBar.setFont(FontManager.getRunescapeSmallFont());
		overallBar.setPreferredSize(new Dimension(100, 16));
		overallBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		overallBar.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		overallBar.setMaximumValue(SeaChartTask.values().length);
		overallBar.setValue(0);
		final JPanel barWrapper = new JPanel(new BorderLayout());
		barWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		barWrapper.setBorder(new EmptyBorder(0, 0, 6, 0));
		barWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		barWrapper.add(overallBar, BorderLayout.CENTER);
		barWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, barWrapper.getPreferredSize().height));

		final JLabel chartingTypeLabel = new JLabel("Charting Type:");
		chartingTypeLabel.setFont(FontManager.getRunescapeSmallFont());
		chartingTypeLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		chartingTypeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		chartingTypeLabel.setBorder(new EmptyBorder(0, 0, 2, 0));

		// GridLayout, not FlowLayout: FlowLayout inside a BoxLayout.Y_AXIS parent miscalculates its
		// wrapped preferred height on the first layout pass (it doesn't yet know the final width),
		// so only the first unwrapped row gets space reserved and later rows become invisible/
		// unclickable. GridLayout reserves the correct height for N rows up front regardless of
		// width-negotiation order.
		filterSection.setLayout(new GridLayout(0, 2, 4, 2));
		filterSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		filterSection.setBorder(new EmptyBorder(4, 4, 4, 4));
		filterSection.setAlignmentX(Component.LEFT_ALIGNMENT);
		for (SeaChartTaskType type : SeaChartTaskType.values())
		{
			JCheckBox box = new JCheckBox(type.getLabel(), true);
			box.setFont(FontManager.getRunescapeSmallFont());
			box.setForeground(Color.WHITE);
			box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			box.setFocusPainted(false);
			box.addActionListener(e ->
			{
				if (box.isSelected())
				{
					visibleTypes.add(type);
				}
				else
				{
					visibleTypes.remove(type);
				}
				rebuildList();
			});
			typeBoxes.put(type, box);
			filterSection.add(box);
		}

		final JLabel otherLabel = new JLabel("Other:");
		otherLabel.setFont(FontManager.getRunescapeSmallFont());
		otherLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		otherLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		otherLabel.setBorder(new EmptyBorder(6, 0, 2, 0));

		// Config-backed option toggles, persisted through the plugin's ConfigManager so they
		// mirror the same keys shown in the plugin config panel.
		hideNotReachableBox = buildToggle("Hide not-yet-reachable",
			SeaChartingQuestHelperConfig.KEY_HIDE_NOT_YET_REACHABLE, v -> hideNotReachable = v);
		hideNotReachableBox.setBorder(new EmptyBorder(4, 0, 0, 0));
		seaCompletionBox = buildToggle("Show sea completion",
			SeaChartingQuestHelperConfig.KEY_SHOW_SEA_COMPLETION, v -> showSeaCompletion = v);
		smartSortBox = buildToggle("Prioritise nearly-done seas",
			SeaChartingQuestHelperConfig.KEY_SMART_SORT, v -> smartSort = v);
		nearestPortBox = buildToggle("Show nearest port hint",
			SeaChartingQuestHelperConfig.KEY_SHOW_NEAREST_PORT, v -> showNearestPort = v);
		nearestPortBox.setBorder(new EmptyBorder(0, 0, 6, 0));

		final JLabel gearFilterLabel = new JLabel("Hide tasks needing gear I don't have yet:");
		gearFilterLabel.setFont(FontManager.getRunescapeSmallFont());
		gearFilterLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		gearFilterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		gearFilterSection.setLayout(new GridLayout(0, 1, 0, 2));
		gearFilterSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		gearFilterSection.setAlignmentX(Component.LEFT_ALIGNMENT);
		gearFilterSection.setBorder(new EmptyBorder(2, 0, 6, 0));
		for (SeaChartGearRequirement requirement : SeaChartGearRequirement.values())
		{
			JCheckBox box = new JCheckBox(requirement.getLabel(), false);
			box.setFont(FontManager.getRunescapeSmallFont());
			box.setForeground(Color.WHITE);
			box.setBackground(ColorScheme.DARK_GRAY_COLOR);
			box.setFocusPainted(false);
			box.addActionListener(e ->
			{
				if (box.isSelected())
				{
					hiddenGearRequirements.add(requirement);
				}
				else
				{
					hiddenGearRequirements.remove(requirement);
				}
				onGearHideToggle.accept(requirement, box.isSelected());
				rebuildList();
			});
			gearHideBoxes.put(requirement, box);
			gearFilterSection.add(box);
		}

		listSection.setLayout(new GridLayout(0, 1, 0, 4));
		listSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listSection.setAlignmentX(Component.LEFT_ALIGNMENT);

		content.add(title);
		content.add(statusLabel);
		content.add(barWrapper);
		content.add(chartingTypeLabel);
		content.add(filterSection);
		content.add(otherLabel);
		content.add(hideNotReachableBox);
		content.add(seaCompletionBox);
		content.add(smartSortBox);
		content.add(nearestPortBox);
		content.add(gearFilterLabel);
		content.add(gearFilterSection);
		content.add(listSection);

		add(content, BorderLayout.NORTH);
		rebuildList();
	}

	private JCheckBox buildToggle(String label, String configKey, Consumer<Boolean> apply)
	{
		JCheckBox box = new JCheckBox(label, true);
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setForeground(Color.WHITE);
		box.setBackground(ColorScheme.DARK_GRAY_COLOR);
		box.setFocusPainted(false);
		box.setAlignmentX(Component.LEFT_ALIGNMENT);
		box.addActionListener(e ->
		{
			apply.accept(box.isSelected());
			onToggle.accept(configKey, box.isSelected());
			rebuildList();
		});
		return box;
	}

	void setCallbacks(BiConsumer<String, Boolean> onToggle, Consumer<SeaChartTask> onTaskClicked)
	{
		this.onToggle = onToggle;
		this.onTaskClicked = onTaskClicked;
	}

	void setGearHideCallback(BiConsumer<SeaChartGearRequirement, Boolean> onGearHideToggle)
	{
		this.onGearHideToggle = onGearHideToggle;
	}

	/** Seeds all option toggles from persisted config, without firing the persist callback. */
	void initOptions(boolean hideNotReachable, boolean showSeaCompletion, boolean smartSort, boolean showNearestPort)
	{
		this.hideNotReachable = hideNotReachable;
		this.showSeaCompletion = showSeaCompletion;
		this.smartSort = smartSort;
		this.showNearestPort = showNearestPort;
		hideNotReachableBox.setSelected(hideNotReachable);
		seaCompletionBox.setSelected(showSeaCompletion);
		smartSortBox.setSelected(smartSort);
		nearestPortBox.setSelected(showNearestPort);
		rebuildList();
	}

	/** Sets the gear-filter checkboxes from persisted config, without firing the callback. */
	void initGearFilters(Set<SeaChartGearRequirement> initiallyHidden)
	{
		hiddenGearRequirements.clear();
		hiddenGearRequirements.addAll(initiallyHidden);
		for (Map.Entry<SeaChartGearRequirement, JCheckBox> entry : gearHideBoxes.entrySet())
		{
			entry.getValue().setSelected(hiddenGearRequirements.contains(entry.getKey()));
		}
		rebuildList();
	}

	void setIcons(Map<SeaChartTaskType, BufferedImage> icons)
	{
		this.icons = icons;
		rebuildList();
	}

	/**
	 * Full, sorted set of remaining tasks for this tick (smart or nearest-first order, decided by
	 * the plugin), plus the live per-sea and overall completion counts backing the "(x/y)"
	 * markers and the progress bar.
	 */
	void setRows(List<SeaChartTaskRow> rows, Map<SeaChartRegion, SeaChartRegionProgress> regionProgress,
		int overallComplete, int overallTotal)
	{
		this.rows = rows == null ? Collections.emptyList() : rows;
		this.regionProgress = regionProgress == null ? Collections.emptyMap() : regionProgress;
		this.overallComplete = overallComplete;
		this.overallTotal = overallTotal;

		overallBar.setMaximumValue(Math.max(1, overallTotal));
		overallBar.setValue(overallComplete);
		double percent = overallTotal <= 0 ? 0.0 : overallComplete * 100.0 / overallTotal;
		overallBar.setCenterLabel(String.format("%d/%d · %.2f%%", overallComplete, overallTotal, percent));

		rebuildList();
	}

	private void rebuildList()
	{
		listSection.removeAll();

		List<SeaChartTaskRow> filtered = new ArrayList<>();
		for (SeaChartTaskRow row : rows)
		{
			if (!visibleTypes.contains(row.getTask().getType()))
			{
				continue;
			}
			if (hideNotReachable && !row.isEligible())
			{
				continue;
			}
			if (!hiddenGearRequirements.isEmpty() && !Collections.disjoint(hiddenGearRequirements, row.getTask().getGearRequirements()))
			{
				continue;
			}
			filtered.add(row);
			if (filtered.size() >= MAX_ROWS)
			{
				break;
			}
		}

		if (rows.isEmpty())
		{
			statusLabel.setText("Log in and open this panel near the sea to populate tasks.");
		}
		else if (filtered.isEmpty())
		{
			statusLabel.setText("No tasks match the current filters. (" + overallComplete + "/" + overallTotal + " charted)");
		}
		else
		{
			statusLabel.setText(filtered.size() + " shown, "
				+ (smartSort ? "nearly-done seas first" : "nearest first")
				+ " (" + overallComplete + "/" + overallTotal + " charted)");
		}

		for (SeaChartTaskRow row : filtered)
		{
			listSection.add(buildRow(row));
		}

		revalidate();
		repaint();
	}

	private JPanel buildRow(SeaChartTaskRow row)
	{
		SeaChartTask task = row.getTask();
		boolean eligible = row.isEligible();

		JPanel panel = new JPanel(new BorderLayout(6, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(4, 6, 4, 6));

		JLabel iconLabel = new JLabel();
		BufferedImage img = icons.get(task.getType());
		if (img != null)
		{
			iconLabel.setIcon(new ImageIcon(img));
		}
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(iconLabel, BorderLayout.WEST);

		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		textPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(task.getTaskName());
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setForeground(eligible ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);

		String detail = row.getDistance() + " tiles · " + task.getType().getLabel();
		if (!eligible)
		{
			detail += " · Requires " + task.getLevel() + " Sailing";
		}
		JLabel detailLabel = new JLabel(detail);
		detailLabel.setFont(FontManager.getRunescapeSmallFont());
		detailLabel.setForeground(eligible ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.PROGRESS_ERROR_COLOR);

		textPanel.add(nameLabel);
		textPanel.add(detailLabel);

		if (row.isStageTwo())
		{
			// Stage two of a two-stage task: the distance above (and any Shortest Path route)
			// now points at the task's secondary location, not its original spot.
			JLabel stageLabel = new JLabel(task.getType() == SeaChartTaskType.WEATHER
				? "Data collected — return the weather station"
				: "Duck stopped — retrieve it at the end point");
			stageLabel.setFont(FontManager.getRunescapeSmallFont());
			stageLabel.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
			textPanel.add(stageLabel);
		}

		if (showSeaCompletion)
		{
			String regionText = task.getRegion().getLabel();
			SeaChartRegionProgress progress = regionProgress.get(task.getRegion());
			if (progress != null)
			{
				regionText += " (" + progress.getComplete() + "/" + progress.getTotal() + ")";
			}
			JLabel regionLabel = new JLabel(regionText);
			regionLabel.setFont(FontManager.getRunescapeSmallFont());
			regionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			textPanel.add(regionLabel);
		}

		if (showNearestPort)
		{
			JLabel portLabel = new JLabel("Nearest: " + task.getRegion().getNearestPort());
			portLabel.setFont(FontManager.getRunescapeSmallFont());
			portLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			textPanel.add(portLabel);
		}

		Set<SeaChartGearRequirement> gearRequirements = task.getGearRequirements();
		if (!gearRequirements.isEmpty())
		{
			StringBuilder gearText = new StringBuilder("Needs: ");
			boolean first = true;
			for (SeaChartGearRequirement requirement : gearRequirements)
			{
				if (!first)
				{
					gearText.append(", ");
				}
				gearText.append(requirement.getLabel());
				first = false;
			}
			JLabel gearLabel = new JLabel(gearText.toString());
			gearLabel.setFont(FontManager.getRunescapeSmallFont());
			gearLabel.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
			textPanel.add(gearLabel);
		}

		panel.add(textPanel, BorderLayout.CENTER);

		panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		panel.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e)
			{
				onTaskClicked.accept(task);
			}
		});

		return panel;
	}
}
