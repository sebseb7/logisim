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

public class BitSelectorHDLGenerator extends HDLGenerator {

  public BitSelectorHDLGenerator(HDLCTX ctx) {
    super(ctx, "plexers", deriveHDLName(ctx.attrs), "i_BitSel");
    parameters.add("WidthSel", selWidth());
    parameters.add("WidthIn", stdWidth());
    if (outWidth() > 1) {
      parameters.add("WidthOut", outWidth());
      outPorts.add("Result", "WidthOut", BitSelector.OUT, null);
    } else {
      outPorts.add("Result", 1, BitSelector.OUT, null);
    }
    // list.put("ExtendedBits", wo * (1 << ws) + 1);
    inPorts.add("DataIn", "WidthIn", BitSelector.IN, false);
    inPorts.add("Sel", "WidthSel", BitSelector.IN, false);
    wires.add("s_vec", "(2**WidthSel)+1");
  }

  private static String deriveHDLName(AttributeSet attrs) {
    int w = attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
    if (w == 1)
      return "SingleBitSelector"; // 1-bit version
    else
      return "MultiBitSelector"; // generic n-bit version
  }

  @Override
  public void generateBehavior(Hdl out) {
    out.indent();
    int wo = outWidth(attrs);
    if (out.isVhdl) {
      out.stmt("s_vec((ExtendedBits-1) DOWNTO WidthIn) <= (others => '0');");
      out.stmt("s_vec((WidthIn-1) DOWNTO 0) <= DataIn;");
      if (wo > 1) {
        out.stmt("Result <= s_vec(((to_integer(unsigned(Sel))+1)*WidthOut)-1");
        out.stmt("                DOWNTO to_integer(unsigned(Sel))*WidthOut);");
      } else {
        out.stmt("Result <= s_vec(to_integer(unsigned(Sel)));");
      }
    } else {
      out.stmt("assign s_vec[ExtendedBits-1:WidthIn] = 0;");
      out.stmt("assign s_vec[WidthIn-1:0] = DataIn;");
      if (wo > 1) {
        out.stmt("wire[513:0] s_select_vector;");
        out.stmt("reg[WidthOut-1:0] s_selected_slice;");
        out.stmt("assign s_select_vector[513:ExtendedBits] = 0;");
        out.stmt("assign s_select_vector[ExtendedBits-1:0] = s_vec;");
        out.stmt("assign Result = s_selected_slice;");
        out.stmt("");
        out.stmt("always @(*)");
        out.stmt("begin");
        out.stmt("  case (Sel)");
        for (int i = 15; i > 0; i--)
          out.stmt("  %d : s_selected_slice <= s_select_vector[(%d*WidthOut)-1 : %d*WidthOut];", i, i+1, i);
        out.stmt("  default : s_selected_slice <= s_select_vector[WidthOut-1:0];");
        out.stmt("  endcase");
        out.stmt("end");
      } else {
        out.stmt("assign Result = s_vec[Sel];");
      }
    }
  }

  protected int outWidth() {
    return attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
  }

  protected int selWidth() {
    int wd = stdWidth();
    int wo = outWidth();
    int groups = (wd + wg - 1) / wo - 1;
    int ws = 1;
    if (groups > 0) {
      while (groups != 1) {
        groups >>= 1;
        ws++;
      }
    }
    return ws;
  }

}
