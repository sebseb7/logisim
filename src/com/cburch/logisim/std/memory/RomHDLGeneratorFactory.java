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

import java.util.SortedMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;

public class RomHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public RomHDLGeneratorFactory(String lang, FPGAReport err) {
    super(lang, err);
  }

  static boolean supports(String lang, AttributeSet attrs, char vendor) {
    return lang.equals("VHDL") || Mem.lineSize(attrs) == 1; // TODO: Verilog support
  }

  @Override
  public String getComponentStringIdentifier() { return "ROM"; }

  @Override
  public String GetSubDir() { return "memory"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Address", addrWidth(attrs));
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int w = dataWidth(attrs);
    list.put("Data", w);
    int n = Mem.lineSize(attrs);
    for (int i = 1; i < n; i++)
      list.put("Data"+i, w);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    list.putAll(GetNetMap("Address", true, info, Mem.ADDR, err, lang, nets));
    list.putAll(GetNetMap("Data", true, info, Mem.DATA, err, lang, nets));
    int n = Mem.lineSize(info.GetComponent().getAttributeSet());
    for (int i = 1; i < n; i++)
      list.putAll(GetNetMap("Data"+i, true, info, Mem.MEM_INPUTS+i-1, err, lang, nets));
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    MemContents rom = attrs.getValue(Rom.CONTENTS_ATTR);
    int n = Mem.lineSize(attrs);
    int wd = dataWidth(attrs);
    int wa = addrWidth(attrs);
    // int shift = (n == 4 ? 2 : n == 2 ? 1 : 0);
    if (out.isVhdl) {
      out.stmt("   MakeRom : PROCESS( Address )");
      out.stmt("      BEGIN");
      out.stmt("         CASE (Address) IS");
      for (long addr = 0; addr < (1 << wa); addr += n) {
        if (filled(rom, addr, n)) {
          out.stmt("            WHEN %s =>\t Data <= %s;",
              out.literal(addr, wa), out.literal(rom.get(addr), wd));
          for (int i = 1; i < n; i++)
            out.cont(" \t Data%d <= %s;", i, out.literal(rom.get(addr+i), wd));
        }
      }
      if (wd == 1) {
        out.stmt("            WHEN others =>\t Data <= '0';");
        for (int i = 1; i < n; i++)
          out.cont(" \t Data%d <= '0';", i);
      } else {
        out.stmt("            WHEN others =>\t Data <= (others => '0');");
        for (int i = 1; i < n; i++)
          out.cont(" \t Data%d <= (others => '0');", i);
      }
      out.stmt("         END CASE;");
      out.stmt("      END PROCESS MakeRom;");
    } else {
      // todo: support verilog with lineSize > 1
      out.stmt("   reg[%d:0] Data;", wd - 1);
      out.stmt("");
      out.stmt("   always @ (Address)");
      out.stmt("   begin");
      out.stmt("      case(Address)");
      for (long addr = 0; addr < (1 << wa); addr++) {
        int data = rom.get(addr);
        if (data != 0)
          out.stmt("         %d : Data = %d;", addr, data);
      }
      out.stmt("         default : Data = 0;");
      out.stmt("      endcase");
      out.stmt("   end");
    }
  }

  protected int addrWidth(AttributeSet attrs) {
    return attrs.getValue(Mem.ADDR_ATTR).getWidth();
  }

  protected int dataWidth(AttributeSet attrs) {
    return attrs.getValue(Mem.DATA_ATTR).getWidth();
  }

  static boolean filled(MemContents rom, long addr, int n) {
    for (int i = 0; i < n; i++)
      if (rom.get(addr+i) != 0)
        return true;
    return false;
  }

}
