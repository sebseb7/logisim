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
    if (!comp.endIsConnected(0))
      return; // output not connected, nothing to do
    
    if (!comp.endIsConnected(1)) {
      _err.AddError("Bit Extender has floating input in circuit \"" + nets.getCircuitName() + "\"");
      return;
    }

    String type = attrs.getValue(BitExtender.ATTR_TYPE);

    if (type.equals("input") && !comp.endIsConnected(2)) {
      _err.AddSevereWarning("Bit Extender has floating input in circuit \"" + nets.getCircuitName() + "\"");
      type = "zero";
    }

    int wo = comp.ports.get(0).getWidth().getWidth();
    int wi = comp.ports.get(1).getWidth().getWidth();

    String e = "???";
    if (type.equals("zero"))
      e = out.zero;
    else if (type.equals("one"))
      e = out.one;
    else if (type.equals("sign"))
      e = _nets.signalForEndBit(comp, 1, wi-1, false, out);
    else if (type.equals("input"))
      e = _nets.signalForEnd1(comp, 2, false, out);

    for (int bit = 0; bit < wo; bit++) {
      String outSignal = _nets.signalForEndBit(comp, 0, bit, false, out);
      String inSignal = bit < wi ? _nets.signalForEndBit(comp, 1, bit, null, out) : e;
      out.assign(outSignal, inSignal)
    }
    out.stmt();
  }
}
