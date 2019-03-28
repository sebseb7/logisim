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
package com.cburch.logisim.std.memory;

import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.cburch.logisim.hdl.Hdl;

public class RegisterHDLGenerator extends HDLGenerator {

  public RegisterHDLGenerator(ComponentContext ctx) {
    super(ctx, "memory", "${TRIGGER}Register", "REGISTER");
    parameters.add("BitWidth", stdWidth());
    inPorts.add("D", "BitWidth", Register.IN, false);
    inPorts.add("Reset", 1, Register.CLR, false);
    inPorts.add("Load", 1, Register.EN, true);
    outPorts.add("Q", "BitWidth", Register.OUT, false);
    clockPort = new ClockPortInfo("GlobalClock", "ClockEnable", Register.CK);

    registers.add("s_state_reg", "BitWidth", ctx.hdl.allZeros);
  }

  @Override
  protected void generateBehavior(Hdl out) {
    if (out.isVhdl) {
      out.stmt("   Q <= s_state_reg;");
      out.stmt("");
      out.stmt("   make_memory : PROCESS( GlobalClock , Reset , Load , ClockEnable , D )");
      out.stmt("   BEGIN");
      out.stmt("      IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');");
      if (edgeTriggered()) {
        out.stmt("      ELSIF (GlobalClock'event AND (GlobalClock = '1')) THEN");
        out.stmt("         IF (Load = '1' AND ClockEnable = '1') THEN");
        out.stmt("            s_state_reg <= D;");
        out.stmt("         END IF;");
        out.stmt("      END IF;");
      } else {
        out.stmt("      ELSIF (GlobalClock = '1') THEN");
        out.stmt("         IF (Load = '1' AND ClockEnable = '1') THEN");
        out.stmt("            s_state_reg <= D;");
        out.stmt("         END IF;");
        out.stmt("      END IF;");
      }
      out.stmt("   END PROCESS make_memory;");
    } else {
      out.stmt("   assign Q = s_state_reg;");
      out.stmt("");
      if (edgeTriggered()) {
        out.stmt("   always @(posedge GlobalClock or posedge Reset)");
        out.stmt("   begin");
        out.stmt("      if (Reset) s_state_reg <= 0;");
        out.stmt("      else if (Load&ClockEnable) s_state_reg <= D;");
        out.stmt("   end");
      } else {
        out.stmt("   always @(*)");
        out.stmt("   begin");
        out.stmt("      if (Reset) s_state_reg <= 0;");
        out.stmt("      else if ((GlobalClock==1)&Load&ClockEnable) s_state_reg <= D;");
        out.stmt("   end");
      }
    }
  }

}
