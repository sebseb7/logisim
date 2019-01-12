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

package com.cburch.logisim.gui.main;
import static com.cburch.logisim.gui.main.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.SyntaxChecker;

public class AttrTableToolModel extends AttributeSetTableModel {
  Project proj;
  Tool tool;

  public AttrTableToolModel(Project proj, Tool tool) {
    super(tool.getAttributeSet());
    this.proj = proj;
    this.tool = tool;
  }

  @Override
  public String getTitle() {
    return S.fmt("toolAttrTitle", tool.getDisplayName());
  }

  public Tool getTool() {
    return tool;
  }

  // fixme: make these read-only instead of throwing error here
  @Override
  public <V> void setValueRequested(Attribute<V> attr, V value)
      throws AttrTableSetException {
    ComponentFactory fact = null;
    if (tool instanceof AddTool)
      fact = ((AddTool)tool).getFactory();
    Circuit circ = null;
    if (fact instanceof SubcircuitFactory)
      circ = ((SubcircuitFactory)fact).getSubcircuit();
    // fixme: also check VhdlEntity.NAME_ATTR
    if (circ != null && !proj.getLogisimFile().contains(circ)) {
      if (attr == CircuitAttributes.NAME_ATTR) {
        String msg = S.get("cannotModifyCircuitError");
        throw new AttrTableSetException(msg);
      }
    }
    String err = null;
    // validate circuit name, label, and other attributes
    if (attr == CircuitAttributes.NAME_ATTR) {
      if (circ == null)
        return;
      String name = ((String)value).trim();
      if (name.equals(circ.getName()))
        return;
      err = ProjectCircuitActions.getNewNameErrors(
              proj.getLogisimFile(), name, false);
    } else if (AttributeSets.isAttrLabel(attr)) {
      String label = (String)value;
      if (!SyntaxChecker.isVariableNameAcceptable(label))
        err = S.get("variableNameNotAcceptable");
    }
    if (err != null) {
      System.out.println("new name err: " + err);
      // try { throw new Exception(); } catch (Exception e) { e.printStackTrace(); }
      throw new AttrTableSetException(err);
    }
    proj.doAction(ToolAttributeAction.create(tool, attr, value));
  }
}
