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

import java.util.List;

public interface AttributeSet {

  public void addAttributeWeakListener(Object owner, AttributeListener l);
  public void removeAttributeWeakListener(Object owner, AttributeListener l);

  public Object clone();

  public List<Attribute<?>> getAttributes();

  public default Attribute<?> getAttribute(String name) {
    for (Attribute<?> attr : getAttributes())
      if (attr.getName().equals(name))
        return attr;
    return null;
  }

  public default boolean containsAttribute(Attribute<?> attr) {
    return getAttributes().contains(attr);
  }

  public default boolean isReadOnly(Attribute<?> attr) { return false; }

  public default boolean isToSave(Attribute<?> attr) { return true; }

  public default void setReadOnly(Attribute<?> attr, boolean value) {
    throw new UnsupportedOperationException("Attribute.setReadOnly");
  }

  public default void setToSave(Attribute<?> attr, boolean value) {
    // optional, so no error
  }

  // getValue() returns null if attr was not found
  public <V> V getValue(Attribute<V> attr);

  public default <V> V getValueOrElse(Attribute<V> attr, V valueIfUnset) {
    V v = getValue(attr);
    return v == null ? valueIfUnset : v;
  }
  
  // Note: changeAttr() and setAttr() must not fail by putting up a dialog. Any
  // validation must be done before these are called. Worst case, these can
  // ignore the new value, or coerce it into soemthing valid. But they should
  // not cause the keyboard or mouse focus to change.
  public <V> void changeAttr(Attribute<V> attr, V value);

  // Normally, callers should use setAttr(), which ignores re-setting the same
  // value that is already set.
  public default <V> void setAttr(Attribute<V> attr, V value) {
    if (isReadOnly(attr)) {
      System.err.printf("attempt change readonly attribute %s to %s\n",
          attr, value);
      return;
    }
    V oldValue = getValue(attr);
    if (oldValue == null && value == null)
      return;
    if (oldValue != null && value != null && oldValue.equals(value))
      return;
    changeAttr(attr, value);
  }
}
