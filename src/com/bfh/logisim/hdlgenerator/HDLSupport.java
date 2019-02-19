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

import com.bfh.logisim.netlist.Netlist;
import com.bfh.logisim.netlist.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import static com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer.IOComponentTypes;

public abstract class HDLSupport {

  // Helper used in constructors to facilitate adding/removing params.
  public static class HDLCTX {
    public final String lang;
    public final FPGAReport err;
    public final Netlist nets;
    public final AttributeSet attrs;
    public final char vendor;
    public HDLCTX(String lang, FPGAReport err, Netlist nets, AttributeSet attrs, char vendor) {
      this.lang = lang;
      this.err = err;
      this.nets = nets;
      this.attrs = attrs != null ? attrs : AttriibuteSets.EMPTY;
      this.vendor = vendor;
    }
  }

  protected final String _projectName; // context - fixme
  // Name for HDL module (i.e. base name of HDL file for generated components,
  // and the GUI display name for both generated and inlined components). Must
  // be unique and distinct for each variation of generated HDL, e.g. "BitAdder"
  // (for a 1-bit HDL version using std_logic) versus "BusAdder" (for a
  // parameterized N-bit version using std_logic_vector). In some cases, this is
  // even globally unique (distinct per-circuit and per-instance), when the VHDL
  // essentially can't be shared between instances like for PLA, Rom, and
  // Non-Volatile Ram components.
  protected final String hdlComponentName; // context - fixme
  protected final String _lang; // context - fixme
  protected final FPGAReport _err; // context - fixme
  protected final Netlist _nets; // context - fixme // signals of the circuit in
  // which this component is embeded. For CircuitHDLGeneratorComponent, this is
  // the signals for the *parent* circuit (or null, if it is the top-level
  // circuit), not the signals within this subcircuit.
  protected final AttributeSet _attrs; // context - fixme
  protected final char _vendor; // context - fixme
  public final boolean inlined;
  protected final Hdl _hdl;

  protected HDLSupport(HDLCTX ctx, String hdlComponentNameTemplate, boolean inlined) {
    this._projectName = ctx.err.getProjectName();
    this._lang = ctx.lang;
    this._err = ctx.err;
    this._nets = ctx.nets; // sometimes null, i.e. for top level circuit and also for quick checks
    this._attrs = ctx.attrs; // empty for Circuit, Ticker, maybe others?
    this._vendor = ctx.vendor;
    this.inlined = inlined;
    this.hdlComponentName = deriveHDLNameWithinCircuit(hdlComponentNameTemplate,
        _attrs, _nets == null ? null : _nets.getCircuitName());
    this._hdl = new Hdl(_lang, _err);
  }

// 	public static final String FPGAToplevelName = "LogisimToplevelShell"; -- TopLevelHDLGenerator.HDL_NAME

  // TODO - fixme these only serve to make code more impenetrable
	
  // public static final String NetName = "s_LOGISIM_NET_";
	// public static final String BusName = "s_LOGISIM_BUS_";
	// public static final String LocalInputBubbleBusname = "LOGISIM_INPUT_BUBBLES"; LOGISIM_HIDDEN_FPGA_INPUT
	// public static final String LocalOutputBubbleBusname = "LOGISIM_OUTPUT_BUBBLES"; LOGISIM_HIDDEN_FPGA_INOUT
	// public static final String LocalInOutBubbleBusname = "LOGISIM_INOUT_BUBBLES"; LOGISIM_HIDDEN_FPGA_OUTPUT
	// public static final String InputBubblePortName = "LOGISIM_INPUT_BUBBLE_";
	// public static final String OutputBubblePortName = "LOGISIM_OUTPUT_BUBBLE_";
	// public static final String InOutBubblePortName = "LOGISIM_INOUTT_BUBBLE_";
	// public static final String BusToBitAddendum = "_bit_";
	// public static final String ClockTreeName = "LOGISIM_CLOCK_TREE_";

  // Return the component name.
  public final String getComponentName() { return hdlComponentName; }

  // For non-inlined HDLGenerator classes.
  public boolean writeHDLFiles(String rootDir) { }
	protected void generateComponentDeclaration(Hdl out) { }
	protected void generateComponentInstance(Hdl out, long id, NetlistComponent comp) { }
	protected String getInstanceNamePrefix() { }

  // For HDLInliner classes.
	protected void generateInlinedCode(Hdl out, NetlistComponent comp) { }


  // Helpers for subclasses...

  protected int stdWidth() {
    return _attrs.getValueOrElse(StdAttr.WIDTH, BitWidth.ONE).getWidth();
  }

  protected boolean isBus() {
    return stdWidth() > 1;
  }

  protected boolean edgeTriggered() {
    return _attrs.containsAttribute(StdAttr.EDGE_TRIGGER)
        || (_attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING)
        || (_attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING);
  }

  // Return a suitable HDL name for this component, e.g. "BitAdder" or
  // "BusAdder". This becomes the name of the vhdl/verilog file, and becomes
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
  // for getComponentName() to simply include in the name as is done for other
  // attributes. Instead, we require each ROM and non-volatile RAM to have a
  // non-zero label unique within the circuit, then getComponentName() computes a
  // name as a function of the circuit name (which is globally unique) and the
  // label.
  private static String deriveHDLNameWithinCircuit(String nameTemplate,
      AttributeSet attrs, String circuitName) {
    String s = hdlComponentName;
    int w = stdWidth();
    // fixme: this will happen for temp-instances with no netlist
    if (s.contains("${CIRCUIT}") && circuitName == null)
      throw new IllegalArgumentException("%s can't appear in top-level circuit");
    if (s.contains("${CIRCUIT}"))
        s = s.replace("${CIRCUIT}", circuitName);
    if (s.contains("${WIDTH}"))
        s = s.replace("${WIDTH}", ""+w);
    if (s.contains("${BUS}"))
        s = s.replace("${BUS}", w == 1 ? "Bit" : "Bus");
    if (s.contains("${TRIGGER}")) {
      if (edgeTriggered())
        s = s.replace("${TRIGGER}", "EdgeTriggered");
      else if (attrs.containsAttribute(StdAttr.TRIGGER))
        s = s.replace("${TRIGGER}", "LevelSensitive");
      else
        s = s.replace("${TRIGGER}", "Asynchronous");
    }
    if (s.contains("${LABEL}")) {
      String label = attrs.getValueOrElse(StdAttr.LABEL, "");
      if (label.isEmpty()) {
        if (_err != null)
          _err.AddSevereWarning("Missing label for %s within circuit \"%s\".",
              hdlComponentName, circuitName);
        label = "ANONYMOUS";
      }
      s = s.replace("${LABEL}", label);
    }
    return CorrectLabel.getCorrectLabel(s);
  }

  // For some components, the name returned by getComponentName() depends on a
  // (preferable friendly) label that is unique within the circuit, because it
  // shows up in the component-mapping GUI dialog where the user assigns FPGA
  // resources. This is the case for Pin, PortIO, LED, BUtton, and similar
  // components. These components should include "${LABEL}" in their name. ROM
  // and volatile RAM components also do the same (see note below). Also,
  // Subcircuit and VhdlEntity need labels for the same reason, and override
  // this method to return true.
  public boolean RequiresNonZeroLabel() {
    return hdlComponentName.contains("${LABEL}");
  }

}
