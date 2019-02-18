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

import java.util.ArrayList;

import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.hdlgenerator.HDLInliner;
import com.cburch.logisim.hdl.Hdl;

public class ConstantHDLGenerator extends HDLInliner {

  private long val;

  public ConstantHDLGenerator(HDLCTX ctx, long val) {
    super(ctx, "Constant_"+val);
    this.val = val;
  }

  @Override
	protected void generateInlinedCode(Hdl out, NetlistComponent comp) {
    if (!comp.EndIsConnected(0))
      return;
    
    ConnectionEnd end = comp.getEnd(0);
    int w = end.NrOfBits();

    if (w == 1) { // easy case: single bit
      String signal = _nets.signalForEndBit(end, 0, out);
      out.assign(signal, out.literal(val, w));
      return;
    }

    int status = _nets.busConnectionStatus(end);
    if (status == Netlist.BUS_UNCONNECTED) {
      return; // should not happen?
    } else if (status == Netlist.BUS_SIMPLYCONNECTED) {
    } else if (status == Netlist.BUS_MULTICONNECTED) {
    } else { // status == BUS_MIXCONNECTED

    }

    // here

    } else if (_nets.IsContinuesBus(comp, 0)) { // another easy case
      String signal = GetBusNameContinues(comp, 0, _lang, _nets);
      out.assign(signal, out.literal(val, w));
    } else { // worst case: we have to enumerate all bits
      for (byte bit = 0; bit < w; bit++) {
        String signal = GetBusEntryName(comp, 0, true, bit, _lang, _nets);
        out.assign(signal, out.literal((val>>>bit)&1, 1));
      }
    }
    
    out.stmt("");
    return out;
  }

}
