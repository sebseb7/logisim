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

public class MultiplexerHDLGenerator extends HDLGenerator {

  public MultiplexerHDLGenerator(HDLCTX ctx) {
    super(ctx, "plexers", deriveHDLName(ctx.attrs), "i_Mux");
    int w = stdWidth();
    int ws = selWidth();
    int n = (1 << ws);
    String bitWidth = (w > 1 ? "BitWidth" : "1");
    if (w > 1)
      parameters.add("BitWidth", w);
    for (int i = 0; i < n; i++)
      outPorts.add("In_"+i, bitWidth, i, false);
    inPorts.add("Sel", ws, n, true);
    if (_attrs.getValue(Plexers.ATTR_ENABLE)) {
      inPorts.add("Enable", 1, n+1, true); // may not be present
      inPorts.add("Result", bitWidth, n+2, null);
    } else {
      inPorts.add("Enable", 1, -1, true); // no port, use default instead
      inPorts.add("Result", bitWidth, n+1, null);
    }

    if (_lang.equals("Verilog"))
      registers.add("s_vec", bitWidth);
  }

  private static String deriveHDLName(AttributeSet attrs) {
    int a = 1 << attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    return "${BUS}Multiplexer_" + a + "_Way";
  }

  @Override
  protected void generateBehavior(Hdl out) {
    int w = stdWidth();
    int ws = selWidth();
    int n = (1 << ws);
    if (out.isVhdl) {
      out.stmt("make_mux : PROCESS(\tEnable,");
      for (int i = 0; i < n; i++)
        out.cont("     \tIn_%d, ", i);
      out.cont("       \tSel)");
      out.stmt("BEGIN");
      out.indent();
      out.stmt("IF (Enable = '0') THEN");
      if (w == 1)
        out.stmt("  Result <= '0';");
      else
        out.stmt("  Result <= (others => '0');");
      out.stmt("ELSE");
      out.stmt("  CASE (Sel) IS");
      for (int i = 0; i < n; i++)
        out.stmt("    WHEN %s => Result <= In_%d;", i<n-1 ? out.literal(i, ws) : "others", i);
      out.stmt("  END CASE;");
      out.stmt("END IF;");
      out.dedent();
      out.stmt("END PROCESS make_mux;");
    } else {
      out.stmt("assign Result = s_vec;");
      out.stmt("");
      out.stmt("always @(*)");
      out.stmt("begin");
      out.stmt("  if (~Enable) s_vec <= 0;");
      out.stmt("  else case (Sel)");
      for (int i = 0; i < n - 1; i++)
        out.stmt("    %d : s_vec <= In_%d;", i<n-1 ? out.literal(i, ws) : "default", i);
      out.stmt("  endcase");
      out.stmt("end");
    }
  }

  protected int selWidth() {
    return _attrs.getValue(Plexers.ATTR_SELECT).getWidth();
  }
}
