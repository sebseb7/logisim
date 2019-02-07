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
import java.util.TreeMap;
import java.util.Map;
import java.io.File;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.StdAttr;

public class PortHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  // todo: verilog support

  public PortHDLGeneratorFactory(String lang, FPGAReport err) {
    super(lang, err);
  }

  private class InOutMap {
    private int end, start, size, busNr, endNr;
    private int type;
    private String name;

    public InOutMap(int type, int end, int start, int busNr, int endNr) {
      this.type = type;
      this.end = end;
      this.start = start;
      this.size = (end - start) + 1;
      this.busNr = busNr;
      this.endNr = endNr;
      switch (type) {
      case ALWAYSINPUT:
      case TRISTATEINPUT_1:
      case TRISTATEINPUT_N:
        name = inBusName + busNr;
        break;
      case ENABLE:
        name = enBusName + busNr;
        break;
      case OUTPUT:
        name = outBusName + busNr;
        break;
      case BUS:
        name = inOutBusName + busNr;
        break;
      }
    }
  }

  private static final int ENABLE = 0;
  private static final int TRISTATEINPUT_1 = 1;
  private static final int TRISTATEINPUT_N = 2;
  private static final int ALWAYSINPUT = 3;
  private static final int OUTPUT = 4;
  private static final int BUS = 5;

  private static final String inBusName = "PIO_IN_BUS_";
  private static final String enBusName = "PIO_EN_BUS_";
  private static final String outBusName = "PIO_OUT_BUS_";
  private static final String inOutBusName = "PIO_INOUT_BUS_";

  ArrayList<InOutMap> getPorts(AttributeSet attrs) {
    // Note: PortIO.INPUT yields *output* from this entity (from off-chip
    // bus pins to circuit), while PortIO.OUTPUT yiels *input* to this
    // entity (from circuit to off-chip bus pins). We swap the direction
    // tags here to avoid having to swap them everywhere else in this file.
    String dir = attrs.getValue(PortIO.ATTR_DIR);
    Integer size = attrs.getValue(PortIO.ATTR_SIZE);
    ArrayList<InOutMap> ports = new ArrayList<InOutMap>();
    int endNr = 0;
    if (dir == PortIO.INOUT_1)
      ports.add(new InOutMap(ENABLE, 0, 0, 0, endNr++));
    int n = size;
    for (int busNr = 0; busNr < (size - 1)/32 + 1; busNr++) {
      int e = (n > 32 ? 32 : n);
      ports.add(new InOutMap(BUS, e - 1, 0, busNr, -1));
      if (dir == PortIO.INOUT_N) {
        ports.add(new InOutMap(ENABLE, e - 1, 0, busNr, endNr++));
        ports.add(new InOutMap(TRISTATEINPUT_N, e - 1, 0, busNr, endNr++));
      } else if (dir == PortIO.INOUT_1) {
        ports.add(new InOutMap(TRISTATEINPUT_1, e - 1, 0, busNr, endNr++));
      } else if (dir == PortIO.OUTPUT) {
        ports.add(new InOutMap(ALWAYSINPUT, e - 1, 0, busNr, endNr++));
      }
      n -= e;
    }
    n = size;
    for (int busNr = 0; busNr < (size - 1)/32 + 1; busNr++) {
      int e = (n > 32 ? 32 : n);
      if (dir == PortIO.INPUT || dir == PortIO.INOUT_1 || dir == PortIO.INOUT_N) {
        ports.add(new InOutMap(OUTPUT, e - 1, 0, busNr, endNr++));
      }
      n -= e;
    }
    return ports;
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

    ArrayList<InOutMap> ports = getPorts(attrs);
    InOutMap last = ports.get(ports.size() - 1);
    for (InOutMap io : ports) {
      String line = "          ";
      switch (io.type) {
      case ALWAYSINPUT:
      case TRISTATEINPUT_1:
      case TRISTATEINPUT_N:
      case ENABLE:
        line += io.name + "  : IN ";
        break;
      case OUTPUT:
        line += io.name + "  : OUT ";
        break;
      case BUS:
        line += io.name + "  : INOUT ";
        break;
      }
      if (io.size == 1)
        line += "std_logic";
      else
        line += "std_logic_vector (" + (io.size - 1) + " DOWNTO 0)";
      if (io == last)
        line += ")";
      line += ";";
      Contents.add(line);
    }
    Contents.add("END " + ComponentName + ";");
    Contents.add("");
    return Contents;
  }

  // #4
  @Override
  public ArrayList<String> GetArchitecture(/*Netlist TheNetlist,*/
      AttributeSet attrs, Map<String, File> MemInitFiles,
      String ComponentName /*, FPGAReport Reporter, String HDLType*/) {
    if (_nets == null) throw new IllegalStateException();
    ArrayList<String> Contents = new ArrayList<String>();
    if (_lang.equals(Settings.VHDL)) {
      Contents.addAll(FileWriter.getGenerateRemark(ComponentName, _lang, _nets.projName()));
      Contents.add("");
      Contents.add("ARCHITECTURE PlatformIndependent OF "
          + ComponentName.toString() + " IS ");
      Contents.add("");
      Contents.add("BEGIN");
      Contents.add("");
      for (InOutMap io : getPorts(attrs)) {
        String ioBus = inOutBusName + io.busNr;
        String ioBusAll = ioBus;
        if (io.size == 1)
          ioBusAll += "(" + io.end + ")";
        else
          ioBusAll += "(" + io.end + " DOWNTO " + io.start + ")";
        switch (io.type) {
        case OUTPUT:
          Contents.add("  " + io.name + " <= " + ioBusAll + ";");
          break;
        case ALWAYSINPUT:
          Contents.add("  " + ioBusAll + " <= " + io.name + ";");
          break;
        case TRISTATEINPUT_1:
        case TRISTATEINPUT_N:
          for (int i = io.end; i >= io.start; i--) {
            String bus = ioBus + "(" + i + ")";
            String en;
            if (io.type == TRISTATEINPUT_N)
              en = enBusName + io.busNr + "(" + i + ")";
            else
              en = enBusName + 0;
            String in = io.name + "(" + i + ")";
            Contents.add("  " + bus + " <= " + in + " when " + en + " = '1' else 'Z';");
          }
          break;
        case BUS:
        case ENABLE:
          // nothing
          break;
        }
      }
      Contents.add("");
      Contents.add("END PlatformIndependent;");
    }
    return Contents;
  }

  // #5
  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    for (InOutMap io : getPorts(attrs)) {
      if (io.type == ALWAYSINPUT || io.type == TRISTATEINPUT_1 || io.type == TRISTATEINPUT_N || io.type == ENABLE)
        Inputs.put(io.name, io.size);
    }
    return Inputs;
  }


  // #6
  @Override
  public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> InOuts = new TreeMap<String, Integer>();
    for (InOutMap io : getPorts(attrs)) {
      if (io.type == BUS)
        InOuts.put(io.name, io.size);
    }
    return InOuts;
  }

  // #7
  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
      AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    for (InOutMap io : getPorts(attrs)) {
      if (io.type == OUTPUT)
        Outputs.put(io.name, io.size);
    }
    return Outputs;
  }


  // #8,10,11,13
  @Override
  public String getComponentStringIdentifier() {
    return "PORTIO";
  }

  // #9,12
  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets,
      NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
    String ComponentName = "PORTIO_" + ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.LABEL);

    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    for (InOutMap io : getPorts(ComponentInfo.GetComponent().getAttributeSet())) {
      switch (io.type) {
      case ALWAYSINPUT:
        PortMap.putAll(GetNetMap(io.name, false, ComponentInfo, io.endNr, Reporter, HDLType, Nets));
        break;
      case TRISTATEINPUT_1:
      case TRISTATEINPUT_N:
        PortMap.putAll(GetNetMap(io.name, true, ComponentInfo, io.endNr, Reporter, HDLType, Nets));
        break;
      case ENABLE:
        PortMap.putAll(GetNetMap(io.name, true, ComponentInfo, io.endNr, Reporter, HDLType, Nets));
        break;
      case OUTPUT:
        PortMap.putAll(GetNetMap(io.name, false, ComponentInfo, io.endNr, Reporter, HDLType, Nets));
        break;
      case BUS:
        String pin = LocalInOutBubbleBusname;
        int offset = ComponentInfo.GetLocalBubbleInOutStartId();
        int start = offset + io.start + io.busNr * 32;
        int end = offset + io.end + io.busNr * 32;
        if (io.size == 1)
          PortMap.put(io.name, pin + "(" + end + ")");
        else
          PortMap.put(io.name, pin + "(" + end + " DOWNTO " + start + ")");
        break;
      }
    }
    return PortMap;
  }

  // #1,3
  @Override
  public String GetSubDir() {
    return "io";
  }

}
