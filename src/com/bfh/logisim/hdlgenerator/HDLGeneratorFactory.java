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
import java.util.Map;
import java.io.File;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;

public abstract class HDLGeneratorFactory {

  protected final String _lang; // context - fixme
  protected final FPGAReport _err; // context - fixme
  protected final Netlist _nets; // context - fixme
  protected final AttributeSet _attrs; // context - fixme
  protected final char _vendor; // context - fixme

  // these next two are not used for inline components
  protected final String _hdlComponentName; // Must be unique, e.g. "ADDER2C", or "ONE_BIT_FULL_ADDER"
  protected final String _hdlInstanceNamePrefix; // e.g. "Adder", which becomes "Adder_1", "Adder_2", etc.

  protected HDLGeneratorFactory(String lang, FPGAReport err, Netlist nets, AttributeSet attrs, char vendor,
      String hdlComponentName, String hdlInstanceNamePrefix) {
    this._lang = lang;
    this._err = err;
    this._nets = nets; // fixme - sometimes null -- maybe null for top level? also for quick checks.
    this._attrs = attrs; // empty for Circuit, Ticker, maybe others?
    this._vendor = vendor;
    this._hdlComponentName = CorrectLabel.getCorrectLabel(hdlComponentName);
    this._hdlInstanceNamePrefix = CorrectLabel.getCorrectLabel(hdlInstanceNamePrefix);
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
			/*AttributeSet attrs,*/ Map<String, File> MemInitFiles,
			String ComponentName /*, FPGAReport Reporter, String HDLType */);

	public abstract ArrayList<String> GetComponentInstantiation(/*Netlist TheNetlist,*/
			/*AttributeSet attrs,*/ String ComponentName /*, String HDLType*/);

	public abstract ArrayList<String> GetComponentMap(/*Netlist Nets,*/ Long ComponentId,
			NetlistComponent ComponentInfo/* , FPGAReport Reporter,*/
			/* String CircuitName , String HDLType*/, String ContainingCircuitName);

  // For some components, getHDLName() only works reliably if the component has
  // a (preferable friendly) name that is unique within the circuit, because it
  // shows up in the component-mapping GUI dialog where the user assigns FPGA
  // resources. This is the case for Pin, PortIO, LED, BUtton, and similar
  // components. These components should include "${LABEL}" in their name. ROM
  // and volatile RAM components also do the same (see note below). Also,
  // Subcircuit and VhdlEntity need labels for the same reason, and override
  // this method to return true.
  public boolean RequiresNonZeroLabel() {
    return _hdlComponentName.contains("${LABEL}");
  }

  // Return a suitable HDL name for this component, e.g. "ADDER2C" or
  // "ADDER2C_BUS". This becomes the name of the vhdl/verilog file, and becomes
  // the name of the vhdl/verilog entity for this component. The name must be
  // unique for each different generated vhdl/verilog code. If any attributes
  // are used to customize the generated vhdl/verilog code, that attribute must
  // also be used as part of the HDL name. 
  //
  // As a convenience, some simple string replacements are made:
  //   ${CIRCUIT} - replaced with name of circuit containing component
  //   ${LABEL}   - replaced with StdAttr.LABEL
  //   ${WIDTH}   - replaced with StdAttr.WIDTH
  //   ${BUS}     - replaced with "Bit" (StdAttr.WIDTH == 1) or "Bus" (otherwise)
  //   ${TRIGGER} - replaced with "LevelSensitive", "EdgeTriggered", or "Asynchronous"
  //                depending on StdAttr.TRIGGER and/or StdAttr.EDGE_TRIGGER.
  //
  // Note: For ROM and non-volatile RAM components, the generated HDL code
  // depends on the contents of the memory, which is too large of an attribute
  // for getHDLName() to simply include in the name as is done for other
  // attributes. Instead, we require each ROM and non-volatile RAM to have a
  // non-zero label unique within the circuit, then we getHDLName() computes a
  // name as a function of the circuit name (which is globally unique) and the
  // label.
  public final String getHDLNameWithinCircuit(String circuitName) { // was: getHDLName();
    String s = _hdlComponentName;
    int w = _attrs.getValueOrElse(StdAttr.WIDTH, BitWidth.ONE).getWidth();
    if (s.contains("${CIRCUIT}"))
        s = s.replace("${CIRCUIT}", circuitName);
    if (s.contains("${WIDTH}"))
        s = s.replace("${WIDTH}", ""+w);
    if (s.contains("${BUS}"))
        s = s.replace("${BUS}", w == 1 ? "Bit" : "Bus");
    if (s.contains("${TRIGGER}")) {
      if (_attrs.containsAttribute(StdAttr.EDGE_TRIGGER)
          || _attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
          || _attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING)
        s = s.replace("${TRIGGER}", "EdgeTriggered");
      else if (_attrs.containsAttribute(StdAttr.TRIGGER))
        s = s.replace("${TRIGGER}", "LevelSensitive");
      else
        s = s.replace("${TRIGGER}", "Asynchronous");
    }
    if (s.contains("${LABEL}")) {
      String label = _attrs.getValueOrElse(StdAttr.LABEL, "");
      if (label.isEmpty()) {
        if (_err != null)
          _err.AddSevereWarning("Missing label for %s within circuit \"%s\".",
              _hdlComponentName, circuitName);
        label = "ANONYMOUS";
      }
      s = s.replace("${LABEL}", label);
    }
    return CorrectLabel.getCorrectLabel(s);
  }

  // Return a suitable stem for naming instances of this component within a
  // circuit, so we can form names like "ADDER_1", "ADDER_2", etc., for
  // instances of ADDER2C. These need not be unique: the
  // CircuitHDLGeneratorFactory will add suffixes to ensure all the instance
  // names within a circuit are unique.
	public final String getComponentStringIdentifier() { return _hdlInstanceNamePrefix; }
	
	public abstract Map<String, ArrayList<String>> GetMemInitData(/*AttributeSet attrs*/);

	public abstract ArrayList<String> GetEntity(/*Netlist TheNetlist,*/ /*AttributeSet attrs,*/
			String ComponentName /*, FPGAReport Reporter, String HDLType*/);

  // CircuitHDLGeneratorFactory calls this for NormalComponents, when nets
  // defined, if IsOnlyInlined returns true.
	public abstract ArrayList<String> GetInlinedCode3(/*Netlist Nets, Long ComponentId,*/
			NetlistComponent ComponentInfo/*, FPGAReport Reporter
			String CircuitName, String HDLType*/);

  // ToplevelHDLGeneratorFactory calls this for components that are not Pin,
  // PortIO, Tty, or Keyboard.
	public abstract ArrayList<String> GetInlinedCode2(/*String HDLType,*/
			ArrayList<String> ComponentIdentifier, /*FPGAReport Reporter,*/
			MappableResourcesContainer MapInfo);

	public abstract String GetRelativeDirectory(/*String HDLType*/);

	public boolean IsOnlyInlined(/*String HDLType*/) { return false; }
}
