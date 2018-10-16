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

package com.cburch.logisim.std.hdl;

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
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.instance.Port;

public class VhdlHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetArchitecture(Netlist TheNetlist,
      AttributeSet attrs, Map<String, File> MemInitFiles, String ComponentName, FPGAReport Reporter,
      String HDLType) {
    ArrayList<String> contents = new ArrayList<String>();
    contents.addAll(FileWriter.getGenerateRemark(ComponentName, HDLType,
          TheNetlist.projName()));

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    contents.add(content.getLibraries());
    contents.add(content.getArchitecture());

    return contents;
  }

  public SortedMap<String, Integer> GetParameterMap(Netlist Nets,
      NetlistComponent ComponentInfo, FPGAReport Reporter) {
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    for (Attribute<Integer> a : content.getGenericAttributes()) {
      VhdlEntityAttributes.VhdlGenericAttribute va = (VhdlEntityAttributes.VhdlGenericAttribute)a;
      VhdlContent.Generic g = va.getGeneric();
      Integer v = attrs.getValue(a);
      if (v != null) {
        ParameterMap.put(g.getName(), v);
        /*
           } else if (!g.hasDefaultValue()) {
           FPGAReport.AddFatalError("VHDL entity instance " +
           attrs.getValue(VhdlEntity.NAME_ATTR) +
           " is missing parameter " +
           g.getName() +", and "
           + content.getName() + " does not provide " +
           " a default value for this parameter.");
           */
      } else {
        ParameterMap.put(g.getName(), g.getDefaultValue());
      }
    }
    return ParameterMap;
  }

  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    int i = -1;
    for (VhdlContent.Generic g : content.getGenerics()) {
      Parameters.put(i--, g.getName());
    }
    return Parameters;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "VHDL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
      AttributeSet attrs) {
    SortedMap<String, Integer> inputs = new TreeMap<String, Integer>();

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    for (VhdlParser.PortDescription p : content.getPorts()) {
      if (p.getType() == Port.INPUT)
        inputs.put(p.getName(), p.getWidth().getWidth());
    }

    return inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
      AttributeSet attrs) {
    SortedMap<String, Integer> outputs = new TreeMap<String, Integer>();

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    for (VhdlParser.PortDescription p : content.getPorts()) {
      if (p.getType() == Port.OUTPUT)
        outputs.put(p.getName(), p.getWidth().getWidth());
    }

    return outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets,
      NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();

    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();

    int i = 0;
    for (VhdlParser.PortDescription p : content.getPorts()) {
      PortMap.putAll(GetNetMap(p.getName(), true,
            ComponentInfo, i++, Reporter, HDLType, Nets));
    }

    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "circuit";
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs,
      char Vendor) {
    return HDLType.equals(Settings.VHDL);
  }

}
