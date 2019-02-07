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
import com.cburch.logisim.instance.StdAttr;

public class DemultiplexerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public DemultiplexerHDLGeneratorFactory(String lang, FPGAReport err) {
    super(lang, err);
  }

  protected final static int GENERIC_PARAM_BUSWIDTH = -1;

  @Override
  public String getComponentStringIdentifier() { return "DEMUX"; }

  @Override
  public String GetSubDir() { return "plexers"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
    list.put("DataIn", w);
    list.put("Enable", 1);
    list.put("Sel", selWidth(attrs));
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
    int ws = selWidth(attrs);
    for (int i = 0; i < (1 << ws); i++)
      list.put("Out_"+i, w);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    int w = width(attrs);
    if (w > 1)
      list.put(GENERIC_PARAM_BUSWIDTH, "BusWidth");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs);
    if (w > 1)
      list.put("BusWidth", w);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
    int ws = selWidth(attrs);
    int n = (1 << ws);
    for (int i = 0; i < n; i++)
      list.putAll(GetNetMap("Out_"+i, true, info, i, err, lang, nets));
    list.putAll(GetNetMap("Sel", true, info, n, err, lang, nets));
    if (attrs.getValue(Plexers.ATTR_ENABLE)) {
      list.putAll(GetNetMap("Enable", false, info, n + 1, err, lang, nets));
    } else {
      list.put("Enable", lang.equals("VHDL") ? "'1'" : "1'b1");
      n--;
    }
    list.putAll(GetNetMap("DataIn", true, info, n+2, err, lang, nets));
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    out.indent();
    int w = width(attrs);
    int ws = selWidth(attrs);
    int n = (1 << ws);
    for (int i = 0; i < n; i++) {
      String s = out.literal(i, ws);
      if (out.isVhdl) {
        if (w == 1) {
          out.stmt("Out_%d <= DataIn WHEN Sel = %d AND Enable = '1' ELSE '0';", i, s);
        } else {
          out.stmt("Out_%d <= DataIn WHEN Sel = %d AND Enable = '1' ELSE (others => '0');", i, s);
        }
      } else {
        out.stmt("assign Out_%d = (Enable & (Sel == %d)) ? DataIn : 0;", i, s);
      }
    }
  }


  protected int selWidth(AttributeSet attrs) {
    return attrs.getValue(Plexers.ATTR_SELECT).getWidth();
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }
}
