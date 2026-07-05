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
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	private final JPanel filterSection = new JPanel();
	private final JPanel listSection = new JPanel();
	private final JCheckBox hideNotReachableBox = new JCheckBox("Hide not-yet-reachable");
	private final Map<SeaChartTaskType, JCheckBox> typeBoxes = new EnumMap<>(SeaChartTaskType.class);
	private final Set<SeaChartTaskType> visibleTypes = EnumSet.allOf(SeaChartTaskType.class);

	private Map<SeaChartTaskType, BufferedImage> icons = Collections.emptyMap();
	private List<SeaChartTaskRow> rows = Collections.emptyList();
	private boolean hideNotReachable = false;

	private Consumer<Boolean> onHideToggle = hide -> { };
	private Consumer<SeaChartTask> onTaskClicked = task -> { };

	SeaChartingQuestHelperPanel()
	{
		super(true);
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JLabel title = new JLabel("Sea Charting Quest Helper");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setBorder(new EmptyBorder(0, 0, 6, 0));

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setBorder(new EmptyBorder(0, 0, 6, 0));

		filterSection.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
		filterSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		filterSection.setBorder(new EmptyBorder(4, 4, 4, 4));
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

		hideNotReachableBox.setFont(FontManager.getRunescapeSmallFont());
		hideNotReachableBox.setForeground(Color.WHITE);
		hideNotReachableBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		hideNotReachableBox.setFocusPainted(false);
		hideNotReachableBox.setBorder(new EmptyBorder(4, 0, 6, 0));
		hideNotReachableBox.addActionListener(e ->
		{
			hideNotReachable = hideNotReachableBox.isSelected();
			onHideToggle.accept(hideNotReachable);
			rebuildList();
		});

		listSection.setLayout(new GridLayout(0, 1, 0, 4));
		listSection.setBackground(ColorScheme.DARK_GRAY_COLOR);

		content.add(title);
		content.add(statusLabel);
		content.add(filterSection);
		content.add(hideNotReachableBox);
		content.add(listSection);

		add(content, BorderLayout.NORTH);
		rebuildList();
	}

	void setCallbacks(Consumer<Boolean> onHideToggle, Consumer<SeaChartTask> onTaskClicked)
	{
		this.onHideToggle = onHideToggle;
		this.onTaskClicked = onTaskClicked;
	}

	/** Sets the hide-not-reachable checkbox from persisted config, without firing the callback. */
	void initHideNotReachable(boolean hide)
	{
		this.hideNotReachable = hide;
		hideNotReachableBox.setSelected(hide);
		rebuildList();
	}

	void setIcons(Map<SeaChartTaskType, BufferedImage> icons)
	{
		this.icons = icons;
		rebuildList();
	}

	/** Full, distance-sorted (nearest first) set of remaining tasks for this tick. */
	void setRows(List<SeaChartTaskRow> rows)
	{
		this.rows = rows == null ? Collections.emptyList() : rows;
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
			statusLabel.setText("No tasks match the current filters.");
		}
		else
		{
			statusLabel.setText(filtered.size() + " shown, nearest first");
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
