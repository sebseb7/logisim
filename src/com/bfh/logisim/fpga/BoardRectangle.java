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

package com.bfh.logisim.fpga;

public class BoardRectangle {

	public final int x, y, width, height;

	public BoardRectangle(int x, int y, int w, int h) {
    this.x = w < 0 ? x+w : x;
    this.y = h < 0 ? y+h : y;
    width = w < 0 ? -w : w;
    height = h < 0 ? -h : h;
	}

	public boolean overlaps(BoardRectangle other) {
    if (x >= other.x+other.width || other.x >= x+width) // side by side
      return false;
    if (y >= other.y+other.height || other.y >= y+height) // above and below
      return false;
    return true;
  }

	public boolean contains(int px, int py) {
		return x <= px && px <= x+width && y <= py && py <= y+height;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BoardRectangle))
			return false;
		BoardRectangle other = (BoardRectangle)o;
		return x == other.x && y == other.y && width == other.width && height == other.height;
	}

  @Override
  public int hashCode() {
    // This appears to be a standard 
     519:     long l = java.lang.Double.doubleToLongBits(getX())
 520:       + 37 * java.lang.Double.doubleToLongBits(getY())
 521:       + 43 * java.lang.Double.doubleToLongBits(getWidth())
 522:       + 47 * java.lang.Double.doubleToLongBits(getHeight());
 523:     return (int) ((l >> 32) ^ l);
  }

}
