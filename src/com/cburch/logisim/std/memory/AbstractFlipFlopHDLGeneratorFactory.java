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

public class AbstractFlipFlopHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private String name, displayName;
  private String[] inPorts;
  private String vhdlUpdate, verilogUpdate;

  AbstractFlipFlopHDLGeneratorFactory(HDLCTX ctx, String name, String displayName,
      String[] inPorts, String vhdlUpdate, String verilogUpdate) {
    super(ctx);
    this.name = name;
    this.displayName = displayName;
    this.inPorts = inPorts;
    this.vhdlUpdate = vhdlUpdate;
    this.verilogUpdate = verilogUpdate;
  }

  protected final static int GENERIC_PARAM_ACTIVELEVEL = -1;
  
  @Override
  public String getComponentStringIdentifier() { return name; }

  @Override
  public String GetSubDir() { return "memory"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Reset", 1);
    list.put("Preset", 1);
    list.put("Tick", 1);
    list.put("Clock", 1);
    for (String p : inPorts)
      list.put(p, 1);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Q", 1);
    list.put("Q_bar", 1);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    list.put(GENERIC_PARAM_ACTIVELEVEL, "ActiveLevel");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);

    int CLK = info.NrOfEnds()-5;
    boolean gatedClk = GetClockNetName(info, CLK, nets).equals("");
    // fixme: differs from ShiftRegister and others, but only slightly
    if (gatedClk && edgeTriggered(attrs))
        err.AddWarning("Found a gated clock for component \""
            + displayName + "\" in circuit \""
            + nets.getCircuitName() + "\"");

    boolean activelo = gatedClk &&
        (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING
        || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
        || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW);

    list.put("ActiveLevel", activelo ? 0 : 1);
    list.put("BusWidth", w);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    // all nearly same as Register
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);
    int NPINS = info.NrOfEnds();
    int CLK = NPINS-5;
    int OUT = NPINS-4;
    int INV = NPINS-3;
    int RST = NPINS-2;
    int PRE = NPINS-1;
    boolean hasClk = info.EndIsConnected(CLK);
    if (!hasClk)
      err.AddSevereWarning("Component \"" + displayName + "\" in circuit \""
          + nets.getCircuitName() + "\" has no clock connection");

    String clk = GetClockNetName(info, CLK, nets);
    boolean gatedClk = clk.equals("");
    boolean activelo = (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING
        || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
        || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW);

    boolean vhdl = lang.equals("VHDL");
    String zero = vhdl ? "'0'" : "1'b0";
    String one = vhdl ? "'1'" : "1'b1";
    String idx = vhdl ? "(%d)" : "[%]"; // fixme: these should be in base class at minimum!

    list.putAll(GetNetMap("Reset", true, info, RST, err, lang, nets));
    list.putAll(GetNetMap("Preset", true, info, PRE, err, lang, nets));

    if (!hasClk) {
      list.put("Tick", zero);
      list.put("Clock", zero);
    } else if (!gatedClk && edgeTriggered(attrs)) {
      list.put("Clock", String.format(clk+idx, ClockHDLGeneratorFactory.GlobalClockIndex));
      if (nets.RequiresGlobalClockConnection())
        list.put("Tick", String.format(clk+idx, ClockHDLGeneratorFactory.GlobalClockIndex)); // bug?
      else if (activelo)
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
        list.put("Clock", GetNetName(info, CLK, true, lang, nets));
    }
    list.putAll(GetNetMap("Q", true, info, OUT, err, lang, nets));
    list.putAll(GetNetMap("Q_bar", true, info, INV, err, lang, nets));
    for (int i = 0; i < inPorts.length; i++)
      list.putAll(GetNetMap(inPorts[i], true, info, i, err, lang, nets));
  }
  
  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    list.put("s_next_state", 1);
  }

  @Override
  public void registers(SortedMap<String, Integer> list, AttributeSet attrs, String lang) {
    list.put("s_current_state_reg", (lang.equals("VHDL")) ? 1 : 2);
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    if (out.isVhdl) {
      out.stmt("   Q     <= s_current_state_reg;");
      out.stmt("   Q_bar <= NOT(s_current_state_reg);");
      out.stmt("");
      out.stmt("   " + vhdlUpdate);
      out.stmt("");
      out.stmt("   make_memory : PROCESS( clock , Reset , Preset , Tick , s_next_state )");
      out.stmt("      VARIABLE temp : std_logic_vector(0 DOWNTO 0);");
      out.stmt("   BEGIN");
      out.stmt("      temp := std_logic_vector(to_unsigned(ActiveLevel,1));");
      out.stmt("      IF (Reset = '1') THEN s_current_state_reg <= '0';");
      out.stmt("      ELSIF (Preset = '1') THEN s_current_state_reg <= '1';");
      if (edgeTriggered(attrs))
        out.stmt("      ELSIF (Clock'event AND (Clock = temp(0))) THEN");
      else
        out.stmt("      ELSIF (Clock = temp(0)) THEN");
      out.stmt("         IF (Tick = '1') THEN");
      out.stmt("            s_current_state_reg <= s_next_state;");
      out.stmt("         END IF;");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_memory;");
    } else {

      out.stmt("   assignQ     = s_current_state_reg[ActiveLevel];");
      out.stmt("   assignQ_bar = ~(s_current_state_reg[ActiveLevel]);");
      out.stmt("");
      out.stmt("   " + verilogUpdate);
      out.stmt("");
      out.comment("define the initial state (hdl simulation only)");
      out.stmt("   initial");
      out.stmt("   begin");
      out.stmt("      s_current_state_reg = 0;");
      out.stmt("   end");
      out.stmt("");
      if (edgeTriggered(attrs)) {
        out.stmt("   always @(posedge Reset or posedge Preset or negedge Clock)");
        out.stmt("   begin");
        out.stmt("      if (Reset) s_current_state_reg[0] <= 1'b0;");
        out.stmt("      else if (Preset) s_current_state_reg[0] <= 1'b1;");
        out.stmt("      else if (Tick) s_current_state_reg[0] <= s_next_state;");
        out.stmt("   end");
        out.stmt("");
        out.stmt("   always @(posedge Reset or posedge Preset or posedge Clock)");
        out.stmt("   begin");
        out.stmt("      if (Reset) s_current_state_reg[1] <= 1'b0;");
        out.stmt("      else if (Preset) s_current_state_reg[1] <= 1'b1;");
        out.stmt("      else if (Tick) s_current_state_reg[1] <= s_next_state;");
        out.stmt("   end");
      } else {
        out.stmt("   always @(*)");
        out.stmt("   begin");
        out.stmt("      if (Reset) s_current_state_reg <= 2'b0;");
        out.stmt("      else if (Preset) s_current_state_reg <= 2'b1;");
        out.stmt("      else if (Tick & (Clock == ActiveLevel)) s_current_state_reg <= {s_next_state,s_next_state};");
        out.stmt("   end");
      }
    }
    out.stmt("");
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }

  private boolean edgeTriggered(AttributeSet attrs) {
    return attrs.containsAttribute(StdAttr.EDGE_TRIGGER)
        || (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING)
        || (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING);
  }

}
