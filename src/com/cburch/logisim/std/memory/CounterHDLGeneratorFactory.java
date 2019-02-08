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

public class CounterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public CounterHDLGeneratorFactory(HDLCTX ctx) {
    super(ctx, "LogisimCounter", "COUNTER");
  }

  protected final static int GENERIC_PARAM_BUSWIDTH = -1;
  protected final static int GENERIC_PARAM_MAXVAL = -2;
  protected final static int GENERIC_PARAM_CLKEDGE = -3;
  protected final static int GENERIC_PARAM_MODE = -4;

  @Override
  public String GetSubDir() { return "memory"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("GlobalClock", 1);
    list.put("ClockEnable", 1);
    list.put("LoadData", GENERIC_PARAM_BUSWIDTH);
    list.put("clear", 1);
    list.put("load", 1);
    list.put("Up_n_Down", 1);
    list.put("Enable", 1);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("CountValue", GENERIC_PARAM_BUSWIDTH);
    list.put("CompareOut", 1);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    list.put(GENERIC_PARAM_BUSWIDTH, "BusWidth");
    list.put(GENERIC_PARAM_MAXVAL, "MaxVal");
    list.put(GENERIC_PARAM_CLKEDGE, "ClkEdge");
    list.put(GENERIC_PARAM_MODE, "Mode");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int mode;
    if (attrs.containsAttribute(Counter.ATTR_ON_GOAL)) {
      if (attrs.getValue(Counter.ATTR_ON_GOAL) == Counter.ON_GOAL_STAY)
        mode = 1;
      else if (attrs.getValue(Counter.ATTR_ON_GOAL) == Counter.ON_GOAL_CONT)
        mode = 2;
      else if (attrs.getValue(Counter.ATTR_ON_GOAL) == Counter.ON_GOAL_LOAD)
        mode = 3;
      else
        mode = 0;
    } else {
      mode = 1;
    }
    list.put("BusWidth", width(attrs));
    list.put("MaxVal", attrs.getValue(Counter.ATTR_MAX).intValue());
    boolean activelo = GetClockNetName(info, Counter.CK, nets).isEmpty()
        && attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING;
    list.put("ClkEdge", activelo ? 0 : 1);
    list.put("Mode", mode);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    
    boolean vhdl = lang.equals("VHDL");
    String zero = vhdl ? "'0'" : "1'b0";
    String one = vhdl ? "'1'" : "1'b1";
    String idx = vhdl ? "(%d)" : "[%]"; // fixme: these should be in base class at minimum!

    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);

    if (!info.EndIsConnected(Counter.CK)) {
      err.AddSevereWarning("Component \"Counter\" in circuit \""
          + nets.getCircuitName() + "\" has no clock connection");
      list.put("GlobalClock", zero);
      list.put("ClockEnable", zero);
    } else {
      String clk = GetClockNetName(info, Counter.CK, nets);
      if (clk.isEmpty()) {
        err.AddSevereWarning("Component \"Counter\" in circuit \""
            + nets.getCircuitName()
            + "\" has a none-clock-component forced clock!\n"
            + "        Functional differences between Logisim simulation and hardware can be expected!");
        list.putAll(GetNetMap("GlobalClock", true, info, Counter.CK, err, lang, nets));
        list.put("ClockEnable", one);
      } else {
        int ClockBusIndex = ClockHDLGeneratorFactory.DerivedClockIndex;
        if (nets.RequiresGlobalClockConnection()) {
          ClockBusIndex = ClockHDLGeneratorFactory.GlobalClockIndex;
        } else {
          if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_LOW)
            ClockBusIndex = ClockHDLGeneratorFactory.InvertedDerivedClockIndex;
          else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_RISING)
            ClockBusIndex = ClockHDLGeneratorFactory.PositiveEdgeTickIndex;
          else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING)
            ClockBusIndex = ClockHDLGeneratorFactory.InvertedDerivedClockIndex;
        }
        list.put("GlobalClock", String.format(clk+idx, ClockHDLGeneratorFactory.GlobalClockIndex));
        list.put("ClockEnable", String.format(clk+idx, ClockBusIndex));
      }
    }
    String ld = "LoadData" + (vhdl & (w == 1) ? "(0)" : "");
    list.putAll(GetNetMap(ld, true, info, Counter.IN, err, lang, nets));
    list.putAll(GetNetMap("clear", true, info, Counter.CLR, err, lang, nets));
    list.putAll(GetNetMap("load", true, info, Counter.LD, err, lang, nets));
    list.putAll(GetNetMap("Enable", false, info, Counter.EN, err, lang, nets));
    list.putAll(GetNetMap("Up_n_Down", false, info, Counter.UD, err, lang, nets));

    String cv = "CountValue" + (vhdl & (w == 1) ? "(0)" : "");
    list.putAll(GetNetMap(cv, true, info, Counter.OUT, err, lang, nets));
    list.putAll(GetNetMap("CompareOut", true, info, Counter.CARRY, err, lang, nets));
  }


  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    list.put("s_real_enable", 1);
  }

  @Override
  public void registers(SortedMap<String, Integer> list, AttributeSet attrs, String lang) {
    list.put("s_next_counter_value", GENERIC_PARAM_BUSWIDTH); // verilog ?
    list.put("s_carry", 1); // verilog ?
    list.put("s_counter_value", GENERIC_PARAM_BUSWIDTH);
    if (lang.equals("Verilog"))
      list.put("s_counter_value_neg_edge", GENERIC_PARAM_BUSWIDTH);
  }
  
  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    out.comment("Counter functionality:");
    out.comment("   Load Count | mode");
    out.comment("   -----------|--------------------");
    out.comment("     0    0   | halt");
    out.comment("     0    1   | count up (default)");
    out.comment("     1    0   | load");
    out.comment("     1    1   | count down");
    if (out.isVhdl) {
      out.stmt("");
      out.stmt("   CompareOut   <= s_carry;");
      out.stmt("   CountValue   <= s_counter_value;");
      out.stmt("");
      out.stmt("   make_carry : PROCESS( Up_n_Down ,");
      out.stmt("                         s_counter_value )");
      out.stmt("   BEGIN");
      out.stmt("      IF (Up_n_Down = '0') THEN");
      out.stmt("         IF (s_counter_value = std_logic_vector(to_unsigned(0,width))) THEN");
      out.stmt("            s_carry <= '1';");
      out.stmt("         ELSE");
      out.stmt("            s_carry <= '0';");
      out.stmt("         END IF; -- Down counting");
      out.stmt("                           ELSE");
      out.stmt("         IF (s_counter_value = std_logic_vector(to_unsigned(max_val,width))) THEN");
      out.stmt("            s_carry <= '1';");
      out.stmt("         ELSE");
      out.stmt("            s_carry <= '0';");
      out.stmt("         END IF; -- Up counting");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_carry;");
      out.stmt("");
      out.stmt("   s_real_enable <= '0' WHEN (load = '0' AND enable = '0') -- Counter disabled");
      out.stmt("                             OR");
      out.stmt("                             (mode = 1 AND s_carry = '1' AND load = '0') -- Stay at value situation");
      out.stmt("                        ELSE ClockEnable;");
      out.stmt("");
      out.stmt("   make_next_value : PROCESS( load , Up_n_Down , s_counter_value ,");
      out.stmt("                              LoadData , s_carry )");
      out.stmt("      VARIABLE v_downcount : std_logic;         ");
      out.stmt("   BEGIN");
      out.stmt("      v_downcount := NOT(Up_n_Down);");
      out.stmt("      IF ((load = '1') OR -- load condition");
      out.stmt("          (mode = 3 AND s_carry = '1')    -- Wrap load condition");
      out.stmt("         ) THEN s_next_counter_value <= LoadData;");
      out.stmt("           ELSE");
      out.stmt("         CASE (mode) IS");
      out.stmt("            WHEN  0     => IF (s_carry = '1') THEN");
      out.stmt("                              IF (v_downcount = '1') THEN ");
      out.stmt("                                 s_next_counter_value <= std_logic_vector(to_unsigned(max_val,width));");
      out.stmt("                                                     ELSE ");
      out.stmt("                                 s_next_counter_value <= (OTHERS => '0');");
      out.stmt("                              END IF;");
      out.stmt("                                              ELSE");
      out.stmt("                              IF (v_downcount = '1') THEN ");
      out.stmt("                                 s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) - 1);");
      out.stmt("                                                     ELSE ");
      out.stmt("                                 s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) + 1);");
      out.stmt("                              END IF;");
      out.stmt("                           END IF;");
      out.stmt("            WHEN OTHERS => IF (v_downcount = '1') THEN ");
      out.stmt("                              s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) - 1);");
      out.stmt("                                                  ELSE ");
      out.stmt("                              s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) + 1);");
      out.stmt("                           END IF;");
      out.stmt("         END CASE;");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_next_value;");
      out.stmt("");
      out.stmt("   make_flops : PROCESS( GlobalClock , s_real_enable , clear , s_next_counter_value )");
      out.stmt("      VARIABLE temp : std_logic_vector(0 DOWNTO 0);");
      out.stmt("   BEGIN");
      out.stmt("      temp := std_logic_vector(to_unsigned(ClkEdge,1));");
      out.stmt("      IF (clear = '1') THEN s_counter_value <= (OTHERS => '0');");
      out.stmt("      ELSIF (GlobalClock'event AND (GlobalClock = temp(0))) THEN");
      out.stmt("         IF (s_real_enable = '1') THEN s_counter_value <= s_next_counter_value;");
      out.stmt("         END IF;");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_flops;");
    } else {
      out.stmt("");
      out.stmt("   assign CompareOut = s_carry;");
      out.stmt("   assign CountValue = (ClkEdge) ? s_counter_value : s_counter_value_neg_edge;");
      out.stmt("");
      out.stmt("   always@(*)");
      out.stmt("   begin");
      out.stmt("      if (Up_n_Down)");
      out.stmt("         begin");
      out.stmt("            if (ClkEdge)");
      out.stmt("               s_carry = (s_counter_value == max_val) ? 1'b1 : 1'b0;");
      out.stmt("            else");
      out.stmt("               s_carry = (s_counter_value_neg_edge == max_val) ? 1'b1 : 1'b0;");
      out.stmt("         end");
      out.stmt("      else");
      out.stmt("         begin");
      out.stmt("            if (ClkEdge)");
      out.stmt("               s_carry = (s_counter_value == 0) ? 1'b1 : 1'b0;");
      out.stmt("            else");
      out.stmt("               s_carry = (s_counter_value_neg_edge == 0) ? 1'b1 : 1'b0;");
      out.stmt("         end");
      out.stmt("   end");
      out.stmt("");
      out.stmt("   assign s_real_enable = ((~(load)&~(Enable))|");
      out.stmt("                           ((mode==1)&s_carry&~(load))) ? 1'b0 : ClockEnable;");
      out.stmt("");
      out.stmt("   always @(*)");
      out.stmt("   begin");
      out.stmt("      if ((load)|((mode==3)&s_carry))");
      out.stmt("         s_next_counter_value = LoadData;");
      out.stmt("      else if ((mode==0)&s_carry&Up_n_Down)");
      out.stmt("         s_next_counter_value = 0;");
      out.stmt("      else if ((mode==0)&s_carry)");
      out.stmt("         s_next_counter_value = max_val;");
      out.stmt("      else if (Up_n_Down)");
      out.stmt("         begin");
      out.stmt("            if (ClkEdge)");
      out.stmt("               s_next_counter_value = s_counter_value + 1;");
      out.stmt("            else");
      out.stmt("               s_next_counter_value = s_counter_value_neg_edge + 1;");
      out.stmt("         end");
      out.stmt("      else");
      out.stmt("         begin");
      out.stmt("            if (ClkEdge)");
      out.stmt("               s_next_counter_value = s_counter_value - 1;");
      out.stmt("            else");
      out.stmt("               s_next_counter_value = s_counter_value_neg_edge - 1;");
      out.stmt("         end");
      out.stmt("   end");
      out.stmt("");
      out.stmt("   always @(posedge GlobalClock or posedge clear)");
      out.stmt("   begin");
      out.stmt("       if (clear) s_counter_value <= 0;");
      out.stmt("       else if (s_real_enable) s_counter_value <= s_next_counter_value;");
      out.stmt("   end");
      out.stmt("");
      out.stmt("   always @(negedge GlobalClock or posedge clear)");
      out.stmt("   begin");
      out.stmt("       if (clear) s_counter_value_neg_edge <= 0;");
      out.stmt("       else if (s_real_enable) s_counter_value_neg_edge <= s_next_counter_value;");
      out.stmt("   end");
      out.stmt("");
    }
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }
}
