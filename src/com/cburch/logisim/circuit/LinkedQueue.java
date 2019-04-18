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
import static com.cburch.logisim.circuit.SplayQueue.Node;

// A simple linked-list queue implementation, using keys of type long, and
// values that extend type SplayQueue.Node. This supports (approximately) a
// subset of the java.util.PriorityQueue API, but only enough to support
// Propagator.
public class LinkedQueue<T extends SplayQueue.Node> {

  // Objects in the queue must be subclasses of SplayQueue.Node.

  private Node head, tail;
  private int size;

  // add(t) inserts a new node into the queue.
  public void add(T t) {
    size++;

    if (tail == null) {
      head = tail = t;
      t.left = t.right = null;
      return;
    }

    // Find node p that should preceed t.
    Node p = tail;
    while (p != null && t.key < p.key)
      p = p.left;

    if (p == null) {
      t.right = head;
      t.left = null;
      head.left = t;
      head = t;
    } else {
      t.right = p.right;
      t.left = p;
      if (p.right == null)
        tail = t;
      else
        p.right.left = t;
      p.right = t;
    }
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public void clear() {
    head = tail = null;
    size = 0;
  }

  // peek() returns the smallest node, or null if the queue is empty.
  public T peek() {
    return (T)head;
  }

  // remove() removes the smallest node, or null if the queue is empty.
  public T remove() {
    if (head == null)
      return null;
    size--;
    T t = (T)head;
    head = head.right;
    if (head == null)
      tail = null;
    else
      head.left = null;
    return t;
  }

  String id(Node n) {
    if (n == null)
      return "null";
    else
      return "@"+n.hashCode();
  }

}
