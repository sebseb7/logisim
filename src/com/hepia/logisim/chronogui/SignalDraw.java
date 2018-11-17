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

import javax.swing.JPanel;

import com.hepia.logisim.chronodata.ChronoData;

// A single-bit or multi-bit waveform display
public class SignalDraw extends JPanel {

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

	private static final Color GRAY = new Color(180, 180, 180, 100);
  private static final int HEIGHT = ChronoPanel.SIGNAL_HEIGHT;
  private static final int HIGH = 6;
  private static final int LOW = HEIGHT - 6;
  private static final int MID = HEIGHT / 2;

	private boolean bold;
	private int width = 10;
	private int tickWidth;
	private int slope;

	private BufferedImage buf;

	private ChronoPanel chronoPanel;
	private ChronoData data;
	private RightPanel rightPanel;
	private ChronoData.Signal signal;

	private MyListener myListener = new MyListener();

	public SignalDraw(ChronoPanel p, RightPanel r, ChronoData.Signal s) {
		chronoPanel = p;
		signal = s;
		rightPanel = r;
    data = chronoPanel.getChronoData();

		tickWidth = rightPanel.getTickWidth();
		width = Math.max(10, tickWidth * data.getValueCount()); // fixme, even clock spacing

		slope = (tickWidth < 8) ? tickWidth / 3 : 5;

		setBackground(Color.WHITE);
		setMaximumSize(new Dimension(width, HEIGHT));
		setPreferredSize(new Dimension(width, HEIGHT));
		setDoubleBuffered(true);

		addMouseListener(myListener);
		addMouseMotionListener(myListener);
		addMouseWheelListener(myListener);
		addMouseListener(new PopupMenu(chronoPanel, s));
	}

	private void drawSignal(Graphics2D g) {
		g.setStroke(new BasicStroke(bold ? 2 : 1));

    float xOff = rightPanel.getCurrentPosition();
    float f = xOff / rightPanel.getSignalWidth();
    int tEnd = data.getValueCount();
		int t = Math.round(tEnd * f);

    String max = signal.getFormattedMaxValue();
    String min = signal.getFormattedMinValue();
		String prec = signal.getFormattedValue(t++);

		int x = 0;
		while (t < tEnd && x < xOff + getVisibleRect().width + (10 * tickWidth)) {
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
				g.setColor(Color.black);
			} else if (suiv.contains("x")) {
				g.setColor(Color.blue);
				g.drawLine(x, HIGH, x + tickWidth, MID);
				g.drawLine(x, MID, x + tickWidth, HIGH);
				g.drawLine(x, MID, x + tickWidth, LOW);
				g.drawLine(x, LOW, x + tickWidth, MID);
				g.setColor(Color.black);
			} else if (suiv.equals(min)) {
				g.drawLine(x, LOW, x + tickWidth, LOW);
        if (!prec.equals(min))
          g.drawLine(x, HIGH, x, LOW);
			} else if (suiv.equals(max)) {
				g.setColor(GRAY);
				g.fillRect(x + 1, HIGH, tickWidth, LOW - HIGH);
				g.setColor(Color.black);
				g.drawLine(x, HIGH, x + tickWidth, HIGH);
        if (!prec.equals(max))
          g.drawLine(x, LOW, x, HIGH);
      } else {
        if (t == 2) // first segment
          g.drawString(suiv, x + 2, MID);
        if (t == 2 || suiv.equals(prec)) {
          g.drawLine(x, LOW, x + tickWidth, LOW);
          g.drawLine(x, HIGH, x + tickWidth, HIGH);
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
      buf = (BufferedImage)createImage(rightPanel.getVisibleWidth() * 2, HEIGHT);
      Graphics2D gb = buf.createGraphics();
      gb.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
          RenderingHints.VALUE_STROKE_DEFAULT);
      drawSignal(gb);
      gb.dispose();
    }
    g2.drawImage(buf, null, rightPanel.getDisplayOffsetX(), 0);
	}

	public ChronoData.Signal getSignal() {
		return signal;
	}

  public void highlight(boolean enable) {
    if (bold != enable) {
      flush();
      bold = enable;
      this.repaint();
    }
	}

	public void flush() {
		buf = null;
	}

	public void setTickWidth(int w) {
		flush();
		tickWidth = w;
		slope = (tickWidth < 8) ? tickWidth / 3 : 5;
		width = Math.max(10, tickWidth * data.getValueCount()); // fixme, even clock spacing
		setMaximumSize(new Dimension(width, HEIGHT));
		setPreferredSize(new Dimension(width, HEIGHT));
	}
}
