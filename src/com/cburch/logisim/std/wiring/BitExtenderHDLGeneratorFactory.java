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

public class BitExtenderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public BitExtenderHDLGeneratorFactory(HDLCTX ctx) {
    super(ctx, "Bit_Extender", "BITEXTEND"); // names irrelevant, b/c always inlined ?
  }

  @Override
  public boolean IsOnlyInlined(/*String lang*/) { return true; }

  @Override
  public ArrayList<String> GetInlinedCode3(/*Netlist nets, Long id, */
      NetlistComponent info /*, FPGAReport err, String circName, String lang*/) {
    if (_nets == null) throw new IllegalStateException();

    Hdl out = new Hdl(_lang, _err);
    out.indent();

    boolean vhdl = _lang.equals("VHDL");
    String zero = vhdl ? "'0'" : "1'b0";
    String one = vhdl ? "'1'" : "1'b1";
    String assn = vhdl ? "%s <= %s;" : "assign %s = %s;";

    // checks input and signinput too
    for (int i = 1; i < info.NrOfEnds(); i++) {
      if (!info.EndIsConnected(i)) {
        String circName = info.GetComponent().getFactory().toString(); // ??
        _err.AddError("Bit Extender has floating input in circuit \"" + circName + "\"");
        return out;
      }
    }

    // fixme: difference b/w info.getEnd() and info.GetComponent().getEnd() ?!?
    int wo = info.GetComponent().getEnd(0).getWidth().getWidth();
    int wi = info.GetComponent().getEnd(1).getWidth().getWidth();
    // int wi2 = info.getEnd(1).NrOfBits();
    String type = (String) info.GetComponent().getAttributeSet().getValue(BitExtender.ATTR_TYPE).getValue();

    String e = "";
    if (type.equals("zero"))
      e = zero;
    else if (type.equals("one"))
      e = one;
    else if (type.equals("sign") && wi == 1)
      e = GetNetName(info, 1, true, _lang, _nets); // fixme: GetBusEntryName should handle this
    else if (type.equals("sign"))
      e = GetBusEntryName(info, 1, true, wi - 1, _lang, _nets);
    else if (type.equals("input"))
      e = GetNetName(info, 2, true, _lang, _nets);

    if (wo == 1) {
      String name = GetNetName(info, 0, true, _lang, _nets);
      out.stmt(assn, name, GetNetName(info, 1, true, _lang, _nets));
    } else {
      for (int bit = 0; bit < wo; bit++) {
        String name = GetBusEntryName(info, 0, true, bit, _lang, _nets);
        if (bit == 0 && wi == 1)
          out.stmt(assn, name, GetNetName(info, 1, true, _lang, _nets) + ";");
        else if (bit < wi)
          out.stmt(assn, name, GetBusEntryName(info, 1, true, bit, _lang, _nets));
        else
          out.stmt(assn, name, e);
      }
    }

    out.stmt("");
    return out;
  }
}
