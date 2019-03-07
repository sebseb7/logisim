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

public class PullBehavior {
  public final String desc, altera, xilinx;

  public static final PullBehavior FLOAT = new PullBehavior("Float", "TRI-STATED", "FLOAT");
  public static final PullBehavior PULL_UP = new PullBehavior("Pull Up", "PULLUP", "PULLUP");
  public static final PullBehavior PULL_DOWN = new PullBehavior("Pull Down", "PULLDOWN", "PULLDOWN");
  public static final PullBehavior UNKNOWN = new PullBehavior("Unknown", "", "FLOAT");
  public static final PullBehavior[] OPTIONS = { FLOAT, PULL_UP, PULL_DOWN };

  private PullBehavior(String d, String a, String x) { desc = d; altera = a; xilinx = x; }

  public static PullBehavior get(String desc) {
    for (PullBehavior p : OPTIONS)
      if (p.desc.equals(desc))
        return p;
    return UNKNOWN;
  }

  @Override
  public String toString() { return desc; }
}
