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

public class FlipFlopHDLGenerator extends HDLGenerator {

  private String displayName;
  private String vhdlUpdate, verilogUpdate;

  FlipFlopHDLGenerator(HDLCTX ctx, String name, String displayName,
      String[] inportNames, String vhdlUpdate, String verilogUpdate) {
    super(ctx, "memory", "${TRIGGER}"+name, "i_"+name);
    this.displayName = displayName;
    this.vhdlUpdate = vhdlUpdate;
    this.verilogUpdate = verilogUpdate;

    int portnum = 0;
    for (String port : inportNames)
      inPorts.add(port, 1, portnum++, false);

    clockPort = new ClockPortInfo("GlobalClock", "ClockEnable", portnum++);
    outPorts.add("Q", 1, portnum++, null);
    outPorts.add("Q_bar", 1, portnum++, null);
    inPorts.add("Reset", 1, portnum++, false);
    inPorts.add("Preset", 1, portnum++, false);

    wires.add("s_next_state", 1);
    registers.add("s_current_state_reg", 1);
  }

  @Override
  protected void generateBehavior(Hdl out) {
    if (out.isVhdl) {
      out.stmt("Q     <= s_current_state_reg;");
      out.stmt("Q_bar <= NOT(s_current_state_reg);");
      out.stmt();
      out.stmt(vhdlUpdate);
      out.stmt();
      out.stmt("make_memory : PROCESS( clock , Reset , Preset , ClockEnable , s_next_state )");
      out.stmt("BEGIN");
      out.stmt("   IF (Reset = '1') THEN s_current_state_reg <= '0';");
      out.stmt("   ELSIF (Preset = '1') THEN s_current_state_reg <= '1';");
      if (edgeTriggered())
        out.stmt("   ELSIF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      else
        out.stmt("   ELSIF (GlobalClock = '1') THEN");
      out.stmt("      IF (ClockEnable = '1') THEN");
      out.stmt("         s_current_state_reg <= s_next_state;");
      out.stmt("      END IF;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS make_memory;");
    } else {

      out.stmt("assignQ     = s_current_state_reg;");
      out.stmt("assignQ_bar = ~(s_current_state_reg);");
      out.stmt();
      out.stmt(verilogUpdate);
      out.stmt();
      out.comment("define the initial state (hdl simulation only)");
      out.stmt("initial");
      out.stmt("begin");
      out.stmt("   s_current_state_reg = 1'b0;");
      out.stmt("end");
      out.stmt();
      if (edgeTriggered()) {
        out.stmt("always @(posedge Reset or posedge Preset or posedge GlobalClock)");
        out.stmt("begin");
        out.stmt("   if (Reset) s_current_state_reg <= 1'b0;");
        out.stmt("   else if (Preset) s_current_state_reg <= 1'b1;");
        out.stmt("   else if (ClockEnable) s_current_state_reg<= s_next_state;");
        out.stmt("end");
      } else {
        out.stmt("always @(*)");
        out.stmt("begin");
        out.stmt("   if (Reset) s_current_state_reg <= 1'b0;");
        out.stmt("   else if (Preset) s_current_state_reg <= 1'b1;");
        out.stmt("   else if (ClockEnable & (GlobalClock == 1)) s_current_state_reg <= s_next_state;");
        out.stmt("end");
      }
    }
  }

}
