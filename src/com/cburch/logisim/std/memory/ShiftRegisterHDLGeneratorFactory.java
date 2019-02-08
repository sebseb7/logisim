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

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;

public class ShiftRegisterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public ShiftRegisterHDLGeneratorFactory(HDLCTX ctx) {
    super(ctx, "Shift_Register", "SHIFTER");
  }

  @Override
  public ArrayList<String> GetArchitecture(/*Netlist TheNetlist,*/ /*AttributeSet attrs,*/
      Map<String, File> MemInitFiles, String ComponentName /*, FPGAReport err, String lang*/) {
    if (_nets == null) throw new IllegalStateException();
    ArrayList<String> C = new ArrayList<String>();
    C.addAll(FileWriter.getGenerateRemark(ComponentName, _lang, _nets.projName()));
    if (_lang.equals("VHDL")) {
      C.add("ARCHITECTURE NoPlatformSpecific OF SingleBitShiftRegStage IS");
      C.add("   SIGNAL s_state_reg  : std_logic_vector(Stages-1 DOWNTO 0);");
      C.add("   SIGNAL s_state_next : std_logic_vector(Stages-1 DOWNTO 0);");
      C.add("BEGIN");
      C.add("   Q        <= s_state_reg;");
      C.add("   ShiftOut <= s_state_reg(Stages-1);");
      C.add("");
      C.add("   s_state_next <= D WHEN ParLoad = '1' ELSE s_state_reg((Stages-2) DOWNTO 0)&ShiftIn;");
      C.add("");
      C.add("   make_state : PROCESS(Clock, ShiftEnable, Tick, Reset, s_state_next, ParLoad)");
      C.add("      VARIABLE temp : std_logic_vector( 0 DOWNTO 0 );");
      C.add("   BEGIN");
      C.add("      temp := std_logic_vector(to_unsigned("+"Trigger"+ ",1));");
      C.add("      IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');");
      C.add("      ELSIF (Clock'event AND (Clock = temp(0) )) THEN");
      C.add("         IF (((ShiftEnable = '1') OR (ParLoad = '1')) AND (Tick = '1')) THEN");
      C.add("            s_state_reg <= s_state_next;");
      C.add("         END IF;");
      C.add("      END IF;");
      C.add("   END PROCESS make_state;");
      C.add("END NoPlatformSpecific;");
    } else {
      C.add("module SingleBitShiftRegStage ( Reset, Tick, Clock, ShiftEnable, ParLoad, ");
      C.add("                           ShiftIn, D, ShiftOut, Q );");
      C.add("   parameter Stages = 1;");
      C.add("   parameter Trigger = 1;");
      C.add("");
      C.add("   input Reset;");
      C.add("   input Tick;");
      C.add("   input Clock;");
      C.add("   input ShiftEnable;");
      C.add("   input ParLoad;");
      C.add("   input ShiftIn;");
      C.add("   input[Stages:0] D;");
      C.add("   output ShiftOut;");
      C.add("   output[Stages:0] Q;");
      C.add("");
      C.add("   wire[Stages:0] s_state_next;");
      C.add("   reg[Stages:0] s_state_reg;");
      C.add("   reg[Stages:0] s_state_reg_neg_edge;");
      C.add("");
      C.add("   assign Q            = (Trigger) ? s_state_reg : s_state_reg_neg_edge;");
      C.add("   assign ShiftOut     = (Trigger) ? s_state_reg[Stages-1] : s_state_reg_neg_edge[Stages-1];");
      C.add("   assign s_state_next = (ParLoad) ? D :");
      C.add("                         (Trigger) ? {s_state_reg[Stages-2:1],ShiftIn} :");
      C.add("                                     {s_state_reg_neg_edge[Stages-2:1],ShiftIn};");
      C.add("");
      C.add("   always @(posedge Clock or posedge Reset)");
      C.add("   begin");
      C.add("      if (Reset) s_state_reg <= 0;");
      C.add("      else if ((ShiftEnable|ParLoad)&Tick) s_state_reg <= s_state_next;");
      C.add("   end");
      C.add("");
      C.add("   always @(negedge Clock or posedge Reset)");
      C.add("   begin");
      C.add("      if (Reset) s_state_reg_neg_edge <= 0;");
      C.add("      else if ((ShiftEnable|ParLoad)&Tick) s_state_reg_neg_edge <= s_state_next;");
      C.add("   end");
      C.add("");
      C.add("endmodule");
    }
    C.add("");
    C.add("");
    C.add("");
    C.addAll(super.GetArchitecture(/*TheNetlist,*/ /*attrs,*/ null, ComponentName /*, err, lang */));
    return C;
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> C = new ArrayList<String>();
    C.add("   COMPONENT SingleBitShiftRegStage ");
    C.add("      GENERIC ( Trigger : INTEGER;");
    C.add("                Stages : INTEGER);");
    C.add("      PORT ( Reset       : IN  std_logic;");
    C.add("             Tick        : IN  std_logic;");
    C.add("             Clock       : IN  std_logic;");
    C.add("             ShiftEnable : IN  std_logic;");
    C.add("             ParLoad     : IN  std_logic;");
    C.add("             ShiftIn     : IN  std_logic;");
    C.add("             D           : IN  std_logic_vector(Stages-1 DOWNTO 0);");
    C.add("             ShiftOut    : OUT std_logic;");
    C.add("             Q           : OUT std_logic_vector(Stages-1 DOWNTO 0));");
    C.add("   END COMPONENT;");
    return C;
  }

  @Override
  public ArrayList<String> GetEntity(/*Netlist TheNetlist,*/ /*AttributeSet attrs,*/
      String ComponentName /*, FPGAReport err, String lang*/) {
    ArrayList<String> C = new ArrayList<String>();
    if (_lang.equals("VHDL")) {
      C.addAll(FileWriter.getGenerateRemark(ComponentName, "VHDL", _nets.projName()));
      C.addAll(FileWriter.getExtendedLibrary());
      C.add("ENTITY SingleBitShiftRegStage  IS");
      C.add("   GENERIC ( Trigger : INTEGER;");
      C.add("             Stages : INTEGER);");
      C.add("   PORT ( Reset       : IN  std_logic;");
      C.add("          Tick        : IN  std_logic;");
      C.add("          Clock       : IN  std_logic;");
      C.add("          ShiftEnable : IN  std_logic;");
      C.add("          ParLoad     : IN  std_logic;");
      C.add("          ShiftIn     : IN  std_logic;");
      C.add("          D           : IN  std_logic_vector(Stages-1 DOWNTO 0);");
      C.add("          ShiftOut    : OUT std_logic;");
      C.add("          Q           : OUT std_logic_vector(Stages-1 DOWNTO 0));");
      C.add("END SingleBitShiftRegStage;");
      C.add("");
      C.add("");
      C.add("");
    }
    C.addAll(super.GetEntity(/*TheNetlist,*/ /*attrs,*/ ComponentName /*, err, lang*/));
    return C;
  }

  protected final static int GENERIC_PARAM_TRIGGER = -1;
  protected final static int GENERIC_PARAM_BUSWIDTH = -2;
  protected final static int GENERIC_PARAM_STAGES = -3;
  protected final static int GENERIC_PARAM_BITS = -4;

  @Override
  public String GetSubDir() { return "memory"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Reset", 1);
    list.put("Tick", 1);
    list.put("Clock", 1);
    list.put("ShiftEnable", 1);
    list.put("ParLoad", 1);
    list.put("ShiftIn", GENERIC_PARAM_BUSWIDTH);
    list.put("D", GENERIC_PARAM_BITS);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("ShiftOut", GENERIC_PARAM_BUSWIDTH);
    list.put("Q", GENERIC_PARAM_BITS);
  }

  @Override
  public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    list.put(GENERIC_PARAM_TRIGGER, "Trigger");
    list.put(GENERIC_PARAM_BUSWIDTH, "BusWidth");
    list.put(GENERIC_PARAM_STAGES, "Stages");
    list.put(GENERIC_PARAM_BITS, "Bits");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);
    int n = stages(attrs);

    boolean gatedClk = GetClockNetName(info, ShiftRegister.CK, nets).equals("");
    if (gatedClk)
      err.AddWarning("Found a gated clock for component \"Shift Register\" in circuit \"" + nets.getCircuitName() + "\"");
    boolean activelo = attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING;

    list.put("Trigger", activelo && gatedClk ? 0 : 1);
    list.put("BusWidth", w);
    list.put("Stages", n);
    list.put("Bits", w * n);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);
    int n = stages(attrs);
    boolean hasClk = info.EndIsConnected(ShiftRegister.CK);
    if (!hasClk)
      err.AddSevereWarning("Component \"Shift Register\" in circuit \""
          + nets.getCircuitName() + "\" has no clock connection");

    String clk = GetClockNetName(info, ShiftRegister.CK, nets);
    boolean gatedClk = clk.equals("");
    boolean activelo = attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING;
    boolean parallel = attrs.getValue(ShiftRegister.ATTR_LOAD);

    boolean vhdl = lang.equals("VHDL");
    String zero = vhdl ? "'0'" : "1'b0";
    String one = vhdl ? "'1'" : "1'b1";
    String idx = vhdl ? "(%d)" : "[%]"; // fixme: these should be in base class at minimum!

    list.putAll(GetNetMap("Reset", true, info, ShiftRegister.CLR, err, lang, nets));

    if (!hasClk) {
      list.put("Clock", zero);
      list.put("Tick", zero);
    } else if (!gatedClk) {
      list.put("Clock", String.format(clk+idx, ClockHDLGeneratorFactory.GlobalClockIndex));
      if (nets.RequiresGlobalClockConnection())
        list.put("Tick", String.format(clk+idx, ClockHDLGeneratorFactory.GlobalClockIndex));
      else if (activelo)
        list.put("Tick", String.format(clk+idx, ClockHDLGeneratorFactory.NegativeEdgeTickIndex));
      else
        list.put("Tick", String.format(clk+idx, ClockHDLGeneratorFactory.PositiveEdgeTickIndex));
    } else {
      list.put("Tick", one);
      if (gatedClk)
        list.put("Clock", GetNetName(info, ShiftRegister.CK, true, lang, nets));
      else if (activelo)
        list.put("Clock", String.format(clk+idx, ClockHDLGeneratorFactory.InvertedDerivedClockIndex));
      else
        list.put("Clock", String.format(clk+idx, ClockHDLGeneratorFactory.DerivedClockIndex));
    }

    list.putAll(GetNetMap("ShiftEnable", false, info, ShiftRegister.SH, err, lang, nets));

    if (parallel)
      list.putAll(GetNetMap("ParLoad", true, info, ShiftRegister.LD, err, lang, nets));
    else
      list.put("ParLoad", zero);

    String si = "ShiftIn" + (vhdl & (w == 1) ? "(0)" : "");
    list.putAll(GetNetMap(si, true, info, ShiftRegister.IN, err, lang, nets));
    String so = "ShiftOut" + (vhdl & (w == 1) ? "(0)" : "");
    list.putAll(GetNetMap(so, true, info, ShiftRegister.OUT, err, lang, nets));

    System.out.println("todo: confirm open for last port, esp for verilog");
    if (parallel && w == 1) {
      if (vhdl) {
        for (int i = 0; i < n; i++) {
          list.putAll(GetNetMap(String.format("D"+idx, i), true, info, 6 + 2 * i, err, lang, nets));
          if (i == n-1 && attrs.getValue(StdAttr.APPEARANCE) != StdAttr.APPEAR_CLASSIC)
            list.put(String.format("Q"+idx, i), "OPEN");
          else
            list.putAll(GetNetMap(String.format("Q"+idx, i), true, info, 7 + 2 * i, err, lang, nets));
        }
      } else {
        // fixme: helper/utility for this
        String[] p = new String[n];
        for (int i = 0; i < n; i++)
          p[i] = GetNetName(info, 6 + 2 * i, true, lang, nets);
        list.put("D", String.join(", ", p));
        for (int i = 0; i < n - 1; i++)
          p[i] = GetNetName(info, 7 + 2 * i, true, lang, nets);
        list.put("Q", String.join(", ", p));
      }
    } else if (parallel) {
      boolean lastBitMissing = attrs.getValue(StdAttr.APPEARANCE) != StdAttr.APPEAR_CLASSIC;
      if (vhdl) {
        for (int bit = 0; bit < w; bit++) {
          for (int i = 0; i < n; i++)
            list.put(String.format("D"+idx, bit * n + i), GetBusEntryName(info, 6 + 2 * i, true, bit, lang, nets));
          for (int i = 0; i < n; i++) {
            if (i == n-1 && lastBitMissing)
              list.put(String.format("Q"+idx, bit * n + i), "OPEN");
            else
              list.put(String.format("Q"+idx, bit * n + i), GetBusEntryName(info, 7 + 2 * i, true, bit, lang, nets));
          }
        }
      } else {
        String[] p = new String[n*w];
        for (int bit = 0; bit < w; bit++)
          for (int i = 0; i < n; i++)
            p[bit * n + i] = GetBusEntryName(info, 6 + 2 * i, true, bit, lang, nets);
        list.put("D", String.join(", ", p));
        for (int bit = 0; bit < w; bit++) {
          for (int i = 0; i < n; i++) {
            if (i == n-1 && lastBitMissing)
              p[bit * n + i] = "";
            else
              p[bit * n + i] = GetBusEntryName(info, 7 + 2 * i, true, bit, lang, nets);
          }
        }
        list.put("Q", String.join(", ", p));
      }
    } else {
      list.put("Q", vhdl ? "OPEN" : "");
      String zeros = vhdl ? "\"" + ("0".repeat(w*n)) + "\"" : "0";
      list.put("D", zeros);
    }
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    if (out.isVhdl) {
      out.stmt("   GenBits : FOR n IN (BusWidth-1) DOWNTO 0 GENERATE");
      out.stmt("      OneBit : SingleBitShiftRegStage");
      out.stmt("      GENERIC MAP ( Trigger => Trigger,");
      out.stmt("                    Stages => Stages )");
      out.stmt("      PORT MAP ( Reset       => Reset,");
      out.stmt("                 Tick        => Tick,");
      out.stmt("                 Clock       => Clock,");
      out.stmt("                 ShiftEnable => ShiftEnable,");
      out.stmt("                 ParLoad     => ParLoad,");
      out.stmt("                 ShiftIn     => ShiftIn(n),");
      out.stmt("                 D           => D((n+1)*Stages-1 DOWNTO n*Stages),");
      out.stmt("                 ShiftOut    => ShiftOut(n),");
      out.stmt("                 Q           => Q((n+1)*Stages-1 DOWNTO n*Stages));");
      out.stmt("   END GENERATE genbits;");
    } else {
      out.stmt("   genvar n;");
      out.stmt("   generate");
      out.stmt("      for (n = 0 ; n < BusWidth-1 ; n =n+1)");
      out.stmt("      begin:Bit");
      out.stmt("         SingleBitShiftRegStage #(.Trigger(Trigger),");
      out.stmt("                             .Stages(Stages))");
      out.stmt("            OneBit (.Reset(Reset),");
      out.stmt("                    .Tick(Tick),");
      out.stmt("                    .Clock(Clock),");
      out.stmt("                    .ShiftEnable(ShiftEnable),");
      out.stmt("                    .ParLoad(ParLoad),");
      out.stmt("                    .ShiftIn(ShiftIn[n]),");
      out.stmt("                    .D(D[((n+1)*Stages)-1:(n*Stages)]),");
      out.stmt("                    .ShiftOut(ShiftOut[n]),");
      out.stmt("                    .Q(Q[((n+1)*Stages)-1:(n*Stages)]));");
      out.stmt("      end");
      out.stmt("   endgenerate");
    }
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }

  protected int stages(AttributeSet attrs) {
    return attrs.getValue(ShiftRegister.ATTR_LENGTH);
  }

}
