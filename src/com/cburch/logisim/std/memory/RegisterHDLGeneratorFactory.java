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

public class RegisterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public RegisterHDLGeneratorFactory(HDLCTX ctx) {
    super(ctx, "${TRIGGER}Register", "REGISTER");
  }
  
  protected final static int GENERIC_PARAM_BUSWIDTH = -1;
  protected final static int GENERIC_PARAM_ACTIVELEVEL = -2;

  @Override
  public String GetSubDir() { return "memory"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Reset", 1);
    list.put("ClockEnable", 1);
    list.put("Tick", 1);
    list.put("Clock", 1);
    list.put("D", GENERIC_PARAM_BUSWIDTH);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Q", GENERIC_PARAM_BUSWIDTH);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    list.put(GENERIC_PARAM_ACTIVELEVEL, "ActiveLevel");
    list.put(GENERIC_PARAM_BUSWIDTH, "BusWidth");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);

    boolean gatedClk = GetClockNetName(info, Register.CK, nets).equals("");
    // fixme: differs from ShiftRegister and others
    if (gatedClk && edgeTriggered(attrs))
      err.AddWarning("Found a gated clock for component \"Register\" in circuit \""
          + nets.getCircuitName() + "\"");

    boolean activelo = gatedClk &&
        (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
         || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW);

    list.put("ActiveLevel", activelo ? 0 : 1);
    list.put("BusWidth", w);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);
    boolean hasClk = info.EndIsConnected(Register.CK);
    if (!hasClk)
      err.AddSevereWarning("Component \"Register\" in circuit \""
          + nets.getCircuitName() + "\" has no clock connection");

    String clk = GetClockNetName(info, Register.CK, nets);
    boolean gatedClk = clk.equals("");
    boolean activelo = attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING
        || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW;

    boolean vhdl = lang.equals("VHDL");
    String zero = vhdl ? "'0'" : "1'b0";
    String one = vhdl ? "'1'" : "1'b1";
    String idx = vhdl ? "(%d)" : "[%]"; // fixme: these should be in base class at minimum!

    list.putAll(GetNetMap("Reset", true, info, Register.CLR, err, lang, nets));
    list.putAll(GetNetMap("ClockEnable", false, info, Register.EN, err, lang, nets));

    if (!hasClk) {
      list.put("Tick", zero);
      list.put("Clock", zero);
    } else if (!gatedClk && edgeTriggered(attrs)) {
      list.put("Clock", String.format(clk+idx, ClockHDLGeneratorFactory.GlobalClockIndex));
      if (activelo)
        list.put("Tick", String.format(clk+idx, ClockHDLGeneratorFactory.NegativeEdgeTickIndex));
      else
        list.put("Tick", String.format(clk+idx, ClockHDLGeneratorFactory.PositiveEdgeTickIndex));
    } else {
      list.put("Tick", one);
      if (!gatedClk && activelo)
        list.put("Clock", String.format(clk+idx, ClockHDLGeneratorFactory.InvertedDerivedClockIndex));
      else if (!gatedClk)
        list.put("Clock", String.format(clk+idx, ClockHDLGeneratorFactory.DerivedClockIndex));
      else
        list.put("Clock", GetNetName(info, Register.CK, true, lang, nets));
    }
    String d = "D" + (vhdl && w == 1 ? "(0)" : "");
    String q = "Q" + (vhdl && w == 1 ? "(0)" : "");
    list.putAll(GetNetMap(d, true, info, Register.IN, err, lang, nets));
    list.putAll(GetNetMap(q, true, info, Register.OUT, err, lang, nets));
  }

  @Override
  public void registers(SortedMap<String, Integer> list, AttributeSet attrs, String lang) {
    System.out.println("BUG? (here and elsewhere)... trigger dependence");
    list.put("s_state_reg", GENERIC_PARAM_BUSWIDTH);
    if (lang.equals("Verilog") & edgeTriggered(attrs))
      list.put("s_state_reg_neg_edge", GENERIC_PARAM_BUSWIDTH);
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    if (out.isVhdl) {
      out.stmt("   Q <= s_state_reg;");
      out.stmt("");
      out.stmt("   make_memory : PROCESS( clock , Reset , ClockEnable , Tick , D )");
      out.stmt("   BEGIN");
      out.stmt("      IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');");
      if (edgeTriggered(attrs)) {
        out.stmt("      ELSIF (ActiveLevel = 1) THEN");
        out.stmt("         IF (Clock'event AND (Clock = '1')) THEN");
        out.stmt("            IF (ClockEnable = '1' AND Tick = '1') THEN");
        out.stmt("               s_state_reg <= D;");
        out.stmt("            END IF;");
        out.stmt("         END IF;");
        out.stmt("      ELSIF (ActiveLevel = 0) THEN");
        out.stmt("         IF (Clock'event AND (Clock = '0')) THEN");
        out.stmt("         IF (ClockEnable = '1' AND Tick = '1') THEN");
        out.stmt("               s_state_reg <= D;");
        out.stmt("            END IF;");
        out.stmt("         END IF;");
        //out.stmt("      ELSIF (Clock'event AND (Clock = std_logic_vector(to_unsigned("
        //    + "ActiveLevel,1)) )) THEN");
      } else {
        out.stmt("      ELSIF (ActiveLevel = 1) THEN");
        out.stmt("         IF (Clock = '1') THEN");
        out.stmt("            IF (ClockEnable = '1' AND Tick = '1') THEN");
        out.stmt("               s_state_reg <= D;");
        out.stmt("            END IF;");
        out.stmt("         END IF;");
        out.stmt("      ELSIF (ActiveLevel = 0) THEN");
        out.stmt("         IF (Clock = '0') THEN");
        out.stmt("            IF (ClockEnable = '1' AND Tick = '1') THEN");
        out.stmt("               s_state_reg <= D;");
        out.stmt("            END IF;");
        out.stmt("         END IF;");
        //out.stmt("      ELSIF (Clock = std_logic_vector(to_unsigned("
        //    + "ActiveLevel,1)) ) THEN");
      }
      //out.stmt("         IF (ClockEnable = '1' AND Tick = '1') THEN");
      //out.stmt("            s_state_reg <= D;");
      //out.stmt("         END IF;");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_memory;");
    } else {
      if (!edgeTriggered(attrs)) {
        out.stmt("   assign Q = s_state_reg;");
        out.stmt("");
        out.stmt("   always @(*)");
        out.stmt("   begin");
        out.stmt("      if (Reset) s_state_reg <= 0;");
        out.stmt("      else if ((Clock==ActiveLevel)&ClockEnable&Tick) s_state_reg <= D;");
        out.stmt("   end");
      } else {
        out.stmt("   assign Q = (ActiveLevel) ? s_state_reg : s_state_reg_neg_edge;");
        out.stmt("");
        out.stmt("   always @(posedge Clock or posedge Reset)");
        out.stmt("   begin");
        out.stmt("      if (Reset) s_state_reg <= 0;");
        out.stmt("      else if (ClockEnable&Tick) s_state_reg <= D;");
        out.stmt("   end");
        out.stmt("");
        out.stmt("   always @(negedge Clock or posedge Reset)");
        out.stmt("   begin");
        out.stmt("      if (Reset) s_state_reg_neg_edge <= 0;");
        out.stmt("      else if (ClockEnable&Tick) s_state_reg_neg_edge <= D;");
        out.stmt("   end");
      }
    }
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }

  private boolean edgeTriggered(AttributeSet attrs) {
    return ((attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING)
        || (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING));
  }

}
