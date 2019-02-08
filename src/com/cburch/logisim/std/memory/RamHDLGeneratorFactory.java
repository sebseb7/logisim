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
package com.cburch.logisim.std.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;

public class RamHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public RamHDLGeneratorFactory(HDLCTX ctx) {
    super(ctx, deriveHDLName(ctx.attrs), "RAM");
  }
  
  private static String deriveHDLName(AttributeSet attrs) {
    if (attrs.getValue(RamAttributes.ATTR_TYPE) == RamAttributes.VOLATILE)
      return "NVRAM_${CIRCUIT}_${LABEL}";
    int wd = dataWidth(attrs);
    int wa = addrWidth(attrs);
    int n = Mem.lineSize(attrs);
    return String.format("RAM_%dx%dx%d", wd, n, 1<<wa);
  }

  private static final int TYPE_MEM_ARRAY = -1;

  static boolean supports(String lang, AttributeSet attrs, char vendor) {
    Object dbus = attrs.getValue(RamAttributes.ATTR_DBUS);
    boolean separate = dbus == RamAttributes.BUS_SEP;
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    boolean synch = trigger == StdAttr.TRIG_RISING || trigger == StdAttr.TRIG_FALLING;
    return lang.equals("VHDL") && separate && synch;
  }

  @Override
  public String GetSubDir() { return "memory"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int n = Mem.lineSize(attrs);
    list.put("Address", addrWidth(attrs));
    for (int i = 0; i < n; i++)
      list.put("DataIn"+i, dataWidth(attrs));
    list.put("WE", 1);
    list.put("Clock", 1);
    list.put("Tick", 1);
    for (int i = 0; i < n && n > 1; i++)
      list.put("LE"+i, 1);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int n = Mem.lineSize(attrs);
    for (int i = 0; i < n; i++)
      list.put("DataOut"+i, dataWidth(attrs));
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();

    int n = Mem.lineSize(attrs);
    int DATA1 = Mem.MEM_INPUTS; // (n-1) of them
    int DATAOUT[] = { Mem.DATA, DATA1, DATA1+1, DATA1+2 };
    int DIN0 = DATA1+(n-1); // (n) of them
    int DATAIN[] = { DIN0, DIN0+1, DIN0+2, DIN0+3 };
    int CLK = (DIN0 + n); // 1, always
    int WE = CLK+1; // 1, always
    int LE = WE+1; // (datalines) of them, only if multiple data lines

    list.putAll(GetNetMap("Address", true, info, Mem.ADDR, err, lang, nets));
    for (int i = 0; i < n; i++)
      list.putAll(GetNetMap("DataIn"+i, true, info, DATAIN[i], err, lang, nets));
    list.putAll(GetNetMap("WE", true, info, WE, err, lang, nets));
    for (int i = 0; i < n && n > 1; i++)
      list.putAll(GetNetMap("LE"+i, false, info, LE+i, err, lang, nets));

    String BracketOpen = (lang.equals("VHDL")) ? "(" : "[";
    String BracketClose = (lang.equals("VHDL")) ? ")" : "]";

    boolean vhdl = lang.equals("VHDL");
    String zero = vhdl ? "'0'" : "1'b0";
    String one = vhdl ? "'1'" : "1'b1";
    String idx = vhdl ? "(%d)" : "[%]"; // fixme: these should be in base class at minimum!

    boolean hasClk = info.EndIsConnected(CLK);
    if (!hasClk)
      err.AddSevereWarning("Component \"RAM\" in circuit \""
          + nets.getCircuitName() + "\" has no clock connection");
    
    String clk = GetClockNetName(info, CLK, nets);
    boolean gatedClk = clk.equals("");
    if (gatedClk)
      err.AddSevereWarning("Component \"RAM\" in circuit \""
          + nets.getCircuitName() + "\" has a none-clock-component forced clock!\n"
          + "   Functional differences between Logisim simulation and hardware can be expected!");

    if (!hasClk) {
      list.put("Clock", zero);
      list.put("Tick", zero);
    } else if (gatedClk) {
      list.putAll(GetNetMap("Clock", true, info, CLK, err, lang, nets));
      list.put("Tick", one);
    } else {
      list.put("Clock", String.format(clk+idx, ClockHDLGeneratorFactory.GlobalClockIndex));
      if (nets.RequiresGlobalClockConnection())
        list.put("Tick", String.format(clk+idx, ClockHDLGeneratorFactory.GlobalClockIndex));
      else if (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING)
        list.put("Tick", String.format(clk+idx, ClockHDLGeneratorFactory.PositiveEdgeTickIndex));
      else
        list.put("Tick", String.format(clk+idx, ClockHDLGeneratorFactory.NegativeEdgeTickIndex));
    }
    for (int i = 0; i < n; i++)
      list.putAll(GetNetMap("DataOut"+i, true, info, DATAOUT[i], err, lang, nets));
  }

  @Override
  public Map<String, ArrayList<String>> GetMemInitData(/*AttributeSet attrs*/) {
    if (!_attrs.getValue(RamAttributes.ATTR_TYPE).equals(RamAttributes.NONVOLATILE)) {
      return null;
    }
    Map<String, ArrayList<String>> m = new HashMap<String, ArrayList<String>>();
    int n = Mem.lineSize(_attrs);
    for (int i = 0; i < n; i++) {
      ArrayList<String> contents = MemInitData(i);
      m.put("s_mem"+i+"_contents", contents);
    }
    return m;
  }

  private ArrayList<String> MemInitData(int offset) {
    int skip = Mem.lineSize(_attrs);
    int width = dataWidth(_attrs);
    MemContents c = null; // _attrs.getValue(Ram.CONTENTS_ATTR); FIXME, if possible ?!?!
    ArrayList<String> out = new ArrayList<String>();
    out.add("-- Memory initialization data line " + offset);
    int depth = (int)((c.getLastOffset() - c.getFirstOffset() + 1) / skip);
    out.add("DEPTH = " + depth + ";");
    out.add("WIDTH = " + width + ";");
    out.add("ADDRESS_RADIX = HEX;");
    out.add("DATA_RADIX = HEX;");
    out.add("CONTENT");
    out.add("BEGIN");
    for (int a = 0; a < depth; a++) {
      int d = c.get(a*skip+offset);
      if (width != 32)
        d &= ((1 << width) - 1);
      out.add(String.format("%8x : %8x;", a, d));
    }
    out.add("END;");
    return out;
  }

  @Override
  public SortedMap<String, Integer> GetMemList(AttributeSet attrs, String lang) {
    SortedMap<String, Integer> list = new TreeMap<String, Integer>();
    int n = Mem.lineSize(attrs);
    for (int i = 0; i < n; i++)
      list.put("s_mem"+i+"_contents", TYPE_MEM_ARRAY);
    return list;
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    out.indent();
    int n = Mem.lineSize(attrs);
    if (n == 1) {
      out.stmt("Mem0 : PROCESS( Clock, DataIn0, Address, WE, Tick )");
      out.stmt("BEGIN");
      out.stmt("   IF (Clock'event AND (Clock = '1')) THEN");
      out.stmt("      IF (WE = '1' and Tick = '1') THEN");
      out.stmt("         s_mem0_contents(to_integer(unsigned(Address))) <= DataIn0;");
      out.stmt("      ELSE");
      out.stmt("          DataOut0 <= s_mem0_contents(to_integer(unsigned(Address)));");
      out.stmt("      END IF;");
      out.stmt("   END IF;");
      out.stmt("END PROCESS Mem0;");
    } else {
      int sa = (n == 4 ? 2 : n == 2 ? 1 : 0);
      for (int i = 0; i < n; i++) {
        out.stmt("Mem%d : PROCESS( Clock, DataIn%d, Address, WE, LE%d, Tick )", i, i, i);
        out.stmt("BEGIN");
        out.stmt("   IF (Clock'event AND (Clock = '1')) THEN");
        out.stmt("      IF (WE = '1' and LE%d = '1' and Tick = '1') THEN", i);
        out.stmt("         s_mem%d_contents(to_integer(shift_right(unsigned(Address),%d))) <= DataIn%d;", i, sa, i);
        out.stmt("      ELSE");
        out.stmt("          DataOut%d <= s_mem%d_contents(to_integer(shift_right(unsigned(Address),%d)));", i, i, sa);
        out.stmt("      END IF;");
        out.stmt("   END IF;");
        out.stmt("END PROCESS Mem%d;", i);
      }
    }
  }

  @Override
  public int GetNrOfTypes(Netlist TheNetlist, AttributeSet attrs, String lang) {
    return 1;
  }

  @Override
  public String GetType(int TypeNr) {
    if (TypeNr == TYPE_MEM_ARRAY)
      return "MEMORY_ARRAY";
    else
      return "";
  }

  @Override
  protected SortedSet<String> GetTypeDefinitions(Netlist TheNetlist, AttributeSet attrs, String lang) {
    SortedSet<String> list = new TreeSet<String>();
    int wd = dataWidth(attrs);
    int wa = addrWidth(attrs);
    int rows = (1 << wa);
    list.add("TYPE MEMORY_ARRAY"
        + " IS ARRAY (" + (rows-1) + " DOWNTO 0)"
        + " OF std_logic_vector(" + (wd-1) + " DOWNTO 0)");
    return list;
  }

  protected static int addrWidth(AttributeSet attrs) {
    return attrs.getValue(Mem.ADDR_ATTR).getWidth();
  }

  protected static int dataWidth(AttributeSet attrs) {
    return attrs.getValue(Mem.DATA_ATTR).getWidth();
  }
}
