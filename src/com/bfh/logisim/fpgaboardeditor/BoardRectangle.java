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

package com.bfh.logisim.fpgaboardeditor;

public class BoardRectangle {

	public final int x, y, width, height;
  public final String kind; // "device", "constant", "ones", "zeros", "disconnected"
  public final int const_val; // only used when kind == "constant"

	public boolean activeHigh = true; // variable, and not part of equality checks
  public String label; // variable, and not part of equality checks

  public int synthetic_bits; // only used when kind != "device"

	public BoardRectangle(int x, int y, int w, int h, String label) {
    this.x = w < 0 ? x+w : x;
    this.y = h < 0 ? y+h : y;
    this.w = w < 0 ? -w : w;
    this.h = h < 0 ? -h : h;
    this.label = label;
    kind = "device";
    const_val = 0; // not used
    synthetic_bits = 0; // not used
	}

	private BoardRectangle(String synthetic_kind, int val) {
    x = y = width = height = 0;
    kind = synthetic_kind;
    const_val = val;
    label = kind.equals("constant") ? String.format("0x%x", val) : kind;
	}

  public static BoardRectangle constant(int val) { return new BoardRectangle("constant", val); }
  public static BoardRectangle ones() { return new BoardRectangle("ones", -1); }
  public static BoardRectangle zeros() { return new BoardRectangle("zeros", 0); }
  public static BoardRectangle disconnected() { return new BoardRectangle("disconnected", -2); }

  public boolean isDeviceSignal() { return "device".equals(kind); }
  public boolean isConstantInput() { return "constant".equals(kind); }
  public boolean isAllOnesInput() { return "ones".equals(kind); }
  public boolean isAllZerosInput() { return "zeros".equals(kind); }
  public boolean isDisconnectedOutput() { return "disconnected".equals(kind); }

	public boolean overlaps(BoardRectangle other) {
    if (x >= other.x+other.w || other.x >= x+w) // side by side
      return false;
    if (y >= other.y+other.h || other.y >= y+h) // above and below
      return false;
    return true;
  }

	public boolean contains(int px, int py) {
		return x <= px && px <= x+w && y <= py && py <= y+h;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BoardRectangle))
			return false;
		BoardRectangle other = (BoardRectangle)o;
		return kind.equals(other.kind) && const_val == other.const_val
        && x == other.x && y == other.y && w == other.w && h == other.h;
	}

  @Override
  public int hashCode() {
    return String.format("(%d,%d) %d x %d %s %d", x, y, w, h, kind, const_val).hashCode();
  }

}
