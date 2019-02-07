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

import com.bfh.logisim.fpgagui.FPGAReport;

// Represents a snippet of VHDL or Verilog Hdl code, and contains
// some helpers for programmatically generating such code.
public class Hdl extends ArrayList<String> {
  
  public enum Lang { VHDL, Verilog }
  public final Lang lang;

  public final boolean isVhdl, isVerilog;
  public final FPGAReport err;

  public final String assn;
  public final String bitAssn;
  public final String assnBit;
  public final String bitAssnBit;
  public final String idx;
  public final String zero;
  public final String one;

  private int indent = 0;
  private String tab = "";
  private StringBuffer buf = new StringBuffer();
  private ArrayList<Integer> align = new ArrayList<>();

  public Hdl(String lang, FPGAReport err) {
    if (lang.equals("VHDL"))
      this.lang = Lang.VHDL;
    else if (lang.equals("Verilog"))
      this.lang = Lang.Verilog;
    else
      throw new IllegalArgumentException("Unrecognized HDL: " + lang);
    this.isVhdl = this.lang == Lang.VHDL;
    this.isVerilog = this.lang == Lang.Verilog;
    this.err = err;
    if (isVhdl) {
      assn = "%s \t<= %s;";
      bitAssn = "%s(%d) \t<= %s;";
      assnBit = "%s \t<= %s(%d);";
      bitAssnBit = "%s(%d) \t<= %s(%d);";
      idx = "(%d)";
      zero = "'0'";
      one = "'1'";
    } else {
      assn = "assign %s \t= %s;";
      bitAssn = "assign %s[%d] \t= %s;";
      assnBit = "assign %s \t= %s[%d];";
      bitAssnBit = "assign %s[%d] \t= %s[%d];";
      idx = "[%d]";
      zero = "1b'0";
      one = "1b'1";
    }
  }

  // public Hdl(Lang lang) {
  //   this.lang = lang;
  //   this.isVhdl = lang == Lang.VHDL;
  //   this.isVerilog = lang == Lang.Verilog;
  //   this.err = null;
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

  public void stmt() {
    align.clear();
    add("");
  }

  // Note: Embedded '\t' chars define alignment points but otherwise are removed.
  public void stmt(String fmt, Object ...args) {
    stmt(String.format(fmt, args).split("\\R", -1));
  }

  public void stmt(String[] lines) {
    align.clear();
    if (lines.length == 0)
      return;
    String line = lines[0];
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
    cont(1, lines);
  }

  // Note: Embedded '\t' chars align to previously-defined alignment points but
  // otherwise are removed.
  public void cont(String fmt, Object ...args) {
    cont(String.format(fmt, args).split("\\R", -1));
  }

  public void cont(String[] lines) {
    cont(0, lines);
  }

  private void cont(int idx, String[] lines) {
    for (int j = idx; j < lines.length; j++) {
      String line = lines[j];
      buf.setLength(0);
      int s = 0;
      int t = line.indexOf('\t', s);
      int i = 0;
      int p = 0;
      while (t >= 0) {
        buf.append(line.substring(s, t));
        p += t-s;
        if (i < align.size()) {
          int a = align.get(i++);
          while (p < a) {
            buf.append(' ');
            p++;
          }
        }
        s = t+1;
        t = line.indexOf('\t', s);
      }
      buf.append(line.substring(s));
      add(buf.toString());
      buf.setLength(0);
    }
  }

  public void assign(String name, String val) {
    stmt(assn, name, val);
  }

  public void assign(String name, int nameIdx, String val) {
    stmt(bitAssn, name, nameIdx, val);
  }

  public void assign(String name, String val, int valIdx) {
    stmt(assnBit, name, val, valIdx);
  }

  public void assign(String name, int nameIdx, String val, int valIdx) {
    stmt(bitAssnBit, name, nameIdx, val, valIdx);
  }

  // todo: consider enforcing max line length (e.g. 80 chars)?
  public void comment(String fmt, Object ...args) {
    comment(String.format(fmt, args).split("\\R", -1));
  }

  public void comment(String[] lines) {
    if (isVhdl)
      for (String line : lines)
        add(tab + "-- " + line);
    else
      for (String line : lines)
        add(tab + "// " + line);
  }

  // For VHDL, returns '0', '1', "00101101", "1011011", etc.
  // For Verilog, returns 1'b0, 1'b1, 5'b01011, 3'b110, etc.
	public String literal(long value, int width) {
    if (width == 0)
      throw new IllegalArgumentException("Can't create HDL literal with zero width");
    if (width == 1)
      return (value & 1) == 0 ? zero : one;
		StringBuffer out = new StringBuffer();
    if (isVhdl) {
      out.append('\'');
      for (int i = width - 1; i >= 0; i--)
        out.append(((value >>> i) & 1) == 0 ? '0' : '1');
      out.append('\'');
    } else {
      out.append(width);
      out.append("'b");
      for (int i = width - 1; i >= 0; i--)
        out.append(((value >>> i) & 1) == 0 ? '0' : '1');
    }
    return out.toString();
	}
 
  // Same as literal(0, 1) or literal(1, 1).
  public String bit(boolean value) {
    return value ? one : zero;
  }

  // Same as literal(value, 1).
  public String bit(int value) {
    return (value & 1) == 0 ? zero : one;
  }

  // Similar to literal(0, width), but uses hex or decimal to be pretty.
  public String zeros(int width) { return fill(false, width); }

  // Similar to literal(1, width), but uses hex or decimal to be pretty.
  public String ones(int width) { return fill(true, width); }

  // Similar to literal(value?-1:0, width), but uses hex or decimal to be pretty.
  // For VHDL, returns X"FFF", or "111"&X"FFFF", etc.
  // For Verilog, returns 7'd-1, 7'd0, etc.
  public String fill(boolean value, int width) {
    if (width == 0)
      throw new IllegalArgumentException("Can't create HDL literal with zero width");
    else if (width == 1)
      return value ? one : zero;
    else if (isVerilog)
      return value ? width+"'d-1" : zero;
    else if (width % 4 == 0)
      return "X\"" + (value?"F":"0").repeat(width/4);
    else if (width > 12)
      return "\"" + (value?"1":"0").repeat(width%4) + "\" & X\"" + ("F".repeat(width/4));
    else 
      return "\"" + (value?"1":"0").repeat(width) + "\"";
  }

}
