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
package com.cburch.logisim.std.arith;

import java.util.SortedMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;

public class ComparatorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
  
  protected final static int GENERIC_PARAM_BUSWIDTH = -1;
  protected final static int GENERIC_PARAM_TWOSCOMPLEMENT = -2;

  @Override
  public boolean HDLTargetSupported(String lang, AttributeSet attrs, char Vendor) { return true; }

  @Override
  public String getComponentStringIdentifier() { return "COMPARATOR"; }

  @Override
  public String GetSubDir() { return "arithmetic"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
    list.put("DataA", w);
    list.put("DataB", w);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("A_GT_B", 1);
    list.put("A_EQ_B", 1);
    list.put("A_LT_B", 1);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    int w = width(attrs);
    if (w > 1)
      list.put(GENERIC_PARAM_BUSWIDTH, "BusWidth");
    list.put(GENERIC_PARAM_TWOSCOMPLEMENT, "TwosComplement");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);
    if (w > 1)
      list.put("BusWidth", w);
    boolean is_signed = attrs.getValue(Comparator.MODE_ATTRIBUTE) == Comparator.SIGNED_OPTION;
    list.put("TwosComplement", is_signed ? 1 : 0);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    list.putAll(GetNetMap("DataA", true, info, 0, err, lang, nets));
    list.putAll(GetNetMap("DataB", true, info, 1, err, lang, nets));
    list.putAll(GetNetMap("A_GT_B", true, info, 2, err, lang, nets));
    list.putAll(GetNetMap("A_EQ_B", true, info, 3, err, lang, nets));
    list.putAll(GetNetMap("A_LT_B", true, info, 4, err, lang, nets));
  }

  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    if (width(attrs) > 1) {
      list.put("s_slt", 1);
      list.put("s_ult", 1);
      list.put("s_sgt", 1);
      list.put("s_ugt", 1);
    }
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    out.indent();
    int w = width(attrs);
    if (out.isVhdl && w == 1) {
      out.stmt("A_EQ_B <= DataA XNOR DataB;");
      out.stmt("A_LT_B <= DataA AND NOT(DataB) WHEN TwosComplement = 1 ELSE");
      out.stmt("          NOT(DataA) AND DataB;");
      out.stmt("A_GT_B <= NOT(DataA) AND DataB WHEN TwosComplement = 1 ELSE");
      out.stmt("          DataA AND NOT(DataB);");
    } else if (out.isVhdl) {
      out.stmt("s_slt <= '1' WHEN signed(DataA) < signed(DataB) ELSE '0';");
      out.stmt("s_ult <= '1' WHEN unsigned(DataA) < unsigned(DataB) ELSE '0';");
      out.stmt("s_sgt <= '1' WHEN signed(DataA) > signed(DataB) ELSE '0';");
      out.stmt("s_ugt <= '1' WHEN unsigned(DataA) > unsigned(DataB) ELSE '0';");
      out.stmt("");
      out.stmt("A_EQ_B <= '1' WHEN DataA = DataB ELSE '0';");
      out.stmt("A_GT_B <= s_sgt WHEN TwosComplement = 1 ELSE s_ugt;");
      out.stmt("A_LT_B <= s_slt WHEN TwosComplement = 1 ELSE s_ult;");
    } else if (out.isVerilog && w == 1) {
      out.stmt("assign A_EQ_B = (DataA == DataB);");
      out.stmt("assign A_LT_B = (DataA < DataB);");
      out.stmt("assign A_GT_B = (DataA > DataB);");
    } else if (out.isVerilog) {
      out.stmt("assign s_slt = ($signed(DataA) < $signed(DataB));");
      out.stmt("assign s_ult = (DataA < DataB);");
      out.stmt("assign s_sgt = ($signed(DataA) > $signed(DataB));");
      out.stmt("assign s_ugt = (DataA > DataB);");
      out.stmt("");
      out.stmt("assign A_EQ_B = (DataA == DataB);");
      out.stmt("assign A_GT_B = (TwosComplement == 1) ? s_sgt : s_ugt;");
      out.stmt("assign A_LT_B = (TwosComplement == 1) ? s_slt : s_ult;");
    }
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }
}
