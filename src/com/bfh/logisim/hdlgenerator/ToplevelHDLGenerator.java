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

import com.bfh.logisim.fpga.BoardIO;
import com.bfh.logisim.fpga.PinBindings;
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
	private PinBindings goResources;
  private Netlist _circNets; // Netlist of the circUnderTest.

  private TickHDLGenerator ticker;
  private CircuitHDLGenerator circgen;
  private HDLCTX ctx;

  // There is no parent netlist for TopLevel, because it is not embedded inside
  // anything. There are no attributes either.
  // There is no parent netlist for circgen or ticker, because TopLevel doesn't
  // create a netlist for itself. Neither of those components uses attributes,
  // so we can leave them empty. So both can use a single context with null nets
  // and empty attributes.
	
  public ToplevelHDLGenerator(String lang, FPGAReport err, char vendor,
      long fpgaClockFreq, int tickerPeriod,
			Circuit circUnderTest, PinBindings ioResources) {
    this(new HDLCTX(lang, err, null /*nets*/, null /*attrs*/, vendor),
      fpgaClockFreq, tickerPeriod, circUnderTest, ioResources);
  }

	private ToplevelHDLGenerator(HDLCTX ctx, long fpgaClockFreq, int tickerPeriod,
			Circuit circUnderTest, PinBindings ioResources) {
    super(ctx, "toplevel", HDL_NAME, "i_Toplevel");

		this.fpgaClockFreq = fpgaClockFreq;
		this.tickerPeriod = tickerPeriod;
		this.circUnderTest = circUnderTest;
		this.ioResources = ioResources;
    this.ctx = ctx;

    _circNets = circUnderTest.getNetlist();
    int numclk = _circNets.clockbus.shapes().size();

    // raw oscillator input
    ioResources.requiresOscilator = numclk > 0;
    if (numclk > 0)
      inPorts.add(new PortInfo(TickHDLGenerator.FPGA_CLK_NET, 1, -1, null));

    // io resources
    Int3 ioPinCount = ioResources.countFPGAPhysicalIOPins();
		for (int i = 0; i < ioPinCount.in; i++)
      inPorts.add(new PortInfo("FPGA_INPUT_PIN_"+i, 1, -1, null));
		for (int i = 0; i < ioPinCount.inout; i++)
      inOutPorts.add(new PortInfo("FPGA_INOUT_PIN_"+i, 1, -1, null));
		for (int i = 0; i < ioPinCount.out; i++)
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

  // Top-level entry point: write all HDL files for the project.
  public boolean writeAllHDLFiles(String rootDir) {
    return circgen.writeAllHDLFiles(rootDir)
        && (ticker == null || ticker.writeHDLFiles(rootDir))
        && writeHDLFiles(rootDir);
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

    out.comment("signal adaptions for I/O related components and top-level pins");
    ioResources.components.forEach((path, shadow) -> {
      generateInlinedCodeSignal(out, path, shadow)
		});
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

  private void pinVectorAssign(Hdl out, String pinName, String portName, int seqno, int n) {
    if (n == 1)
      out.assign(pinName, portName, seqno);
    else if (n > 1)
      out.assign(pinName, portName, seqno+n-1, seqno);
  }

  private boolean needTopLevelInversion(Component comp, BoardIO io) {
    boolean boardIsActiveHigh = io.activity == PinActivity.ACTIVE_HIGH;
    boolean compIsActiveHigh = comp.getFactory()ActiveOnHigh(comp.getAttributeSet());
    return boardIsActiveHigh ^ compIsActiveHigh;
  }

  private void generateInlinedCodeSignal(Hdl out, Path path, NetlistComponent shadow) {
    // Note: Any logisim component that is not active-high will get an inversion
    // here. Also, any FPGA I/O device that is not active-high will get an
    // inversion. In cases where there would be two inversions, we leave them
    // both off.
    String signal;
    String bit;
    int offset;
    if (shadow.original.getFactory() instanceOf Pin) {
      signal = "s_" + NetlistComponent.labelOf(pin);
      bit = signal+hdl.idx;
      offset = 0;
    } else {
      NetlistComponent.PortIndices3 indices = comp.getGlobalHiddenPortIndices(path);
      if (indices == null) {
        out.err.AddFatalError("INTERNAL ERROR: Missing index data for I/O component %s", path);
        return;
      }
      if (indices.in.end == indices.in.start) {
        // signal[5] is the only bit
        offset = indices.in.start;
        bit = "s_LOGISIM_HIDDEN_FPGA_INPUT"+hdl.idx;
        signal = String.format(bit, offset);
      } else if (indices.in.end > indices.in.start) {
        // signal[5] versus signal[8:3]
        offset = indices.in.start;
        signal = "s_LOGISIM_HIDDEN_FPGA_INPUT";
        bit = signal+hdl.idx;
      } else if (indices.out.end == indices.out.start) {
        // signal[5] is the only bit
        offset = indices.out.start;
        bit = "s_LOGISIM_HIDDEN_FPGA_OUTPUT"+hdl.idx;
        signal = String.format(bit, offset);
      } else if (indices.out.end > indices.out.start) {
        // signal[5] versus signal[8:3]
        offset = indices.out.start;
        signal = "s_LOGISIM_HIDDEN_FPGA_OUTPUT";
        bit = signal+hdl.idx;
      } else {
        out.err.AddFatalError("INTERNAL ERROR: Hidden net without input or output bits for path %s", path);
        return;
      }
    }

    PinBindings.Dest dest = ioResources.mappings(ioResources.sourceFor(path));
    if (dest != null) { // Entire pin is mapped to one BoardIO resource.
      boolean invert = needTopLevelInversion(pin, dest.io);
      String maybeNot = (invert ? hdl.not + " " : "");
      if (dest.io.type == BoardIO.Type.Unconnected) {
        // If user assigned type "unconnected", do nothing. Synthesis will warn,
        // but optimize away the signal.
        continue;
      } else if (!BoardIO.PhysicalTypes.contains(dest.io.type)) {
        // Handle synthetic input types.
        int constval = dest.io.syntheticValue;
        out.assign(signal, maybeNot+out.literal(constval, dest.io.width.size()));
      } else {
        // Handle physical I/O device types.
        Int3 seqno = dest.seqno();
        // Input pins
        if (dest.io.width.in == 1)
          out.assign(signal, maybeNot+"FPGA_INPUT_PIN_"+seqno.in);
        else for (int i = 0; i < dest.io.width.in)
          out.assign(signal, i, maybeNot+"FPGA_INPUT_PIN_"+(seqno.in+i));
        // Output pins
        if (dest.io.width.out == 1)
          out.assign("FPGA_OUTPUT_PIN_"+seqno.out, maybeNot+signal);
        else for (int i = 0; i < dest.io.width.out)
          out.assign("FPGA_OUTPUT_PIN_"+(seqno.out+i), maybeNot+signal, i);
        // Note: no such thing as inout pins
      }
    } else { // Each bit of pin is assigned to a different BoardIO resource.
      ArrayList<PinBinding.Source> srcs = ioResources.bitSourcesFor(path);
      for (int i = 0; i < srcs.size(); i++)  {
        dest = ioResources.mappings(srcs.get(i));
        boolean invert = needTopLevelInversion(pin, dest.io);
        String maybeNot = (invert ? hdl.not + " " : "");
        if (dest.io.type == BoardIO.Type.Unconnected) {
          // If user assigned type "unconnected", do nothing. Synthesis will warn,
          // but optimize away the signal.
          continue;
        } else if (!BoardIO.PhysicalTypes.contains(dest.io.type)) {
          // Handle synthetic input types.
          int constval = dest.io.syntheticValue;
          out.assign(bit, offset+i, maybeNot+out.literal(constval, 1));
        } else {
          // Handle physical I/O device types.
          Int3 seqno = dest.seqno();
          if (dest.io.width.in == 1)
            out.assign(bit, offset+i, maybeNot+"FPGA_INPUT_PIN_"+seqno.in);
          // Output pins
          if (dest.io.width.out == 1)
            out.assign("FPGA_OUTPUT_PIN_"+seqno.out, maybeNot+bit, offset+i);
          // Note: no such thing as inout pins
        }
      }
    }
  }

}
