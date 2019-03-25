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
import com.cburch.logisim.hdl.Hdl;

public class NegatorHDLGenerator extends HDLGenerator {

  public NegatorHDLGenerator(ComponentContext ctx) {
    super(ctx, "arithmetic", "${BUS}Negator", "i_Neg");
    int w = stdWidth();
    if (w > 1) {
      // Generic n-bit version
      parameters.add("BitWidth", w);
      inPorts.add("DataX", "BitWidth", Negator.IN, false);
      outPorts.add("Result", "BitWidth", Negator.OUT, null);
    } else {
      // 1-bit version
      inPorts.add("DataX", 1, Negator.IN, false);
      outPorts.add("Result", 1, Negator.OUT, null);
    }
  }

  @Override
  protected void generateBehavior(Hdl out) {
    if (out.isVhdl && isBus())
      out.stmt("Result <= std_logic_vector(unsigned(not(DataX))) + 1;");
    else if (out.isVhdl)
      out.stmt("Result <= DataX;");
    else if (out.isVerilog)
      out.stmt("assign Result = -DataX;");
  }

}
