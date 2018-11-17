/**
 * This file is part of Logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + Haute École Spécialisée Bernoise
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 *   + REDS Institute - HEIG-VD, Yverdon-les-Bains, Switzerland
 *     http://reds.heig-vd.ch
 * This version of the project is currently maintained by:
 *   + Kevin Walsh (kwalsh@holycross.edu, http://mathcs.holycross.edu/~kwalsh)
 */
package com.hepia.logisim.chronogui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.hepia.logisim.chronodata.ChronoData;

// Right panel has timeline on top and multiple SignalDraw components.
public class RightPanel extends JPanel {

	private ChronoPanel chronoPanel;
	private TimelineDraw timeline;

	private ArrayList<SignalDraw> rows = new ArrayList<>();
	private Box waveforms;
	private JLayeredPane overlay;
	private Cursor cursor;

	private int curX = 0;
	private int tickWidth = 20;
	private int displayOffsetX = 0;

	private int height;

	public RightPanel(ChronoPanel chronoPanel) {
		this.chronoPanel = chronoPanel;
		configure();
	}

	public RightPanel(RightPanel oldPanel) {
		chronoPanel = oldPanel.chronoPanel;
		tickWidth = oldPanel.tickWidth;
		curX = oldPanel.curX;
		displayOffsetX = oldPanel.displayOffsetX;
		configure();
	}

	private void configure() {
    ChronoData data = chronoPanel.getChronoData();
    int n = data.getSignalCount();
		height = n * ChronoPanel.SIGNAL_HEIGHT;

		setLayout(new BorderLayout());
		setBackground(Color.WHITE);

		waveforms = Box.createVerticalBox();
		waveforms.setOpaque(true);

		timeline = new TimelineDraw(tickWidth);

    for (int i = 0; i < n; i++) {
      ChronoData.Signal s = data.getSignal(i);
      SignalDraw w = new SignalDraw(chronoPanel, this, s);
      rows.add(w);
			waveforms.add(w);
		}

		cursor = new Cursor();

		overlay = new JLayeredPane();
		computeSize();

		overlay.add(cursor, new Integer(1));
		overlay.add(timeline, new Integer(0));
		overlay.add(waveforms, new Integer(0));

		add(overlay, BorderLayout.WEST);
	}

	private void computeSize() {
		int totalWidth = tickWidth * 2;
		overlay.setPreferredSize(new Dimension(totalWidth, height));
		waveforms.setBounds(0, ChronoPanel.HEADER_HEIGHT, totalWidth, height);
		timeline.setBounds(0, 0, totalWidth, ChronoPanel.HEADER_HEIGHT);
		cursor.setBounds(0, 0, totalWidth, height);
	}

	public void setSignalCursor(int posX) {
		curX = posX;
		cursor.setPosition(posX);
		cursor.repaint();
	}

  public int getSignalCursor() {
    return curX;
  }

	public int getDisplayOffsetX() {
		return displayOffsetX;
	}

	public int getSignalWidth() {
    ChronoData data = chronoPanel.getChronoData();
		return 2 * tickWidth * data.getValueCount();
	}

	public int getTickWidth() {
		return tickWidth;
	}

	public int getVisibleWidth() {
		return chronoPanel.getVisibleSignalsWidth();
	}

  public int getTotalWidth() {
    return 2 * tickWidth;
  }

  public int getTotalHeight() {
    return height;
  }

  public void highlight(ChronoData.Signal s) {
    for (SignalDraw w : rows)
      w.highlight(w.getSignal() == s);
  }

  public void repaintAll() {
    cursor.repaint();
    int width = getTickWidth();
    for (SignalDraw w : rows) {
      w.setTickWidth(width);
      w.repaint();
    }
    computeSize(); // todo: why last?
  }

  public int getCurrentPosition() {
    return (curX + tickWidth) / tickWidth; // todo: -1?
  }

  // todo: later
  public void zoom(int sens, int posX) {
//     int nbrOfTick = curX / tickWidth;
// 
//     tickWidth += sens;
//     if (tickWidth <= 1)
//       tickWidth = 1;
// 
//     // make the curX follow the zoom
//     int newPosX = nbrOfTick * tickWidth;
//     curX = newPosX;
//     // set the cusor position
//     cursor.setPosition(newPosX);
// 
//     // Scrollbar follow the zoom
//     int scrollBarCursorPos = cursor.getPosition()
//         - (chronoPanel.getVisibleSignalsWidth() / 2);
// 
//     // zoom on every signals
//     for (SignalDraw sDraw : rows) {
//       sDraw.setTickWidth(tickWidth);
//     }
// 
//     // zoom on the timeline
//     timeline.setTickWidth(tickWidth, 2 /* chronoPanel.getNbrOfTick() */);
// 
//     computeSize();
// 
//     // force redraw everything
//     SwingUtilities.updateComponentTreeUI(chronoPanel);
// 
//     // scrollbar position
//     chronoPanel.setScrollbarPosition(scrollBarCursorPos);
  }

    // todo: later
  public void adjustmentValueChanged(int value) {
//    float posPercent = (float) value / (float) getSignalWidth();
//    int i = Math.round(/* chronoPanel.getNbrOfTick()*/ 2 * posPercent);
//    i = i > 5 ? i - 5 : 0;
//    displayOffsetX = i * tickWidth;
//    for (SignalDraw sDraw : rows) {
//      sDraw.flush();
//      sDraw.repaint();
//    }
  }
}
