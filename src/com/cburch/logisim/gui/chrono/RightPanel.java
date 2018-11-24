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
import static com.cburch.logisim.gui.chrono.Strings.S;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

// Right panel has timeline on top and multiple Waveform components.
public class RightPanel extends JPanel {

  private static final int WAVE_HEIGHT = ChronoPanel.SIGNAL_HEIGHT;
  private static final int EXTRA_SPACE = 40; // at right side, to allow for label overhang
  private static final int CURSOR_GAP = 20; // don't put cursor too close to sides side

	private ChronoPanel chronoPanel;
  DefaultListSelectionModel selectionModel;
  private ChronoData data;

	private ArrayList<Waveform> rows = new ArrayList<>();

	private int curX = Integer.MAX_VALUE; // pixel coordinate of cursor, or MAX_INT to pin at right
	private int curT = Integer.MAX_VALUE; // tick number of cursor, or MAX_INT to pin at right
	private int tickWidth = 20; // display width of one time unit 
  private int numTicks, slope;
  private int width, height;
  private MyListener myListener = new MyListener();

	public RightPanel(ChronoPanel p, ListSelectionModel m) {
		chronoPanel = p;
    selectionModel = (DefaultListSelectionModel)m;
    data = p.getChronoData();
		slope = (tickWidth < 12) ? tickWidth / 3 : 4;
		configure();
	}

	public RightPanel(RightPanel oldPanel, ListSelectionModel m) {
    try { throw new Exception(); }
    catch (Exception e) { e.printStackTrace(); }
		chronoPanel = oldPanel.chronoPanel;
    selectionModel = (DefaultListSelectionModel)m;
		tickWidth = oldPanel.tickWidth;
		slope = (tickWidth < 12) ? tickWidth / 3 : 4;
		curX = oldPanel.curX;
    curT = oldPanel.curT;
		configure();
	}

	private void configure() {
    int n = data.getSignalCount();
		height = ChronoPanel.HEADER_HEIGHT + n * ChronoPanel.SIGNAL_HEIGHT;

		setBackground(Color.WHITE);

    numTicks = data.getValueCount();
		width = tickWidth * numTicks + EXTRA_SPACE;

		addMouseListener(myListener);
		addMouseMotionListener(myListener);
		addMouseWheelListener(myListener);

    updateSignals();
	}

  int indexOf(ChronoData.Signal s) {
    int n = rows.size();
    for (int i = 0; i < n; i++) {
      Waveform w = rows.get(i);
      if (w.signal == s)
        return i;
    }
    return -1;
  }

  public void updateSignals() {
    int n = data.getSignalCount();
    for (int i = 0; i < n; i++) {
      ChronoData.Signal s = data.getSignal(i);
      int idx = indexOf(s);
      if (idx < 0) {
        // new signal, add in correct position
        rows.add(i, new Waveform(s));
      } else if (idx != i) {
        // existing signal, move to correct position
        rows.add(i, rows.remove(idx));
      }
		}
    if (rows.size() > n)
      rows.subList(n, rows.size()).clear();
    numTicks = -1; // forces updateWaveforms() to refresh waveforms
    updateWaveforms();
  }

  public void updateWaveforms() {
    int n = data.getValueCount();
    if (n == numTicks)
      return; // display has not changed
    numTicks = n;
    updateSize();
    flushWaveforms();
    repaint();
  }

  private void updateSize() {
    int m = data.getSignalCount();
		height = ChronoPanel.HEADER_HEIGHT + m * ChronoPanel.SIGNAL_HEIGHT;
		width = tickWidth * numTicks + EXTRA_SPACE; // todo: even clock spacing

    Dimension d = getPreferredSize();
    if (d.width == width && d.height == height)
      return;

    int oldWidth = d.width;
    JViewport v = chronoPanel.getRightViewport();
    JScrollBar sb = chronoPanel.getHorizontalScrollBar();
    Rectangle oldR = v == null ? null : v.getViewRect();

    d.width = width;
    d.height = height;
    setPreferredSize(d); // necessary for scrollbar
    revalidate();

    if (sb == null || v == null || sb.getValueIsAdjusting())
      return;

    // if cursor is off screen, but right edge was on screen, scroll to max position
    // if cursor is on screen, scroll as far as possible while still keeping cursor on screen
    // .....(.....|....... )
    // .....(........|.... )
    // ...(.|..........)..
    // (.|..........).....
    // (...|...     )     
    // ^                   ^
    // never go below left=0 (0%) or above right=width-1 (100%)
    // try to not go above cursor-CURSOR_GAP

    Rectangle r = v.getViewRect(); // has this updated yet?

    if (oldR.x <= curX && curX <= oldR.x + oldR.width) {
      // cursor was on screen, keep it on screen
      r.x = Math.max(oldR.x, curX - CURSOR_GAP);
      r.width = Math.max(r.width, width - r.x);
      SwingUtilities.invokeLater(new Runnable() {
        public void run() { scrollRectToVisible(r); }
      });
    } else if (oldWidth <= oldR.x + oldR.width) {
      // right edge was on screen, keep it on screen
      r.x = Math.max(0, width - r.width);
      r.width = width - r.x;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() { scrollRectToVisible(r); }
      });
    } else {
      // do nothing ?
    }
  }

	public void setSignalCursor(int posX) {
    if (posX >= width - EXTRA_SPACE - 2) {
      curX = Integer.MAX_VALUE; // pin to right side
      curT = Integer.MAX_VALUE; // pin to right side
    } else {
      curX = Math.max(0, posX);
      curT = Math.max(0, Math.min(numTicks-1, (curX - slope/2) / tickWidth));
    }
    repaint(); // todo: partial repaint
	}

  public int getSignalCursor() {
    return curX == Integer.MAX_VALUE ? tickWidth * numTicks : curX;
  }

  public int getCurrentTick() {
    return curT == Integer.MAX_VALUE ? numTicks-1 : curT;
  }

  public void changeSpotlight(ChronoData.Signal oldSignal, ChronoData.Signal newSignal) {
    if (oldSignal != null) {
      Waveform w = rows.get(oldSignal.idx);
      w.flush();
      repaint(w.getBounds());
    }
    if (newSignal != null) {
      Waveform w = rows.get(newSignal.idx);
      w.flush();
      repaint(w.getBounds());
    }
  }

  public void updateSelected(int firstIdx, int lastIdx) {
    for (int i = firstIdx; i <= lastIdx; i++) {
      Waveform w = rows.get(i);
      boolean selected = selectionModel.isSelectedIndex(i);
      if (selected != w.selected) {
        w.selected = selected;
        w.flush();
        repaint(w.getBounds());
      }
    }
  }

  public void flushWaveforms() {
    for (Waveform w : rows)
      w.flush();
  }

  private static final Font MSG_FONT = new Font("Serif", Font.ITALIC, 12);

  @Override
  public void paintComponent(Graphics gr) {
    Graphics2D g = (Graphics2D)gr;
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, getWidth(), getHeight()); // entire viewport, not just (width, height)
    g.setColor(Color.BLACK);
    if (rows.size() == 0) {
      Font f = g.getFont();
      g.setFont(MSG_FONT);
      String lines = S.get("NoSignalsSelected");
      int x = 15, y = 15;
      for (String s : lines.split("\\|")) {
        g.drawString(s.trim(), x, y);
        y += 14;
      }
      g.setFont(f);
      return;
    }
    for (Waveform w : rows)
      w.paintWaveform(g);
    int h = ChronoPanel.HEADER_HEIGHT;
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(0, 0, width, ChronoPanel.HEADER_HEIGHT);
    paintCursor(g);
    paintTimeline(g);
  }

  public void paintTimeline(Graphics2D g) {
    int h = ChronoPanel.HEADER_HEIGHT;
    g.setColor(Color.BLACK);
    g.drawLine(0, h, width, h);
    for (int x = 0; x < width; x += tickWidth)
      g.drawLine(x, h*2/3, x, h);
    // todo: later
    // int minimalWidthToDisp = 60;
    // int nbrTick = 0;
    // int lastDispPos = -minimalWidthToDisp;

    // if (clk != null) {
    //   for (int i = 1; i < clk.getSignalValues().size(); ++i) {
    //     // is it a clk rising edge ?
    //     if (clk.getSignalValues().get(i - 1).equals("0")
    //         && clk.getSignalValues().get(i).equals("1")) {

    //       // is there enough place to display the text?
    //       if ((i - 1) * tickWidth - lastDispPos > minimalWidthToDisp) {
    //         lastDispPos = (i - 1) * tickWidth;
    //         g2.setStroke(new BasicStroke(2));
    //         g2.drawLine(lastDispPos, 6, lastDispPos, 12);
    //         g2.setStroke(new BasicStroke(1));
		// DecimalFormat df = new DecimalFormat("#.##");
		// return df.format(i/10.0) + " ms";
    //         g.drawString(getTimeString(nbrTick), lastDispPos + 3,
    //             20);
    //       }
    //       nbrTick++;
    //     }
    //   }
    // }
  }


	private void paintCursor(Graphics2D g) {
    int pos = getSignalCursor();
		g.setStroke(new BasicStroke(1));
		g.setPaint(Color.RED);
		g.drawLine(pos, getHeight(), pos, 0);
	}

	private class MyListener extends MouseAdapter {
    boolean shiftDrag, controlDrag, subtracting;

    ChronoData.Signal getSignal(int y, boolean force) {
      int idx = (y - ChronoPanel.HEADER_HEIGHT) / WAVE_HEIGHT;
      int n = data.getSignalCount();
      if (idx < 0 && force)
        idx = 0;
      else if (idx >= n && force)
        idx = n - 1;
      return (idx < 0 || idx >= n) ? null : data.getSignal(idx);
    }

		@Override
		public void mouseMoved(MouseEvent e) {
      chronoPanel.changeSpotlight(getSignal(e.getY(), false));
		}

		@Override
		public void mouseEntered(MouseEvent e) {
      chronoPanel.changeSpotlight(getSignal(e.getY(), false));
		}

		@Override
		public void mouseExited(MouseEvent e) {
      chronoPanel.changeSpotlight(null);
		}

		@Override
		public void mousePressed(MouseEvent e) {
      if (SwingUtilities.isLeftMouseButton(e)) {
        chronoPanel.setSignalCursor(e.getX());
        ChronoData.Signal signal = getSignal(e.getY(), false);
        if (signal == null) {
          shiftDrag = controlDrag = subtracting = false;
          return;
        }
        shiftDrag = e.isShiftDown();
        controlDrag = !shiftDrag && e.isControlDown();
        subtracting = controlDrag && selectionModel.isSelectedIndex(signal.idx);
        selectionModel.setValueIsAdjusting(true);
        if (shiftDrag) {
          if (selectionModel.getAnchorSelectionIndex() < 0)
            selectionModel.setAnchorSelectionIndex(0);
          selectionModel.setLeadSelectionIndex(signal.idx);
        } else if (controlDrag) {
          if (subtracting)
            selectionModel.removeSelectionInterval(signal.idx, signal.idx);
          else
            selectionModel.addSelectionInterval(signal.idx, signal.idx);
        } else {
          selectionModel.setSelectionInterval(signal.idx, signal.idx);
        }
      }
		}

		@Override
		public void mouseDragged(MouseEvent e) {
      chronoPanel.changeSpotlight(getSignal(e.getY(), false));
      if (SwingUtilities.isLeftMouseButton(e)) {
        chronoPanel.setSignalCursor(e.getX());
        if (!selectionModel.getValueIsAdjusting())
          return;
        ChronoData.Signal signal = getSignal(e.getY(), false);
        if (signal == null)
          return;
        selectionModel.setLeadSelectionIndex(signal.idx);
      }
		}

		@Override
		public void mouseReleased(MouseEvent e) {
      if (SwingUtilities.isLeftMouseButton(e)) {
        if (!selectionModel.getValueIsAdjusting())
          return;
        ChronoData.Signal signal = getSignal(e.getY(), true);
        if (signal == null)
          return;
        int idx = selectionModel.getAnchorSelectionIndex();
        if (idx < 0) {
          idx = signal.idx;
          selectionModel.setAnchorSelectionIndex(signal.idx);
        }
        selectionModel.setLeadSelectionIndex(signal.idx);
        shiftDrag = controlDrag = subtracting = false;
        selectionModel.setValueIsAdjusting(false);
      }
    }

		@Override
		public void mouseClicked(MouseEvent e) {
      if (SwingUtilities.isRightMouseButton(e)) {
        List<ChronoData.Signal> signals = chronoPanel.getLeftPanel().getSelectedValuesList();
        if (signals.size() == 0) {
          ChronoData.Signal signal = getSignal(e.getY(), false);
          if (signal == null)
            return;
          signals.add(signal);
          PopupMenu m = new PopupMenu(chronoPanel, signals);
          m.doPop(e);
        }
      }
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
      zoom(e.getWheelRotation() > 0 ? -1 : +1, e.getPoint().x);
		}
	}

  private class Waveform {

    private static final int HIGH = ChronoPanel.GAP;
    private static final int LOW = WAVE_HEIGHT - ChronoPanel.GAP;
    private static final int MID = WAVE_HEIGHT / 2;

    final ChronoData.Signal signal;
    private BufferedImage buf;
    boolean selected;

    public Waveform(ChronoData.Signal s) {
      this.signal = s;
    }

    Rectangle getBounds() {
      int y = ChronoPanel.HEADER_HEIGHT + WAVE_HEIGHT * signal.idx;
      return new Rectangle(0, y, width, WAVE_HEIGHT);
    }

    private void drawSignal(Graphics2D g, boolean bold, Color bg, Color fg) {
      g.setStroke(new BasicStroke(bold ? 2 : 1));

      int t = 0;

      String max = signal.getFormattedMaxValue();
      String min = signal.getFormattedMinValue();
      String prec = signal.getFormattedValue(t);

      int x = 0;
      while (t < numTicks) {
        String suiv = signal.getFormattedValue(t++);

        if (suiv.equals("-")) {
          x += tickWidth;
          continue;
        }

        if (suiv.contains("E")) {
          g.setColor(Color.red);
          g.drawLine(x, HIGH, x + tickWidth, MID);
          g.drawLine(x, MID, x + tickWidth, HIGH);
          g.drawLine(x, MID, x + tickWidth, LOW);
          g.drawLine(x, LOW, x + tickWidth, MID);
          g.setColor(Color.BLACK);
        } else if (suiv.contains("x")) {
          g.setColor(Color.blue);
          g.drawLine(x, HIGH, x + tickWidth, MID);
          g.drawLine(x, MID, x + tickWidth, HIGH);
          g.drawLine(x, MID, x + tickWidth, LOW);
          g.drawLine(x, LOW, x + tickWidth, MID);
          g.setColor(Color.BLACK);
        } else if (suiv.equals(min)) {
          if (!prec.equals(min)) {
            if (slope > 0) {
              g.setColor(fg);
              g.fillPolygon(
                  new int[] { x, x + slope, x },
                  new int[] { HIGH, LOW+1, LOW+1 },
                  3);
              g.setColor(Color.BLACK);
            }
            g.drawLine(x, HIGH, x + slope, LOW);
            g.drawLine(x + slope, LOW, x + tickWidth, LOW);
          } else {
            g.drawLine(x, LOW, x + tickWidth, LOW);
          }
        } else if (suiv.equals(max)) {
          if (!prec.equals(max)) {
            g.setColor(fg);
            g.fillPolygon(
                new int[] { x, x + slope, x + tickWidth + 1, x + tickWidth + 1},
                new int[] { LOW+1, HIGH, HIGH, LOW+1 },
                4);
            g.setColor(Color.BLACK);
            g.drawLine(x, LOW, x + slope, HIGH);
            g.drawLine(x + slope, HIGH, x + tickWidth, HIGH);
          } else {
            g.setColor(fg);
            g.fillRect(x, HIGH, tickWidth + 1, LOW - HIGH + 1);
            g.setColor(Color.BLACK);
            g.drawLine(x, HIGH, x + tickWidth, HIGH);
          }
        } else {
          if (suiv.equals(prec)) {
            g.drawLine(x, LOW, x + tickWidth, LOW);
            g.drawLine(x, HIGH, x + tickWidth, HIGH);
            if (t == 1) // first segment also gets a label
              g.drawString(suiv, x + 2, MID);
          } else {
            g.drawLine(x, LOW, x + slope, HIGH);
            g.drawLine(x, HIGH, x + slope, LOW);
            g.drawLine(x + slope, HIGH, x + tickWidth, HIGH);
            g.drawLine(x + slope, LOW, x + tickWidth, LOW);
            g.drawString(suiv, x + tickWidth, MID);
          }
        }

        prec = suiv;
        x += tickWidth;
      }
    }

    private void createOffscreen() {
      buf = (BufferedImage)createImage(width, WAVE_HEIGHT);
      Graphics2D g = buf.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
          RenderingHints.VALUE_STROKE_DEFAULT);
      boolean bold = data.getSpotlight() ==  signal;
      Color[] colors = chronoPanel.rowColors(signal.info, selected);
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, width, ChronoPanel.GAP-1);
      g.fillRect(0, LOW, width, ChronoPanel.GAP-1);
      g.setColor(colors[0]);
      g.fillRect(0, HIGH, width, LOW - HIGH);
      g.setColor(Color.BLACK);
      drawSignal(g, bold, colors[0], colors[1]);
      g.dispose();
    }

    public void paintWaveform(Graphics2D g) {
      if (buf == null) // todo: reallocating image each time seems silly
        createOffscreen();
      int y = ChronoPanel.HEADER_HEIGHT + WAVE_HEIGHT * signal.idx;
      g.drawImage(buf, null, 0, y);
    }

    public void flush() {
      buf = null;
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
    //    for (Waveform sDraw : rows) {
    //      sDraw.flush();
    //      sDraw.repaint();
    //    }
  }
}
