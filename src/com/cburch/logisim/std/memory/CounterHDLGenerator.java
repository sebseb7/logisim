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

public class CounterHDLGeneratorextends HDLGenerator {

  public CounterHDLGenerator(HDLCTX ctx) {
    super(ctx, "memory", "Counter", "i_Ctr");
    parameters.add("BitWidth", stdWidth());
    parameters.add("MaxVal", attrs.getValue(Counter.ATTR_MAX));
    parameters.add("Mode", mode());

    inPorts.add("LoadData", "BitWidth", Counter.IN, false);
    inPorts.add("Clear", 1, Counter.CLR, false);
    inPorts.add("Load", 1, Counter.LD, false);
    inPorts.add("Enable", 1, Counter.EN, true);
    inPorts.add("Direction", 1, Counter.UD, true);
    outPorts.add("CountValue", "BitWidth", Counter.OUT, null);
    outPorts.add("CompareOut", 1, Counter.CARRY, null);

    wires.add("s_real_enable", 1);
    registers.add(new WireInfo("s_counter_value", "BitWidth"));
    registers.add(new WireInfo("s_next_counter_value", "BitWidth"));
    registers.add(new WireInfo("s_carry", 1));
  }

  @Override
	protected void generateFileHeader(Hdl out) {
    out.comment("Counter behavior after max value (or 0, if counting down) is reached:");
    out.comment("   Mode = 0 : Wrap around to 0 (or max, if counting down).");
    out.comment("   Mode = 1 : Stay at max value (or 0, if counting down).")
    out.comment("   Mode = 2 : Continue counting anyway.");
    out.comment("   Mode = 3 : Load new value from input.");
    out.stmt();
    out.comment("Counter functionality:");
    out.comment("   Load Count Direction | function");
    out.comment("   ---------------------|--------------------");
    out.comment("     0    0      x      | halt");
    out.comment("     0    1      0      | count down");
    out.comment("     0    1      1      | count up");
    out.comment("     1    x      x      | load new value");
    out.stmt();
  }

  @Override
  protected void generateBehavior(Hdl out) {
    if (out.isVhdl) {
      out.stmt();
      out.stmt("CompareOut   <= s_carry;");
      out.stmt("CountValue   <= s_counter_value;");
      out.stmt();
      out.stmt("make_carry : PROCESS( Direction , s_counter_value )");
      out.stmt("BEGIN");
      out.stmt("   IF (Direction = '0') THEN");
      out.stmt("      IF (s_counter_value = std_logic_vector(to_unsigned(0,width))) THEN");
      out.stmt("         s_carry <= '1';");
      out.stmt("      ELSE");
      out.stmt("         s_carry <= '0';");
      out.stmt("      END IF; -- Down counting");
      out.stmt("   ELSE");
      out.stmt("      IF (s_counter_value = std_logic_vector(to_unsigned(max_val,width))) THEN");
      out.stmt("         s_carry <= '1';");
      out.stmt("      ELSE");
      out.stmt("         s_carry <= '0';");
      out.stmt("      END IF; -- Up counting");
      out.stmt("   END IF;");
      out.stmt("END PROCESS make_carry;");
      out.stmt();
      out.stmt("s_real_enable <= '0' WHEN (Load = '0' AND Enable = '0') -- Counter disabled");
      out.stmt("                          OR");
      out.stmt("                          (Mode = 1 AND s_carry = '1' AND Load = '0') -- Stay at value situation");
      out.stmt("                     ELSE ClockEnable;");
      out.stmt();
      out.stmt("make_next_value : PROCESS( Load, Direction, s_counter_value, LoadData, s_carry )");
      out.stmt("   VARIABLE v_downcount : std_logic;");
      out.stmt("BEGIN");
      out.stmt("   v_downcount := NOT(Direction);");
      out.stmt("   IF ((Load = '1') OR -- Load condition");
      out.stmt("       (Mode = 3 AND s_carry = '1')    -- Wrap Load condition");
      out.stmt("      ) THEN s_next_counter_value <= LoadData;");
      out.stmt("        ELSE");
      out.stmt("      CASE (Mode) IS");
      out.stmt("         WHEN  0     => IF (s_carry = '1') THEN");
      out.stmt("                           IF (v_downcount = '1') THEN ");
      out.stmt("                              s_next_counter_value <= std_logic_vector(to_unsigned(max_val,width));");
      out.stmt("                                                  ELSE ");
      out.stmt("                              s_next_counter_value <= (OTHERS => '0');");
      out.stmt("                           END IF;");
      out.stmt("                                           ELSE");
      out.stmt("                           IF (v_downcount = '1') THEN ");
      out.stmt("                              s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) - 1);");
      out.stmt("                                                  ELSE ");
      out.stmt("                              s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) + 1);");
      out.stmt("                           END IF;");
      out.stmt("                        END IF;");
      out.stmt("         WHEN OTHERS => IF (v_downcount = '1') THEN ");
      out.stmt("                           s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) - 1);");
      out.stmt("                                               ELSE ");
      out.stmt("                           s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) + 1);");
      out.stmt("                        END IF;");
      out.stmt("      END CASE;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS make_next_value;");
      out.stmt();
      out.stmt("make_flops : PROCESS( GlobalClock , s_real_enable , Clear , s_next_counter_value )");
      out.stmt("BEGIN");
      out.stmt("   IF (Clear = '1') THEN s_counter_value <= (OTHERS => '0');");
      out.stmt("   ELSIF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      out.stmt("      IF (s_real_enable = '1') THEN s_counter_value <= s_next_counter_value;");
      out.stmt("      END IF;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS make_flops;");
    } else {
      out.stmt();
      out.stmt("assign CompareOut = s_carry;");
      out.stmt("assign CountValue = s_counter_value;");
      out.stmt();
      out.stmt("always@(*)");
      out.stmt("begin");
      out.stmt("   if (Direction)");
      out.stmt("         s_carry = (s_counter_value == max_val) ? 1'b1 : 1'b0;");
      out.stmt("   else");
      out.stmt("         s_carry = (s_counter_value == 0) ? 1'b1 : 1'b0;");
      out.stmt("end");
      out.stmt();
      out.stmt("assign s_real_enable = ((~(Load)&~(Enable))|");
      out.stmt("                        ((Mode==1)&s_carry&~(Load))) ? 1'b0 : ClockEnable;");
      out.stmt();
      out.stmt("always @(*)");
      out.stmt("begin");
      out.stmt("   if ((Load)|((Mode==3)&s_carry))");
      out.stmt("      s_next_counter_value = LoadData;");
      out.stmt("   else if ((Mode==0)&s_carry&Direction)");
      out.stmt("      s_next_counter_value = 0;");
      out.stmt("   else if ((Mode==0)&s_carry)");
      out.stmt("      s_next_counter_value = max_val;");
      out.stmt("   else if (Direction)");
      out.stmt("      s_next_counter_value = s_counter_value + 1;");
      out.stmt("   else");
      out.stmt("      s_next_counter_value = s_counter_value - 1;");
      out.stmt("end");
      out.stmt();
      out.stmt("always @(posedge GlobalClock or posedge Clear)");
      out.stmt("begin");
      out.stmt("    if (Clear) s_counter_value <= 0;");
      out.stmt("    else if (s_real_enable) s_counter_value <= s_next_counter_value;");
      out.stmt("end");
      out.stmt();
    }
  }

  protected int mode() {
    Object m = attrs.getValue(Counter.ATTR_ON_GOAL);
    if (m == Counter.ON_GOAL_LOAD)
      return 3;
    else if (m == Counter.ON_GOAL_CONT)
      return 2;
    else if (m == Counter.ON_GOAL_STAY)
      return 1;
    else // if (m == Counter.ON_GOAL_WRAP)
      return 0;
  }
}
