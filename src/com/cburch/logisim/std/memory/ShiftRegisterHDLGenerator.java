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
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;

public class ShiftRegisterHDLGenerator extends HDLGenerator {

  public ShiftRegisterHDLGenerator(HDLCTX ctx) {
    super(ctx, "memory", deriveHDLName(ctx.attrs), "i_Shft");
    int w = stdWidth();
    int n = stages(attrs);
    boolean parallel = attrs.getValue(ShiftRegister.ATTR_LOAD);

    parameters.add(new ParameterInfo("BitWidth", w));
    if (!parallel)
      parameters.add(new ParameterInfo("Stages", n);

    clockPort = new ClockPortInfo("GlobalClock", "ClockEnable", ShiftRegister.CK);

    inPorts.add(new PortInfo("Reset", 1, ShiftRegister.CLR, false));
    inPorts.add(new PortInfo("ShiftEnable", 1, ShiftRegister.SH, false));
    inPorts.add(new PortInfo("ShiftIn", "BitWidth", ShiftRegister.IN, false));
    outPorts.add(new PortInfo("ShiftOut", "BitWidth", ShiftRegister.OUT, null));
    if (parallel) {
      int portnr = ShiftRegister.LD;
      inPorts.add(new PortInfo("ParLoad", 1, portnr++, false));
      for (int i = 0; i < n; i++) {
        inPorts.add(new PortInfo("D"+i, "BitWidth", portnr++ false));
        outPorts.add(new PortInfo("Q"+i, "BitWidth", portnr++ null));
      }
    }
  }

  private static String deriveHDLName(AttributeSet attrs) {
    if (!attrs.getValue(ShiftRegister.ATTR_LOAD))
      return "ShiftRegister"; // variation with 5 ports, no D/Q ports
    else
      return "ShiftRegister_"+stages(attrs)+"_stages"; // variation with 5 ports + 2*N D/Q ports
  }


  @Override
	protected Hdl getArchitecture() {
    // Override this to emit our SingleBitShiftRegStage architecture in the same
    // file before the regular architecture.
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out);

    if (out.isVhdl) {
      out.stmt("ARCHITECTURE logisim_generated OF SingleBitShiftRegStage IS");
      out.stmt("   SIGNAL s_state_reg  : std_logic_vector(Stages-1 DOWNTO 0);");
      out.stmt("   SIGNAL s_state_next : std_logic_vector(Stages-1 DOWNTO 0);");
      out.stmt("BEGIN");
      out.stmt("   Q        <= s_state_reg;");
      out.stmt("   ShiftOut <= s_state_reg(Stages-1);");
      out.stmt();
      out.stmt("   s_state_next <= D WHEN ParLoad = '1' ELSE s_state_reg((Stages-2) DOWNTO 0)&ShiftIn;");
      out.stmt();
      out.stmt("   make_state : PROCESS(GlobalClock, ShiftEnable, ClockEnable, Reset, s_state_next, ParLoad)");
      out.stmt("      VARIABLE temp : std_logic_vector( 0 DOWNTO 0 );");
      out.stmt("   BEGIN");
      out.stmt("      temp := std_logic_vector(to_unsigned("+"Trigger"+ ",1));");
      out.stmt("      IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');");
      out.stmt("      ELSIF (GlobalClock'event AND (GlobalClock = temp(0) )) THEN");
      out.stmt("         IF (((ShiftEnable = '1') OR (ParLoad = '1')) AND (ClockEnable = '1')) THEN");
      out.stmt("            s_state_reg <= s_state_next;");
      out.stmt("         END IF;");
      out.stmt("      END IF;");
      out.stmt("   END PROCESS make_state;");
      out.stmt("END logisim_generated;");
    } else {
      out.stmt("module SingleBitShiftRegStage ( Reset, ClockEnable, GlobalClock, ShiftEnable, ParLoad, ");
      out.stmt("                           ShiftIn, D, ShiftOut, Q );");
      out.stmt("   parameter Stages = 1;");
      out.stmt("   parameter Trigger = 1;");
      out.stmt();
      out.stmt("   input Reset;");
      out.stmt("   input ClockEnable;");
      out.stmt("   input GlobalClock;");
      out.stmt("   input ShiftEnable;");
      out.stmt("   input ParLoad;");
      out.stmt("   input ShiftIn;");
      out.stmt("   input[Stages:0] D;");
      out.stmt("   output ShiftOut;");
      out.stmt("   output[Stages:0] Q;");
      out.stmt();
      out.stmt("   wire[Stages:0] s_state_next;");
      out.stmt("   reg[Stages:0] s_state_reg;");
      out.stmt("   reg[Stages:0] s_state_reg_neg_edge;");
      out.stmt();
      out.stmt("   assign Q            = (Trigger) ? s_state_reg : s_state_reg_neg_edge;");
      out.stmt("   assign ShiftOut     = (Trigger) ? s_state_reg[Stages-1] : s_state_reg_neg_edge[Stages-1];");
      out.stmt("   assign s_state_next = (ParLoad) ? D :");
      out.stmt("                         (Trigger) ? {s_state_reg[Stages-2:1],ShiftIn} :");
      out.stmt("                                     {s_state_reg_neg_edge[Stages-2:1],ShiftIn};");
      out.stmt();
      out.stmt("   always @(posedge GlobalClock or posedge Reset)");
      out.stmt("   begin");
      out.stmt("      if (Reset) s_state_reg <= 0;");
      out.stmt("      else if ((ShiftEnable|ParLoad)&ClockEnable) s_state_reg <= s_state_next;");
      out.stmt("   end");
      out.stmt();
      out.stmt("   always @(negedge GlobalClock or posedge Reset)");
      out.stmt("   begin");
      out.stmt("      if (Reset) s_state_reg_neg_edge <= 0;");
      out.stmt("      else if ((ShiftEnable|ParLoad)&ClockEnable) s_state_reg_neg_edge <= s_state_next;");
      out.stmt("   end");
      out.stmt();
      out.stmt("endmodule");
    }
    out.stmt();
    out.stmt();
    out.stmt();
    out.addAll(super.getArchitecture());
    return out;
  }
  
  @Override
	protected Hdl getVhdlEntity() {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out);
    
    if (out.isVhdl) {
      generateVhdlLibraries(out);
      out.stmt("ENTITY SingleBitShiftRegStage IS");
      out.stmt("   GENERIC ( Trigger : INTEGER;");
      out.stmt("             Stages : INTEGER);");
      out.stmt("   PORT ( Reset       : IN  std_logic;");
      out.stmt("          ClockEnable : IN  std_logic;");
      out.stmt("          GlobalClock : IN  std_logic;");
      out.stmt("          ShiftEnable : IN  std_logic;");
      out.stmt("          ParLoad     : IN  std_logic;");
      out.stmt("          ShiftIn     : IN  std_logic;");
      out.stmt("          D           : IN  std_logic_vector(Stages-1 DOWNTO 0);");
      out.stmt("          ShiftOut    : OUT std_logic;");
      out.stmt("          Q           : OUT std_logic_vector(Stages-1 DOWNTO 0));");
      out.stmt("END SingleBitShiftRegStage;");
      out.stmt();
      out.stmt();
      out.stmt();
    }
    out.addAll(super.getVhdlEntity());
    return out;
  }

  @Override
	protected void generateVhdlTypes(Hdl out) { // slight abuse, but this puts the VHDL constant in the right place
    boolean parallel = attrs.getValue(ShiftRegister.ATTR_LOAD);
    if (parallel && out.isVhdl)
      out.stmt("constant Stages : integer := %d;", stages(attrs));
  }

  @Override
	protected void generateComponentDeclaration(Hdl out) {
    out.stmt("   COMPONENT SingleBitShiftRegStage ");
    out.stmt("      GENERIC ( Trigger : INTEGER;");
    out.stmt("                Stages : INTEGER);");
    out.stmt("      PORT ( Reset       : IN  std_logic;");
    out.stmt("             ClockEnable : IN  std_logic;");
    out.stmt("             GlobalClock : IN  std_logic;");
    out.stmt("             ShiftEnable : IN  std_logic;");
    out.stmt("             ParLoad     : IN  std_logic;");
    out.stmt("             ShiftIn     : IN  std_logic;");
    out.stmt("             D           : IN  std_logic_vector(Stages-1 DOWNTO 0);");
    out.stmt("             ShiftOut    : OUT std_logic;");
    out.stmt("             Q           : OUT std_logic_vector(Stages-1 DOWNTO 0));");
    out.stmt("   END COMPONENT;");
  }

  @Override
  protected void generateBehavior(Hdl out) {
    boolean parallel = attrs.getValue(ShiftRegister.ATTR_LOAD);
    if (out.isVhdl) {
      out.stmt("GenBits : FOR n IN (BitWidth-1) DOWNTO 0 GENERATE");
      out.stmt("   OneBit : SingleBitShiftRegStage");
      out.stmt("   GENERIC MAP ( Trigger => Trigger,");
      out.stmt("                 Stages => Stages )");
      out.stmt("   PORT MAP ( Reset       => Reset,");
      out.stmt("              ClockEnable => ClockEnable,");
      out.stmt("              GlobalClock => GlobalClock,");
      out.stmt("              ShiftEnable => ShiftEnable,");
      out.stmt("              ParLoad     => ParLoad,");
      out.stmt("              ShiftIn     => ShiftIn(n),");
      out.stmt("              ShiftOut    => ShiftOut(n),");
      if (parallel) {
        for (int i = stages(attrs)-1; i >= 0; i--) {
          out.stmt("              D(%d)       => D%d(n),", i);
          out.stmt("              Q(%d)       => Q%d(n),", i);
        }
      } else {
        out.stmt("              D           => (others => '0'),");
        out.stmt("              Q           => OPEN;");
      }
      out.stmt("END GENERATE genbits;");
    } else {
      if (parallel)
        out.stmt("   localparam Stages = %d;", stages(attrs));
      out.stmt("genvar n;");
      out.stmt("generate");
      out.stmt("   for (n = 0 ; n < BitWidth-1 ; n =n+1)");
      out.stmt("   begin:Bit");
      out.stmt("      SingleBitShiftRegStage #(.Trigger(Trigger),");
      out.stmt("                          .Stages(Stages))");
      out.stmt("         OneBit (.Reset(Reset),");
      out.stmt("                 .ClockEnable(ClockEnable),");
      out.stmt("                 .GlobalClock(GlobalClock),");
      out.stmt("                 .ShiftEnable(ShiftEnable),");
      out.stmt("                 .ParLoad(ParLoad),");
      out.stmt("                 .ShiftIn(ShiftIn[n]),");
      out.stmt("                 .ShiftOut(ShiftOut[n]),");
      if (parallel) {
        ArrayList<String> d = new Arraylist<>();
        ArrayList<String> q = new Arraylist<>();
        for (int i = stages(attrs)-1; i >= 0; i--) {
          d.add("D"+i+"(n)");
          d.add("Q"+i+"(n)");
        }
        out.stmt("                 .D(%s)", String.join(",", d));
        out.stmt("                 .Q(%s)", String.join(",", q));
      } else {
        out.stmt("                 .D(0),");
        out.stmt("                 .Q());");
      }
      out.stmt("   end");
      out.stmt("endgenerate");
    }
  }

  protected int stages(AttributeSet attrs) {
    return attrs.getValue(ShiftRegister.ATTR_LENGTH);
  }

}
