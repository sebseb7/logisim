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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.LinkedList;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.util.GraphicsUtil;

public class TextField {
	public static final int H_LEFT = GraphicsUtil.H_LEFT;
	public static final int H_CENTER = GraphicsUtil.H_CENTER;
	public static final int H_RIGHT = GraphicsUtil.H_RIGHT;
	public static final int V_TOP = GraphicsUtil.V_TOP;
	public static final int V_CENTER = GraphicsUtil.V_CENTER;
	public static final int V_CENTER_OVERALL = GraphicsUtil.V_CENTER_OVERALL;
	public static final int V_BASELINE = GraphicsUtil.V_BASELINE;
	public static final int V_BOTTOM = GraphicsUtil.V_BOTTOM;

	protected int x;
	protected int y;
	protected int halign;
	protected int valign;
	protected Font font;
	protected String text = "";
	private LinkedList<TextFieldListener> listeners = new LinkedList<TextFieldListener>();

	public TextField(int x, int y, int halign, int valign) {
		this(x, y, halign, valign, null);
	}

	public TextField(int x, int y, int halign, int valign, Font font) {
		this.x = x;
		this.y = y;
		this.halign = halign;
		this.valign = valign;
		this.font = font;
	}

	//
	// listener methods
	//
	public void addTextFieldListener(TextFieldListener l) {
		listeners.add(l);
	}

	public void draw(Graphics g) {
                GraphicsUtil.drawText(g, font, text, x, y, halign, valign);
        }

	public void fireTextChanged(TextFieldEvent e) {
		for (TextFieldListener l : new ArrayList<TextFieldListener>(listeners)) {
			l.textChanged(e);
		}
	}

	public Bounds getBounds(Graphics g) {
                return Bounds.create(GraphicsUtil.getTextBounds(g, font, text, x, y, halign, valign));
	}

	public TextFieldCaret getCaret(Graphics g, int pos) {
		return new TextFieldCaret(this, g, pos);
	}

	public TextFieldCaret getCaret(Graphics g, int x, int y) {
		return new TextFieldCaret(this, g, x, y);
	}

	public Font getFont() {
		return font;
	}

	public int getHAlign() {
		return halign;
	}

	public String getText() {
		return text;
	}

	public int getVAlign() {
		return valign;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void removeTextFieldListener(TextFieldListener l) {
		listeners.remove(l);
	}

	public void setAlign(int halign, int valign) {
		this.halign = halign;
		this.valign = valign;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public void setHorzAlign(int halign) {
		this.halign = halign;
	}

	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void setLocation(int x, int y, int halign, int valign) {
		this.x = x;
		this.y = y;
		this.halign = halign;
		this.valign = valign;
	}

	public void setText(String text) {
		if (!text.equals(this.text)) {
			TextFieldEvent e = new TextFieldEvent(this, this.text, text);
			this.text = text;
			fireTextChanged(e);
		}
	}

	public void setVertAlign(int valign) {
		this.valign = valign;
	}

}
