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
package com.cburch.logisim.std.io;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.Map;
import java.io.File;

import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;

public class HexDigitHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public HexDigitHDLGeneratorFactory(String lang, FPGAReport err) {
    super(lang, err);
  }

  @Override
  public String getComponentStringIdentifier() { return "HEXDIGIT"; }

  @Override
  public String GetSubDir() { return "io"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Hex", 4);
    list.put("DecimalPoint", 1);
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Segment_A", 1);
    list.put("Segment_B", 1);
    list.put("Segment_C", 1);
    list.put("Segment_D", 1);
    list.put("Segment_E", 1);
    list.put("Segment_F", 1);
    list.put("Segment_G", 1);
    list.put("Segment_DP", 1);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    list.putAll(GetNetMap("Hex", true, info, HexDigit.HEX, err, lang, nets));
    list.putAll(GetNetMap("DecimalPoint", true, info, HexDigit.DP, err, lang, nets));
    String pin = LocalOutputBubbleBusname;
    int b = info.GetLocalBubbleOutputStartId();
    list.put("Segment_A", pin + "(" + (b+0) + ")");
    list.put("Segment_B", pin + "(" + (b+1) + ")");
    list.put("Segment_C", pin + "(" + (b+2) + ")");
    list.put("Segment_D", pin + "(" + (b+3) + ")");
    list.put("Segment_E", pin + "(" + (b+4) + ")");
    list.put("Segment_F", pin + "(" + (b+5) + ")");
    list.put("Segment_G", pin + "(" + (b+6) + ")");
    list.put("Segment_DP", pin + "(" + (b+7) + ")");
  }

  @Override
  public ArrayList<String> GetEntity(Netlist nets, AttributeSet attrs,
      String ComponentName, FPGAReport err, String lang) {
    // fixme: is this necessary? or maybe better?
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.addAll(FileWriter.getGenerateRemark(ComponentName, "VHDL", nets.projName()));
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
  public ArrayList<String> GetArchitecture(Netlist nets,
      AttributeSet attrs, Map<String, File> MemInitFiles, String ComponentName,
      FPGAReport err, String lang) {
    // fixme: is this necessary? or maybe better?
    ArrayList<String> Contents = new ArrayList<String>();
    if (lang.equals("VHDL")) {
      Contents.addAll(FileWriter.getGenerateRemark(ComponentName, lang, nets.projName()));
      Contents.add("ARCHITECTURE PlatformIndependent OF " + ComponentName + " IS ");
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
      Contents.add("      END CASE;");
      Contents.add("   END PROCESS MakeSegs;");
      Contents.add("END PlatformIndependent;");
    }
    return Contents;
  }
}
