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

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;

public class ConstantHDLGenerator extends AbstractHDLGeneratorFactory {

  long val;

  public ConstantHDLGenerator(HDLCTX ctx, long val) {
    super(ctx, "Constant"+val, "Const"); // names irrelevant, b/c always inlined ?
    this.val = val;
  }

  @Override
  public boolean IsOnlyInlined(/*String lang*/) { return true; }

  @Override
  public ArrayList<String> GetInlinedCode3(/*Netlist nets, Long id, */
      NetlistComponent info /*, FPGAReport err, String circName, String lang*/) {
    if (_nets == null) throw new IllegalStateException();

    Hdl out = new Hdl(_lang, _err);
    out.indent();

    if (!info.EndIsConnected(0))
      return out;
    
    int w = info.GetComponent().getEnd(0).getWidth().getWidth();

    if (w == 1) { // easy case: single bit
      String name = GetNetName(info, 0, true, _lang, _nets);
      out.assign(name, out.literal(val, w));
    } else if (_nets.IsContinuesBus(info, 0)) { // another easy case
      String name = GetBusNameContinues(info, 0, _lang, _nets);
      out.assign(name, out.literal(val, w));
    } else { // worst case: we have to enumerate all bits
      for (byte bit = 0; bit < w; bit++) {
        String name = GetBusEntryName(info, 0, true, bit, _lang, _nets);
        out.assign(name, out.literal((val>>>bit)&1, 1));
      }
    }
    
    out.stmt("");
    return out;
  }

}
