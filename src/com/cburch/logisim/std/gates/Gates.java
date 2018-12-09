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
import static com.cburch.logisim.std.Strings.S;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Gates extends Library {
  private List<Tool> tools = null;

  public Gates() {
    tools = Arrays.asList(new Tool[] {
      new AddTool(Gates.class, NotGate.FACTORY),
      new AddTool(Gates.class, Buffer.FACTORY),
      new AddTool(Gates.class, AndGate.FACTORY),
      new AddTool(Gates.class, OrGate.FACTORY),
      new AddTool(Gates.class, NandGate.FACTORY),
      new AddTool(Gates.class, NorGate.FACTORY),
      new AddTool(Gates.class, XorGate.FACTORY),
      new AddTool(Gates.class, XnorGate.FACTORY),
      new AddTool(Gates.class, OddParityGate.FACTORY),
      new AddTool(Gates.class, EvenParityGate.FACTORY),
      new AddTool(Gates.class, ControlledBuffer.FACTORY_BUFFER),
      new AddTool(Gates.class, ControlledBuffer.FACTORY_INVERTER),
      new AddTool(Gates.class, PLA.FACTORY),
    });
  }

  @Override
  public String getDisplayName() {
    return S.get("gatesLibrary");
  }

  @Override
  public String getName() {
    return "Gates";
  }

  @Override
  public List<Tool> getTools() {
    return tools;
  }
}
