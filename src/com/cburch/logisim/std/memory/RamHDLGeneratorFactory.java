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
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;

public class RamHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final int MemArrayId = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "RAM";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    int NrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    int n = Mem.lineSize(attrs);
    Inputs.put("Address", attrs.getValue(Mem.ADDR_ATTR).getWidth());
    for (int i = 0; i < n; i++)
      Inputs.put("DataIn"+i, NrOfBits);
    Inputs.put("WE", 1);
    Inputs.put("Clock", 1);
    Inputs.put("Tick", 1);
    for (int i = 0; i < n && n > 1; i++)
      Inputs.put("LE"+i, 1);
    return Inputs;
  }

  @Override
  public Map<String, ArrayList<String>> GetMemInitData(AttributeSet attrs) {
    if (!attrs.getValue(RamAttributes.ATTR_TYPE).equals(RamAttributes.NONVOLATILE)) {
      return null;
    }
    Map<String, ArrayList<String>> m = new HashMap<String, ArrayList<String>>();
    int dataLines = Mem.lineSize(attrs);
    for (int i = 0; i < dataLines; i++) {
      ArrayList<String> contents = MemInitData(attrs, i);
      m.put("s_mem"+i+"_contents", contents);
    }
    return m;
  }

  private ArrayList<String> MemInitData(AttributeSet attrs, int offset) {
    int skip = Mem.lineSize(attrs);
    int width = attrs.getValue(Mem.DATA_ATTR).getWidth();
    MemContents c = attrs.getValue(Ram.CONTENTS_ATTR);
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
  public SortedMap<String, Integer> GetMemList(AttributeSet attrs, String HDLType) {
    SortedMap<String, Integer> Mems = new TreeMap<String, Integer>();
    if (HDLType.equals(Settings.VHDL)) {
      int dataLines = Mem.lineSize(attrs);
      for (int i = 0; i < dataLines; i++)
        Mems.put("s_mem"+i+"_contents", MemArrayId);
    }
    return Mems;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
      AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    int dataLines = Mem.lineSize(attrs);
    if (HDLType.equals(Settings.VHDL)) {
      Contents.addAll(MakeRemarkBlock("Here the actual memorie(s) is(are) defined", 3, HDLType));
      if (dataLines == 1) {
        Contents.add("   Mem0 : PROCESS( Clock, DataIn0, Address, WE, Tick )");
        Contents.add("   BEGIN");
        Contents.add("      IF (Clock'event AND (Clock = '1')) THEN");
        Contents.add("         IF (WE = '1' and Tick = '1') THEN");
        Contents.add("            s_mem0_contents(to_integer(unsigned(Address))) <= DataIn0;");
        Contents.add("         ELSE");
        Contents.add("             DataOut0 <= s_mem0_contents(to_integer(unsigned(Address)));");
        Contents.add("         END IF;");
        Contents.add("      END IF;");
        Contents.add("   END PROCESS Mem0;");
      } else {
        int sa = (dataLines == 4 ? 2 : dataLines == 2 ? 1 : 0);
        for (int i = 0; i < dataLines; i++) {
          Contents.add("   Mem"+i+" : PROCESS( Clock, DataIn"+i+", Address, WE, LE"+i+", Tick )");
          Contents.add("   BEGIN");
          Contents.add("      IF (Clock'event AND (Clock = '1')) THEN");
          Contents.add("         IF (WE = '1' and LE"+i+" = '1' and Tick = '1') THEN");
          Contents.add("            s_mem"+i+"_contents(to_integer(shift_right(unsigned(Address),"+sa+"))) <= DataIn"+i+";");
          Contents.add("         ELSE");
          Contents.add("             DataOut"+i+" <= s_mem"+i+"_contents(to_integer(shift_right(unsigned(Address),"+sa+")));");
          Contents.add("         END IF;");
          Contents.add("      END IF;");
          Contents.add("   END PROCESS Mem"+i+";");
        }
      }
    }
    return Contents;
  }

  @Override
  public int GetNrOfTypes(Netlist TheNetlist, AttributeSet attrs, String HDLType) {
    return 1;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    int dataLines = Mem.lineSize(attrs);
    for (int i = 0; i < dataLines; i++)
      Outputs.put("DataOut"+i, attrs.getValue(Mem.DATA_ATTR).getWidth());
    return Outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets,
      NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();

    int dataLines = Mem.lineSize(attrs);
    int DATA1 = Mem.MEM_INPUTS; // (dataLines-1) of them
    int DATAOUT[] = { Mem.DATA, DATA1, DATA1+1, DATA1+2 };
    int DIN0 = DATA1+(dataLines-1); // (dataLines) of them
    int DATAIN[] = { DIN0, DIN0+1, DIN0+2, DIN0+3 };
    int CLK = (DIN0 + dataLines); // 1, always
    int WE = CLK+1; // 1, always
    int LE = WE+1; // (datalines) of them, only if multiple data lines

    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    PortMap.putAll(GetNetMap("Address", true, ComponentInfo, Mem.ADDR, Reporter, HDLType, Nets));
    for (int i = 0; i < dataLines; i++)
      PortMap.putAll(GetNetMap("DataIn"+i, true, ComponentInfo, DATAIN[i], Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("WE", true, ComponentInfo, WE, Reporter, HDLType, Nets));
    for (int i = 0; i < dataLines && dataLines > 1; i++)
      PortMap.putAll(GetNetMap("LE"+i, false, ComponentInfo, LE+i, Reporter, HDLType, Nets));

    String SetBit = (HDLType.equals(Settings.VHDL)) ? "'1'" : "1'b1";
    String ZeroBit = (HDLType.equals(Settings.VHDL)) ? "'0'" : "1'b0";
    String BracketOpen = (HDLType.equals(Settings.VHDL)) ? "(" : "[";
    String BracketClose = (HDLType.equals(Settings.VHDL)) ? ")" : "]";
    if (!ComponentInfo.EndIsConnected(CLK)) {
      Reporter.AddError("Component \"RAM\" in circuit \"" + Nets.getCircuitName() + "\" has no clock connection!");
      PortMap.put("Clock", ZeroBit);
      PortMap.put("Tick", ZeroBit);
    } else {
      String ClockNetName = GetClockNetName(ComponentInfo, CLK, Nets);
      if (ClockNetName.isEmpty()) {
        Reporter.AddSevereWarning("Component \"RAM\" in circuit \""
            + Nets.getCircuitName()
            + "\" has a none-clock-component forced clock!\n"
            + "        Functional differences between Logisim simulation and hardware can be expected!");
        PortMap.putAll(GetNetMap("Clock", true, ComponentInfo, CLK, Reporter, HDLType, Nets));
        PortMap.put("Tick", SetBit);
      } else {
        int ClockBusIndex;
        if (Nets.RequiresGlobalClockConnection()) {
          ClockBusIndex = ClockHDLGeneratorFactory.GlobalClockIndex;
        } else {
          ClockBusIndex = (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING)
              ? ClockHDLGeneratorFactory.PositiveEdgeTickIndex
              : ClockHDLGeneratorFactory.NegativeEdgeTickIndex;
        }
        PortMap.put("Clock", ClockNetName + BracketOpen + ClockHDLGeneratorFactory.GlobalClockIndex + BracketClose);
        PortMap.put("Tick", ClockNetName + BracketOpen + ClockBusIndex + BracketClose);
      }
    }
    for (int i = 0; i < dataLines; i++)
      PortMap.putAll(GetNetMap("DataOut"+i, true, ComponentInfo, DATAOUT[i], Reporter, HDLType, Nets));
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "memory";
  }

  @Override
  public String GetType(int TypeNr) {
    if (TypeNr == MemArrayId) return "MEMORY_ARRAY";
    else return "";
  }

  @Override
  public SortedSet<String> GetTypeDefinitions(Netlist TheNetlist, AttributeSet attrs, String HDLType) {
    SortedSet<String> MyTypes = new TreeSet<String>();
    if (HDLType.equals(Settings.VHDL)) {
      int NrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
      int NrOfAddressLines = attrs.getValue(Mem.ADDR_ATTR).getWidth();
      int RamEntries = (1 << NrOfAddressLines);
      MyTypes.add("TYPE MEMORY_ARRAY"
          + " IS ARRAY (" + (RamEntries-1) + " DOWNTO 0)"
          + " OF std_logic_vector(" + (NrOfBits-1) + " DOWNTO 0)");
    }
    return MyTypes;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs, char Vendor) {
    Object busVal = attrs.getValue(RamAttributes.ATTR_DBUS);
    boolean separate = busVal == null ? true : busVal
        .equals(RamAttributes.BUS_SEP);
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    boolean asynch = trigger.equals(StdAttr.TRIG_HIGH)
        || trigger.equals(StdAttr.TRIG_LOW);
    return HDLType.equals(Settings.VHDL) && separate && !asynch;
  }
}
