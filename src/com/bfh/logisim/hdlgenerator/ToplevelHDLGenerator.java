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

import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.library.DynamicClock;
import com.bfh.logisim.netlist.NetlistComponent;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.std.wiring.ClockHDLGenerator;
import com.cburch.logisim.std.wiring.Pin;

public class ToplevelHDLGenerator extends HDLGenerator {

  // Name of the top-level HDL module.
  public static final String HDL_NAME = "LogisimToplevelShell";

	private long fpgaClockFreq;
	private int tickerPeriod; // 0:"use fpga clock", -1:"dynamic", >0:"divided clock"
	private Circuit circUnderTest;
	private MappableResourcesContainer ioResources;
  private Netlist _circNets; // Netlist of the circUnderTest.

  private TickHDLGenerator ticker;
  private CircuitHDLGenerator circgen;
  private HDLCTX ctx;

	public ToplevelHDLGenerator(String lang, FPGAReport err, char vendor,
      long fpgaClockFreq, int tickerPeriod,
			Circuit circUnderTest, MappableResourcesContainer ioResources) {
    this(new HDLCTX(lang, err, null /*nets*/, null /*attrs*/, vendor),
      fpgaClockFreq, tickerPeriod, circUnderTest, ioResources);
  }

	public ToplevelHDLGenerator(HDLCTX ctx, long fpgaClockFreq, int tickerPeriod,
			Circuit circUnderTest, MappableResourcesContainer ioResources) {
    super(ctx, "toplevel", HDL_NAME, "i_Toplevel");

		this.fpgaClockFreq = fpgaClockFreq;
		this.tickerPeriod = tickerPeriod;
		this.circUnderTest = circUnderTest;
		this.ioResources = ioResources;
    this.ctx = ctx;

    _circNets = circUnderTest.getNetlist();
    int numclk = _circNets.clockbus.shapes().size();

    // raw oscillator input
    if (numclk > 0)
      inPorts.add(new PortInfo(TickHDLGenerator.FPGA_CLK_NET, 1, -1, null));

    // io resources
		for (int i = 0; i < ioResources.GetNrOfToplevelInputPins(); i++)
      inPorts.add(new PortInfo("FPGA_INPUT_PIN_"+i, 1, -1, null));
		for (int i = 0; i < ioResources.GetNrOfToplevelInOutPins(); i++)
      inOutPorts.add(new PortInfo("FPGA_INOUT_PIN_"+i, 1, -1, null));
		for (int i = 0; i < ioResources.GetNrOfToplevelOutputPins(); i++)
      outPorts.add(new PortInfo("FPGA_OUTPUT_PIN_"+i, 1, -1, null));

    // internal clock networks
		if (numclk > 0) {
      wires.add(new WireInfo(TickHDLGenerator.FPGA_TICK_NET, 1));
			for (int i = 0; i < numclk; i++)
				wires.add(new WireInfo("s_"+ClockHDLGenerator.CLK_TREE_NET+i,
              ClockHDLGenerator.CLK_TREE_WIDTH));
		}

    // wires for hidden ports for circuit design under test
    // note: inout ports never get inversions, so no wire for those
    Netlist.Int3 hidden = _circNets.numHiddenBits();
    addWireVector("s_LOGISIM_HIDDEN_FPGA_INPUT", hidden.in);
		// skip: addWireVector("s_LOGISIM_HIDDEN_FPGA_INOUT", hidden.inout);
    addWireVector("s_LOGISIM_HIDDEN_FPGA_OUTPUT", hidden.out);

    // wires for normal ports for circuit design under test
    for (NetlistComponent shadow : _circNets.inpins) {
      int w = shadow.original.getEnd(0).getWidth().getWidth();
      wires.add(new WireInfo("s_"+shadow.label(), w));
    }
    for (NetlistComponent shadow : _circNets.outpins) {
      int w = shadow.original.getEnd(0).getWidth().getWidth();
      wires.add(new WireInfo("s_"+shadow.label(), w));
    }

    // wires for dynamic clock
    NetlistComponent dynClock = _circNets.dynamicClock();
    if (dynClock != null) {
      int w = dynClock.original.getAttributeSet().getValue(DynamicClock.WIDTH_ATTR).getWidth();
      wires.add(new WireInfo("s_LOGISIM_DYNAMIC_CLOCK", w));
    }

		if (numclk > 0)
			ticker = new TickHDLGenerator(ctx, fpgaClockFreq, tickerPeriod);

		circgen = new CircuitHDLGenerator(ctx, circUnderTest);
	}

  private void addWireVector(String busname, int w) {
    // Note: the parens around "(1)" are a hack to force vector creation.
    // See: HDLGenerator and Hdl.typeForWidth().
    if (w > 0)
      wires.add(new WireInfo(busname, w == 1 ? "(1)" : ""+w));
  }

	@Override
	protected void generateComponentDeclaration(Hdl out) {
		if (ticker != null) {
      ticker.generateComponentDeclaration(out);
      // Clock components are lifted to the top level, so declare one here.
      _circNets.clocks.get(0).hdlSupport.generateComponentDeclaration(out);
		}
    circgen.generateComponentDeclaration(out);
	}

	@Override
  protected void generateBehavior(Hdl out) {

    out.comment("signal adaptions for pin components");
		for (Path path : ioResources.GetComponents()) {
      Component pin = ioResources.GetComponent(path).original;
      if (pin.getFactory() instanceof Pin)
        generateInlinedCodeForPinSignals(out, path, pin);
		}
    out.stmt();

    out.comment("signal adaptions for hidden signals");
		for (Path path : ioResources.GetComponents()) {
      // Component comp = ioResources.GetComponent(path).original;
      // if (!(comp.getFactory() instanceof Pin))
      generateInlinedCodeForHiddenSignals(out, path);
    }
    out.stmt();

		if (nets.NumberOfClockTrees() > 0) {
      out.comment("clock signal distribution");
      ticker.generateComponentInstance(out, 0L /*id*/, null /*comp*/);

			long id = 0;
			for (NetlistComponent clk : _circNets.clocks)
        clk.hdlSupport.generateComponentInstance(out, id++, clk); // FIXME
      out.stmt();
		}

    out.comment("connections for circuit design under test");
    circgen.generateComponentInstance(out, 0L /*id*/, null /*comp*/);
	}

  private static void generateInlinedCodeForPinSignals(Hdl out, Path path, Component pin) {
    String pinName = "s_" + NetlistComponent.labelOf(pin);
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

	private static void generateInlinedCodeForHiddenSignals(Hdl out, Path path) {
    // Note: This only happens for input and output ports, as only they can get
    // inversions. InOut ports do not get inversions, so don't get touched here.

    System.out.printf("Generating inline code for I/O type %s path %s\n", factory, path);

		NetlistComponent comp = ioResources.GetComponent(path);
		if (comp == null) {
			out.err.AddFatalError("INTERNAL ERROR: Shadow I/O component missing for %s", path);
			return;
		}

    NetlistComponent.PortIndices3 indices = comp.getGlobalHiddenPortIndices(path);
		if (indices == null) {
			out.err.AddFatalError("INTERNAL ERROR: Missing index data for I/O component %s", path);
			return;
		}

    int inputIdx = indices.in.start;
    int outputIdx = indices.out.start;

		for (String signal : ioResources.GetMapNamesList(path);
			boolean invert = ioResources.RequiresToplevelInversion(path, signal);
      String maybeNot = (invert ? hdl.not + " " : "");
			int inputId = ioResources.GetFPGAInputPinId(signal);
			int outputId = ioResources.GetFPGAOutputPinId(signal);
			int n = ioResources.GetNrOfPins(signal);
      // Note: inout ports are included here (see note above).
			for (int pin = 0; pin < n; pin++) {
        // Example VHDL for input pin:
        //   s_LOGISIM_HIDDEN_FPGA_INPUT(3) <= FPGA_INPUT_PIN_28;
				if (inputId >= 0 && inputIdx <= indices.in.end)
					out.assign("s_LOGISIM_HIDDEN_FPGA_INPUT", inputIdx++,
              maybeNot + "FPGA_INPUT_PIN_"+ (inputId + pin));
        // Example VHDL for output pin:
        //   FPGA_OUTPUT_PIN_35 <= s_LOGISIM_HIDEN_FPGA_OUTPUT(4);
				if (outputId >= 0 && outputIdx <= indices.out.end)
          out.assign("FPGA_OUTPUT_PIN_" + (outputId + pin),
              maybeNot + "s_LOGISIM_HIDEN_FPGA_OUTPUT", outputIdx++);
			}
		}
	}

}
