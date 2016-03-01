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

package com.cburch.logisim.util;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;

public class GraphicsUtil {
	static public void drawArrow(Graphics g, int x0, int y0, int x1, int y1,
			int headLength, int headAngle) {
		double offs = headAngle * Math.PI / 180.0;
		double angle = Math.atan2(y0 - y1, x0 - x1);
		int[] xs = { x1 + (int) (headLength * Math.cos(angle + offs)), x1,
				x1 + (int) (headLength * Math.cos(angle - offs)) };
		int[] ys = { y1 + (int) (headLength * Math.sin(angle + offs)), y1,
				y1 + (int) (headLength * Math.sin(angle - offs)) };
		g.drawLine(x0, y0, x1, y1);
		g.drawPolyline(xs, ys, 3);
	}

	static public void drawCenteredArc(Graphics g, int x, int y, int r,
			int start, int dist) {
		g.drawArc(x - r, y - r, 2 * r, 2 * r, start, dist);
	}

	static public void drawCenteredText(Graphics g, String text, int x, int y) {
		drawText(g, text, x, y, H_CENTER, V_CENTER);
	}

        // Returns point on the baseline at specified character position.
        static public Point getTextPoint(FontMetrics fm, String text,
                    int x, int y, int pos, int halign, int valign) {
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int height = fm.getHeight();
                Rectangle r = getTextBounds(fm, text, x, y, halign, valign);
                x = (int)r.getX();
                y = (int)r.getY();
                if (pos > 0)
                    x += fm.stringWidth(text.substring(0, pos));
                return new Point(x, y + ascent);
        }

        static public int getTextPosition(FontMetrics fm, String text,
                    int x, int y, int halign, int valign) {
	        Rectangle r = getTextBounds(fm, text, 0, 0, halign, valign);
		x -= (int)r.getX();
		int last = 0;
		for (int i = 0; i < text.length(); i++) {
			int cur = fm.stringWidth(text.substring(0, i + 1));
			if (x <= (last + cur) / 2) {
				return i;
			}
			last = cur;
		}
                return text.length();
        }

	static public void drawText(Graphics g, Font font, String text, int x,
			int y, int halign, int valign) {
		Font oldfont = g.getFont();
		if (font != null)
			g.setFont(font);
		drawText(g, text, x, y, halign, valign);
		if (font != null)
			g.setFont(oldfont);
	}

	static public void drawText(Graphics g, String text, int x, int y,
			int halign, int valign) {
		if (text.length() == 0)
			return;
		Rectangle bd = getTextBounds(g, text, x, y, halign, valign);
		g.drawString(text, bd.x, bd.y + g.getFontMetrics().getAscent());
	}

	static public Rectangle getTextBounds(Graphics g, Font font, String text,
			int x, int y, int halign, int valign) {
		if (g == null)
			return new Rectangle(x, y, 0, 0);
                FontMetrics fm = (font == null ? g.getFontMetrics() : g.getFontMetrics(font));
                return getTextBounds(fm, text, x, y, halign, valign);
	}

	static public Rectangle getTextBounds(Graphics g, String text, int x,
			int y, int halign, int valign) {
            if (g == null)
                return new Rectangle(x, y, 0, 0);
            else
                return getTextBounds(g.getFontMetrics(), text, x, y, halign, valign);
        }

	static public Rectangle getTextBounds(FontMetrics mets, String text, int x,
			int y, int halign, int valign) {
		int width = mets.stringWidth(text);
		int ascent = mets.getAscent();
		int descent = mets.getDescent();
		int height = ascent + descent;

		Rectangle ret = new Rectangle(x, y, width, height);
		switch (halign) {
		case H_CENTER:
			ret.translate(-(width / 2), 0);
			break;
		case H_RIGHT:
			ret.translate(-width, 0);
			break;
		default:
			;
		}
		switch (valign) {
		case V_TOP:
			break;
		case V_CENTER:
			ret.translate(0, -(ascent / 2));
			break;
		case V_CENTER_OVERALL:
			ret.translate(0, -(height / 2));
			break;
		case V_BASELINE:
			ret.translate(0, -ascent);
			break;
		case V_BOTTOM:
			ret.translate(0, -height);
			break;
		default:
			;
		}
		return ret;
	}

        static String TAB = "    "; // TAB = four spaces

        static public int tabStringWidth(FontMetrics fm, String text) {
               String segments[] = text.split("\t", -1);
               if (segments.length == 0)
                   return 0;
               if (segments.length == 1)
                   return fm.stringWidth(segments[0]);
               int w = (segments.length - 1) * fm.stringWidth(TAB);
               for (String s : segments)
                        w += fm.stringWidth(s);
               return w;
        }

        // Returns point on the baseline at specified character position.
        static public Point getTextPoint(FontMetrics fm, String text[],
                    int x, int y, int pos, int halign, int valign) {
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int height = fm.getHeight();
                Rectangle r = getTextBounds(fm, text, x, y, halign, valign);
		int width = (int)r.getWidth();
                // "ab\nc\n" --> 0 a 1 b 2 \n 3 \n 4 c 5 \n 6
                x = (int)r.getX();
                y = (int)r.getY();
                for (String line: text) {
                    if (pos > line.length()) {
                        pos -= line.length() + 1;
                        y += height;
                        continue;
                    }
                    int linewidth = tabStringWidth(fm, line);
                    switch (halign) {
                        case GraphicsUtil.H_CENTER:
                            x += (width - linewidth)/2;
                            break;
                        case GraphicsUtil.H_RIGHT:
                            x += (width - linewidth);
                            break;
                    }
                    if (pos > 0)
                        x += tabStringWidth(fm, line.substring(0, pos));
                    return new Point(x, y + ascent);
                }
                return null;
        }

        static public int getTextPosition(FontMetrics fm, String text[],
                    int x, int y, int halign, int valign) {
	        Rectangle r = getTextBounds(fm, text, 0, 0, halign, valign);
		x -= (int)r.getX();
                y -= (int)r.getY();
                int pos = 0;
		int height = fm.getHeight();
                for (String line : text) {
                    if (y >= height) {
                        y -= height;
                        pos += line.length() + 1;
                        continue;
                    }
                    int last = 0;
                    for (int i = 0; i < line.length(); i++) {
                            int cur = tabStringWidth(fm, line.substring(0, i + 1));
                            if (x <= (last + cur) / 2) {
                                    return pos + i;
                            }
                            last = cur;
                    }
                    return pos + line.length();
                }
                return pos - 1;
        }

	static public void drawText(Graphics g, Font font, String text[], int x,
			int y, int halign, int valign) {
		Font oldfont = g.getFont();
		if (font != null)
			g.setFont(font);
		drawText(g, text, x, y, halign, valign);
		if (font != null)
			g.setFont(oldfont);
	}

	static public void drawText(Graphics g, String text[], int x, int y,
			int halign, int valign) {
		if (text.length == 0)
                        return;
		Rectangle bds = getTextBounds(g, text, x, y, halign, valign);
                FontMetrics mets = g.getFontMetrics();
                int ascent = mets.getAscent();
                int descent = mets.getDescent();
                int height = mets.getHeight();
                y = bds.y + ascent;
                for (String line : text) {
                    int linewidth = tabStringWidth(mets, line);
                    switch (halign) {
                        case H_CENTER:
                            x = bds.x + (bds.width - linewidth)/2;
                            break;
                        case H_RIGHT:
                            x = bds.x + (bds.width - linewidth);
                            break;
                        default:
                            x = bds.x;
                    }
                    for (String s : line.split("\t", -1)) {
                        g.drawString(s, x, y);
                        x += mets.stringWidth(s) + mets.stringWidth(TAB);
                    }
                    y += height;
                }
	}

	static public Rectangle getTextBounds(Graphics g, String text[], int x,
			int y, int halign, int valign) {
            if (g == null)
                return new Rectangle(x, y, 0, 0);
            else
                return getTextBounds(g.getFontMetrics(), text, x, y, halign, valign);
        }

	static public Rectangle getTextBounds(Graphics g, Font font, String text[],
			int x, int y, int halign, int valign) {
		if (g == null)
			return new Rectangle(x, y, 0, 0);
                FontMetrics fm = (font == null ? g.getFontMetrics() : g.getFontMetrics(font));
                return getTextBounds(fm, text, x, y, halign, valign);
	}

	static public Rectangle getTextBounds(FontMetrics mets, String text[], int x,
			int y, int halign, int valign) {
            int n = text.length;
            if (n == 0)
                return getTextBounds(mets, "", x, y, halign, valign);
            int width = tabStringWidth(mets, text[0]);
            for (int i = 1; i < n; i++) {
                int w = tabStringWidth(mets, text[i]);
                if (w > width)
                    width = w;
            }
            int ascent = mets.getAscent();
            int descent = mets.getDescent();
            int height = ascent + descent;
            if (n > 1)
                height += (n-1) * mets.getHeight();

            Rectangle ret = new Rectangle(x, y, width, height);
            switch (halign) {
            case H_CENTER:
                    ret.translate(-(width / 2), 0);
                    break;
            case H_RIGHT:
                    ret.translate(-width, 0);
                    break;
            default:
                    ;
            }
            switch (valign) {
            case V_TOP:
                    break;
            case V_CENTER:
            case V_CENTER_OVERALL:
                    ret.translate(0, -(height / 2));
                    break;
            case V_BASELINE:
                    ret.translate(0, -ascent);
                    break;
            case V_BOTTOM:
                    ret.translate(0, -height);
                    break;
            default:
                    ;
            }
            return ret;
        }

	static public void switchToWidth(Graphics g, int width) {
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setStroke(new BasicStroke((float) width));
		}
	}

	public static final int H_LEFT = -1;

	public static final int H_CENTER = 0;

	public static final int H_RIGHT = 1;
	public static final int V_TOP = -1;

	public static final int V_CENTER = 0;
	public static final int V_BASELINE = 1;
	public static final int V_BOTTOM = 2;

	public static final int V_CENTER_OVERALL = 3;
}
