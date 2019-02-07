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

package com.bfh.logisim.hdlgenerator;

import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.io.File;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.cburch.logisim.data.AttributeSet;

public abstract class HDLGeneratorFactory {

  protected final String _lang; // context - fixme
  protected final FPGAReport _err; // context - fixme
  protected /* final */ Netlist _nets; // context - fixme

  protected HDLGeneratorFactory(String lang, FPGAReport err, Netlist nets) {
    this._lang = lang;
    this._err = err;
    this._nets = nets;
  }

	public static final int PallignmentSize = 26;
	public static final int SallignmentSize = 35;
	public static final String NetName = "s_LOGISIM_NET_";
	public static final String BusName = "s_LOGISIM_BUS_";
	public static final String LocalInputBubbleBusname = "LOGISIM_INPUT_BUBBLES";
	public static final String LocalOutputBubbleBusname = "LOGISIM_OUTPUT_BUBBLES";
	public static final String LocalInOutBubbleBusname = "LOGISIM_INOUT_BUBBLES";
	public static final String FPGAToplevelName = "LogisimToplevelShell";
	public static final String InputBubblePortName = "LOGISIM_INPUT_BUBBLE_";
	public static final String OutputBubblePortName = "LOGISIM_OUTPUT_BUBBLE_";
	public static final String InOutBubblePortName = "LOGISIM_INOUTT_BUBBLE_";
	public static final String BusToBitAddendum = "_bit_";
	public static final String ClockTreeName = "LOGISIM_CLOCK_TREE_";
	public static final String FPGAInputPinName = "FPGA_INPUT_PIN";
	public static final String FPGAInOutPinName = "FPGA_INOUT_PIN";
	public static final String FPGAOutputPinName = "FPGA_OUTPUT_PIN";

	public abstract ArrayList<String> GetArchitecture(/*Netlist TheNetlist,*/
			AttributeSet attrs, Map<String, File> MemInitFiles,
			String ComponentName /*, FPGAReport Reporter, String HDLType */);

	public abstract ArrayList<String> GetComponentInstantiation(Netlist TheNetlist,
			AttributeSet attrs, String ComponentName, String HDLType);

	public abstract ArrayList<String> GetComponentMap(Netlist Nets, Long ComponentId,
			NetlistComponent ComponentInfo, FPGAReport Reporter,
			String CircuitName, String HDLType);

	public abstract String getComponentStringIdentifier();
	
	public abstract Map<String, ArrayList<String>> GetMemInitData(AttributeSet attrs);

	public abstract ArrayList<String> GetEntity(Netlist TheNetlist, AttributeSet attrs,
			String ComponentName, FPGAReport Reporter, String HDLType);

	public abstract ArrayList<String> GetInlinedCode(Netlist Nets, Long ComponentId,
			NetlistComponent ComponentInfo, FPGAReport Reporter,
			String CircuitName, String HDLType);

	public abstract ArrayList<String> GetInlinedCode(String HDLType,
			ArrayList<String> ComponentIdentifier, FPGAReport Reporter,
			MappableResourcesContainer MapInfo);

	public abstract String GetRelativeDirectory(String HDLType);

	public abstract boolean IsOnlyInlined(String HDLType);
}
