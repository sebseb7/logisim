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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;

import javax.swing.JPanel;

// A single-bit or multi-bit waveform display
public class Waveform extends JPanel {

	private class MyListener extends MouseAdapter {
		@Override
		public void mouseDragged(MouseEvent e) {
			int x = Math.min(getWidth() - 1, Math.max(0, e.getX()));
			chronoPanel.mouseDragged(signal, x);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			chronoPanel.mouseEntered(signal);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			chronoPanel.mouseExited(signal);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			int x = e.getX() >= 0 ? e.getX() : 0;
			chronoPanel.mousePressed(signal, x);
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (e.getWheelRotation() > 0)
				chronoPanel.zoom(signal, -1, e.getPoint().x);
			else
				chronoPanel.zoom(signal, 1, e.getPoint().x);
		}
	}

	public static final Color LIGHTPINK = new Color(0xff, 0xcc, 0xe6);
	public static final Color DARKPINK = new Color(0xff, 0x99, 0xcc);
	private static final Color LIGHTGRAY = new Color(180, 180, 180);
	private static final Color DARKGRAY = new Color(140, 140, 140);
  private static final int HEIGHT = ChronoPanel.SIGNAL_HEIGHT;
  public static final int GAP = 6;
  private static final int HIGH = GAP;
  private static final int LOW = HEIGHT - GAP;
  private static final int MID = HEIGHT / 2;

	private int tickWidth, numTicks, width;
	private int slope;

	private BufferedImage buf;

	private ChronoPanel chronoPanel;
	private ChronoData data;
	private RightPanel rightPanel;
	private ChronoData.Signal signal;

	private MyListener myListener = new MyListener();

	public Waveform(ChronoPanel p, RightPanel r, ChronoData.Signal s,
      int tickWidth, int numTicks, int width) {
		chronoPanel = p;
		signal = s;
		rightPanel = r;
    data = chronoPanel.getChronoData();

    setOpaque(true);
		setBackground(Color.MAGENTA);
		setDoubleBuffered(true);

		addMouseListener(myListener);
		addMouseMotionListener(myListener);
		addMouseWheelListener(myListener);
		addMouseListener(new PopupMenu(chronoPanel, Collections.singletonList(s)));

    update(tickWidth, numTicks, width);
	}

  public ChronoData.Signal getSignal() {
    return signal;
  }

	private void drawSignal(Graphics2D g, boolean bold, Color bg, Color fg) {
		g.setStroke(new BasicStroke(bold ? 2 : 1));

    // float xOff = rightPanel.getCurrentPosition();
    // float f = xOff / width;
		int t = 0; // (int)Math.round(numTicks * f); // huh ?

    String max = signal.getFormattedMaxValue();
    String min = signal.getFormattedMinValue();
		String prec = signal.getFormattedValue(t);

		int x = 0;
		while (t < numTicks) { //  && x < xOff + getVisibleRect().width + (10 * tickWidth)) {
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

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    // todo: reallocating image each time seems silly
    if (buf == null) {
      buf = (BufferedImage)createImage(width, HEIGHT);
      Graphics2D gb = buf.createGraphics();
      gb.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
          RenderingHints.VALUE_STROKE_DEFAULT);
      gb.setColor(Color.WHITE);
      boolean bold = data.getSpotlight() ==  signal;
      Color bg = bold ? LIGHTPINK : LIGHTGRAY;
      Color fg = bold ? DARKPINK : DARKGRAY;
      gb.fillRect(0, 0, width, GAP-1);
      gb.fillRect(0, LOW, width, GAP-1);
      gb.setColor(bg);
      gb.fillRect(0, HIGH, width, LOW - HIGH);
      gb.setColor(Color.BLACK);
      drawSignal(gb, bold, bg, fg);
      gb.dispose();
    }
    g2.drawImage(buf, null, rightPanel.getDisplayOffsetX(), 0); // always 0, 0 ?
	}

	public void flush() {
		buf = null;
	}

	public void update(int tickWidth, int numTicks, int width) {
    this.tickWidth = tickWidth;
    this.numTicks = numTicks;
    this.width = width;
		slope = (tickWidth < 12) ? tickWidth / 3 : 4;
		setMaximumSize(new Dimension(width, HEIGHT));
		setPreferredSize(new Dimension(width, HEIGHT));
		flush();
	}
}
