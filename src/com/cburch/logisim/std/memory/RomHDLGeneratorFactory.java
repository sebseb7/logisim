/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/
package com.cburch.logisim.std.memory;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.data.AttributeSet;

public class RomHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "ROM";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
      AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("Address", attrs.getValue(Mem.ADDR_ATTR).getWidth());
    return Inputs;
  }

  static boolean filled(MemContents rom, long addr, int n) {
    for (int i = 0; i < n; i++)
      if (rom.get(addr+i) != 0)
        return true;
    return false;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
      AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    MemContents rom = attrs.getValue(Rom.CONTENTS_ATTR);
    int n = Mem.lineSize(attrs);
    // int shift = (n == 4 ? 2 : n == 2 ? 1 : 0);
    if (HDLType.equals(Settings.VHDL)) {
      Contents.add("   MakeRom : PROCESS( Address )");
      Contents.add("      BEGIN");
      Contents.add("         CASE (Address) IS");
      for (long addr = 0; addr < (1 << attrs.getValue(Mem.ADDR_ATTR).getWidth()); addr += n) {
        if (filled(rom, addr, n)) {
          Contents.add("            WHEN "
              + IntToBin(addr, attrs.getValue(Mem.ADDR_ATTR).getWidth(), Settings.VHDL)
              + " => Data <= "
              + IntToBin(rom.get(addr), attrs.getValue(Mem.DATA_ATTR).getWidth(), Settings.VHDL)
              + ";");
          for (int i = 1; i < n; i++) {
            Contents.add("                         "
                + " Data"+i+" <= "
                + IntToBin(rom.get(addr+i), attrs.getValue(Mem.DATA_ATTR).getWidth(), Settings.VHDL)
                + ";");
          }
        }
      }
      if (attrs.getValue(Mem.DATA_ATTR).getWidth() == 1) {
        Contents.add("            WHEN OTHERS => Data <= '0';");
        for (int i = 1; i < n; i++) {
          Contents.add("                           Data"+i+" <= '0';");
        }
      } else {
        Contents.add("            WHEN OTHERS => Data <= (OTHERS => '0');");
        for (int i = 1; i < n; i++) {
          Contents.add("                           Data"+i+" <= (OTHERS => '0');");
        }
      }
      Contents.add("         END CASE;");
      Contents.add("      END PROCESS MakeRom;");
    } else {
      // todo: support lineSize > 1
      Contents.add("   reg[" + Integer.toString(attrs.getValue(Mem.DATA_ATTR).getWidth() - 1) + ":0] Data;");
      Contents.add("");
      Contents.add("   always @ (Address)");
      Contents.add("   begin");
      Contents.add("      case(Address)");
      for (long addr = 0; addr < (1 << attrs.getValue(Mem.ADDR_ATTR).getWidth()); addr++) {
        if (rom.get(addr) != 0) {
          Contents.add("         " + addr + " : Data = "
              + rom.get(addr) + ";");
        }
      }
      Contents.add("         default : Data = 0;");
      Contents.add("      endcase");
      Contents.add("   end");
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
      AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    Outputs.put("Data", attrs.getValue(Mem.DATA_ATTR).getWidth());
    int n = Mem.lineSize(attrs);
    for (int i = 1; i < n; i++) {
      Outputs.put("Data"+i, attrs.getValue(Mem.DATA_ATTR).getWidth());
    }
    return Outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets,
      NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    PortMap.putAll(GetNetMap("Address", true, ComponentInfo, Mem.ADDR, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Data", true, ComponentInfo, Mem.DATA, Reporter, HDLType, Nets));
    int n = Mem.lineSize(ComponentInfo.GetComponent().getAttributeSet());
    for (int i = 1; i < n; i++) {
      PortMap.putAll(GetNetMap("Data"+i, true, ComponentInfo, Mem.MEM_INPUTS+i-1, Reporter, HDLType, Nets));
    }
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "memory";
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs, char Vendor) {
    return HDLType.equals(Settings.VHDL) || (Mem.lineSize(attrs) == 1);
  }
}
