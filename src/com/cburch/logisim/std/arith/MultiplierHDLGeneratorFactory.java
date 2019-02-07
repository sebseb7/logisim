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

public class MultiplierHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public MultiplierHDLGeneratorFactory(HDLCTX ctx) { super(ctx); }

  protected final static int GENERIC_PARAM_BUSWIDTH = -1;
  protected final static int GENERIC_PARAM_EXTENDEDBITS = -2;

  @Override
  public String getComponentStringIdentifier() { return "MULT"; }

  @Override
  public String GetSubDir() { return "arithmetic"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("DataA", GENERIC_PARAM_BUSWIDTH);
    list.put("DataB", GENERIC_PARAM_BUSWIDTH);
    list.put("CarryIn", GENERIC_PARAM_BUSWIDTH);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("ProductLo", GENERIC_PARAM_BUSWIDTH);
    list.put("ProductHi", GENERIC_PARAM_BUSWIDTH);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    list.put(GENERIC_PARAM_BUSWIDTH, "BusWidth");
    list.put(GENERIC_PARAM_EXTENDEDBITS, "ExtendedBits");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    // TODO(kwalsh) - null the upper if not connected, or add a parameter
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);
    list.put("BusWidth", w);
    list.put("ExtendedBits", 2*w);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    list.putAll(GetNetMap("DataA", true, info, Multiplier.IN0, err, lang, nets));
    list.putAll(GetNetMap("DataB", true, info, Multiplier.IN1, err, lang, nets));
    list.putAll(GetNetMap("Upper", true, info, Multiplier.C_IN, err, lang, nets));
    list.putAll(GetNetMap("ProductLo", true, info, Multiplier.OUT, err, lang, nets));
    list.putAll(GetNetMap("ProductHi", true, info, Multiplier.C_OUT, err, lang, nets));
  }

  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    list.put("s_mul", GENERIC_PARAM_EXTENDEDBITS);
    list.put("s_cin", GENERIC_PARAM_EXTENDEDBITS);
    list.put("s_res", GENERIC_PARAM_EXTENDEDBITS);
  }
  // fixme: EXTENDEDBITS is only needed b/c we have no way of saying "BusWidth+1 if BusWidth is defined otherwise 2"
  
  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    System.out.println("BUG?!?! sign/unsigned needs to be a generic param?!");
    out.indent();
    int w = width(attrs);
    if (attrs.getValue(Multiplier.MODE_ATTR).equals(Multiplier.UNSIGNED_OPTION)) {
      out.stmt("s_cin(ExtendedBits-1 DOWNTO BusWidth) <= (OTHERS => '0');");
      out.stmt("s_cin(BusWidth-1 DOWNTO 0)            <= Cin;");
      out.stmt("s_mul <= std_logic_vector(unsigned(DataA) * unsigned(DataB));");
      out.stmt("s_res <= std_logic_vector(unsigned(s_mul) + unsigned(s_cin));");
      out.stmt("");
      out.stmt("ProductHi <= s_res(ExtendedBits-1 DOWNTO BusWidth);");
      out.stmt("ProductLo <= s_res(BusWidth-1 DOWNTO 0);");
    } else {
      out.stmt("s_cin(ExtendedBits-1 DOWNTO BusWidth) <= (OTHERS => '0');");
      out.stmt("s_cin(BusWidth-1 DOWNTO 0)            <= Cin;");
      out.stmt("s_mul <= std_logic_vector(signed(DataA) * signed(DataB));");
      out.stmt("s_res <= std_logic_vector(signed(s_mul) + signed(s_cin));");
      out.stmt("");
      out.stmt("ProductHi <= s_res(ExtendedBits-1 DOWNTO BusWidth);");
      out.stmt("ProductLo <= s_res(BusWidth-1 DOWNTO 0);");
    }
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }

}
