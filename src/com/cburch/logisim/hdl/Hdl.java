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

package com.cburch.logisim.hdl;

import java.util.ArrayList;

public class Hdl extends ArrayList<String> {
  
  public enum Lang { VHDL, Verilog }
  public final Lang lang;
  public final boolean isVhdl, isVerilog;

  private int indent = 0;
  private String tab = "";
  private StringBuffer buf = new StringBuffer();
  private ArrayList<Integer> align = new ArrayList<>();

  public Hdl(String oldHdlType) {
    if (oldHdlType.equals("VHDL"))
      lang = Lang.VHDL;
    else if (oldHdlType.equals("Verilog"))
      lang = Lang.Verilog;
    else
      throw new IllegalArgumentException("Unrecognized HDL: " + oldHdlType);
    this.isVhdl = lang == Lang.VHDL;
    this.isVerilog = lang == Lang.Verilog;
  }

  public Hdl(Lang lang) {
    this.lang = lang;
    this.isVhdl = lang == Lang.VHDL;
    this.isVerilog = lang == Lang.Verilog;
  }

  // public Vhdl(String filename) throws IOException {
  // }

  // public void close() throws IOException {
  // }

  public void indent() {
    indent += 2;
    tab = " ".repeat(indent);
  }

  public void dedent() {
    if (indent >= 2) {
      indent -= 2;
      tab = " ".repeat(indent);
    }
  }

  // Note: Embedded '\t' chars define alignment points but otherwise are removed.
  public void stmt(String fmt, Object ...args) {
    String line = tab + String.format(fmt, args);
    align.clear();
    buf.setLength(0);
    int s = 0;
    int t = line.indexOf('\t', s);
    while (t >= 0) {
      align.add(t - align.size());
      buf.append(line.substring(s, t));
      s = t+1;
      t = line.indexOf('\t', s);
    }
    buf.append(line.substring(s));
    add(buf.toString());
    buf.setLength(0);
  }

  // Note: Embedded '\t' chars align to previously-defined alignment points but
  // otherwise are removed.
  public void cont(String fmt, Object ...args) {
    String line = tab + String.format(fmt, args);
    // System.out.println("aligns: " + String.join(",", align));
    System.out.println("alinns:");
    for (int a : align) System.out.printf("  %d\n", a);
    System.out.println("string: " + line);
    buf.setLength(0);
    int s = 0;
    int t = line.indexOf('\t', s);
    int i = 0;
    int p = 0;
    while (t >= 0) {
      System.out.printf("i=%d s=%d t=%d str=\"%s\"\n", i, s, t, line.substring(s, t));
      buf.append(line.substring(s, t));
      p += t-s;
      if (i < align.size()) {
        int a = align.get(i++);
        System.out.printf("a = %d\n", a);
        while (p < a) {
          buf.append(' ');
          p++;
        }
      }
      s = t+1;
      t = line.indexOf('\t', s);
    }
    System.out.printf("i=%d s=%d t=%d str=\"%s\"\n", i, s, t, line.substring(s));
    buf.append(line.substring(s));
    add(buf.toString());
    buf.setLength(0);
  }

}
