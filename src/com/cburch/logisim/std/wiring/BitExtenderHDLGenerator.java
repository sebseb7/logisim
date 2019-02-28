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
package com.cburch.logisim.std.wiring;

import com.bfh.logisim.netlist.NetlistComponent;
import com.bfh.logisim.hdlgenerator.HDLInliner;
import com.cburch.logisim.hdl.Hdl;

public class BitExtenderHDLGenerator extends HDLInliner {

  public BitExtenderHDLGenerator(HDLCTX ctx) {
    super(ctx, "BitExtender");
  }

  @Override
	protected void generateInlinedCode(Hdl out, NetlistComponent comp) {
    Net dest = comp.getConnection(0);
    if (dest == null)
      return; // output not connected, nothing to do
    
    Net src = comp.getConnection(1);
    if (src == null) {
      _err.AddError("BitExtender has floating input in circuit '%s'", nets.circName());
      return;
    }

    int wo = dest.width;
    int wi = src.width;

    if (wo <= wi) {
      out.assign(dst, src.slice(wo-1, 0));
      return;
    }

    String type = attrs.getValue(BitExtender.ATTR_TYPE);

    Net inp = null;
    if (type.equals("input")) {
      inp = comp.getConnection(2);
      if (inp == null) {
        err.AddSevereWarning("Bit Extender has floating input in circuit '%s'", nets.circName());
        type = "zero";
      }
    }

    String e = "???";
    if (type.equals("zero"))
      e = out.zero;
    else if (type.equals("one"))
      e = out.one;
    else if (type.equals("sign"))
      e = src.bit(wi-1);
    else if (type.equals("input"))
      e = inp.name;

    if (out.isVhdl && wo == wi+1)
      out.assign(dst, String.format("%s & %s", e, src.name));
    else if (out.isVhdl)
      out.assign(dst, String.format("(%d downto %d => %s) & %s", wo-1, wi, e, src.name));
    else if (wo == wi+1)
      out.assign(dst, String.format("{%s, %s}", e, src.name));
    else
      out.assign(dst, String.format("{{%d{%s}}, %s}", wo-wi, e, src.name));
  }
}
