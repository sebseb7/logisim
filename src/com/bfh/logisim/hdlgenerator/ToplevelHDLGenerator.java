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

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.library.DynamicClock;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGenerator;
import com.cburch.logisim.std.wiring.Pin;

public class ToplevelHDLGenerator extends HDLGenerator {

  // Name of the top-level HDL module.
  public static final String HDL_NAME = "LogisimToplevelShell";

  // Names for top-level ioResource ports.
  // public static final String FPGA_INPUT_PIN = "FPGA_INPUT_PIN_"; // %d
  // public static final String FPGA_INOUT_PIN = "FPGA_INOUT_PIN_"; // %d
  // public static final String FPGA_OUTPUT_PIN = "FPGA_OUTPUT_PIN_"; // %d
	// public static final String FPGAInputPinName = "FPGA_INPUT_PIN";
	// public static final String FPGAInOutPinName = "FPGA_INOUT_PIN";
	// public static final String FPGAOutputPinName = "FPGA_OUTPUT_PIN";

	private long fpgaClockFreq;
	private int tickerPeriod; // 0:"use fpga clock", -1:"dynamic", >0:"divided clock"
	private Circuit circUnderTest;
	private MappableResourcesContainer ioResources;

	public ToplevelHDLGenerator(HDLCTX ctx, long fpgaClockFreq, int tickerPeriod,
			Circuit designUnderTest, MappableResourcesContainer ioResources) {
    super(ctx, "toplevel", HDL_NAME, "i_Toplevel");

		this.fpgaClockFreq = fpgaClockFreq;
		this.tickerPeriod = tickerPeriod;
		this.circUnderTest = circUnderTest;
		this.ioResources = ioResources;

    // raw oscillator input
    if (_nets.NumberOfClockTrees() > 0)
      inPorts.add(new PortInfo(TickHDLGenerator.FPGA_CLK_NET, 1, -1, null));

    // io resources
		for (int i = 0; i < ioResources.GetNrOfToplevelInputPins(); i++)
      inPorts.add(new PortInfo("FPGA_INPUT_PIN_"+i, 1, -1, null));
		for (int i = 0; i < ioResources.GetNrOfToplevelInOutPins(); i++)
      inOutPorts.add(new PortInfo("FPGA_INOUT_PIN_"+i, 1, -1, null));
		for (int i = 0; i < ioResources.GetNrOfToplevelOutputPins(); i++)
      outPorts.add(new PortInfo("FPGA_OUTPUT_PIN_"+i, 1, -1, null));

    // internal clock networks
    int nClk = Nets.NumberOfClockTrees();
		if (nClk > 0) {
      wires.add(new WireInfo(TickHDLGenerator.FPGA_TICK_NET, 1));
			for (int i = 0; i < nClk; i++)
				wires.add(new WireInfo("s_"+ClockHDLGenerator.CLK_TREE_NET+i,
              ClockHDLGenerator.CLK_TREE_WIDTH));
		}

    // wires for hidden ports for circuit design under test
    addWireVector("s_LOGISIM_HIDDEN_FPGA_INPUT", Nets.NumberOfInputBubbles());
		// addWireVector("s_LOGISIM_HIDDEN_FPGA_INOUT", Nets.NumberOfInOutBubbles()); // not needed
    addWireVector("s_LOGISIM_HIDDEN_FPGA_OUTPUT", Nets.NumberOfOutputBubbles());

    // wires for normal ports for circuit design under test
    ArrayList<NetlistComponent> ports = new ArrayList<>();
    ports.addAll(Nets.GetInputPorts());
    ports.addAll(Nets.GetInOutPorts());
    ports.addAll(Nets.GetOutputPorts());
    for (NetlistComponent port : ports) {
      Component comp = port.getComponent();
      String name = comp.getAttributeSet().getValue(StdAttr.LABEL);
      int w = comp.getEnd(0).getWidth().getWidth();
      wires.add(new WireInfo("s_"+CorrectLabel.getCorrectLabel(name), w));
    }

    // wires for dynamic clock
    NetlistComponent dynClock = Nets.GetDynamicClock();
    if (dynClock != null) {
      int w = dynClock.GetComponent().getAttributeSet().getValue(DynamicClock.WIDTH_ATTR).getWidth();
      wires.add(new WireInfo("s_LOGISIM_DYNAMIC_CLOCK", w));
    }
	}

  private void addWireVector(String busname, int w) {
    // Note: the parens around "(1)" are a hack to force vector creation.
    // See: HDLGenerator and Hdl.typeForWidth().
    if (w > 0)
      wires.add(new WireInfo(busname, w == 1 ? "(1)" : ""+w));
  }

	@Override
	protected void generateComponentDeclaration(Hdl out) {
    _nets.SetRawFPGAClock(tickerPeriod == 0);
    _nets.SetRawFPGAClockFreq(fpgaClockFreq);

		if (_nets.NumberOfClockTrees() > 0) {
      HDLCTX ctx = new HDLCTX(_lang, _err, _nets /*wrong nets?*/, null /*attrs*/, _vendor);
			TickHDLGenerator ticker = new TickHDLGenerator(ctx, fpgaClockFreq, tickerPeriod);
      ticker.generateComponentDeclaration(out);
      // Clock components are lifted to the top level, so declare one here.
			ClockHDLGenerator clk = new ClockHDLGenerator(ctx);
      clk.generateComponentDeclaration(out);
		}

		CircuitHDLGenerator g = new CircuitHDLGenerator(ctx, circUnderTest);
    g.generateComponentDeclaration(out);
	}

	@Override
  protected void generateBehavior(Hdl out) {

    out.comment("signal adaptions for pin components");
		for (ArrayList<String> path : ioResources.GetComponents()) {
      Component pin = ioResources.GetComponent(path).GetComponent();
      if (!(pin.getFactory() instanceof Pin))
        continue;
      generateInlinedCodeForPinSignals(out, path, pin);
		}
    out.stmt();

    out.comment("signal adaptions for hidden signals");
		for (ArrayList<String> path : ioResources.GetComponents())
      generateInlinedCodeForHiddenSignals(out, path);
    out.stmt();

		if (nets.NumberOfClockTrees() > 0) {
      out.comment("clock signal distribution");
      HDLCTX ctx = new HDLCTX(_lang, _err, _nets /*wrong nets?*/, null /*attrs*/, _vendor);
			TickHDLGenerator ticker = new TickHDLGenerator(ctx, fpgaClockFreq, tickerPeriod);
      ticker.generateComponentInstance(out, 0L /*id*/, null /*comp*/);

			long id = 0;
			for (Component clk : _nets.GetAllClockSources()) {
        HDLGenerator g = clk.getFactory().getHDLGenerator(_lang, _err,
            _nets /*wrong nets?*/, clk.getAttributeSet(), _vendor);
        g.generateComponentInstance(out, id++, new NetlistComponent(clk));
      }
      out.stmt();
		}

    out.comment("connections for circuit design under test");

		CircuitHDLGenerator g = new CircuitHDLGenerator(_lang, _err, _nets, _vendor, circUnderTest);
    g.generateComponentInstance(out, 0L /*id*/, null /*comp*/);
	}

  private static void generateInlinedCodeForPinSignals(Hdl out, ArrayList<String> path, Component pin) {
    String pinName = "s_" + pin.getAttributeSet().getValue(StdAttr.LABEL);
    pinName = CorrectLabel.getCorrectLabel(pinName);
    int signalIdx = 0;
    for (String signal : ioResources.GetMapNamesList(path)) {
      String signal = signalNames.get(MapOffset);

      // Handle disconnected output pin mappings
      if (ioResources.IsDisconnectedOutput(signal))
        continue; // do nothing, vhdl will warn but will optimize away the signal

      boolean invert = ioResources.RequiresToplevelInversion(path, signal);
      String maybeNot = invert ? out.not+" " : "";
      boolean multibit = pin.getEnd(0).getWidth().getWidth() > 1;

      // Handle synthetic constant-value input pin mappings
      if (!ioResources.IsDeviceSignal(signal)) {
        int constinput = ioResources.GetSyntheticInputValue(signal);
        int w = 1; // fixme ?
        for (int i = 0; i < w; i++) {
          // Example VHDL for constant-value input pin:
          //   s_SomeInputLabel(3) <= '1'
          String constbit = out.bit((constinput>>i)&1);
          if (multibit)
            out.assign(pinName, signalIdx++, maybeNot + constbit);
          else
            out.assign(pinName, maybeNot + constbit);
        }
        continue;
      }

      // Handle normal pin mappings
      int inputId = ioResources.GetFPGAInputPinId(signal);
      int outputId = ioResources.GetFPGAOutputPinId(signal);
      int w = ioResources.GetNrOfPins(signal);
      System.out.println("w = " + w); // fixme: suspect this is always 1
      for (int i = 0; i < w; i++) {
        // Example VHDL for normal input pin:
        //   s_SomeCircuitInputLabel(3) <= FPGA_INPUT_PIN_27
        if (inputId >= 0 && multibit)
          out.assign(pinName, signalIdx++, maybeNot + "FPGA_INPUT_PIN_" + (inputId+i));
        else if (inputId >= 0)
          out.assign(pinName, maybeNot + "FPGA_INPUT_PIN_" + (inputId+i));
        // Example VHDL for normal input pin:
        //   FPGA_OUTPUT_PIN_27 <= s_SomeCircuitOutputLabel(3)
        if (outputId >= 0 && multibit)
          out.assign("FPGA_OUTPUT_PIN_" + (outputId+i), pinName, signalIdx++);
        else if (outputId >= 0)
          out.assign("FPGA_OUTPUT_PIN_" + (outputId+i), pinName);
      }
    }
  }

	private static void generateInlinedCodeForHiddenSignals(Hdl out, ArrayList<String> path) {
    // Note: This only happens for input and output ports, as only they can get
    // inversions. InOut ports do not get inversions, so don't get touched here.

    // FIXME Q: is this only for Button? or also other things like LED?
    // FIXME Q: Why are only these three discarded, specifically?
    // I suspect that PortIO, Tty, and Keyboard used to do their own mappings to
    // things like FPGA_INPUT_PIN_, and maybe they used to only be able to appear
    // at the top level. That seems unnecessary.
    // InstanceFactory factory = ioResources.GetComponent(path).GetComponent().getFactory();
    // if (factory instanceof PortIO) {
    //   // ((PortIO)factory).setMapInfo(ioResources);
    //   return;
    // } else if (factory instanceof Tty) {
    //   // ((Tty)factory).setMapInfo(ioResources);
    //   return;
    // } else if (factory instanceof Keyboard) {
    //   // ((Keyboard)factory).setMapInfo(ioResources);
    //   return;
    // }

    String pathstr = String.join("/", path);
    System.out.println("Generating inline code for I/O type " + factory);
    System.out.println("Path is: " + String.join("/", path);

		NetlistComponent comp = ioResources.GetComponent(path);
		if (comp == null) {
			out.err.AddFatalError("INTERNAL ERROR: Shadow I/O component missing for %s", pathstr);
			return;
		}

		ArrayList<String> partialpath = new ArrayList<>();
		partialpath.addAll(path);
		partialpath.remove(0); // ?? removes TopLevel from path?
		BubbleInformationContainer bubbles = comp.GetGlobalBubbleId(partialpath);
		if (bubbles == null) {
			out.err.AddFatalError("INTERNAL ERROR: Missing bubble data for I/O component %s", pathstr);
			return;
		}

    int inputIdx = bubbles.GetInputStartIndex();
    int inputIdxEnd = bubbles.GetInputEndIndex();
    int outputIdx = bubbles.GetOutputStartIndex();
    int outputIdxEnd = bubbles.GetOutputEndIndex();

		for (String signal : ioResources.GetMapNamesList(path);
			boolean invert = ioResources.RequiresToplevelInversion(path, signal);
      String maybeNot = (invert ? hdl.not + " " : "");
			int inputId = ioResources.GetFPGAInputPinId(signal);
			int outputId = ioResources.GetFPGAOutputPinId(signal);
			int n = ioResources.GetNrOfPins(signal);
			for (int pin = 0; pin < n; pin++) {
        // Example VHDL for input pin:
        //   s_LOGISIM_HIDDEN_FPGA_INPUT(3) <= FPGA_INPUT_PIN_28;
				if (inputId >= 0 && inputIdx <= inputIdxEnd)
					out.assign("s_LOGISIM_HIDDEN_FPGA_INPUT", inputIdx++,
              maybeNot + "FPGA_INPUT_PIN_"+ (inputId + pin));
        // Example VHDL for output pin:
        //   FPGA_OUTPUT_PIN_35 <= s_LOGISIM_HIDEN_FPGA_OUTPUT(4);
				if (outputId >= 0 && outputIdx <= outputIdxEnd)
          out.assign("FPGA_OUTPUT_PIN_" + (outputId + pin),
              maybeNot + "s_LOGISIM_HIDEN_FPGA_OUTPUT", outputIdx++);
			}
		}
	}

}
