/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.comp;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;

import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.CaretEvent;
import com.cburch.logisim.tools.CaretListener;
import com.cburch.draw.util.TextMetrics;

class TextFieldMultilineCaret extends TextFieldCaret {
	private TextFieldMultiline field;

	public TextFieldMultilineCaret(TextFieldMultiline field, Graphics g, int pos) {
                super(field, g, pos);
                this.field = field;
	}

	public TextFieldMultilineCaret(TextFieldMultiline field, Graphics g, int x, int y) {
		this(field, g, 0);
		moveCaret(x, y);
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

		// draw text
                g.setColor(Color.BLACK);
                GraphicsUtil.drawText(g, lines, x, y, halign, valign);

		// draw cursor
		Rectangle p = GraphicsUtil.getTextCursor(g, lines, x, y, pos, halign, valign);
		if (p != null)
			g.drawLine(p.x, p.y, p.x, p.y + p.height);
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

	public void keyPressed(KeyEvent e) {
		int ign = InputEvent.ALT_MASK | InputEvent.CTRL_MASK
			| InputEvent.META_MASK;
		if ((e.getModifiers() & ign) != 0)
			return;
		String lines[];
		switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
			case KeyEvent.VK_KP_UP:
				lines = curText.split("\n", -1); // keep blank lines at end
				if (lines.length > 1 && pos > lines[0].length()) {
					TextMetrics tm = new TextMetrics(g);
					int halign = field.getHAlign();
					int valign = field.getVAlign();
					Rectangle r = GraphicsUtil.getTextCursor(g, lines, 0, 0, pos, halign, valign);
					if (r != null) {
						pos = GraphicsUtil.getTextPosition(g, lines,
								r.x, r.y + tm.ascent - tm.height, halign, valign);
					}
				}
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_KP_DOWN:
				lines = curText.split("\n", -1); // keep blank lines at end
				if (lines.length > 1 && pos < curText.length() - lines[lines.length-1].length()) {
					TextMetrics tm = new TextMetrics(g);
					int halign = field.getHAlign();
					int valign = field.getVAlign();
					Rectangle p = GraphicsUtil.getTextCursor(g, lines, 0, 0, pos, halign, valign);
					if (p != null) {
						pos = GraphicsUtil.getTextPosition(g, lines,
								p.x, p.y + tm.ascent + tm.height, halign, valign);
					}
				}
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_KP_LEFT:
				if (pos > 0)
					--pos;
				break;
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_KP_RIGHT:
				if (pos < curText.length())
					++pos;
				break;
			case KeyEvent.VK_HOME:
				pos = 0;
				break;
			case KeyEvent.VK_END:
				pos = curText.length();
				break;
			case KeyEvent.VK_ESCAPE:
			case KeyEvent.VK_CANCEL:
				cancelEditing();
				break;
			case KeyEvent.VK_CLEAR:
				curText = "";
				pos = 0;
				break;
			case KeyEvent.VK_BACK_SPACE:
				if (pos > 0) {
					curText = curText.substring(0, pos - 1)
						+ curText.substring(pos);
					--pos;
				}
				break;
			case KeyEvent.VK_DELETE:
				if (pos < curText.length()) {
					curText = curText.substring(0, pos)
						+ curText.substring(pos + 1);
				}
				break;
			case KeyEvent.VK_INSERT:
			case KeyEvent.VK_COPY:
			case KeyEvent.VK_CUT:
			case KeyEvent.VK_PASTE:
				// TODO: enhance label editing
				break;
			default:
				; // ignore
		}
	}

	public void keyTyped(KeyEvent e) {
		int ign = InputEvent.ALT_MASK | InputEvent.CTRL_MASK
				| InputEvent.META_MASK;
		if ((e.getModifiers() & ign) != 0)
			return;

		char c = e.getKeyChar();
		if (c == '\n' || c == '\t' || (c != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c))) {
			if (pos < curText.length()) {
				curText = curText.substring(0, pos) + c
						+ curText.substring(pos);
			} else {
				curText += c;
			}
			++pos;
		}
	}

	protected void moveCaret(int x, int y) {
		x -= field.getX();
		y -= field.getY();
		int halign = field.getHAlign();
		int valign = field.getVAlign();
		String lines[] = curText.split("\n", -1); // keep blank lines at end
		pos = GraphicsUtil.getTextPosition(g, lines, x, y, halign, valign);
	}

}
