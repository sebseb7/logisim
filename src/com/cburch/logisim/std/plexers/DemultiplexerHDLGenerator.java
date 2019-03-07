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
package com.cburch.logisim.std.plexers;

import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;

public class DemultiplexerHDLGenerator extends HDLGenerator {

  public DemultiplexerHDLGenerator(HDLCTX ctx) {
    super(ctx, "plexers", deriveHDLName(ctx.attrs), "i_Demux");
    int w = stdWidth();
    int ws = selWidth();
    int n = (1 << ws);
    if (w > 1) {
      parameters.add("BitWidth", w);
      for (int i = 0; i < n; i++)
        outPorts.add("Out_"+i, "BitWidth", i, null);
    } else {
      for (int i = 0; i < n; i++)
        outPorts.add("Out_"+i, 1, i, null);
    }
    inPorts.add("Sel", ws, n, true);
    if (_attrs.getValue(Plexers.ATTR_ENABLE)) {
      inPorts.add("Enable", 1, n+1, true); // may not be present
    } else {
      inPorts.add("Enable", 1, -1, true); // no port, use default instead
      n--;
    }
    if (w > 1)
      inPorts.add("DataIn", "BitWidth", n+2, false);
    else
      inPorts.add("DataIn", 1, n+2, false);
  }

  private static String deriveHDLName(AttributeSet attrs) {
    int a = 1 << attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    return "${BUS}Demultiplexer_" + a + "_Way";
  }

  @Override
  protected void generateBehavior(Hdl out) {
    int w = stdWidth();
    int ws = selWidth();
    int n = (1 << ws);
    for (int i = 0; i < n; i++) {
      String s = out.literal(i, ws);
      if (out.isVhdl) {
        if (w == 1) {
          out.stmt("Out_%d <= DataIn WHEN Sel = %d AND Enable = '1' ELSE '0';", i, s);
        } else {
          out.stmt("Out_%d <= DataIn WHEN Sel = %d AND Enable = '1' ELSE (others => '0');", i, s);
        }
      } else {
        out.stmt("assign Out_%d = (Enable & (Sel == %d)) ? DataIn : 0;", i, s);
      }
    }
  }

  protected int selWidth() {
    return _attrs.getValue(Plexers.ATTR_SELECT).getWidth();
  }
}
