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

import java.util.List;

import com.cburch.logisim.circuit.CircuitState;

class SimulationTreeTopNode extends SimulationTreeNode {

  public SimulationTreeTopNode(SimulationTreeModel model,
      List<CircuitState> allRootStates) {
    super(model, null);
    for (CircuitState state : allRootStates)
      children.add(new SimulationTreeCircuitNode(model, this, state, null));
  }

  public void clear() {
    children.clear();
    fireStructureChanged();
  }

  public SimulationTreeNode addState(CircuitState state) {
    int i = children.size();
    SimulationTreeNode node = new SimulationTreeCircuitNode(model, this, state, null);
    children.add(node);
    model.fire(model.getPath(this), new int[] { i }, new SimulationTreeNode[] { node },
        (l,e) -> l.treeNodesInserted(e));
    return node;
  }

  public void removeState(CircuitState state) {
    for (int i = 0; i < children.size(); i++) {
      SimulationTreeCircuitNode node = (SimulationTreeCircuitNode)children.get(i);
      if (node.getCircuitState() == state) {
        children.remove(i);
        model.fire(model.getPath(this), new int[] { i }, new SimulationTreeNode[] { node },
            (l,e) -> l.treeNodesRemoved(e));
        break;
      }
    }
  }

  @Override
  public String toString() {
    return S.get("activeSimulations");
  }
}
