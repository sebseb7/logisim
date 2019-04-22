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

package com.cburch.logisim.comp;

import java.awt.Graphics;
import java.util.List;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;

// Tentative Design Notes (3 of 3): There are only four known things that
// implement the Component interface. One is InstanceComponent (with it's
// matching twin helper Instance). Another is Wire. The other two are Splitter
// and Video, which both descend from ManagedComponent, which descends from
// AbstractComponent, which implements the Component interface. Seems a bit
// overkill, no?
//
// To sum up:
//
//             interface Component
//                     |
//      _______________|___________________
//     |               |                   |
//    Wire     AbstractComponent   InstanceComponent <---> Instance
//                     |
//              _______|_______
//             |               |
//          Splitter         Video
//

public interface Component extends Location.At {

  public void addComponentWeakListener(Object owner, ComponentListener l);

  public boolean contains(Location pt);

  public boolean contains(Location pt, Graphics g);

  public void draw(ComponentDrawContext context);

  default public void drawLabel(ComponentDrawContext context) { }

  public boolean endsAt(Location pt);

  public void expose(ComponentDrawContext context);

  public AttributeSet getAttributeSet();

  public Bounds getBounds();

  public Bounds getBounds(Graphics g);

  public EndData getEnd(int index);

  public List<EndData> getEnds();

  public default EndData getEnd(Location p) {
    for (EndData e : getEnds())
      if (p.equals(e.getLocation()))
        return e;
    return null;
  }

  public ComponentFactory getFactory();

  /**
   * Retrieves information about a special-purpose feature for this component.
   * This technique allows future Logisim versions to add new features for
   * components without requiring changes to existing components. It also
   * removes the necessity for the Component API to directly declare methods
   * for each individual feature. In most cases, the <code>key</code> is a
   * <code>Class</code> object corresponding to an interface, and the method
   * should return an implementation of that interface if it supports the
   * feature.
   *
   * As of this writing, possible values for <code>key</code> include:
   * <code>Pokable.class</code>, <code>CustomHandles.class</code>,
   * <code>WireRepair.class</code>, <code>TextEditable.class</code>,
   * <code>MenuExtender.class</code>, <code>ToolTipMaker.class</code>,
   * <code>ExpressionComputer.class</code>, and <code>Loggable.class</code>.
   *
   * @param key
   *            an object representing a feature.
   * @return an object representing information about how the component
   *         supports the feature, or <code>null</code> if it does not support
   *         the feature.
   */
  public Object getFeature(Object key);

  public void propagate(CircuitState state);

  public void removeComponentWeakListener(Object owner, ComponentListener l);

  default public void fireInvalidated() { }

  default public String getDisplayName() {
    String label = getAttributeSet().getValue(StdAttr.LABEL);
    Location loc = this instanceof Wire ? null : getLocation();
    String s = getFactory().getDisplayName();
    if (label != null && label.length() > 0)
      s += " \"" + label + "\"";
    else if (loc != null)
      s += " " + loc;
    return s;
  }
}
