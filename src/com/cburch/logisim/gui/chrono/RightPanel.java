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
package com.cburch.logisim.gui.chrono;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// Right panel has timeline on top and multiple Waveform components.
public class RightPanel extends JPanel {

	private ChronoPanel chronoPanel;
	private Timeline timeline;

	private ArrayList<Waveform> rows = new ArrayList<>();
	private Box waveforms;
	private JLayeredPane overlay;
	private Cursor cursor;

	private int curX = Integer.MAX_VALUE; // pixel coordinate of cursor, or MAX_INT to pin at right
	private int curT = Integer.MAX_VALUE; // tick number of cursor, or MAX_INT to pin at right
	private int tickWidth = 20; // display width of one time unit 
  private int numTicks;
  private int width, height;
	private int displayOffsetX = 0;

	public RightPanel(ChronoPanel chronoPanel) {
		this.chronoPanel = chronoPanel;
		configure();
	}

	public RightPanel(RightPanel oldPanel) {
    try { throw new Exception(); }
    catch (Exception e) { e.printStackTrace(); }
		chronoPanel = oldPanel.chronoPanel;
		tickWidth = oldPanel.tickWidth;
		curX = oldPanel.curX;
    curT = oldPanel.curT;
		displayOffsetX = oldPanel.displayOffsetX;
		configure();
	}

	private void configure() {
    ChronoData data = chronoPanel.getChronoData();
    int n = data.getSignalCount();
		height = ChronoPanel.HEADER_HEIGHT + n * ChronoPanel.SIGNAL_HEIGHT;

		setLayout(new BorderLayout());
		setBackground(Color.WHITE);

		waveforms = Box.createVerticalBox();
		waveforms.setOpaque(true);
    waveforms.setBackground(Color.WHITE);

    numTicks = data.getValueCount();
		width = tickWidth * numTicks + 1;

		timeline = new Timeline(tickWidth, numTicks, width);
		cursor = new Cursor(this);
		overlay = new JLayeredPane();
		overlay.add(cursor, new Integer(1));
		overlay.add(timeline, new Integer(0));
		overlay.add(waveforms, new Integer(0));

		add(overlay, BorderLayout.CENTER);

    updateSignals();
	}

  int indexOf(ChronoData.Signal s) {
    int n = rows.size();
    for (int i = 0; i < n; i++) {
      Waveform w = rows.get(i);
      if (w.getSignal() == s)
        return i;
    }
    return -1;
  }

  public void updateSignals() {
    ChronoData data = chronoPanel.getChronoData();
    int n = data.getSignalCount();
    System.out.println("resetting waveforms");
    waveforms.removeAll();
    for (int i = 0; i < n; i++) {
      ChronoData.Signal s = data.getSignal(i);
      int idx = indexOf(s);
      Waveform w;
      if (idx < 0) {
        // new signal, add in correct position
        w = new Waveform(chronoPanel, this, s, tickWidth, numTicks, width);
        rows.add(i, w);
      } else if (idx != i) {
        // existing signal, move to correct position
        w = rows.remove(idx);
        rows.add(i, w);
      } else {
        w = rows.get(idx);
      }
      waveforms.add(w);
		}
    // computeSize();
    repaintAll();
  }

  void computeSize() {
    ChronoData data = chronoPanel.getChronoData();
    numTicks = data.getValueCount();
		width = tickWidth * numTicks + 1; // fixme, even clock spacing

    setPreferredSize(new Dimension(width, height)); // necessary for scrollbar

    int hh = ChronoPanel.HEADER_HEIGHT;
		// overlay.setPreferredSize(new Dimension(width, height));
    overlay.setBounds(0, 0, width, height);
		cursor.setBounds(0, 0, width, height);
		timeline.setBounds(0, 0, width, hh);
		waveforms.setBounds(0, hh, width, height - hh);
    // invalidate();
    // repaintAll();
	}

	public void setSignalCursor(int posX) {
    if (posX >= width - 3) {
      curX = Integer.MAX_VALUE; // pin to right side
      curT = Integer.MAX_VALUE; // pin to right side
    } else {
      curX = Math.max(0, posX);
      int slope = (tickWidth < 12) ? tickWidth / 3 : 4;
      curT = Math.max(0, Math.min(numTicks-1, (curX - slope/2) / tickWidth));
    }
		cursor.repaint();
	}

  public int getSignalCursor() {
    return curX == Integer.MAX_VALUE ? width-1 : curX;
  }

  public int getCurrentTick() {
    return curT == Integer.MAX_VALUE ? numTicks-1 : curT;
  }

	public int getDisplayOffsetX() {
		return displayOffsetX;
	}

  public int getSignalWidth() {
    return width;
  }

  public void highlight(ChronoData.Signal s) {
    for (Waveform w : rows)
      w.highlight(w.getSignal() == s);
  }

  public void repaintAll() {
    System.out.println("repaint right panel");
    computeSize();
    super.repaint();
    cursor.repaint();
    timeline.update(tickWidth, numTicks, width);
    timeline.repaint();
    for (Waveform w : rows) {
      w.update(tickWidth, numTicks, width);
      w.repaint();
    }
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
//     for (Waveform sDraw : rows) {
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
//    for (Waveform sDraw : rows) {
//      sDraw.flush();
//      sDraw.repaint();
//    }
  }
}
