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

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;

public abstract class HDLSupport {

  public static class HDLCTX { // fixme - temporary hack?
    public final String lang;
    public final FPGAReport err;
    public final Netlist nets;
    public final AttributeSet attrs;
    public final char vendor;
    public HDLCTX(String lang, FPGAReport err, Netlist nets, AttributeSet attrs, char vendor) {
      this.lang = lang;;
      this.err = err;;
      this.nets = nets;;
      this.attrs = attrs;;
      this.vendor = vendor;;
    }
  }

  protected final String _projectName; // context - fixme
  protected final String _lang; // context - fixme
  protected final FPGAReport _err; // context - fixme
  protected final Netlist _nets; // context - fixme // signals of the circuit in
  // which this component is embeded. For CircuitHDLGeneratorComponent, this is
  // the signals for the *parent* circuit (or null, if it is the top-level
  // circuit), not the signals within this subcircuit.
  protected final AttributeSet _attrs; // context - fixme
  protected final char _vendor; // context - fixme
  public final boolean inlined;

  protected HDLSupport(HDLCTX ctx, boolean inlined) {
    this._projectName = ctx.err.getProjectName();
    this._lang = ctx.lang;
    this._err = ctx.err;
    this._nets = ctx.nets; // sometimes null, i.e. for top level circuit and also for quick checks
    this._attrs = ctx.attrs; // empty for Circuit, Ticker, maybe others?
    this._vendor = ctx.vendor;
    this.inlined = inlined;
  }

  // TODO - fixme these only serve to make code more impenetrable
	// public static final String NetName = "s_LOGISIM_NET_";
	// public static final String BusName = "s_LOGISIM_BUS_";
	// public static final String LocalInputBubbleBusname = "LOGISIM_INPUT_BUBBLES";
	// public static final String LocalOutputBubbleBusname = "LOGISIM_OUTPUT_BUBBLES";
	// public static final String LocalInOutBubbleBusname = "LOGISIM_INOUT_BUBBLES";
	// public static final String FPGAToplevelName = "LogisimToplevelShell";
	// public static final String InputBubblePortName = "LOGISIM_INPUT_BUBBLE_";
	// public static final String OutputBubblePortName = "LOGISIM_OUTPUT_BUBBLE_";
	// public static final String InOutBubblePortName = "LOGISIM_INOUTT_BUBBLE_";
	// public static final String BusToBitAddendum = "_bit_";
	// public static final String ClockTreeName = "LOGISIM_CLOCK_TREE_";
	// public static final String FPGAInputPinName = "FPGA_INPUT_PIN";
	// public static final String FPGAInOutPinName = "FPGA_INOUT_PIN";
	// public static final String FPGAOutputPinName = "FPGA_OUTPUT_PIN";

  // For non-inlined HDLGenerator classes.
  public boolean writeHDLFiles(String rootDir) { }
	protected void generateComponentDeclaration(Hdl out) { }
	protected void generateComponentInstance(Hdl out, long id, NetlistComponent comp) { }

  // For HDLInliner classes.
	protected void generateInlinedCode(Hdl out, NetlistComponent comp) { }


  // Helpers for subclasses...

  protected int stdWidth() {
    return _attrs.getValueOrElse(StdAttr.WIDTH, BitWidth.ONE).getWidth();
  }

  protected boolean isBus() {
    return stdWidth() > 1;
  }
}
