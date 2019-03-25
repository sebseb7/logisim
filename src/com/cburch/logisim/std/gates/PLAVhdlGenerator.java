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
package com.cburch.logisim.std.gates;

import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.cburch.logisim.hdl.Hdl;

public class PLAVhdlGenerator extends HDLGenerator {

  public PLAVhdlGenerator(ComponentContext ctx) {
    super(ctx, "gates", "PLA_${CIRCUIT}_${LABEL}", "i_PLA");

    inPorts.add("Index", _attrs.getValue(PLA.ATTR_IN_WIDTH).getWidth(), PLA.IN_PORT, false);
    outPorts.add("Result", _attrs.getValue(PLA.ATTR_OUT_WIDTH).getWidth(), PLA.OUT_PORT, null);
  }

  private static String bits(char b[]) {
    String s = "";
    for (char c : b)
      s = ((c == '0' || c == '1') ? c : '-') + s;
    if (b.length == 1)
      return "'" + s + "'";
    else
      return "\"" + s + "\"";
  }

  @Override
  protected void generateBehavior(Hdl out) {
    PLATable tt = _attrs.getValue(PLA.ATTR_TABLE);
    int w = _attrs.getValue(PLA.ATTR_OUT_WIDTH).getWidth();
    if (tt.rows().isEmpty()) {
      out.stmt("Result <= %s;", out.zeros(w));
    } else {
      out.stmt("Result <= \t");
      for (PLATable.Row r : tt.rows())
        out.cont("\t%s WHEN std_match(Index, %s) ELSE", bits(r.outBits), bits(r.inBits));
      out.cont("\t%s;", out.zeros(w));
    }
  }

}
