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

import java.util.SortedMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;

public class RandomHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public RandomHDLGeneratorFactory(String lang, FPGAReport err) {
    super(lang, err);
  }

  protected final static int GENERIC_PARAM_BUSWIDTH = -1;
  protected final static int GENERIC_PARAM_SEED = -2;

  @Override
  public String getComponentStringIdentifier() { return "RNG"; }

  @Override
  public String GetSubDir() { return "memory"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("GlobalClock", 1);
    list.put("ClockEnable", 1);
    list.put("clear", 1);
    list.put("enable", 1);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Q", GENERIC_PARAM_BUSWIDTH);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    list.put(GENERIC_PARAM_BUSWIDTH, "BusWidth");
    list.put(GENERIC_PARAM_SEED, "Seed");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);
    int seed = attrs.getValue(Random.ATTR_SEED);
    if (seed == 0)
      seed = (int) System.currentTimeMillis();

    list.put("BusWidth", w);
    list.put("Seed", seed);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);
    boolean hasClk = info.EndIsConnected(Random.CK);
    if (!hasClk)
      err.AddSevereWarning("Component \"Random\" in circuit \""
          + nets.getCircuitName() + "\" has no clock connection");

    String clk = GetClockNetName(info, Random.CK, nets);
    boolean gatedClk = clk.equals("");
    boolean activelo = attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING;

    boolean vhdl = lang.equals("VHDL");
    String zero = vhdl ? "'0'" : "1'b0";
    String one = vhdl ? "'1'" : "1'b1";
    String idx = vhdl ? "(%d)" : "[%]"; // fixme: these should be in base class at minimum!

    if (!hasClk) {
      err.AddError("Found a gated clock for component \"Random\" in circuit \""
          + nets.getCircuitName() + "\"");
      err.AddError("This RNG will not work!");
    }

    // fixme: same as several others?
    if (!hasClk || gatedClk) {
      list.put("GlobalClock", zero);
      list.put("ClockEnable", zero);
    } else {
      list.put("GlobalClock", String.format(clk+idx, ClockHDLGeneratorFactory.GlobalClockIndex));;
      if (nets.RequiresGlobalClockConnection())
        list.put("ClockEnable", String.format(clk+idx, ClockHDLGeneratorFactory.GlobalClockIndex));
      else if (activelo)
        list.put("ClockEnable", String.format(clk+idx, ClockHDLGeneratorFactory.NegativeEdgeTickIndex));
      else
        list.put("ClockEnable", String.format(clk+idx, ClockHDLGeneratorFactory.PositiveEdgeTickIndex));
    }
    list.putAll(GetNetMap("clear", true, info, Random.RST, err, lang, nets));
    list.putAll(GetNetMap("enable", false, info, Random.NXT, err, lang, nets));
    String q = "Q" + (vhdl && w == 1 ? "(0)" : "");
    list.putAll(GetNetMap(q, true, info, Random.OUT, err, lang, nets));
  }


  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    list.put("s_InitSeed", 48);
    list.put("s_reset", 1);
    list.put("s_reset_next", 3);
    list.put("s_mult_shift_next", 36);
    list.put("s_seed_shift_next", 48);
    list.put("s_mult_busy", 1);
    list.put("s_start", 1);
    list.put("s_mac_lo_in_1", 25);
    list.put("s_mac_lo_in_2", 25);
    list.put("s_mac_hi_1_next", 24);
    list.put("s_mac_hi_in_2", 24);
    list.put("s_busy_pipe_next", 2);
  }

  @Override
  public void registers(SortedMap<String, Integer> list, AttributeSet attrs, String lang) {
    list.put("s_current_seed", 48);
    list.put("s_reset_reg", 3);
    list.put("s_mult_shift_reg", 36);
    list.put("s_seed_shift_reg", 48);
    list.put("s_start_reg", 1);
    list.put("s_mac_lo_reg", 25);
    list.put("s_mac_hi_reg", 24);
    list.put("s_mac_hi_1_reg", 24);
    list.put("s_busy_pipe_reg", 2);
    list.put("s_output_reg", GENERIC_PARAM_BUSWIDTH);
  }
  
  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    String lang = out.isVhdl ? "VHLD" : "Verilog";
    out.comment("Multi-cycle random number generator component implementation");
    out.stmt("");
    if (out.isVhdl) {
      out.stmt("   Q            <= s_output_reg;");
      out.stmt("   s_InitSeed   <= X\"0005DEECE66D\" WHEN Seed = 0 ELSE");
      out.stmt("                   X\"0000\"&std_logic_vector(to_unsigned(Seed,32));");
      out.stmt("   s_reset      <= '1' WHEN s_reset_reg /= \"010\" ELSE '0';");
      out.stmt("   s_reset_next <= \"010\" WHEN (s_reset_reg = \"101\" OR");
      out.stmt("                               s_reset_reg = \"010\") AND");
      out.stmt("                               clear = '0' ELSE");
      out.stmt("                   \"101\" WHEN s_reset_reg = \"001\" ELSE");
      out.stmt("                   \"001\";");
      out.stmt("   s_start      <= '1' WHEN (ClockEnable = '1' AND enable = '1') OR");
      out.stmt("                            (s_reset_reg = \"101\" AND clear = '0') ELSE '0';");
      out.stmt("   s_mult_shift_next <= (OTHERS => '0') WHEN s_reset = '1' ELSE");
      out.stmt("                        X\"5DEECE66D\" WHEN s_start_reg = '1' ELSE");
      out.stmt("                        '0'&s_mult_shift_reg(35 DOWNTO 1);");
      out.stmt("   s_seed_shift_next <= (OTHERS => '0') WHEN s_reset = '1' ELSE");
      out.stmt("                        s_current_seed WHEN s_start_reg = '1' ELSE");
      out.stmt("                        s_seed_shift_reg(46 DOWNTO 0)&'0';");
      out.stmt("   s_mult_busy       <= '0' WHEN s_mult_shift_reg = X\"000000000\" ELSE '1';");
      out.stmt("");
      out.stmt("   s_mac_lo_in_1     <= (OTHERS => '0') WHEN s_start_reg = '1' OR");
      out.stmt("                                             s_reset = '1' ELSE");
      out.stmt("                        '0'&s_mac_lo_reg(23 DOWNTO 0);");
      out.stmt("   s_mac_lo_in_2     <= '0'&X\"00000B\"");
      out.stmt("                           WHEN s_start_reg = '1' ELSE");
      out.stmt("                        '0'&s_seed_shift_reg(23 DOWNTO 0) ");
      out.stmt("                           WHEN s_mult_shift_reg(0) = '1' ELSE");
      out.stmt("                        (OTHERS => '0');");
      out.stmt("   s_mac_hi_in_2     <= (OTHERS => '0') WHEN s_start_reg = '1' ELSE");
      out.stmt("                        s_mac_hi_reg;");
      out.stmt("   s_mac_hi_1_next   <= s_seed_shift_reg(47 DOWNTO 24) ");
      out.stmt("                           WHEN s_mult_shift_reg(0) = '1' ELSE");
      out.stmt("                        (OTHERS => '0');");
      out.stmt("   s_busy_pipe_next  <= \"00\" WHEN s_reset = '1' ELSE");
      out.stmt("                        s_busy_pipe_reg(0)&s_mult_busy;");
      out.stmt("");
      out.stmt("   make_current_seed : PROCESS( GlobalClock , s_busy_pipe_reg , s_reset )");
      out.stmt("   BEGIN");
      out.stmt("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      out.stmt("         IF (s_reset = '1') THEN s_current_seed <= s_InitSeed;");
      out.stmt("         ELSIF (s_busy_pipe_reg = \"10\") THEN");
      out.stmt("            s_current_seed <= s_mac_hi_reg&s_mac_lo_reg(23 DOWNTO 0 );");
      out.stmt("         END IF;");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_current_seed;");
      out.stmt("   ");
      out.stmt("   make_shift_regs : PROCESS(GlobalClock,s_mult_shift_next,s_seed_shift_next,");
      out.stmt("                             s_mac_lo_in_1,s_mac_lo_in_2)");
      out.stmt("   BEGIN");
      out.stmt("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      out.stmt("         s_mult_shift_reg <= s_mult_shift_next;");
      out.stmt("         s_seed_shift_reg <= s_seed_shift_next;");
      out.stmt("         s_mac_lo_reg     <= std_logic_vector(unsigned(s_mac_lo_in_1)+unsigned(s_mac_lo_in_2));");
      out.stmt("         s_mac_hi_1_reg   <= s_mac_hi_1_next;");
      out.stmt("         s_mac_hi_reg     <= std_logic_vector(unsigned(s_mac_hi_1_reg)+unsigned(s_mac_hi_in_2)+");
      out.stmt("                             unsigned(s_mac_lo_reg(24 DOWNTO 24)));");
      out.stmt("         s_busy_pipe_reg  <= s_busy_pipe_next;");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_shift_regs;");
      out.stmt("");
      out.stmt("   make_start_reg : PROCESS(GlobalClock,s_start)");
      out.stmt("   BEGIN");
      out.stmt("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      out.stmt("         s_start_reg <= s_start;");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_start_reg;");
      out.stmt("");
      out.stmt("   make_reset_reg : PROCESS(GlobalClock,s_reset_next)");
      out.stmt("   BEGIN");
      out.stmt("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      out.stmt("         s_reset_reg <= s_reset_next;");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_reset_reg;");
      out.stmt("");
      out.stmt("   make_output : PROCESS( GlobalClock , s_reset , s_InitSeed )");
      out.stmt("   BEGIN");
      out.stmt("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      out.stmt("         IF (s_reset = '1') THEN s_output_reg <= s_InitSeed( (BusWidth-1) DOWNTO 0 );");
      out.stmt("         ELSIF (ClockEnable = '1' AND enable = '1') THEN");
      out.stmt("            s_output_reg <= s_current_seed((BusWidth+11) DOWNTO 12);");
      out.stmt("         END IF;");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_output;");
    } else {
      out.stmt("   assign Q = s_output_reg;");
      out.stmt("   assign s_InitSeed = (Seed) ? Seed : 48'h5DEECE66D;");
      out.stmt("   assign s_reset = (s_reset_reg==3'b010) ? 1'b1 : 1'b0;");
      out.stmt("   assign s_reset_next = (((s_reset_reg == 3'b101)|");
      out.stmt("                           (s_reset_reg == 3'b010))&~clear) ? 3'b010 :");
      out.stmt("                         (s_reset_reg==3'b001) ? 3'b101 : 3'b001;");
      out.stmt("   assign s_start = ((ClockEnable&enable)|((s_reset_reg == 3'b101)&~clear)) ? 1'b1 : 1'b0;");
      out.stmt("   assign s_mult_shift_next = (s_reset) ? 36'd0 :");
      out.stmt("                              (s_start_reg) ? 36'h5DEECE66D : {1'b0,s_mult_shift_reg[35:1]};");
      out.stmt("   assign s_seed_shift_next = (s_reset) ? 48'd0 :");
      out.stmt("                              (s_start_reg) ? s_current_seed : {s_seed_shift_reg[46:0],1'b0};");
      out.stmt("   assign s_mult_busy = (s_mult_shift_reg == 0) ? 1'b0 : 1'b1;");
      out.stmt("   assign s_mac_lo_in_1 = (s_start_reg|s_reset) ? 25'd0 : {1'b0,s_mac_lo_reg[23:0]};");
      out.stmt("   assign s_mac_lo_in_2 = (s_start_reg) ? 25'hB :");
      out.stmt("                          (s_mult_shift_reg[0]) ? {1'b0,s_seed_shift_reg[23:0]} : 25'd0;");
      out.stmt("   assign s_mac_hi_in_2 = (s_start_reg) ? 0 : s_mac_hi_reg;");
      out.stmt("   assign s_mac_hi_1_next = (s_mult_shift_reg[0]) ? s_seed_shift_reg[47:24] : 0;");
      out.stmt("   assign s_busy_pipe_next = (s_reset) ? 2'd0 : {s_busy_pipe_reg[0],s_mult_busy};");
      out.stmt("");
      out.stmt("   always @(posedge GlobalClock)");
      out.stmt("   begin");
      out.stmt("      if (s_reset) s_current_seed <= s_InitSeed;");
      out.stmt("      else if (s_busy_pipe_reg == 2'b10) s_current_seed <= {s_mac_hi_reg,s_mac_lo_reg[23:0]};");
      out.stmt("   end");
      out.stmt("");
      out.stmt("   always @(posedge GlobalClock)");
      out.stmt("   begin");
      out.stmt("         s_mult_shift_reg <= s_mult_shift_next;");
      out.stmt("         s_seed_shift_reg <= s_seed_shift_next;");
      out.stmt("         s_mac_lo_reg     <= s_mac_lo_in_1+s_mac_lo_in_2;");
      out.stmt("         s_mac_hi_1_reg   <= s_mac_hi_1_next;");
      out.stmt("         s_mac_hi_reg     <= s_mac_hi_1_reg+s_mac_hi_in_2+s_mac_lo_reg[24];");
      out.stmt("         s_busy_pipe_reg  <= s_busy_pipe_next;");
      out.stmt("         s_start_reg      <= s_start;");
      out.stmt("         s_reset_reg      <= s_reset_next;");
      out.stmt("   end");
      out.stmt("");
      out.stmt("   always @(posedge GlobalClock)");
      out.stmt("   begin");
      out.stmt("      if (s_reset) s_output_reg <= s_InitSeed[(BusWidth-1):0];");
      out.stmt("      else if (ClockEnable&enable) s_output_reg <= s_current_seed[(BusWidth+11):12];");
      out.stmt("   end");
    }
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }
}
