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

package com.cburch.logisim.gui.generic;
import static com.cburch.logisim.tools.Strings.S;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.cburch.logisim.gui.main.Canvas;

public class Callout extends JPanel {

  private static final Color bgColor = new Color(255, 255, 150);
  private static final int MARGIN = 5;
  private static final int N = 1;
  private static final int W = 2;
  public static final int SE = 0;
  public static final int NE = N;
  public static final int SW = W;
  public static final int NW = N|W;
  public static int DURATION = 5000;

  JLabel label;
  int pos;

  public Callout(String text) {
    setLayout(null);
    label = new JLabel("<html>"+text+"</html>");
    label.setBackground(bgColor);
    add(label);
    label.setFont(label.getFont().deriveFont(Font.PLAIN));
    Dimension d = label.getPreferredSize();
    label.setBounds(MARGIN, MARGIN, d.width, d.height);
    // todo: hide when clicked?
  }

  @Override
  public void paintComponent(Graphics g) {
    Color c = g.getColor();
    int w = getWidth()-1;
    int h = getHeight()-1;
    g.setColor(bgColor);

    int[] xp, yp;
    if (pos == NE) {
      xp = new int[] { 0,    0, w, w,    30,   0, 15   };
      yp = new int[] { h-15, 0, 0, h-15, h-15, h, h-15 };
    } else if (pos == NW) {
      xp = new int[] { 0,    0, w, w,    w-15, w, w-30 };
      yp = new int[] { h-15, 0, 0, h-15, h-15, h, h-15 };
    } else if (pos == SE) {
      xp = new int[] { 0,  0, w, w,  30, 0, 15 };
      yp = new int[] { 15, h, h, 15, 15, 0, 15 };
    } else { // SW
      xp = new int[] { 0,  0, w, w,  w-15, w, w-30 };
      yp = new int[] { 15, h, h, 15, 15,   0, 15   };
    }
    g.fillPolygon(xp, yp, xp.length);
    g.setColor(Color.BLACK);
    g.drawPolygon(xp, yp, xp.length);
    g.setColor(c);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension d = label.getPreferredSize();
    return new Dimension(d.width+2*MARGIN, d.height+2*MARGIN+15);
  }

  @Override
  public boolean isOpaque() {
    return false;
  }

  public void show(Canvas parent, int x, int y, int pos) {
    pos = pos & (N|W);
    Dimension dl = label.getPreferredSize();
    Dimension d = getPreferredSize();
    int w = d.width, h = d.height;

    Rectangle r = parent.getViewableRect();
    if ((pos & W) != 0 && (x-w) < r.x && (x+w) <= r.x+r.width)
      pos = pos^W;
    else if ((pos & W) == 0 && (x+w) > r.x+r.width && (x-w) >= r.x)
      pos = pos^W;

    if ((pos & N) != 0 && (y-h) < r.y && (y+h) <= r.y+r.height)
      pos = pos^N;
    else if ((pos & N) == 0 && (y+h) > r.y+r.height && (y-h) >= r.y)
      pos = pos^N;

    this.pos = pos;

    switch (pos) {
    case NE:
      label.setBounds(MARGIN, MARGIN, dl.width, dl.height);
      y -= h;
      break;
    case SE:
      label.setBounds(MARGIN, MARGIN+15, dl.width, dl.height);
      break;
    case NW:
      label.setBounds(MARGIN, MARGIN, dl.width, dl.height);
      y -= h;
      x -= w;
      break;
    default: // SW
      label.setBounds(MARGIN, MARGIN+15, dl.width, dl.height);
      x -= w;
      break;
    }
    setBounds(x, y, w, h);
    parent.add(this);
    parent.repaint(x-1, y-1, w+2, h+2);
  }

  public void show(Canvas parent, int x, int y, int pos, int duration) {
    show(parent, x, y, pos);
    Timer timer = new Timer(duration, new ActionListener() {
      public void actionPerformed(ActionEvent e) { hide(); }
    });
    timer.setRepeats(false);
    timer.start();
  }

  public void hide() {
    Container parent = getParent();
    if (parent != null) {
      parent.remove(this);
      Rectangle r = getBounds();
      parent.repaint(r.x-1, r.y-1, r.width+2, r.height+2);
    }
  }
}
