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

package com.cburch.logisim.circuit;
import static com.cburch.logisim.circuit.Strings.S;

import java.awt.Graphics;
import java.util.HashMap;
import java.util.HashSet;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;

class PropagationPoints {
  private static class Entry<T> {
    private CircuitState state;
    private T item;

    private Entry(CircuitState state, T item) {
      this.state = state;
      this.item = item;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Entry))
        return false;
      Entry o = (Entry) other;
      return state.equals(o.state) && item.equals(o.item);
    }

    @Override
    public int hashCode() {
      return state.hashCode() * 31 + item.hashCode();
    }
  }

  private HashSet<Entry<Location>> data =  new HashSet<>();
  private HashSet<Entry<Component>> pendingInputs =  new HashSet<>();

  PropagationPoints() { }

  void addPendingInput(CircuitState state, Component comp) {
    pendingInputs.add(new Entry<>(state, comp));
  }

  void add(CircuitState state, Location loc) {
    data.add(new Entry<>(state, loc));
  }

  private void addSubstates(HashMap<CircuitState, CircuitState> map,
      CircuitState source, CircuitState value) {
    map.put(source, value);
    for (CircuitState s : source.getSubstates()) {
      addSubstates(map, s, value);
    }
  }

  void clear() {
    data.clear();
    pendingInputs.clear();
  }

  void draw(ComponentDrawContext context) {
    if (data.isEmpty())
      return;

    CircuitState state = context.getCircuitState();
    HashMap<CircuitState, CircuitState> stateMap = new HashMap<>();
    for (CircuitState s : state.getSubstates())
      addSubstates(stateMap, s, s);

    Graphics g = context.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    for (Entry<Location> e : data) {
      if (e.state == state) {
        Location p = e.item;
        g.drawOval(p.getX() - 4, p.getY() - 4, 8, 8);
      } else if (stateMap.containsKey(e.state)) {
        CircuitState substate = stateMap.get(e.state);
        Component subcirc = substate.getSubcircuit();
        Bounds b = subcirc.getBounds();
        g.drawRect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
      }
    }
    GraphicsUtil.switchToWidth(g, 1);
  }

  void drawPendingInputs(ComponentDrawContext context) {
    if (pendingInputs.isEmpty())
      return;

    CircuitState state = context.getCircuitState();
    HashMap<CircuitState, CircuitState> stateMap = new HashMap<>();
    for (CircuitState s : state.getSubstates())
      addSubstates(stateMap, s, s);

    Graphics g = context.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    for (Entry<Component> e : pendingInputs) {
      Component comp;
      if (e.state == state)
        comp = e.item;
      else if (stateMap.containsKey(e.state))
        comp = stateMap.get(e.state).getSubcircuit();
      else
        continue;
      Bounds b = comp.getBounds();
      g.drawRect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
    }

    GraphicsUtil.switchToWidth(g, 1);
  }

  String getSingleStepMessage() {
    String n = data.isEmpty() ? "no" : ""+data.size();
    String m = pendingInputs.isEmpty() ? "no" : ""+pendingInputs.size();
    return S.fmt("singleStepMessage", n, m);
  }
}
