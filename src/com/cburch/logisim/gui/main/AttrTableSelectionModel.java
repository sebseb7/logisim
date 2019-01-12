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
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.gui.main.Selection.Event;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.SetAttributeAction;

class AttrTableSelectionModel extends AttributeSetTableModel
  implements Selection.Listener {
  private Project project;
  private Frame frame;

  public AttrTableSelectionModel(Project project, Frame frame) {
    super(frame.getCanvas().getSelection().getAttributeSet());
    this.project = project;
    this.frame = frame;
    frame.getCanvas().getSelection().addListener(this);
  }

  @Override
  public String getTitle() {
    ComponentFactory wireFactory = null;
    ComponentFactory factory = null;
    String label = null;
    Location loc = null;
    int factoryCount = 0;
    int totalCount = 0;
    boolean variousFound = false;

    Selection selection = frame.getCanvas().getSelection();
    for (Component comp : selection.getComponents()) {
      ComponentFactory fact = comp.getFactory();
      if (fact.equals(factory)) {
        factoryCount++;
      } else if (comp instanceof Wire) {
        wireFactory = fact;
        if (factory == null) {
          factoryCount++;
        }
      } else if (factory == null) {
        factory = fact;
        factoryCount = 1;
        label = comp.getAttributeSet().getValue(StdAttr.LABEL);
        loc = comp.getLocation();
      } else {
        variousFound = true;
      }
      if (!(comp instanceof Wire)) {
        totalCount++;
      }
    }

    if (factory == null) {
      factory = wireFactory;
    }

    if (variousFound) {
      return S.fmt("selectionVarious", "" + totalCount);
    } else if (factoryCount == 0) {
      String circName = frame.getCanvas().getCircuit().getName();
      return S.fmt("circuitAttrTitle", circName);
    } else if (factoryCount == 1 && label != null && label.length() > 0) {
      return S.fmt("selectionOne", factory.getDisplayName()) + " \"" + label + "\"";
    } else if (factoryCount == 1 && loc != null) {
      return S.fmt("selectionOne", factory.getDisplayName() + " " + loc);
    } else if (factoryCount == 1) {
      return S.fmt("selectionOne", factory.getDisplayName());
    } else {
      return S.fmt("selectionMultiple", factory.getDisplayName(), "" + factoryCount);
    }
  }

  //
  // Selection.Listener methods
  public void selectionChanged(Event event) {
    fireTitleChanged();
    if (!frame.getEditorView().equals(Frame.EDIT_APPEARANCE)) {
      frame.setAttrTableModel(this);
    }
  }

  @Override
  public <V> void setValueRequested(Attribute<V> attr, V value)
      throws AttrTableSetException {
    Selection selection = frame.getCanvas().getSelection();
    Circuit circuit = frame.getCanvas().getCircuit();
    if (selection.isEmpty() && circuit != null) {
      AttrTableCircuitModel circuitModel = new AttrTableCircuitModel(
          project, circuit);
      circuitModel.setValueRequested(attr, value);
    } else {
      // idea: if attr = label, make each label unique by appending number
      SetAttributeAction act = new SetAttributeAction(circuit,
          S.getter("selectionAttributeAction"));
      for (Component comp : selection.getComponents()) {
        if (!(comp instanceof Wire)) {
          act.set(comp, attr, value);
        }
      }
      project.doAction(act);
    }
  }
}
