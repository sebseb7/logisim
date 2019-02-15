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
package com.cburch.logisim.std.wiring;

import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.bfh.logisim.hdlgenerator.TickHDLGenerator;
import com.cburch.logisim.hdl.Hdl;

public class ClockHDLGenerator extends HDLGenerator {

  public static final int CLK_SLOW = 0; // Oscillates at user-chosen rate and shape
  public static final int CLK_INV = 1;  // Inverse of CLK_SLOW
  public static final int POS_EDGE = 2; // High pulse when CLK_SLOW rises
  public static final int NEG_EDGE = 3; // High pulse when CLK_SLOW falls
  public static final int CLK_RAW = 4;  // The underlying raw FPGA clock
  // public static final int NrOfClockBits = 5;

  // See TickHDLGenerator for details on the rationale for these.
  public static String[] clkSignalFor(HDLGenerator downstream, int clkId) {
    String clkNet = "LOGISIM_LOCK_TREE_"+ clkId + downstream._hdl.idx;
    String one = downstream._hdl.one;
    if (downstream.nets.RawFPGAClock()) {
      // Raw mode: use ck=CLK_RAW en=1 or ck=~CLK_RAW en=1
      if (downstream.attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING 
          || downstream.attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
          || downstream.attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW)
        return new String[] { String.format(clkNet, CLK_RAW), one };
      else
        return new String[] { String.format(clkNet, CLK_INV), one }; // == ~CLK_RAW
    } else if (downstream.attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING 
        || downstream.attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING) {
      // Slow mode falling: use ck=CLK_RAW en=NEG_EDGE
      return new String[] {
        String.format(clkNet, CLK_RAW),
        String.format(clkNet, NEG_EDGE) };
    } else if (downstream.attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_HIGH) {
      // Slow mode active high: use ck=CL_SLOW en=1
      return new String[] { String.format(clkNet, CLK_SLOW), one };
    } else if (downstream.attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) {
      // Slow mode active high: use ck=~CLK_SLOW en=1
      return new String[] { String.format(clkNet, CLK_INV), one }; // == ~CLK_SLOW
    } else { // default: TRIG_RISING
      // Slow mode rising: use ck=CLK_RAW en=POS_EDGE
      return new String[] {
        String.format(clkNet, CLK_RAW),
        String.format(clkNet, POS_EDGE) };
    }
  }

  public ClockHDLGenerator(HDLCTX ctx) {
    super(ctx, "base", "LogisimClock", "i_ClockGen");
    int hi = attrs.getValue(Clock.ATTR_HIGH);
    int lo = attrs.getValue(Clock.ATTR_LOW);
    int raw = nets.RawFPGAClock() ? 1 : 0;
    if (raw && hi != lo)
      _err.AddFatalError("Clock component detected with " +hi+":"+lo+ " hi:lo duty cycle,"
          + " but maximum clock speed was selected. Only 1:1 duty cycle is supported with "
          + " maximum clock speed.");
    int ph = attrs.getValue(Clock.ATTR_PHASE);
    ph = ph % (hi + lo);
    if (ph != 0) // todo: support phase offset
      _err.AddFatalError("Clock component detected with "+ph+" tick phase offset,"
          + " but currently only 0 tick phase offset is supported for FPGA synthesis.");
    int max = (hi > lo) ? hi : lo;
    int w = 0;
    while (max != 0) {
      w++;
      max /= 2;
    }
    parameters.add(new ParameterInfo("HighTicks", hi));
    parameters.add(new ParameterInfo("LowTicks", lo));
    parameters.add(new ParameterInfo("Phase", ph));
    parameters.add(new ParameterInfo("CtrWidth", w));
    parameters.add(new ParameterInfo("Raw", raw));
    inPorts.add(new PortInfo("FPGAClock", 1, -1, null)); // see getPortMappings below
    inPorts.add(new PortInfo("FPGATick", 1, -1, null)); // see getPortMappings below
    outPorts.add(new PortInfo("ClockBus", 5, -1, null)); // see getPortMappings below

    registers.add(new WireInfo("s_output_regs", 4));
    registers.add(new WireInfo("s_counter_reg", "CtrWidth"));
    registers.add(new WireInfo("s_derived_clock_reg", 1));
    wires.add(new WireInfo("s_counter_next", "CtrWidth"));
    wires.add(new WireInfo("s_counter_is_zero", 1));
  }

  @Override
  public void generateBehavior(Hdl out) {
    if (out.isVhdl) {
      out.stmt("ClockBus <= IF (Raw = '1') THEN");
      out.stmt("            FPGACLock & '1' & '1' & NOT(FPGACLock) & FPGACLock;");
      out.stmt("            ELSE FPGACLock&s_output_regs;");
      out.stmt("makeOutputs : PROCESS( FPGACLock )");
      out.stmt("BEGIN");
      out.stmt("   IF (FPGACLock'event AND (FPGACLock = '1')) THEN");
      out.stmt("      s_output_regs(0)  <= s_derived_clock_reg;");
      out.stmt("      s_output_regs(1)  <= NOT(s_derived_clock_reg);");
      out.stmt("      s_output_regs(2)  <= NOT(s_derived_clock_reg) AND --rising edge tick");
      out.stmt("                           FPGATick AND");
      out.stmt("                           s_counter_is_zero;");
      out.stmt("      s_output_regs(3)  <= s_derived_clock_reg AND --falling edge tick");
      out.stmt("                           FPGATick AND");
      out.stmt("                           s_counter_is_zero;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS makeOutputs;");
      out.stmt("");
      out.stmt("s_counter_is_zero <= '1' WHEN s_counter_reg = std_logic_vector(to_unsigned(0,CtrWidth)) ELSE '0';");
      out.stmt("s_counter_next    <= std_logic_vector(unsigned(s_counter_reg) - 1)");
      out.stmt("                        WHEN s_counter_is_zero = '0' ELSE");
      out.stmt("                     std_logic_vector(to_unsigned((LowTicks-1),CtrWidth))");
      out.stmt("                        WHEN s_derived_clock_reg = '1' ELSE");
      out.stmt("                     std_logic_vector(to_unsigned((HighTicks-1),CtrWidth));");
      out.stmt("");
      out.stmt("makeDerivedClock : PROCESS( FPGACLock , FPGATick , s_counter_is_zero ,");
      out.stmt("                            s_derived_clock_reg)");
      out.stmt("BEGIN");
      out.stmt("   IF (FPGACLock'event AND (FPGACLock = '1')) THEN");
      out.stmt("      IF (s_derived_clock_reg /= '0' AND s_derived_clock_reg /= '1') THEN --For simulation only");
      out.stmt("         s_derived_clock_reg <= '0';");
      out.stmt("      ELSIF (s_counter_is_zero = '1' AND FPGATick = '1') THEN");
      out.stmt("         s_derived_clock_reg <= NOT(s_derived_clock_reg);");
      out.stmt("      END IF;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS makeDerivedClock;");
      out.stmt("");
      out.stmt("makeCounter : PROCESS( FPGACLock , FPGATick , s_counter_next ,");
      out.stmt("                       s_derived_clock_reg )");
      out.stmt("BEGIN");
      out.stmt("   IF (FPGACLock'event AND (FPGACLock = '1')) THEN");
      out.stmt("      IF (s_derived_clock_reg /= '0' AND s_derived_clock_reg /= '1') THEN --For simulation only");
      out.stmt("         s_counter_reg <= (OTHERS => '0');");
      out.stmt("      ELSIF (FPGATick = '1') THEN");
      out.stmt("         s_counter_reg <= s_counter_next;");
      out.stmt("      END IF;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS makeCounter;");
    } else {
      out.stmt("assign ClockBus = (Raw == 1)");
      out.stmt("                  ? {FPGACLock, 1'b1, 1'b1, ~FPGACLock, FPGACLock};");
      out.stmt("                  : {FPGACLock,s_output_regs};");
      out.stmt("always @(posedge FPGACLock)");
      out.stmt("begin");
      out.stmt("   s_output_regs[0] <= s_derived_clock_reg;");
      out.stmt("   s_output_regs[1] <= ~s_derived_clock_reg;");
      out.stmt("   s_output_regs[2] <= ~s_derived_clock_reg & FPGATick & s_counter_is_zero;");
      out.stmt("   s_output_regs[3] <= s_derived_clock_reg & FPGATick & s_counter_is_zero;");
      out.stmt("end");
      out.stmt("");
      out.stmt("assign s_counter_is_zero = (s_counter_reg == 0) ? 1'b1 : 1'b0;");
      out.stmt("assign s_counter_next = (s_counter_is_zero == 1'b0) ? s_counter_reg - 1 :");
      out.stmt("                        (s_derived_clock_reg == 1'b1) ? LowTicks - 1 :");
      out.stmt("                                                        HighTicks - 1;");
      out.stmt("");
      out.stmt("initial");
      out.stmt("begin");
      out.stmt("   s_output_regs = 0;");
      out.stmt("   s_derived_clock_reg = 0;");
      out.stmt("   s_counter_reg = 0;");
      out.stmt("end");
      out.stmt("");
      out.stmt("always @(posedge FPGACLock)");
      out.stmt("begin");
      out.stmt("   if (s_counter_is_zero & FPGATick)");
      out.stmt("   begin");
      out.stmt("      s_derived_clock_reg <= ~s_derived_clock_reg;");
      out.stmt("   end");
      out.stmt("end");
      out.stmt("");
      out.stmt("always @(posedge FPGACLock)");
      out.stmt("begin");
      out.stmt("   if (FPGATick)");
      out.stmt("   begin");
      out.stmt("      s_counter_reg <= s_counter_next;");
      out.stmt("   end");
      out.stmt("end");
    }
  }

  @Override
  protected void getPortMappings(ArrayList<String> assn, NetlistComponent comp, PortInfo p) {
    if (p.name.equals("FPGAClock")) {
      assn.add(_hdl.map, p.name, TickHDLGenerator.FPGA_CLK_NET);
    } else if (p.name.equals("FPGATick")) {
      assn.add(_hdl.map, p.name, TickHDLGenerator.FPGA_CLK_ENABLE_NET);
    } else if (p.name.equals("ClockBus")) {
      int id = _nets.GetClockSourceId(comp.GetComponent());
      if (id < 0)
        err.AddFatalError("INTERNAL ERROR: missing clock net for pin '%s' of '%s' in circuit '%s'.",
            p.name, hdlComponentName, _nets.getCircuitName());
      assn.add(String.format(_hdl.map, p.name, "LOGISIM_CLOCK_TREE_" + id));
    }
  }

}
