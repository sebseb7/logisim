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

package com.cburch.logisim.comp;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.data.Bounds;
import com.cburch.draw.util.TextMetrics;

class TextFieldMultilineCaret extends TextFieldCaret {
  private TextFieldMultiline field;

  public TextFieldMultilineCaret(TextFieldMultiline field, Graphics g, int pos) {
    super(field, g, pos);
    this.field = field;
  }

  public TextFieldMultilineCaret(TextFieldMultiline field, Graphics g, int x, int y) {
    this(field, g, 0);
    pos = end = findCaret(x, y);
  }

  public void draw(Graphics g) {
    String lines[] = curText.split("\n", -1); // keep blank lines at end
    int x = field.getX();
    int y = field.getY();
    int halign = field.getHAlign();
    int valign = field.getVAlign();
    Font font = field.getFont();
    if (font != null)
      g.setFont(font);

    // draw boundary
    Bounds box = getBounds(g);
    g.setColor(EDIT_BACKGROUND);
    g.fillRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
    g.setColor(EDIT_BORDER);
    g.drawRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());

    // draw selection
    if (pos != end) {
      g.setColor(SELECTION_BACKGROUND);
      Rectangle p = GraphicsUtil.getTextCursor(g, font, lines, x, y, pos < end ? pos : end, halign, valign);
      Rectangle e = GraphicsUtil.getTextCursor(g, font, lines, x, y, pos < end ? end : pos, halign, valign);
      if (p.y == e.y) {
        g.fillRect(p.x, p.y - 1, e.x - p.x + 1, e.height + 2);
      } else {
        int lx = box.getX()+3;
        int rx = box.getX()+box.getWidth()-3;
        g.fillRect(p.x, p.y - 1, rx - p.x + 1, (e.y - p.y) + 1);
        g.fillRect(lx, p.y + e.height, rx - lx + 1, (e.y - p.y) - e.height);
        g.fillRect(lx, p.y + e.height, e.x - lx + 1, (e.y - p.y) + 1);
      }
    }

    // draw text
    g.setColor(Color.BLACK);
    GraphicsUtil.drawText(g, lines, x, y, halign, valign);

    // draw cursor
    if (pos == end) {
      Rectangle p = GraphicsUtil.getTextCursor(g, font, lines, x, y, pos, halign, valign);
      if (p != null)
        g.drawLine(p.x, p.y, p.x, p.y + p.height);
    }
  }

  public Bounds getBounds(Graphics g) {
    String lines[] = curText.split("\n", -1); // keep blank lines at end
    int x = field.getX();
    int y = field.getY();
    int halign = field.getHAlign();
    int valign = field.getVAlign();
    Font font = field.getFont();
    Bounds bds = Bounds.create(GraphicsUtil.getTextBounds(g, font, lines, x, y, halign, valign));
    Bounds box = bds.add(field.getBounds(g)).expand(3);
    return box;
  }

  protected void moveCaret(int dx, int dy, boolean shift, boolean ctrl) {
    if (!shift)
      normalizeSelection();

    if (dy != 0) {
      if (!shift && pos != end) {
        if (dx < 0) end = pos;
        else pos = end;
      }
      String lines[] = curText.split("\n", -1); // keep blank lines at end
      TextMetrics tm = new TextMetrics(g);
      int halign = field.getHAlign();
      int valign = field.getVAlign();
      Rectangle r = GraphicsUtil.getTextCursor(g, field.getFont(), lines, 0, 0, pos, halign, valign);
      if (r != null) {
        pos = GraphicsUtil.getTextPosition(g, field.getFont(), lines,
            r.x, r.y + tm.ascent + dy * tm.height, halign, valign);
      } else if (dy < 0) {
        pos = 0;
      } else {
        pos = curText.length();
      }
      // control up/down moves to start/end of previous/next line
      while (ctrl && dy < 0 && pos > 0 && curText.charAt(pos-1) != '\n')
        pos--;
      while (ctrl && dy > 0 && pos < curText.length()-1 && curText.charAt(pos) != '\n')
        pos++;
    } else if (pos+dx >= 0 && pos+dx <= curText.length()) {
      if (!shift && pos != end) {
        if (dx < 0) end = pos;
        else pos = end;
      } else {
        pos += dx;
      }
      while (ctrl && !wordBoundary(pos))
        pos += dx;
    }

    if (!shift)
      end = pos;
  }

  protected void normalKeyPressed(KeyEvent e, boolean shift) {
    if (e.getKeyCode() != KeyEvent.VK_ENTER)
      super.normalKeyPressed(e, shift);
  }

  protected boolean allowedCharacter(char c) {
    return (c != KeyEvent.CHAR_UNDEFINED)
        && (c == '\n' || c == '\t' || !Character.isISOControl(c));
  }

  protected int findCaret(int x, int y) {
    x -= field.getX();
    y -= field.getY();
    int halign = field.getHAlign();
    int valign = field.getVAlign();
    String lines[] = curText.split("\n", -1); // keep blank lines at end
    return GraphicsUtil.getTextPosition(g, field.getFont(), lines, x, y, halign, valign);
  }

}
