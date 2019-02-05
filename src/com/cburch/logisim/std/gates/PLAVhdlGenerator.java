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
package com.cburch.logisim.std.gates;

import java.util.SortedMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;

public class PLAVhdlGenerator extends AbstractHDLGeneratorFactory {

  private static String bits(char b[]) {
    String s = "";
    for (char c : b)
      s = ((c == '0' || c == '1') ? c : '-') + s;
    if (b.length == 1)
      return "'" + s + "'";
    else
      return "\"" + s + "\"";
  }

  private static String zeros(int sz) {
    if (sz == 1)
      return "'0'";
    else
      return "\"" + ("0".repeat(sz)) + "\"";
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    out.indent();
    PLATable tt = attrs.getValue(PLA.ATTR_TABLE);
    int w = attrs.getValue(PLA.ATTR_OUT_WIDTH).getWidth();
    if (tt.rows().isEmpty()) {
      out.stmt("Result <= %s;", zeros(w));
    } else {
      out.stmt("Result <= \t");
      for (PLATable.Row r : tt.rows())
        out.cont("\t%s WHEN std_match(Index, %s) ELSE", bits(r.outBits), bits(r.inBits));
      out.cont("\t%s;", zeros(w));
    }
  }
  
  @Override
  public boolean HDLTargetSupported(String lang, AttributeSet attrs, char vendor) { return lang.equals("VHDL"); }

  @Override
  public String getComponentStringIdentifier() { return "PLA"; }

  @Override
  public String GetSubDir() { return "gates"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Index", attrs.getValue(PLA.ATTR_IN_WIDTH).getWidth());
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Result", attrs.getValue(PLA.ATTR_OUT_WIDTH).getWidth());
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    list.putAll(GetNetMap("Index", true, info, PLA.IN_PORT, err, lang, nets));
    list.putAll(GetNetMap("Result", true, info, PLA.OUT_PORT, err, lang, nets));
  }

}
