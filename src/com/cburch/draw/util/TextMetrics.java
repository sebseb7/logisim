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

package com.cburch.draw.util;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.FontMetrics;

public class TextMetrics {

	public int ascent;
	public int descent;
	public int leading;
	public int height; // = ascent + height + leading
	public int width; // valid only if constructor was given a string

	public TextMetrics(Graphics g) {
		this(g, null, null);
	}

	public TextMetrics(Graphics g, String text) {
		this(g, null, text);
	}

	public TextMetrics(Graphics g, Font font) {
		this(g, font, null);
	}

	public TextMetrics(Graphics g, Font font, String text) {
		if (g == null) {
			throw new IllegalStateException("need g");
		}
		if (font == null)
			font = g.getFont();
		FontRenderContext fr = ((Graphics2D)g).getFontRenderContext();

		if (text == null) {
			text = "ÄAy";
			width = 0;
		} else {
			width = (int)font.getStringBounds(text, fr).getWidth();
		}

		LineMetrics lm = font.getLineMetrics(text, fr);
		ascent = (int)Math.ceil(lm.getAscent());
		descent = (int)Math.ceil(lm.getDescent());
		leading = (int)Math.ceil(lm.getLeading());
		height = ascent + descent + leading; // (int)Math.ceil(lm.getHeight());
	
		// sanity checks...
		/*
		int fa = g.getFontMetrics(font).getAscent();
		int fd = g.getFontMetrics(font).getDescent();
		int fh = g.getFontMetrics(font).getHeight();
		if (fa != ascent || fd != descent || fh != height)
			System.out.println(
					"  a: " + ascent + " vs " + fa +
					"  d: " + descent + " vs " + fd +
					"  h: " + height + " vs " + fh);
		*/
	}

	private static Canvas canvas = new Canvas();

	public TextMetrics(Component c, Font font, String text) {
		if (c == null)
			c = canvas;
		if (font == null)
			font = c.getFont();
		FontMetrics fm = c.getFontMetrics(font);
		width = (text != null ? fm.stringWidth(text) : 0);
		ascent = fm.getAscent();
		descent = fm.getDescent();
		leading = 0;
		height = ascent + descent + leading;

		// sanity checks...
		/*
		if (width <= 0 || ascent <= 0 || height <= 0)
			System.out.println("  a: " + ascent + " d: " + descent + " h: " + height);
		*/
	}

}
