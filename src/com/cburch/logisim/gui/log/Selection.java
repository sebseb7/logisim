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
package com.cburch.logisim.gui.log;

import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.circuit.CircuitState;

public class Selection {

  private CircuitState root;
  private Model model;
  private ArrayList<SelectionItem> components;

  public Selection(CircuitState root, Model model) {
    this.root = root;
    this.model = model;
    components = new ArrayList<SelectionItem>();
  }

  public void add(SelectionItem item) {
    components.add(item);
    model.fireSelectionChanged(new Model.Event());
  }

  public void addModelListener(Model.Listener l) {
    model.addModelListener(l);
  }

  public boolean contains(SelectionItem item) {
    return components.contains(item);
  }

  public SelectionItem get(int index) {
    return components.get(index);
  }

  public CircuitState getCircuitState() {
    return root;
  }

  public int indexOf(SelectionItem value) {
    return components.indexOf(value);
  }

  public void add(List<SelectionItem> items, int idx) {
    for (SelectionItem item : items) {
      int i = components.indexOf(item);
      if (i < 0)
        components.add(idx++, item); // put new item at idx
      else if (i > idx)
        components.add(idx++, components.remove(i)); // move later item up
      else if (i < idx)
        components.add(idx-1, components.remove(i)); // move earlier item down
    }
    model.fireSelectionChanged(new Model.Event());
  }

  public int remove(List<SelectionItem> items) {
    int count = 0;
    for (SelectionItem item : items) {
      if (components.remove(item))
        count++;
    }
    if (count > 0)
      model.fireSelectionChanged(new Model.Event());
    return count;
  }

  public void move(int fromIndex, int toIndex) {
    if (fromIndex == toIndex)
      return;
    SelectionItem o = components.remove(fromIndex);
    components.add(toIndex, o);
    model.fireSelectionChanged(new Model.Event());
  }

  public void remove(int index) {
    components.remove(index);
    model.fireSelectionChanged(new Model.Event());
  }

  public void removeModelListener(Model.Listener l) {
    model.removeModelListener(l);
  }

  public int size() {
    return components.size();
  }
}
