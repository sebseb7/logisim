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

public class BitSelectorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public BitSelectorHDLGeneratorFactory(HDLCTX ctx) { super(ctx); }

  protected final static int GENERIC_PARAM_WIDTH_IN = -1;
  protected final static int GENERIC_PARAM_WIDTH_OUT = -2;
  protected final static int GENERIC_PARAM_WIDTH_SEL = -3;
  protected final static int GENERIC_PARAM_EXTENDEDBITS = -4;

  @Override
  public String getComponentStringIdentifier() { return "BITSELECTOR"; }

  @Override
  public String GetSubDir() { return "plexers"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("DataIn", GENERIC_PARAM_WIDTH_IN);
    list.put("Sel", GENERIC_PARAM_WIDTH_SEL);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int wo = outWidth(attrs) > 1 ? GENERIC_PARAM_WIDTH_OUT : 1;
    list.put("Result", wo);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    list.put(GENERIC_PARAM_WIDTH_IN, "WidthIn");
    if (outWidth(attrs) > 1)
      list.put(GENERIC_PARAM_WIDTH_OUT, "WidthOut");
    list.put(GENERIC_PARAM_WIDTH_SEL, "WidthSel");
    list.put(GENERIC_PARAM_EXTENDEDBITS, "ExtendedBits");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    int wo = info.GetComponent().getEnd(0).getWidth().getWidth();
    int ws = info.GetComponent().getEnd(2).getWidth().getWidth();
    int wi = info.GetComponent().getEnd(1).getWidth().getWidth();
    list.put("WidthIn", wi);
    if (wo > 1)
      list.put("WidthOut", wo);
    list.put("WidthSel", ws);
    list.put("ExtendedBits", wo * (1 << ws) + 1);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    list.putAll(GetNetMap("DataIn", true, info, 1, err, lang, nets));
    list.putAll(GetNetMap("Sel", true, info, 2, err, lang, nets));
    list.putAll(GetNetMap("Result", true, info, 0, err, lang, nets));
  }

  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    list.put("s_vec", GENERIC_PARAM_EXTENDEDBITS);
  }
  
  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    System.out.println("BUG? use param for output width == 1?");
    out.indent();
    int wo = outWidth(attrs);
    if (out.isVhdl) {
      out.stmt("s_vec((ExtendedBits-1) DOWNTO WidthIn) <= (others => '0');");
      out.stmt("s_vec((WidthIn-1) DOWNTO 0) <= DataIn;");
      if (wo > 1) {
        out.stmt("Result <= s_vec(((to_integer(unsigned(Sel))+1)*WidthOut)-1");
        out.stmt("                DOWNTO to_integer(unsigned(Sel))*WidthOut);");
      } else {
        out.stmt("Result <= s_vec(to_integer(unsigned(Sel)));");
      }
    } else {
      out.stmt("assign s_vec[ExtendedBits-1:WidthIn] = 0;");
      out.stmt("assign s_vec[WidthIn-1:0] = DataIn;");
      if (wo > 1) {
        out.stmt("wire[513:0] s_select_vector;");
        out.stmt("reg[WidthOut-1:0] s_selected_slice;");
        out.stmt("assign s_select_vector[513:ExtendedBits] = 0;");
        out.stmt("assign s_select_vector[ExtendedBits-1:0] = s_vec;");
        out.stmt("assign Result = s_selected_slice;");
        out.stmt("");
        out.stmt("always @(*)");
        out.stmt("begin");
        out.stmt("case (Sel)");
        for (int i = 15; i > 0; i--)
          out.stmt("%d : s_selected_slice <= s_select_vector[(%d*WidthOut)-1 : %d*WidthOut];", i, i+1, i);
        out.stmt("default : s_selected_slice <= s_select_vector[WidthOut-1:0];");
        out.stmt("endcase");
        out.stmt("end");
      } else {
        out.stmt("assign Result = s_vec[Sel];");
      }
    }
  }

  protected int outWidth(AttributeSet attrs) {
    return attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
  }
}
