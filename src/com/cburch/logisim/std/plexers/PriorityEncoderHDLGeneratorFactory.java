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
package com.cburch.logisim.std.plexers;

import java.util.SortedMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;

public class PriorityEncoderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public PriorityEncoderHDLGeneratorFactory(HDLCTX ctx) {
    super(ctx, "Priority_Encoder", "PRIENC");
  }

  protected final static int GENERIC_PARAM_WIDTH_SEL = -1;
  protected final static int GENERIC_PARAM_WIDTH_IN = -2;

  @Override
  public String GetSubDir() { return "plexers"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Enable", 1);
    list.put("In", GENERIC_PARAM_WIDTH_IN);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Sel", 1);
    list.put("EnableOut", 1);
    list.put("Address", GENERIC_PARAM_WIDTH_SEL);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    list.put(GENERIC_PARAM_WIDTH_IN, "SelWidth");
    list.put(GENERIC_PARAM_WIDTH_SEL, "InWidth");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int ws = selWidth(attrs);
    list.put("SelWidth", ws);
    list.put("InWidth", 1 << ws);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int ws = selWidth(attrs);
    int n = (1 << ws);
    if (lang.equals("VHDL")) {
      for (int i = n - 1; i >= 0; i--)
        list.putAll(GetNetMap("In("+i+")", true, info, i, err, lang, nets));
    } else {
      String[] p = new String[n];
      for (int i = n - 1; i >= 0; i--)
        p[n-i-1] = GetNetName(info, i, true, lang, nets);
      list.put("In", String.join(", ", p));
    }
    list.putAll(GetNetMap("Enable", false, info, n + PriorityEncoder.EN_IN, err, lang, nets));
    list.putAll(GetNetMap("Sel", true, info, n + PriorityEncoder.GS, err, lang, nets));
    list.putAll(GetNetMap("EnableOut", true, info, n + PriorityEncoder.EN_OUT, err, lang, nets));
    list.putAll(GetNetMap("Address", true, info, n + PriorityEncoder.OUT, err, lang, nets));
  }

  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    list.put("s_in_is_zero", 1);
    list.put("s_addr", 5);
    list.put("v_sel_1", 33);
    list.put("v_sel_2", 16);
    list.put("v_sel_3", 8);
    list.put("v_sel_4", 4);
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    out.indent();
    if (out.isVhdl) {
      out.stmt("Sel       <= Enable AND NOT(s_in_is_zero);");
      out.stmt("EnableOut <= Enable AND s_in_is_zero;");
      out.stmt("Address   <= (others => '0') WHEN Enable = '0' ELSE");
      out.stmt("             s_addr(SelWidth-1 DOWNTO 0);");
      out.stmt("");
      out.stmt("s_in_is_zero <= '1' WHEN In = std_logic(to_unsigned(0, InWidth)) ELSE '0';");
      out.stmt("");
      out.stmt("make_addr : PROCESS( In, v_sel_1, v_sel_2, v_sel_3, v_sel_4 )");
      out.stmt("BEGIN");
      out.stmt("  v_sel_1(32 DOWNTO InWidth)  <= (others => '0');");
      out.stmt("  v_sel_1(InWidth-1 DOWNTO 0) <= In;");
      out.stmt("  IF (v_sel_1(31 DOWNTO 16) = X\"0000\") THEN");
      out.stmt("    s_addr(4) <= '0';");
      out.stmt("    v_sel_2   <= v_sel_1(15 DOWNTO 0);");
      out.stmt("  ELSE");
      out.stmt("    s_addr(4) <= '1';");
      out.stmt("    v_sel_2   <= v_sel_1(31 DOWNTO 16);");
      out.stmt("  END IF;");
      out.stmt("  IF (v_sel_2(15 DOWNTO 8) = X\"00\") THEN");
      out.stmt("    s_addr(3) <= '0';");
      out.stmt("    v_sel_3   <= v_sel_2(7 DOWNTO 0);");
      out.stmt("  ELSE");
      out.stmt("    s_addr(3) <= '1';");
      out.stmt("    v_sel_3   <= v_sel_2(15 DOWNTO 8);");
      out.stmt("  END IF;");
      out.stmt("  IF (v_sel_3(7 DOWNTO 4) = X\"0\") THEN");
      out.stmt("    s_addr(2) <= '0';");
      out.stmt("    v_sel_4   <= v_sel_3(3 DOWNTO 0);");
      out.stmt("  ELSE");
      out.stmt("    s_addr(2) <= '1';");
      out.stmt("    v_sel_4   <= v_sel_3(7 DOWNTO 4);");
      out.stmt("  END IF;");
      out.stmt("  IF (v_sel_4(3 DOWNTO 2) = \"00\") THEN");
      out.stmt("     s_addr(1) <= '0';");
      out.stmt("     s_addr(0) <= v_sel_4(1);");
      out.stmt("  ELSE");
      out.stmt("     s_addr(1) <= '1';");
      out.stmt("     s_addr(0) <= v_sel_4(3);");
      out.stmt("  END IF;");
      out.stmt("END PROCESS make_addr;");
    } else {
      out.stmt("assign Sel       = Enable & ~s_in_is_zero;");
      out.stmt("assign EnableOut = Enable & s_in_is_zero;");
      out.stmt("assign Address   = (~Enable) ? 0 : s_addr[SelWidth-1:0];");
      out.stmt("assign s_in_is_zero = (In == 0) ? 1'b1 : 1'b0;");
      out.stmt("");
      out.stmt("assign v_sel_1[32:InWidth] = 0;");
      out.stmt("assign v_sel_1[InWidth-1:0] = In;");
      out.stmt("assign s_addr[4] = (v_sel_1[31:16] == 0) ? 1'b0 : 1'b1;");
      out.stmt("assign v_sel_2 = (v_sel_1[31:16] == 0) ? v_sel_1[15:0] : v_sel_1[31:16];");
      out.stmt("assign s_addr[3] = (v_sel_2[15:8] == 0) ? 1'b0 : 1'b1;");
      out.stmt("assign v_sel_3 = (v_sel_2[15:8] == 0) ? v_sel_2[7:0] : v_sel_2[15:8];");
      out.stmt("assign s_addr[2] = (v_sel_3[7:4] == 0) ? 1'b0 : 1'b1;");
      out.stmt("assign v_sel_4 = (v_sel_3[7:4] == 0) ? v_sel_3[3:0] : v_sel_2[7:4];");
      out.stmt("assign s_addr[1] = (v_sel_4[3:2] == 0) ? 1'b0 : 1'b1;");
      out.stmt("assign s_addr[0] = (v_sel_4[3:2] == 0) ? v_sel_4[1] : v_sel_4[3];");
    }
  }

  protected int selWidth(AttributeSet attrs) {
    return attrs.getValue(Plexers.ATTR_SELECT).getWidth();
  }
}
