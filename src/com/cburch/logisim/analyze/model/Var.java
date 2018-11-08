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

package com.cburch.logisim.analyze.model;

import java.text.ParseException;
import java.util.Iterator;

public class Var implements Iterable<String> {
  public final int width;
  public final String name;

  public Var(String n, int w) {
    name = n;
    width = w;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Var))
      return false;
    Var other = (Var)o;
    return (other.name.equals(this.name) && other.width == this.width);
  }

  @Override
  public int hashCode() {
    return name.hashCode() + width;
  }

  @Override
  public String toString() {
    if (width > 1)
      return name + "[" +(width-1) + "..0]";
    else
      return name;
  }

  public static Var parse(String s) throws ParseException {
    s = s.trim();
    int i = s.indexOf('[');
    int j = s.lastIndexOf(']');
    int w = 1;
    if (0 < i && i < j && j == s.length()-1) {
      String braces = s.substring(i+1, j);
      if (!braces.endsWith("..0"))
        throw new ParseException("Variables must be of the form 'name[N..0]'", i);
      try {
        w = 1+Integer.parseInt(braces.substring(0, braces.length()-3));
      } catch (NumberFormatException e) {
        throw new ParseException("Variables must be of the form 'name[N..0]'", i);
      }
      if (w < 1)
        throw new ParseException("Variables must be of the form 'name[N..0]'", i);
      else if (w > 32)
        throw new ParseException("Variables can't be more than 32 bits wide", i);
      s = s.substring(0, i).trim();
    } else if (i >= 0 || j >= 0) {
      throw new ParseException("Variables must be of the form 'name[N..0]'", i >= 0 ? i : j);
    } else {
      s = s.trim();
    }
    return new Var(s, w);
  }


  public String bitName(int b) {
    if (b >= width) {
      throw new IllegalArgumentException("Can't access bit " + b + " of " + width);
    }
    if (width > 1)
      return name + ":" + b;
    else
      return name;
  }

  public Iterator<String> iterator() {
    return new Iterator<String>() {
      int b = width - 1;
      @Override
      public boolean hasNext() {
        return (b >= 0);
      }
      @Override
      public String next() {
        return bitName(b--);
      }
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
