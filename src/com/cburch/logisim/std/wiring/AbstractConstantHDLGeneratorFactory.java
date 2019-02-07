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

public abstract class AbstractConstantHDLGeneratorFactory
  extends AbstractHDLGeneratorFactory {

  public AbstractConstantHDLGeneratorFactory(String lang, FPGAReport err) {
    super(lang, err);
  }

  @Override
  public boolean IsOnlyInlined(String lang) { return true; }

  @Override
  public ArrayList<String> GetInlinedCode(Netlist nets, Long id,
      NetlistComponent info, FPGAReport err, String circName, String lang) {

    Hdl out = new Hdl(lang, err);
    out.indent();

    if (!info.EndIsConnected(0))
      return out;
    
    int w = info.GetComponent().getEnd(0).getWidth().getWidth();
    int val = getConstant(info.GetComponent().getAttributeSet());

    if (lang.equals("VHDL")) {
      if (w == 1) { // easy case: single bit
        String name = GetNetName(info, 0, true, lang, nets);
        out.stmt("%s <= '%d';", name, val);
      } else if (nets.IsContinuesBus(info, 0)) { // another easy case
        String name = GetBusNameContinues(info, 0, lang, nets);
        out.stmt("%s <= std_logic_vector(to_unsigned(%d,%d));", name, val, w);
      } else { // worst case: we have to enumerate all bits
        for (byte bit = 0; bit < w; bit++) {
          String name = GetBusEntryName(info, 0, true, bit, lang, nets);
          out.stmt("%s <= '%d';", name, (val>>>bit)&1);
        }
      }
    } else {
      if (w == 1) { // easy case: single bit
        String name = GetNetName(info, 0, true, lang, nets);
        out.stmt("%s <= 1'b%d';", name, val);
      } else if (nets.IsContinuesBus(info, 0)) { // another easy case
        String name = GetBusNameContinues(info, 0, lang, nets);
        out.stmt("%s = %d'd%d;", name, w, val);
      } else { // worst case: we have to enumerate all bits
        for (byte bit = 0; bit < w; bit++) {
          String name = GetBusEntryName(info, 0, true, bit, lang, nets);
          out.stmt("%s = 1'b%d;", name, (val>>>bit)&1);
        }
      }
    }
    
    out.stmt("");
    return out;
  }

  public abstract int getConstant(AttributeSet attrs);

}
