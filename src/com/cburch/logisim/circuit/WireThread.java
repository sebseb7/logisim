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

import java.util.HashSet;

class WireThread {
  private WireThread representative;
  private HashSet<BundlePosition> tempBundlePositions = new HashSet<>();
  public int steps; // will be set when BundleMap is done being constructed
  public WireBundle[] bundle; // will be set when BundleMap is done being constructed
  public int[] position; // will be set when BundleMap is done being constructed

  private static class BundlePosition {
    int pos;
    WireBundle b;
    BundlePosition(int pos, WireBundle b) {
      this.pos = pos;
      this.b = b;
    }
  }

  WireThread() {
    representative = this;
  }

  void addBundlePosition(int pos, WireBundle b) {
    tempBundlePositions.add(new BundlePosition(pos, b));
  }

  void finishConstructing() {
    if (tempBundlePositions == null)
      return;
    steps = tempBundlePositions.size();
    bundle = new WireBundle[steps];
    position = new int[steps];
    int i = 0;
    for (BundlePosition bp : tempBundlePositions) {
      bundle[i] = bp.b;
      position[i] = bp.pos;
      i++;
    }
    tempBundlePositions = null;
  }

  WireThread getRepresentative() {
    WireThread ret = this;
    if (ret.representative != ret) {
      do
        ret = ret.representative;
      while (ret.representative != ret);
      this.representative = ret;
    }
    return ret;
  }

  void unite(WireThread other) {
    WireThread us = this.getRepresentative();
    WireThread them = other.getRepresentative();
    if (us != them)
      us.representative = them;
  }
}
