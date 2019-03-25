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

import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;

public class DividerHDLGenerator extends HDLGenerator {

  public DividerHDLGenerator(ComponentContext ctx) {
    super(ctx, "arithmetic", uMode(ctx.attrs) ? "UnsignedDivider" : "SignedDivider", "i_Div");
    // todo: 1-bit version?
    // todo: Verilog version
    int w = stdWidth();
    parameters.add("BitWidth", w);
    inPorts.add("DataA", "BitWidth", Divider.IN0, false);
    inPorts.add("DataB", "BitWidth", Divider.IN1, false);
    inPorts.add("Upper", "BitWidth", Divider.UPPER, false);
    outPorts.add("Quotient", "BitWidth", Divider.OUT, null);
    outPorts.add("Remainder", "BitWidth", Divider.REM, null);
    if (_hdl.isVhdl) {
      wires.add("s_div", "2*BitWidth");
      wires.add("s_mod", "BitWidth");
      wires.add("s_num", "2*BitWidth");
    } else {
      throw new IllegalArgumentException("Verilog divider not yet implemented");
    }
  }

  @Override
  protected void generateBehavior(Hdl out) {
    if (uMode(_attrs)) {
      out.stmt("s_num(2*BitWidth-1 DOWNTO BitWidth) <= Upper;");
      out.stmt("s_num(BitWidth-1 DOWNTO 0)          <= DataA;");
      out.stmt("s_div <= std_logic_vector(unsigned(s_num) / unsigned(DataB));");
      out.stmt("s_mod <= std_logic_vector(unsigned(s_num) mod unsigned(DataB));");
      out.stmt("Quotient  <= s_div(BitWidth-1 DOWNTO 0);");
      out.stmt("Remainder <= s_mod(BitWidth-1 DOWNTO 0);");
    } else {
      out.stmt("s_num(2*BitWidth-1 DOWNTO BitWidth) <= Upper;");
      out.stmt("s_num(BitWidth-1 DOWNTO 0)          <= DataA;");
      out.stmt("s_div <= std_logic_vector(signed(s_num) / signed(DataB));");
      out.stmt("s_mod <= std_logic_vector(signed(s_num) mod signed(DataB));");
      out.stmt("Quotient  <= s_div(BitWidth-1 DOWNTO 0);");
      out.stmt("Remainder <= s_mod(BitWidth-1 DOWNTO 0);");
    }
  }

  protected static boolean uMode(AttributeSet attrs) {
    return attrs.getValue(Divider.MODE_ATTR) == Divider.UNSIGNED_OPTION;
  }

}
