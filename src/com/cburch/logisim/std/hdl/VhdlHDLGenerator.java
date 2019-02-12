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

package com.cburch.logisim.std.hdl;

import java.io.File;

import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.Port;

public class VhdlHDLGenerator extends HDLGenerator {

  public VhdlHDLGenerator(HDLCTX ctx) {
    super(ctx, "circuit", deriveHDLName(ctx.attrs), "i_Vhdl");
    VhdlContent content = content();
    for (VhdlContent.Generic g : content.getGenerics()) {
      int v = attrs.getValue(VhdlEntityAttributes.forGeneric(g));
      parameters.add(new ParameterInfo(g.getName(), g.getType(), v, g.getDefaultValue()));
    }
    int i = 0;
    for (VhdlParser.PortDescription p : content.getPorts()) {
      if (p.getType() == Port.INPUT)
        inPorts.add(new PortInfo(p.getName(), p.getWidth().getWidth(), i++, false));
      else
        outPorts.add(new PortInfo(p.getName(), p.getWidth().getWidth(), i++, false));
    }
  }

  static String deriveHDLName(AttributeSet attrs) {
    return ((VhdlEntityAttributes) attrs).getContent().getName(); // .toLowerCase();
  }

  @Override
	protected Hdl getVhdlEntity() {
    Hdl out = new Hdl(_lang, _err);
    VhdlContent content = content();
    generateFileHeader(out);
    out.add(content.getLibraries()); // use libraries from user VHDL content
    generateVhdlBlackBox(out, true);
    return out;
	}

  @Override
	protected Hdl getArchitecture(HashMap<String, File> memInitFiles) {
    Hdl out = new Hdl(_lang, _err);
    VhdlContent content = content();
    out.add(content.getContent()); // take entire user VHDL content as-is
    return out;
  }

  private VhdlContent content() {
    return ((VhdlEntityAttributes) attrs).getContent();
  }

  @Override
  public boolean RequiresNonZeroLabel() { return true; }
}
