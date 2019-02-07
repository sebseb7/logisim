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

import java.util.SortedMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;

public class ClockHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public ClockHDLGeneratorFactory(String lang, FPGAReport err) {
    super(lang, err);
  }

  private static final int GENERIC_PARAM_HIGHTICKS = -1;
  private static final int GENERIC_PARAM_LOWTICKS = -2;
  private static final int GENERIC_PARAM_PHASE = -3;
  private static final int GENERIC_PARAM_CTRWIDTH = -4;

  public static final int DerivedClockIndex = 0;
  public static final int InvertedDerivedClockIndex = 1;
  public static final int PositiveEdgeTickIndex = 2;
  public static final int NegativeEdgeTickIndex = 3;
  public static final int GlobalClockIndex = 4;

  public static final int NrOfClockBits = 5;

  @Override
  public String getComponentStringIdentifier() { return "CLOCKGEN"; }

  @Override
  public String GetSubDir() { return "base"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("GlobalClock", 1);
    list.put("ClockTick", 1);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("ClockBus", NrOfClockBits);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    list.put(GENERIC_PARAM_HIGHTICKS, "HighTicks");
    list.put(GENERIC_PARAM_LOWTICKS, "LowTicks");
    list.put(GENERIC_PARAM_PHASE, "Phase");
    list.put(GENERIC_PARAM_CTRWIDTH, "CtrWidth");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int hi = attrs.getValue(Clock.ATTR_HIGH);
    int lo = attrs.getValue(Clock.ATTR_LOW);
    int ph = attrs.getValue(Clock.ATTR_PHASE);
    ph = ph % (hi + lo);
    int max = (hi > lo) ? hi : lo;
    int w = 0;
    while (max != 0) {
      w++;
      max /= 2;
    }
    list.put("HighTicks", hi);
    list.put("LowTicks", lo);
    list.put("Phase", ph);
    list.put("CtrWidth", w);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    int id = nets.GetClockSourceId(info.GetComponent());
    list.put("GlobalClock", TickComponentHDLGeneratorFactory.FPGAClock);
    list.put("ClockTick", TickComponentHDLGeneratorFactory.FPGATick);
    list.put("ClockBus", id >= 0 ? "s_" + ClockTreeName + id : "s_missing_clock_id"); // fixme: ??
  }

  @Override
  public void registers(SortedMap<String, Integer> list, AttributeSet attrs, String lang) {
    list.put("s_output_regs", NrOfClockBits - 1);
    list.put("s_counter_reg", GENERIC_PARAM_CTRWIDTH);
    list.put("s_derived_clock_reg", 1);
  }

  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    list.put("s_counter_next", GENERIC_PARAM_CTRWIDTH);
    list.put("s_counter_is_zero", 1);
  }

  @Override
  public void behavior(Hdl out, Netlist nets, AttributeSet attrs) {
    out.indent();

    int hi = attrs.getValue(Clock.ATTR_HIGH);
    int lo = attrs.getValue(Clock.ATTR_LOW);
    int ph = attrs.getValue(Clock.ATTR_PHASE);
    if (ph != 0) // todo: support phase offset
      out.err.AddFatalError("Clock component detected with "+ph+" tick phase offset,"
          + " but currently only 0 tick phase offset is supported for FPGA synthesis.");

    if (nets.RawFPGAClock()) {
      // raw clock b/c use selected maximum clock speed
      if (hi != lo)
        out.err.AddFatalError("Clock component detected with " +hi+":"+lo+ " hi:lo duty cycle,"
            + " but maximum clock speed was selected. Only 1:1 duty cycle is supported with "
            + " maximum clock speed.");
      if (out.isVhdl)
        out.stmt("ClockBus <= GlobalClock & '1' & '1' & NOT(GlobalClock) & GlobalClock;");
      else
        out.stmt("assign ClockBus = {GlobalClock, 1'b1, 1'b1, ~GlobalClock, GlobalClock};");
      return;
    }

    if (out.isVhdl) {
      out.stmt("ClockBus <= GlobalClock&s_output_regs;");
      out.stmt("makeOutputs : PROCESS( GlobalClock )");
      out.stmt("BEGIN");
      out.stmt("   IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      out.stmt("      s_output_regs(0)  <= s_derived_clock_reg;");
      out.stmt("      s_output_regs(1)  <= NOT(s_derived_clock_reg);");
      out.stmt("      s_output_regs(2)  <= NOT(s_derived_clock_reg) AND --rising edge tick");
      out.stmt("                           ClockTick AND");
      out.stmt("                           s_counter_is_zero;");
      out.stmt("      s_output_regs(3)  <= s_derived_clock_reg AND --falling edge tick");
      out.stmt("                           ClockTick AND");
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
      out.stmt("makeDerivedClock : PROCESS( GlobalClock , ClockTick , s_counter_is_zero ,");
      out.stmt("                            s_derived_clock_reg)");
      out.stmt("BEGIN");
      out.stmt("   IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      out.stmt("      IF (s_derived_clock_reg /= '0' AND s_derived_clock_reg /= '1') THEN --For simulation only");
      out.stmt("         s_derived_clock_reg <= '0';");
      out.stmt("      ELSIF (s_counter_is_zero = '1' AND ClockTick = '1') THEN");
      out.stmt("         s_derived_clock_reg <= NOT(s_derived_clock_reg);");
      out.stmt("      END IF;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS makeDerivedClock;");
      out.stmt("");
      out.stmt("makeCounter : PROCESS( GlobalClock , ClockTick , s_counter_next ,");
      out.stmt("                       s_derived_clock_reg )");
      out.stmt("BEGIN");
      out.stmt("   IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      out.stmt("      IF (s_derived_clock_reg /= '0' AND s_derived_clock_reg /= '1') THEN --For simulation only");
      out.stmt("         s_counter_reg <= (OTHERS => '0');");
      out.stmt("      ELSIF (ClockTick = '1') THEN");
      out.stmt("         s_counter_reg <= s_counter_next;");
      out.stmt("      END IF;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS makeCounter;");
    } else {
      out.stmt("assign ClockBus = {GlobalClock,s_output_regs};");
      out.stmt("always @(posedge GlobalClock)");
      out.stmt("begin");
      out.stmt("   s_output_regs[0] <= s_derived_clock_reg;");
      out.stmt("   s_output_regs[1] <= ~s_derived_clock_reg;");
      out.stmt("   s_output_regs[2] <= ~s_derived_clock_reg & ClockTick & s_counter_is_zero;");
      out.stmt("   s_output_regs[3] <= s_derived_clock_reg & ClockTick & s_counter_is_zero;");
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
      out.stmt("always @(posedge GlobalClock)");
      out.stmt("begin");
      out.stmt("   if (s_counter_is_zero & ClockTick)");
      out.stmt("   begin");
      out.stmt("      s_derived_clock_reg <= ~s_derived_clock_reg;");
      out.stmt("   end");
      out.stmt("end");
      out.stmt("");
      out.stmt("always @(posedge GlobalClock)");
      out.stmt("begin");
      out.stmt("   if (ClockTick)");
      out.stmt("   begin");
      out.stmt("      s_counter_reg <= s_counter_next;");
      out.stmt("   end");
      out.stmt("end");
    }
  }

}
