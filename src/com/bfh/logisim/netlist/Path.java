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

package com.bfh.logisim.netlist;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;

// A heirarchical path to a component within the tree of circuits and
// subcircuits. Path is immutable. Examples:
//   MainCirc
//   MainCirc/FooSubCirc0/
//   MainCirc/FooSubCirc1/
//   MainCirc/FooSubCirc1/BarSubcirc
public class Path {

  private final String path; // slash-separated representation

  private Path(String s) { path = s; }

  // Construct a root path for a top-level circuit.
  public Path(Circuit circ) {
    this(CorrectLabel.getCorrectLabel(circ.getName().toUpperCase()));
  }

  // Construct a non-root path for component or subcircuit in some circuit.
  public Path extend(NetlistComponent shadow) {
    return new Path(path + "/" + shadow.label());
  }

  // Construct a non-root path for component or subcircuit in some circuit.
  public Path extend(Component comp) {
    return new Path(path + "/" + NetlistComponent.labelOf(comp));
  }

  // Return the parent's path for this non-root path. Fails for root path.
  public Path parent() {
    return new Path(path.substring(0, path.indexOf('/')));
  }

  public boolean isRoot() {
    return path.indexOf('/') < 0;
  }

  // Return the last part of this path. Works even for root paths.
  public String tail() {
    return path.substring(path.indexOf('/')+1);
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof Path) && path.equals(((Path)other).path);
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public String toString() {
    return path;
  }

}
