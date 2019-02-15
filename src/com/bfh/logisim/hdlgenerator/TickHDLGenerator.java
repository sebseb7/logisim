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

package com.bfh.logisim.hdlgenerator;

import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.library.DynamicClock;
import com.cburch.logisim.hdl.Hdl;

public class TickHDLGenerator extends HDLGenerator {

  public static final String FPGA_CLK_NET = "FPGA_GlobalClock";
  public static final String FPGA_CLK_ENABLE_NET = "FPGA_Tick";
  public static final String FPGA_DYNCLK_PERIOD_NET = "s_LOGISIM_DYNAMIC_CLOCK";

  // Note: The discussion below should be checkec against recommended best
  // practices. For example, see:
  // https://www.intel.cn/content/dam/altera-www/global/zh_CN/pdfs/literature/hb/qts/qts_qii51006.pdf
  // (starting on page 8, "Internally Generated Clocks")
  //
  // Generates periodic ticks meant to be used as a clock enable by other
  // components.
  // * In full-speed mode (period=0), the tick is high all the time, and a
  //   special "raw" mode is enabled.
  // * When period>0, downstream Clock components use only the rising edges to
  //   count high and low periods, so even with 1:1 high:low shape, the fastest
  //   case, things downstream from them run at half speed. In this component,
  //   we can slow things down further by a factor of 1/N, where period=N. So
  //   the overall slowdown is by a factor of 1/(2N).
  // * So when period=1, we get a slowdown of 1/2: the tick is high all the time
  //   (just as in raw mode), but the lack of "raw" mode means the downstream
  //   Clock components (assuming 1:1 shape) use only rising edges leading to
  //   half the transitions being ignored.
  // * When period=2, the slowdown is 1/4, with tick high every other period.
  // * When period=3, the slowdown is 1/6, with tick high every third period.
  //
  // Dynamic-speed mode works basically the same, but the period is adjustable
  // via a "Dynamic Clock Speed" component within the top-level circuit. The
  // tick signal transitions on the rising edge of the raw clock.
  //
  // Logisim Clock components (ClockHDLGenerator) uses FPGA_GlobalClock and
  // FPGA_Tick to derive clock buses (containing 5 signals) for driving clocked
  // components. These all transition at the rising edge of the raw clock as
  // well, so we basically lose half the clock speed here no matter what. But
  // there is also a "Raw=1" mode that just passes through the underlying clock,
  // allowing all downstream clocked components to run at full speed.
  // The derived buses contain:
  //   DerivedClock         -- Oscillates at user-chosen rate and shape
  //   InvertedDerivedClock -- Inverse of DerivedClock
  //   PositiveEdgeTick     -- Goes high for one raw cycle when DerivedClock rises
  //   NegativeEdgeTick     -- Goes high for one raw cycle when DerivedClock falls
  //   GlobalClock          -- The same raw clock signal as FPGA_GlobalClock
  //
  // Downstream components select from these depending on the clock discipline.
  //
  // For TRIG_RISING:
  //    clk_signal = GlobalClock
  //    clk_enable = PositiveEdgeTick (=1 when using raw mode)
  //    --> coded to transition on enabled rising edges
  // For TRIG_FALLING [non-raw mode]:
  //    clk_signal = GlobalClock
  //    clk_enable = NegativeEdgeTick (=1 when using raw mode)
  //    --> coded to transition on enabled rising edges
  // For TRIG_HIGH:
  //    clk_signal = DerivedClock (=GlobalClock when using raw mode)
  //    clk_enable = 1
  //    --> coded to transition on enabled high clock signal
  // For TRIG_LOW:
  //    clk_signal = InvertedDerivedClock (=~GlobalClock when using raw mode)
  //    clk_enable = 1
  //    --> coded to transition on enabled high clock signal
  //
  // Note: in raw mode, the above strategy still mostly works. For TRIG_HIGH and
  // TRIG_LOW, because DerivedClock = GlobalClock, these will trigger whenever
  // the raw clock is either high or low, as desired. For TRIG_RISING,
  // PositiveEdgeTick = 1, so it will fire on every rising edge of the raw
  // clock. But for TRIG_FALLING in raw mode, we need a different setup:
  //
  // For TRIG_FALLING [raw mode]:
  //    clk_signal = InvertedDerivedClock
  //    clk_enable = NegativeEdgeTick (= 1 when using raw mode)
  //    --> coded to transition on enabled rising edges [same as above]
  //
  //
  // (so TRIG_HIGH/TRIG_LOW will fire whenever the raw clock is
  // high/low as desired), and since PositiveEdgeTick = 1 (so TRIG_RISING will
  // fire on every rising edge of the raw clock) and NegativeEdgeTick = 1 (so
  // TRIG_FALLING will fire on every falling edge of the falling clock).
  //
  // Example, period=3:     0     1     2     3     4     5     6     7     8
  // FPGA_GlobalClock:    __@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\
  // FPGA_Tick:           __/~~~~~\___________/~~~~~\___________/~~~~~\_________
  // enabled rising edges         |                 |                 |
  // ClockTickGenerator, assuming 1:1 hi:lo shape
  // DerivedClock         ________/~~~~~~~~~~~~~~~~~\_________________/~~~~~~~~~
  // InvertedDerivedClock ~~~~~~~~\_________________/~~~~~~~~~~~~~~~~~\_________
  // PositiveEdgeTick     __/~~~~~\_____________________________/~~~~~\_________
  // NegativeEdgeTick     ____________________/~~~~~\___________________________
  // GlobalClock          __/~~\__@~~\__/~~\__/~~\__@~~\__/~~\__/~~\__@~~\__/~~\
  // TRIG_RISING events           |                 ^                 |
  // TRIG_FALLING events                            |
  //
  // Example, period=1:     0     1     2     3     4     5     6     7     8
  // FPGA_GlobalClock:    __@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\
  // FPGA_Tick:           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // enabled rising edges   |     |     |     |     |     |     |     |     |
  // ClockTickGenerator, assuming 1:1 hi:lo shape
  // DerivedClock         __/~~~~~\_____/~~~~~\_____/~~~~~\_____/~~~~~\_____/~~~
  // InvertedDerivedClock ~~\_____/~~~~~\_____/~~~~~\_____/~~~~~\_____/~~~~~\___
  // PositiveEdgeTick     ~~\_____/~~~~~\_____/~~~~~\_____/~~~~~\_____/~~~~~\___
  // NegativeEdgeTick     __/~~~~~\_____/~~~~~\_____/~~~~~\_____/~~~~~\_____/~~~
  // GlobalClock          __@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\
  // TRIG_RISING events     |           |           |           |           |
  // TRIG_FALLING events          |           |           |           |
  //
  // Example raw,period=0:  0     1     2     3     4     5     6     7     8
  // FPGA_GlobalClock:    __@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\
  // FPGA_Tick:           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // enabled rising edges   |     |     |     |     |     |     |     |     |
  // ClockTickGenerator, assuming 1:1 hi:lo shape
  // DerivedClock         __/~~\__/~~\__/~~\__/~~\__/~~\__/~~\__/~~\__/~~\__/~~\
  // InvertedDerivedClock ~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@
  // PositiveEdgeTick     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // NegativeEdgeTick     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // GlobalClock          __@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\__@~~\
  // TRIG_RISING events     |     |     |     |     |     |     |     |     |
  // TRIG_FALLING events       |     |     |     |     |     |     |     |     |

  private long freq;
  private int period;

  public TickHDLGenerator(HDLCTX ctx, long clock_frequency, int tick_period) {
    super(ctx, "base", deriveHdlName(tick_period), "i_TickGenerator");
    freq = clock_frequency;
    period = tick_period; // negative means dynamically adjustable period

    inPorts.add(new PortInfo("FPGARawClock", 1, -1, null)); // see getPortMappings below

    if (period == 0) {
      // full speed, no params
    } else if (period < 0) {
      // dynamic adjustable period
      int w = getDynamicCounterWidth();
      parameters.add(new ParameterInfo("CtrWidth", w));
      inPorts.add(new PortInfo("ReloadValueLessOne", "CtrWidth", -1, null)); // see getPortMappings below
      registers.add(new WireInfo("s_tick_reg", 1));
      registers.add(new WireInfo("s_count_reg", "CtrWidth"));
    } else {
      // static fixed period
      int w = getStaticCounterWidth();
      parameters.add(new ParameterInfo("CtrWidth", w));
      parameters.add(new ParameterInfo("ReloadValue", period));
      registers.add(new WireInfo("s_tick_reg", 1));
      registers.add(new WireInfo("s_count_reg", "CtrWidth"));
      wires.add(new WireInfo("ReloadValueLessOne", "CtrWidth");
    }

    outPorts.add(new PortInfo("FPGATick", 1, -1, null)); // see getPortMappings below
  }

  private static String deriveHdlName(int period) {
    if (period == 0)
      return "FullSpeedTickGenerator";
    else if (period < 0)
      return "DynamicTickGenerator";
    else
      return "TickGenerator";
  }

  private int getDynamicCounterWidth() {
    NetlistComponent dynClock = Nets.GetDynamicClock();
    if (dynClock == null) {
      _err.AddFatalError("Dynamic clock speed selected, but no dynamic clock control "
          + "component found. To use the dynamic clock speed feature, you must place a "
          + "\"dynamic clock control\" component in your main top-level circuit.");
      return 0;
    }
    return dynClock.GetComponent().getAttributeSet().getValue(DynamicClock.WIDTH_ATTR).getWidth();
  }

  private int getStaticCounterWidth() {
    int w = 0, p = period;
    while (p != 0) {
      w++;
      p /= 2;
    }
    return w;
  }

  @Override
  public void generateBehavior(Hdl out) {
    if (period == 0) {
      out.assign("FPGATick", out.one);
      return;
    }

    out.assign("FPGATick", "s_tick_reg");

    if (period > 0 && out.isVhdl)
      out.assign("ReloadValueLessOne", "std_logic_vector(to_unsigned((ReloadValue-1), CtrWidth))");
    else if (period > 0)
      out.assign("ReloadValueLessOne", "ReloadValue - 1");

    out.stmt();

    if (out.isVhdl) {
      out.stmt("s_tick_next   <= '1' WHEN s_count_reg = std_logic_vector(to_unsigned(0, CtrWidth)) ELSE '0';");
      out.stmt("s_count_next  <= (OTHERS => '0') WHEN s_tick_reg /= '0' AND s_tick_reg /= '1' ELSE -- For simulation only!");
      out.stmt("                 ReloadValueLessOne WHEN s_tick_next = '1' ELSE");
      out.stmt("                 std_logic_vector(unsigned(s_count_reg)-1);");
      out.stmt();
      out.stmt("make_tick : PROCESS( FPGARawClock , s_tick_next )");
      out.stmt("BEGIN");
      out.stmt("   IF (FPGARawClock'event AND (FPGARawClock = '1')) THEN");
      out.stmt("      s_tick_reg <= s_tick_next;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS make_tick;");
      out.stmt();
      out.stmt("make_counter : PROCESS( FPGARawClock , s_count_next )");
      out.stmt("BEGIN");
      out.stmt("   IF (FPGARawClock'event AND (FPGARawClock = '1')) THEN");
      out.stmt("      s_count_reg <= s_count_next;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS make_counter;");
    } else {
      out.stmt("assign s_tick_next  = (s_count_reg == 0) ? 1'b1 : 1'b0;");
      out.stmt("assign s_count_next = (s_count_reg == 0) ? ReloadValue-1 : s_count_reg-1;");
      out.stmt();
      out.stmt("initial");
      out.stmt("begin");
      out.stmt("   s_count_reg = 0; -- for hdl simulation only");
      out.stmt("   s_tick_reg  = 1'b0; -- for hdl simulation only");
      out.stmt("end");
      out.stmt();
      out.stmt("always @(posedge FPGARawClock)");
      out.stmt("begin");
      out.stmt("    s_count_reg <= s_count_next;");
      out.stmt("    s_tick_reg  <= s_tick_next;");
      out.stmt("end");
    }
  }

  @Override
  protected void getPortMappings(ArrayList<String> assn, NetlistComponent compUnused, PortInfo p) {
    if (p.name.equals("FPGARawClock")) {
      assn.add(_hdl.map, "FPGARawClock", FPGA_CLK_NET);
    } else if (p.name.equals("ReloadValueLessOne")) {
      assn.add(_hdl.map, "ReloadValueLessOne", FPGA_DYNCLK_PERIOD_NET);
    } else if (p.name.equals("FPGATick")) {
      assn.add(_hdl.map, "FPGATick", FPGA_CLK_ENABLE_NET);
    }
  }

}
