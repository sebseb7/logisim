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

import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.bfh.logisim.hdlgenerator.HDLInliner;
import com.bfh.logisim.hdlgenerator.TickHDLGenerator;
import com.bfh.logisim.netlist.Net;;
import com.bfh.logisim.netlist.NetlistComponent;
import com.bfh.logisim.netlist.ClockBus;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;

// Note: Clock HDL functionality is split into two parts. A counter part is
// lifted up to the toplevel where it can be duplicated, ensuring that even if
// the user uses many logisim clocks, all those ones with the same shape
// parameters will share a common generator and be reasonably well synchronized.
// A stub part remains within the circuit, but it does nothing other than select
// out from the hidden clock bus the derived clock signal, which is hopefully
// needed only in the cases where a logisim clock component output is fed into
// combinational logic.
public class ClockHDLGenerator {

  // Name used by circuit and top-level netslists for clock-related shadow buses.
  public static String CLK_TREE_NET = "LOGISIM_CLOCK_TREE"; // %d
  public static int CLK_TREE_WIDTH = 6; // bus contains the five signals below

  public static final int CLK_USR = 0; // Oscillates at user-chosen rate and shape
  public static final int CLK_INV = 1;  // Inverse of CLK_USR
  public static final int POS_EDGE = 2; // High pulse when CLK_USR rises
  public static final int NEG_EDGE = 3; // High pulse when CLK_USR falls
  public static final int CLK_RAW = 4;  // The underlying raw FPGA clock
  public static final int CLK_INVRAW = 5;  // The underlying raw FPGA clock, inverted

  // See TickHDLGenerator for details on the rationale for these.
  public static String[] clkSignalFor(HDLGenerator downstream, int clkid) {
    String clkNet = CLK_TREE_NET + clkid + downstream._hdl.idx;
    String one = downstream._hdl.one;
    if (downstream.ctx.clkPeriod == 0) {
      // Raw mode: use ck=CLK_RAW en=1 or ck=~CLK_RAW en=1
      if (downstream._attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING 
          || downstream._attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
          || downstream._attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW)
        return new String[] { String.format(clkNet, CLK_INV), one }; // == ~CLK_RAW when phase=0
      else
        return new String[] { String.format(clkNet, CLK_USR), one }; // == CLK_RAW when phase=0
    } else if (downstream._attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING 
        || downstream._attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING) {
      // Slow mode falling: use ck=CLK_RAW en=NEG_EDGE
      return new String[] {
        String.format(clkNet, CLK_RAW),
        String.format(clkNet, NEG_EDGE) };
    } else if (downstream._attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_HIGH) {
      // Slow mode active high: use ck=CL_SLOW en=1
      return new String[] { String.format(clkNet, CLK_USR), one };
    } else if (downstream._attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) {
      // Slow mode active high: use ck=~CLK_USR en=1
      return new String[] { String.format(clkNet, CLK_INV), one }; // == ~CLK_USR
    } else { // default: TRIG_RISING
      // Slow mode rising: use ck=CLK_RAW en=POS_EDGE
      return new String[] {
        String.format(clkNet, CLK_RAW),
        String.format(clkNet, POS_EDGE) };
    }
  }

  public static class StubPart extends HDLInliner {

    public StubPart(ComponentContext ctx) {
      super(ctx);
    }

    @Override
    protected void generateInlinedCode(Hdl out, NetlistComponent comp) {
      Net net = comp.getConnection(0);
      if (net != null) {
        int clkid = _nets.getClockId(net);
        out.assign(net.name, CLK_TREE_NET + clkid, CLK_USR);
      }
    }

  }

  public static class CounterPart extends HDLGenerator {

    public final long id;
    private final ClockBus.Shape shape;

    public CounterPart(ComponentContext ctx, ClockBus.Shape shape, long id) {
      // Note: Only one declaration is made at the top level, so we can only
      // have one HDL implementation version of the counter here. If we wanted
      // multiple HDL implmentation versions here, ToplevelHDLGenerator would to
      // make one declaration for each version used in the circuit under test.
      super(ctx, "base", "LogisimClock", "i_ClockGen");
      this.shape = shape;
      this.id = id;
      int hi = shape.hi;
      int lo = shape.lo;
      int ph = shape.ph;
      int raw = ctx.clkPeriod == 0 ? 1 : 0;
      if (raw == 1 && hi != lo)
        _err.AddFatalError("Clock component detected with " +hi+":"+lo+ " hi:lo duty cycle,"
            + " but maximum clock speed was selected. Only 1:1 duty cycle is supported with "
            + " maximum clock speed.");
      ph = ph % (hi + lo);
      // if ((hi != 1 || lo != 1) && ph != 0) // todo: support phase offset for other than 1:1 duty cycle
      //   _err.AddFatalError("Clock component detected with "+ph+" tick phase offset,"
      //       + " but currently only 0 tick phase offset is supported for FPGA synthesis,"
      //       + " except when using 1:1 duty cycle.");
      int max = (hi > lo) ? hi : lo;
      int w = 0;
      while (max != 0) {
        w++;
        max /= 2;
      }
      parameters.add("HighTicks", hi);
      parameters.add("LowTicks", lo);
      parameters.add("Phase", ph);
      parameters.add("CtrWidth", w);
      parameters.add("Raw", raw);

      inPorts.add("CLKp", 1, -1, null); // see getPortMappings below
      inPorts.add("CLKn", 1, -1, null); // see getPortMappings below
      inPorts.add("Tick", 1, -1, null); // see getPortMappings below
      outPorts.add("ClockBus", CLK_TREE_WIDTH, -1, null); // see getPortMappings below

      // depends on phase, and shape
      // String initialOut = ctx.hdl.isVhdl ? "\"0101\"" : "4'b0101'";
      // String initialCount = ctx.hdl.isVhdl
      //     ? "std_logic_vector(to_unsigned((HighTicks-1), CtrWidth))" : "HighTicks-1";
      // Example: 5:3 shape
      //      ctr: 4 3 2 1 0 2 1 0 4 3 2 1 0 2 1 0
      //  phase=0: ^       .     .  initial usr=1, inv=0, ctr=4, ctz=0
      //  phase=1:   ^     .     .  initial usr=1, inv=0, ctr=3, ctz=0
      //  phase=2:     ^   .     .  initial usr=1, inv=0, ctr=2, ctz=0
      //  phase=3:       ^ .     .  initial usr=1, inv=0, ctr=1, ctz=0
      //  phase=4:         ^     .  initial usr=1, inv=0, ctr=0, ctz=1
      //  phase=5:           ^   .  initial usr=0, inv=1, ctr=2, ctz=0
      //  phase=6:             ^ .  initial usr=0, inv=1, ctr=1, ctz=0
      //  phase=7:               ^  initial usr=0, inv=1, ctr=0, ctz=1
      if (ctx.hdl.isVhdl) {
        // VHDL limitation: keyword "WHEN" can't be used in a signal
        // initializer, for seemingly no good reason. So we need to make the
        // initial values be generics.
        // registers.add("usr", 1, "'1' WHEN Phase < HighTicks ELSE '0'");
        // registers.add("inv", 1, "'0' WHEN Phase < HighTicks ELSE '0'");
        // registers.add("counter", "CtrWidth",
        //     "std_logic_vector(to_unsigned(HighTicks-Phase-1, CtrWidth))"
        //     + " WHEN Phase < HighTicks ELSE"
        //     + " std_logic_vector(to_unsigned(HighTicks+LowTicks-Phase-1, CtrWidth))");
        // registers.add("ctrzero", 1, "'1' WHEN Phase = HighTicks-1 OR Phase = HighTicks+LowTicks-1 ELSE '0'");
        parameters.add("InitUsr", ph < hi ? 1 : 0);
        parameters.add("InitInv", ph < hi ? 0 : 1);
        parameters.add("InitCounter", ph < hi ? hi - ph - 1 : hi+lo - ph - 1);
        parameters.add("InitCtrZero", (ph == hi-1 || ph == hi+lo-1) ? 1 : 0);
        registers.add("usr", 1, "std_logic(to_unsigned(InitUsr,1)(0))");
        registers.add("inv", 1, "std_logic(to_unsigned(InitInv,1)(0))");
        registers.add("counter", "CtrWidth", "std_logic_vector(to_unsigned(InitCounter, CtrWidth))");
        registers.add("ctrzero", 1, "std_logic(to_unsigned(InitCtrZero,1)(0))");
      } else {
        registers.add("usr", 1, "(Phase < HighTicks) ? 1'b1 : 1'b0");
        registers.add("inv", 1, "(Phase < HighTicks) ? 1'b0 : 1'b1");
        registers.add("counter", "CtrWidth", "(Phase < HighTicks) ? HighTicks-Phase-1 : HighTicks+LowTicks-Phase-1"); // note: warns about trucnation
        registers.add("ctrzero", 1, "(Phase == HighTicks-1 || Phase == HighTicks+LowTicks-1) ? 1'b1 : 1'b0");
      }
      wires.add("s_rise", 1);
      wires.add("s_fall", 1);
      wires.add("s_counter_next", "CtrWidth");
    }

    @Override
    protected void generateBehavior(Hdl out) {
      if (out.isVhdl) {
        out.stmt("ClockBus <= CLKn & CLKp & '1' & '1' & CLKn & CLKp");
        out.stmt("            WHEN (Raw = 1 AND Phase = 0) ELSE");
        out.stmt("            CLKn & CLKp & '1' & '1' & CLKp & CLKn");
        out.stmt("            WHEN (Raw = 1 AND Phase = 1) ELSE");
        out.stmt("            CLKn & CLKp & s_fall & s_rise & inv & usr;");
        out.stmt();
        out.stmt("makeClocks : PROCESS( CLKp, Tick )");
        out.stmt("BEGIN");
        out.stmt("  IF (CLKp'event AND CLKp = '1' AND Tick = '1') THEN");
        out.stmt("    IF (s_rise = '1' OR s_fall = '1') THEN");
        out.stmt("      usr <= s_rise;");
        out.stmt("      inv <= s_fall;");
        out.stmt("    END IF;");
        out.stmt("    IF (s_counter_next = std_logic_vector(to_unsigned(0, CtrWidth))) THEN");
        out.stmt("      ctrzero <= '1';");
        out.stmt("    ELSE");
        out.stmt("      ctrzero <= '0';");
        out.stmt("    END IF;");
        out.stmt("    counter <= s_counter_next;");
        out.stmt("  END IF;");
        out.stmt("END PROCESS makeClocks;");
        out.stmt();
        out.stmt("s_rise <= '1' WHEN (Tick = '1' AND ctrzero = '1' AND usr = '0') ELSE '0';");
        out.stmt("s_fall <= '1' WHEN (Tick = '1' AND ctrzero = '1' AND usr = '1') ELSE '0';");
        out.stmt("s_counter_next <= std_logic_vector(unsigned(counter) - 1)");
        out.stmt("                  WHEN ctrzero = '0' ELSE");
        out.stmt("                  std_logic_vector(to_unsigned((LowTicks-1),CtrWidth))");
        out.stmt("                  WHEN usr = '1' ELSE");
        out.stmt("                  std_logic_vector(to_unsigned((HighTicks-1),CtrWidth));");
      } else {
        out.stmt("assign ClockBus = (Raw == 1 && Phase == 0)");
        out.stmt("                  ? {CLKn, CLKp, 1'b1, 1'b1, CLKn, CLKp}");
        out.stmt("                  : (Raw == 1 && Phase == 1)");
        out.stmt("                  ? {CLKn, CLKp, 1'b1, 1'b1, CLKp, CLKn}");
        out.stmt("                  : {CLKn, CLKp, s_fall, s_rise, inv, usr};");
        out.stmt();
        out.stmt("always @(posedge CLKp)");
        out.stmt("begin");
        out.stmt("  if (Tick) begin");
        out.stmt("    if (s_rise || s_fall) begin");
        out.stmt("      usr <= s_rise;");
        out.stmt("      inv <= s_fall;");
        out.stmt("    end");
        out.stmt("    ctrzero <= (s_counter_next == 0) ? 1'b1 : 1'b0;");
        out.stmt("    counter <= s_counter_next;");
        out.stmt("  end");
        out.stmt("end");
        out.stmt();
        out.stmt("assign s_rise = Tick & ctrzero & ~usr;");
        out.stmt("assign s_fall = Tick & ctrzero & usr;");
        out.stmt("assign s_counter_next = (ctrzero == 1'b0) ? counter - 1 :");
        out.stmt("                        (usr == 1'b1) ? LowTicks - 1 :");
        out.stmt("                                        HighTicks - 1;");
      }
    }

    @Override
    protected void getPortMappings(Hdl.Map map, NetlistComponent compUnused, PortInfo p) {
      if (p.name.equals("CLKp"))
        map.add(p.name, TickHDLGenerator.FPGA_CLKp_NET);
      else if (p.name.equals("CLKn"))
        map.add(p.name, TickHDLGenerator.FPGA_CLKn_NET);
      else if (p.name.equals("Tick"))
        map.add(p.name, TickHDLGenerator.FPGA_TICK_NET);
      else if (p.name.equals("ClockBus"))
        map.add(p.name, CLK_TREE_NET + id);
    }

  }
}
