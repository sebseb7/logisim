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
package com.cburch.logisim.std.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map;
import java.io.File;

import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;

public class HexDigitHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "HEXDIGIT";
  }

  @Override
  public String GetSubDir() {
    return "io";
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs, char Vendor) {
    return HDLType.equals(Settings.VHDL);
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("Hex", 4);
    Inputs.put("DecimalPoint", 1);
    return Inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    Outputs.put("Segment_A", 1);
    Outputs.put("Segment_B", 1);
    Outputs.put("Segment_C", 1);
    Outputs.put("Segment_D", 1);
    Outputs.put("Segment_E", 1);
    Outputs.put("Segment_F", 1);
    Outputs.put("Segment_G", 1);
    Outputs.put("Segment_DP", 1);
    return Outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> InOuts = new TreeMap<String, Integer>();
    return InOuts;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets,
      NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();

    PortMap.putAll(GetNetMap("Hex", true, ComponentInfo, HexDigit.HEX, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("DecimalPoint", true, ComponentInfo, HexDigit.DP, Reporter, HDLType, Nets));

    String pin = LocalOutputBubbleBusname;
    int offset = ComponentInfo.GetLocalBubbleOutputStartId();
    PortMap.put("Segment_A", pin + "(" + (offset+0) + ")");
    PortMap.put("Segment_B", pin + "(" + (offset+1) + ")");
    PortMap.put("Segment_C", pin + "(" + (offset+2) + ")");
    PortMap.put("Segment_D", pin + "(" + (offset+3) + ")");
    PortMap.put("Segment_E", pin + "(" + (offset+4) + ")");
    PortMap.put("Segment_F", pin + "(" + (offset+5) + ")");
    PortMap.put("Segment_G", pin + "(" + (offset+6) + ")");
    PortMap.put("Segment_DP", pin + "(" + (offset+7) + ")");
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs, String HDLType) {
    SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
    return Regs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    return Wires;
  }

  // #2
  @Override
  public ArrayList<String> GetEntity(Netlist TheNetlist, AttributeSet attrs,
      String ComponentName, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.addAll(FileWriter.getGenerateRemark(ComponentName,
          Settings.VHDL, TheNetlist.projName()));
    Contents.addAll(FileWriter.getExtendedLibrary());
    Contents.add("ENTITY " + ComponentName + " IS");
    Contents.add("   PORT ( ");
    Contents.add("      Hex          : IN std_logic_vector (3 downto 0);");
    Contents.add("      DecimalPoint : IN std_logic;");
    Contents.add("      Segment_A    : OUT std_logic;");
    Contents.add("      Segment_B    : OUT std_logic;");
    Contents.add("      Segment_C    : OUT std_logic;");
    Contents.add("      Segment_D    : OUT std_logic;");
    Contents.add("      Segment_E    : OUT std_logic;");
    Contents.add("      Segment_F    : OUT std_logic;");
    Contents.add("      Segment_G    : OUT std_logic;");
    Contents.add("      Segment_DP   : OUT std_logic");
    Contents.add("   );");
    Contents.add("END " + ComponentName + ";");
    Contents.add("");
    return Contents;
  }

  @Override
  public ArrayList<String> GetArchitecture(Netlist TheNetlist,
      AttributeSet attrs, Map<String, File> MemInitFiles, String ComponentName,
      FPGAReport Reporter, String HDLType) {

    ArrayList<String> Contents = new ArrayList<String>();
    if (HDLType.equals(Settings.VHDL)) {
      Contents.addAll(FileWriter.getGenerateRemark(ComponentName,
            HDLType, TheNetlist.projName()));
      Contents.add("ARCHITECTURE PlatformIndependent OF " + ComponentName.toString() + " IS ");
      Contents.add("  signal s_output_value : std_logic_vector(6 downto 0);");
      Contents.add("begin");
      Contents.add("   Segment_A <= s_output_value(0);");
      Contents.add("   Segment_B <= s_output_value(1);");
      Contents.add("   Segment_C <= s_output_value(2);");
      Contents.add("   Segment_D <= s_output_value(3);");
      Contents.add("   Segment_E <= s_output_value(4);");
      Contents.add("   Segment_F <= s_output_value(5);");
      Contents.add("   Segment_G <= s_output_value(6);");
      Contents.add("   Segment_DP <= DecimalPoint;");
      Contents.add("");
      Contents.add("   MakeSegs : PROCESS( Hex )");
      Contents.add("   BEGIN");
      Contents.add("      CASE (Hex) IS");
      Contents.add("         WHEN \"0000\" => s_output_value <= \"0111111\";");
      Contents.add("         WHEN \"0001\" => s_output_value <= \"0000110\";");
      Contents.add("         WHEN \"0010\" => s_output_value <= \"1011011\";");
      Contents.add("         WHEN \"0011\" => s_output_value <= \"1001111\";");
      Contents.add("         WHEN \"0100\" => s_output_value <= \"1100110\";");
      Contents.add("         WHEN \"0101\" => s_output_value <= \"1101101\";");
      Contents.add("         WHEN \"0110\" => s_output_value <= \"1111101\";");
      Contents.add("         WHEN \"0111\" => s_output_value <= \"0000111\";");
      Contents.add("         WHEN \"1000\" => s_output_value <= \"1111111\";");
      Contents.add("         WHEN \"1001\" => s_output_value <= \"1101111\";");
      Contents.add("         WHEN \"1010\" => s_output_value <= \"1110111\";");
      Contents.add("         WHEN \"1011\" => s_output_value <= \"1111100\";");
      Contents.add("         WHEN \"1100\" => s_output_value <= \"0111001\";");
      Contents.add("         WHEN \"1101\" => s_output_value <= \"1011110\";");
      Contents.add("         WHEN \"1110\" => s_output_value <= \"1111001\";");
      Contents.add("         WHEN \"1111\" => s_output_value <= \"1110001\";");
      // Contents.add("         WHEN OTHERS => s_output_value <= \"-------\";");
      Contents.add("      END CASE;");
      Contents.add("   END PROCESS MakeSegs;");
      Contents.add("END PlatformIndependent;");
    }
    return Contents;
  }
}
