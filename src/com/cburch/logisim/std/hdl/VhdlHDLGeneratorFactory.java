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

package com.cburch.logisim.std.hdl;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.Port;

public class VhdlHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public VhdlHDLGeneratorFactory(String lang, FPGAReport err) {
    super(lang, err);
  }

  @Override
  public String getComponentStringIdentifier() { return "VHDL"; }

  @Override
  public String GetSubDir() { return "circuit"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    for (VhdlParser.PortDescription p : content(attrs).getPorts())
      if (p.getType() == Port.INPUT)
        list.put(p.getName(), p.getWidth().getWidth());
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    for (VhdlParser.PortDescription p : content(attrs).getPorts())
      if (p.getType() == Port.OUTPUT)
        list.put(p.getName(), p.getWidth().getWidth());
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    int id = -1;
    for (VhdlContent.Generic g : content(attrs).getGenerics())
      list.put(id--, g.getName());
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    VhdlContent content = content(attrs);
    for (Attribute<Integer> a : content.getGenericAttributes()) {
      VhdlEntityAttributes.VhdlGenericAttribute va = (VhdlEntityAttributes.VhdlGenericAttribute)a;
      VhdlContent.Generic g = va.getGeneric();
      Integer v = attrs.getValue(a);
      list.put(g.getName(), v != null ? v : g.getDefaultValue());
    }
  }
  
  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int i = 0;
    for (VhdlParser.PortDescription p : content(attrs).getPorts())
      list.putAll(GetNetMap(p.getName(), true, info, i++, err, lang, nets));
  }

  @Override
	public ArrayList<String> GetArchitecture(Netlist nets,
			AttributeSet attrs, Map<String, File> memInitFiles,
			String name, FPGAReport err, String lang) {
		ArrayList<String> list = new ArrayList<>();
    list.addAll(FileWriter.getGenerateRemark(name, lang, nets.projName()));
    VhdlContent content = content(attrs);
    list.add(content.getLibraries());
    list.add(content.getArchitecture());
    return list;
  }

  private VhdlContent content(AttributeSet attrs) {
    return ((VhdlEntityAttributes) attrs).getContent();
  }
}
