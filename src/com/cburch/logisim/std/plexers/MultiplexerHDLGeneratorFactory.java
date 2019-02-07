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

public class MultiplexerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public MultiplexerHDLGeneratorFactory(HDLCTX ctx) { super(ctx); }

  protected final static int GENERIC_PARAM_BUSWIDTH = -1;

  @Override
  public String getComponentStringIdentifier() { return "MUX"; }

  @Override
  public String GetSubDir() { return "plexers"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
    int ws = selWidth(attrs);
    for (int i = 0; i < (1 << ws); i++)
      list.put("In_"+i, w);
    list.put("Enable", 1);
    list.put("Sel", ws);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
    list.put("Out", w);
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    int w = width(attrs);
    if (w > 1)
      list.put(GENERIC_PARAM_BUSWIDTH, "BusWidth");
  }

  @Override
  public void registers(SortedMap<String, Integer> list, AttributeSet attrs, String lang) {
    if (lang.equals("Verilog")) {
      int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
      list.put("s_vec", w);
    }
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
      list.putAll(GetNetMap("In_"+i, true, info, i, err, lang, nets));
    list.putAll(GetNetMap("Sel", true, info, n, err, lang, nets));
    if (attrs.getValue(Plexers.ATTR_ENABLE)) {
      list.putAll(GetNetMap("Enable", false, info, n + 1, err, lang, nets));
    } else {
      list.put("Enable", lang.equals("VHDL") ? "'1'" : "1'b1");
      n--;
    }
    list.putAll(GetNetMap("Out", true, info, n+2, err, lang, nets));
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    out.indent();
    int w = width(attrs);
    int ws = selWidth(attrs);
    int n = (1 << ws);
    if (out.isVhdl) {
      String lang = "VHDL";
      out.stmt("make_mux : PROCESS(\tEnable,");
      for (int i = 0; i < n; i++)
        out.cont("     \tIn_%d, ", i);
      out.cont("       \tSel)");
      out.stmt("BEGIN");
      out.indent();
      out.stmt("IF (Enable = '0') THEN");
      if (w == 1)
        out.stmt("  Out <= '0';");
      else
        out.stmt("  Out <= (others => '0');");
      out.stmt("ELSE");
      out.stmt("  CASE (Sel) IS");
      for (int i = 0; i < n; i++)
        out.stmt("    WHEN %s => Out <= In_%d;", i<n-1 ? out.literal(i, ws) : "others", i);
      out.stmt("  END CASE;");
      out.stmt("END IF;");
      out.dedent();
      out.stmt("END PROCESS make_mux;");
    } else {
      String lang = "Verilog";
      out.stmt("assign Out = s_vec;");
      out.stmt("");
      out.stmt("always @(*)");
      out.stmt("begin");
      out.stmt("  if (~Enable) s_vec <= 0;");
      out.stmt("  else case (Sel)");
      for (int i = 0; i < n - 1; i++)
        out.stmt("    %d : s_vec <= In_%d;", i<n-1 ? out.literal(i, ws) : "default", i);
      out.stmt("  endcase");
      out.stmt("end");
    }
  }

  protected int selWidth(AttributeSet attrs) {
    return attrs.getValue(Plexers.ATTR_SELECT).getWidth();
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }
}
