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
package com.cburch.logisim.std.arith;

import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.hdl.Hdl;

public class AdderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public AdderHDLGeneratorFactory(HDLCTX ctx) {
    super(ctx, "${BUS}Adder", "ADDER2C");
    int w = stdWidth();
    if (w > 1) {
      // Generic n-bit version
      parameters.add(new ParameterInfo(ws, w));
      inPorts.add(new PortInfo("DataA", "BitWidth", 0, false));
      inPorts.add(new PortInfo("DataB", "BitWidth", 1, false));
      outPorts.add(new PortInfo("Result", "BitWidth", 2, null));
      inPorts.add(new PortInfo("CarryIn", 1, 3, false));
      outPorts.add(new PortInfo("CarryOut", 1, 4, null));
      if (ctx.isVhdl) {
        wires.add(new WireInfo("s_A", "BitWidth+1"));
        wires.add(new WireInfo("s_B", "BitWidth+1"));
        wires.add(new WireInfo("s_R", "BitWidth+1"));
      }
    } else {
      // 1-bit version
      inPorts.add(new PortInfo("DataA", 1, 0, false));
      inPorts.add(new PortInfo("DataB", 1, 1, false));
      outPorts.add(new PortInfo("Result", 1, 2, null));
      inPorts.add(new PortInfo("CarryIn", 1, 3, false));
      outPorts.add(new PortInfo("CarryOut", 1, 4, null));
      if (ctx.isVhdl) {
        wires.add(new WireInfo("s_A", 2));
        wires.add(new WireInfo("s_B", 2));
        wires.add(new WireInfo("s_R", 2));
      }
    }
  }

  @Override
  protected String subdir() { return "arithmetic"; }

  @Override
  public void generateBehavior(Hdl out) {
    out.indent();
    if (out.isVhdl) {
      out.stmt("s_A <= \"0\" & DataA;");
      out.stmt("s_B <= \"0\" & DataB;");
      out.stmt("s_R <= std_logic_vector(unsigned(s_A) + unsigned(s_B) + (\"\" & CarryIn));");
      if (isBus()) {
        out.stmt("Result <= s_R((BusWidth-1) DOWNTO 0);");
        out.stmt("CarryOut <= s_R(BusWidth);");
      } else {
        out.stmt("Result <= s_R(0);");
        out.stmt("CarryOut <= s_R(1);");
      }
    } else {
      out.stmt("assign {CarryOut, Result} = DataA + DataB + CarryIn;");
    }
  }

}
