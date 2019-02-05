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

public class AdderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
  
  protected final static int GENERIC_PARAM_BUSWIDTH = -1;
  protected final static int GENERIC_PARAM_EXTENDEDBITS = -2;

  public boolean HDLTargetSupported(String lang, AttributeSet attrs, char Vendor) { return true; }

  @Override
  public String getComponentStringIdentifier() { return "ADDER2C"; }

  @Override
  public String GetSubDir() { return "arithmetic"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
    list.put("DataA", w);
    list.put("DataB", w);
    list.put("CarryIn", 1);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
    list.put("Result", w);
    list.put("CarryOut", 1);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    int w = width(attrs);
    if (w > 1)
      list.put(GENERIC_PARAM_BUSWIDTH, "BusWidth");
    list.put(GENERIC_PARAM_EXTENDEDBITS, "ExtendedBits");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);
    if (w > 1)
      list.put("BusWidth", w);
    list.put("ExtendedBits", w + 1);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    list.putAll(GetNetMap("DataA", true, info, 0, err, lang, nets));
    list.putAll(GetNetMap("DataB", true, info, 1, err, lang, nets));
    list.putAll(GetNetMap("Result", true, info, 2, err, lang, nets));
    list.putAll(GetNetMap("CarryIn", true, info, 3, err, lang, nets));
    list.putAll(GetNetMap("CarryOut", true, info, 4, err, lang, nets));
  }

  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    list.put("s_A", GENERIC_PARAM_EXTENDEDBITS);
    list.put("s_B", GENERIC_PARAM_EXTENDEDBITS);
    list.put("s_R", GENERIC_PARAM_EXTENDEDBITS);
  }
  // fixme: EXTENDEDBITS is only needed b/c we have no way of saying "BusWidth+1 if BusWidth is defined otherwise 2"
  
  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    out.indent();
    int w = width(attrs);
    if (out.isVhdl) {
      out.stmt("s_A <= \"0\" & DataA;");
      out.stmt("s_B <= \"0\" & DataB;");
      out.stmt("s_R <= std_logic_vector(unsigned(s_A) + unsigned(s_B)+ (\"\" & CarryIn));");
      out.stmt("");
      if (w == 1)
        out.stmt("Result <= s_R(0);");
      else
        out.stmt("Result <= s_R((BusWidth-1) DOWNTO 0);");
      out.stmt("CarryOut <= s_R(BusWidth);");
    } else {
      out.stmt("assign {CarryOut, Result} = DataA + DataB + CarryIn;");
    }
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }
}
