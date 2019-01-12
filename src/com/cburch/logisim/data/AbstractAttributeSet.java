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

package com.cburch.logisim.data;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAttributeSet implements Cloneable, AttributeSet {
  private ArrayList<AttributeListener> listeners = null;

  public AbstractAttributeSet() { }

  public void addAttributeListener(AttributeListener l) {
    if (listeners == null)
      listeners = new ArrayList<AttributeListener>();
    listeners.add(l);
  }

  public void removeAttributeListener(AttributeListener l) {
    if (listeners == null)
      return;
    listeners.remove(l);
    if (listeners.isEmpty())
      listeners = null;
  }

  public boolean amIListening(AttributeListener l) {
    return listeners != null && listeners.contains(l);
  }

  @Override
  public Object clone() {
    AbstractAttributeSet ret;
    try {
      ret = (AbstractAttributeSet)super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new UnsupportedOperationException("AbstractAttributeSet.clone");
    }
    // old listeners don't want to listen to clone instead?!?
    ret.listeners = null;
    this.copyInto(ret);
    return ret;
  }

  protected abstract void copyInto(AbstractAttributeSet dest);

  protected void fireAttributeListChanged() {
    if (listeners == null)
      return;
    AttributeEvent event = new AttributeEvent(this);
    List<AttributeListener> ls = new ArrayList<>(listeners);
    for (AttributeListener l : ls)
      l.attributeListChanged(event);
  }

  protected <V> void fireAttributeValueChanged(Attribute<? super V> attr, V value) {
    if (listeners == null)
      return;
    AttributeEvent event = new AttributeEvent(this, attr, value);
    List<AttributeListener> ls = new ArrayList<>(listeners);
    for (AttributeListener l : ls)
      l.attributeValueChanged(event);
  }

  // public abstract List<Attribute<?>> getAttributes();

  // public abstract <V> V getValue(Attribute<V> attr);

  public final <V> void changeAttr(Attribute<V> attr, V value) {
    updateAttr(attr, value);
    fireAttributeValueChanged(attr, value);
  }

  public abstract <V> void updateAttr(Attribute<V> attr, V value);

}
