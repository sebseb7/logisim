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

import java.io.File;
import java.util.ArrayList;

import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.cburch.logisim.hdl.Hdl;

public class HDLGenerator extends HDLSupport {

  // Details of input, output, and inout ports.
  protected static class PortInfo {
    final String name; // signal name
    final String width; // generic param (but not an expression) or constant integer
    final int index; // port/end number within logisim Component and HDL NetlistComponent
    final static int UNCONNECTED = -1; // When used as index, we just use default value.
    final Boolean defaultValue; // only for inputs; null means port is required
    PortInfo(String n, String w, int i, Boolean v) {
      name = n;
      width = w;
      index = i;
      defaultValue = v;
    }
  }

  // Details of signals for wire and registers.
  protected static class WireInfo {
    final String name; // signal name
    final String width; // generic param, or param expression, or constant integer
    WireInfo(String n, String w) {
      name = n;
      width = w;
    }
  }

  // Details of generic parameters
  protected static class ParameterInfo {
    final String name; // param name
    final String type; // integer, natural, or positive (only used for VhdlHDLGenerator)
    final int value; // constant integer for a given instance
    final int defaultValue; // default value that appears in declarations (only used for VhdlHDLGenerator)
    ParameterInfo(String n, String t, int v, int d) {
      name = n;
      type = t;
      value = v;
      defaultValue = d;
    }
    ParameterInfo(String n, int v) {
      this(n, "integer", v, 0);
    }
  }
  protected static class Generics extends ArrayList<ParameterInfo> {
    public ParameterInfo get(String name) {
      for (ParameterInfo p : this)
        if (p.name.equals(name))
          return p;
      return null;
    }
    public Integer getValue(String name) {
      for (ParameterInfo p : this)
        if (p.name.equals(name))
          return p.value;
      return null;
    }
  }

  protected static class ClockPortInfo {
    String ckPortName, enPortName; // Port names to use for HDL
    final int index; // port/end number within logisim Component and HDL NetlistComponent
    public ClockPortInfo(String cn, String en, int i) {
      ckPortName = cn;
      enPortName = en;
      index = i;
    }
  }

  // Generic parameters and ports.
  protected final Generics parameters = new Generics();
  protected final ArrayList<PortInfo> inPorts = new ArrayList<>();
  protected final ArrayList<PortInfo> inOutPorts = new ArrayList<>();
  protected final ArrayList<PortInfo> outPorts = new ArrayList<>();

  // Wires and registers, which appear before the "begin" statement in VHDL.
  protected final ArrayList<WireInfo> wires = new ArrayList<>();
  protected final ArrayList<WireInfo> registers = new ArrayList<>();

  // Components that have a clock input (like Keyboard, Tty, Register, etc.)
  // don't put the clock input into inPorts because the clock needs special
  // handling. The clockPort object, if not null, holds info about the clock
  // port allowing us to properly configure clock-related inputs for the
  // component. Generally, this means two inputs: one clock signal, and one
  // clock enable. Depending on user options (to set the global speed), Clock
  // component options (to set the shape for individual clock nets), and wiring
  // (to route various clock signals to the component clock ports), the two
  // clock inputs will either be the raw fpga clock, a derived clock signal, or
  // an inverted clock, and the enable will either be a constant 1 or will pulse
  // high for one period at appropriate times. HDL should be coded to use rising
  // edges (for edge-triggered components) or high level (for level-senstive
  // components). Below, we do the adaptions to handle falling edge or
  // active-low clocks for components so configured.
  ClockPortInfo clockPort = null;

  // Some components need direct access to the raw FPGA clock signal and the
  // ClockTick signal coming from the TickHDLGenerator (which gets added
  // automatically to the top-level circuit, and propagated into subcircuits as
  // needed (or always?)). Instead of using inPorts, these components put two
  // port names into tickerPort, which get mapped to the clock signal and clock
  // enable from the top-level Ticker.
  // ClockPortInfo tickerPort = null;

  // Some components can have hidden connections to FPGA board resource, e.g. an
  // LED component has a regular logisim input, but it also has a hidden FPGA
  // output that needs to "bubble up" to the top-level HDL circuit and
  // eventually be connected to an FPGA "LED" or "Pin" resource. Similarly, a
  // Button component has a regular logisim output and a hidden FPGA input that
  // "bubbles up" to the top level and can be connected to an FPGA "Button",
  // "DipSwitch", "Pin", or other compatable FPGA resource. The hiddenPorts
  // object, if not null, holds info about what type of FPGA resource is most
  // suitable, alternate resource types, how many in/out/inout pins are
  // involved, names for the signals, etc.
  protected IOComponentInformationContainer hiddenPort = null;
  // public final IOComponentInformationContainer getHiddenPort() { return hiddenPort; }

  // A suitable subdirectory for storing HDL files.
  protected final String subdir;

  // Prefix for generating pretty instance names. No need to be unique, as it
  // will get a unique ID suffix: "i_Add" will become "i_Add_1", "i_Add_2", etc.
  protected final String hdlInstanceNamePrefix;

  protected HDLGenerator(HDLCTX ctx, String subdir,
      String hdlComponentNameTemplate, String hdlInstanceNamePrefix) {
    super(ctx, hdlComponentNameTemplate, false);
    this.subdir = subdir;
    this.hdlInstanceNamePrefix = CorrectLabel.getCorrectLabel(hdlInstanceNamePrefix);
    this.vhdlLibraries.add(IEEE_LOGIC);
    this.vhdlLibraries.add(IEEE_NUMERIC);
  }

  // Generate and write all necessary HDL files for this component, using the
  // given root directory. Depeding on the specific subclass involved, this may
  // include entity, architecture, and several memory init files (for NV Ram).
  public boolean writeHDLFiles(String rootDir) {
    return writeEntity(rootDir) && writeArchitecture(rootDir);
  }

  // Generate and write an "architecture" file for this component, using the
  // given root directory.
  protected boolean writeArchitecture(String rootDir) {
    Hdl hdl = getArchitecture();
		if (hdl == null || hdl.isEmpty()) {
			_err.AddFatalError("INTERNAL ERROR: Generated empty architecture for HDL `%s'.", hdlComponentName);
			return false;
		}
		File f = openFile(rootDir, false, false);
		if (f == null)
			return false;
		return FileWriter.WriteContents(f, hdl, _err);
	}

  // Generate the full HDL code for the "architecture" file.
	protected Hdl getArchitecture() {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out);

		if (out.isVhdl) {

			out.stmt("architecture logisim_generated of " + hdlComponentName + " is ");
      out.indent();

      generateVhdlTypes(out);
      generateVhdlComponentDeclarations(out);

      if (!wires.isEmpty() || !registers.isEmpty()) {
        for (WireInfo s : wires)
          out.stmt("signal %s : %s;", s.name, out.typeForWidth(s.width));
        for (WireInfo s : registers)
          out.stmt("signal %s : %s;", s.name, out.typeForWidth(s.width));
        out.stmt();
			}
      out.dedent();

			out.stmt("begin");

      out.indent();
			out.stmt();
      generateBehavior(out);
			out.stmt();
      out.dedent();

			out.stmt("end logisim_generated;");

    } else {

      ArrayList<String> portNames = new ArrayList<>();
      inPorts.forEach(p -> portNames.add(p.name));
      inOutPorts.forEach(p -> portNames.add(p.name));
      outPorts.forEach(p -> portNames.add(p.name));
      if (clockPort != null) {
        portNames.add(clockPort.ckPortName);
        portNames.add(clockPort.enPortName);
      }
      // if (tickerPort != null) {
      //   portNames.add(tickerPort.ckPortName);
      //   portNames.add(tickerPort.enPortName);
      // }
      if (hiddenPort != null) {
        portNames.addAll(hiddenPort.GetInportLabels());
        portNames.addAll(hiddenPort.GetInOutLabels());
        portNames.addAll(hiddenPort.GetOutportLabels());
      }
      out.stmt("module %s(\t %s );", hdlComponentName, String.join(",\n\t ", portNames));

      out.indent();
      for (ParameterInfo p : parameters)
        out.stmt("parameter %s = %d;", p.name, p.defaultValue); // todo: verilog parameter types?
			for (PortInfo p : inPorts)
				out.stmt("input %s%s;", out.typeForWidth(p.width), p.name);
			for (PortInfo p : inOutPorts)
				out.stmt("inout %s%s;", out.typeForWidth(p.width), p.name);
			for (PortInfo p : outPorts)
				out.stmt("output %s%s;", out.typeForWidth(p.width), p.name);
      if (clockPort != null) {
				out.stmt("input %s%s; // special clock signal", clockPort.ckPortName);
				out.stmt("input %s%s; // special clock enable", clockPort.enPortName);
      }
      // if (tickerPort != null) {
			// 	out.stmt("input %s%s; // special clock signal", clockPort.ckPortName);
      if (hiddenPort != null) {
        for (String name : hiddenPort.GetInportLabels())
          out.stmt("input %s; // special hidden port", name);
        for (String name : hiddenPort.GetInOutLabels())
          out.stmt("inout %s; // special hidden port", name);
        for (String name : hiddenPort.GetOutportLabels())
          out.stmt("output %s; // special hidden port", name);
      }
      out.stmt();

      for (WireInfo s : wires)
        out.stmt("wire %s%s;", out.typeForWidth(s.width), s.name);
      if (!wires.isEmpty())
        out.stmt();

      for (String s : registers)
        out.stmt("reg %s%s;", out.typeForWidth(s.width), s.name);
      if (!registers.isEmpty())
        out.stmt();

			out.stmt();
      generateBehavior(out);
			out.stmt();

      out.dedent();
			out.stmt("endmodule");

		}
		return out;
	}




  // Generate and write an "entity" file (for VHDL) for this component, using
  // the given root directory.
  protected boolean writeEntity(String rootDir) {
    if (!_lang.equals("VHDL"))
      return true;
    Hdl hdl = getVhdlEntity();
		if (hdl == null || hdl.isEmpty()) {
			_err.AddFatalError("INTERNAL ERROR: Generated empty entity for VHDL `%s'.", hdlComponentName);
			return false;
		}
		File f = openFile(rootDir, false, true);
		if (f == null)
			return false;
		return FileWriter.WriteContents(f, hdl, _err);
	}

	protected Hdl getVhdlEntity() {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out);
    generateVhdlLibraries(out);
    generateVhdlBlackBox(out, true);
    return out;
	}

	protected void generateComponentDeclaration(Hdl out) {
		if (_lang.equals("VHDL"))
      generateVhdlBlackBox(out, false);
	}

	private void generateVhdlBlackBox(Hdl out, boolean isEntity) {
    if (isEntity)
      out.stmt("entity %s is", hdlComponentName);
    else
      out.stmt("component %s", hdlComponentName);

		if (!parameters.isEmpty()) {
      ArrayList<String> generics = new ArrayList<>();
      for (ParameterInfo p : parameters)
        generics.add(p.name + " : " + p.type + " := " + p.defaultValue);
      out.stmt("generic(\t %s );", String.join(";\n\t ", generics));
    }

		if (!inPorts.isEmpty() || !outPorts.isEmpty() || !inOutPorts.isEmpty()
        || clockPort != null
        || (hiddenPort != null && hiddenPort.GetNrOfPorts() > 0)) {
      ArrayList<String> ports = new ArrayList<>();
      for (PortInfo s: inPorts)
        ports.add(s + " : in " + out.typeForWidth(s.width));
      for (PortInfo s: inOutPorts)
        ports.add(s + " : inout " + out.typeForWidth(s.width));
      for (PortInfo s: outPorts)
        ports.add(s + " : out " + out.typeForWidth(s.width));
      if (clockPort != null) {
				ports.add(clockPort.ckPortName, " : in " + out.typeForWidth(1));
				ports.add(clockPort.enPortName, " : in " + out.typeForWidth(1));
      }
      if (hiddenPort != null) {
        for (String name : hiddenPort.GetInportLabels())
          ports.add(name + " : in " + out.typeForWidth(1));
        for (String name : hiddenPort.GetInOutLabels())
          ports.add(name + " : inout " + out.typeForWidth(1));
        for (String name : hiddenPort.GetOutportLabels())
          ports.add(name + " : out " + out.typeForWidth(1));
      }
      out.stmt("port(\t %s );", String.join(";\n\t ", generics));
		}

    if (isEntity)
      out.stmt("end %s;", hdlComponentName);
    else
      out.stmt("end component;");
		out.stmt();
	}

  // Generate an instantiation of this component.
  // VHDL Example:
  //  MyAdder_4 : BusAdder
  //  generic map( BitWidth => 8,
  //               OtherParam => 5 )
  //  port map( A => foo,
  //            B => bar,
  //            C => baz );
  //
  // Verilog Example:
  // BusAdder #( .BitWidth(8),
  //             .OtherParam(5) ) MyAdder_4 (
  //             .A(foo),
  //             .B(bar),
  //             .C(baz) );
	protected void generateComponentInstance(Hdl out, long id, NetlistComponent comp) {

		String instName = getInstanceName(id);

    ArrayList<String> portVals = getPortMappings(comp);

    ArrayList<String> generics = new ArrayList<>();
    ArrayList<String> values = new ArrayList<>();

		if (out.isVhdl) {

      out.stmt("%s : %s", instName, hdlComponentName);
			if (!parameters.isEmpty()) {
        for (ParameterInfo s : parameters)
          generics.add(s.name + " => " +  s.value);
        out.stmt("generic map(\t %s )", String.join(",\n\t ", generics));
			}

			if (!portVals.isEmpty()) {
        for (String s : portVals.keySet())
          values.add(s + " => " +  portVals.get(s));
        out.stmt("port map(\t %s )", String.join(",\n\t ", values));
      }

		} else {

      for (ParameterInfo s : parameters)
        generics.add(".%s(%d)", s.name, s.value);
      String g = String.join(",\n\t ", generics);
      for (String s : portVals.keySet())
        values.add(".%s(%d)", s, portVals.get(s));
      String v = String.join(",\n\t ", values);

			if (generics.isEmpty() && values.isEmpty())
        out.stmt("%s %s;", hdlComponentName, instName);
      else if (generics.isEmpty())
        out.stmt("%s %s (\t %s );", hdlComponentName, instName, v);
      else if (values.isEmpty())
        out.stmt("%s #(\t %s ) %s;", hdlComponentName, g, instName);
      else
        out.stmt("%s #(\t %s ) %s ( %s );", hdlComponentName, g, instName, v);
		}

    out.stmt();
	}

	protected void generateFileHeader(Hdl out) {
    out.comment(" === Logisim-Evolution Holy Cross Edition auto-generated %s code", _lang);
    out.comment(" === Project: %s", _projectName);
    out.comment(" === Component: %s", hdlComponentName);
    out.stmt();
	}

  // Generate any external component declarations that should appear within the
  // VHDL declarations.
	protected void generateVhdlComponentDeclarations(Hdl out) { }

  // Generate any custom type declarations that should appear within the VHDL
  // declarations.
	protected void generateVhdlTypes(Hdl out) { }

  // Generate HDL code for the component, i.e. the actual behavioral RTL code.
  protected void generateBehavior(Hdl out) { }

  static final String IEEE_LOGIC = "std_logic_1164"; // standard + extended + extended2
  static final String IEEE_UNSIGNED = "std_logic_unsigned"; //                extended2
  static final String IEEE_NUMERIC = "numeric_std"; //             extended + extended2
  protected ArrayList<String> vhdlLibraries = new ArrayList<>();
	protected static void generateVhdlLibraries(Hdl out) {
    if (vhdlLibraries.length == 0)
      return;
    out.stmt("library ieee;");
    for (String lib : vhdlLibraries)
      out.stmt("use ieee.%s.all;", lib);
    out.stmt();
  }

  // Returns a file opened for writing.
	protected File openFile(String rootDir, boolean isMif, boolean isEntity) {
    if (!rootDir.endsWith(File.separator))
      rootDir += File.separatorChar;
    if (!subdir.endsWith(File.separator) && !subdir.isEmpty())
      subdir += File.separatorChar;
    String path = rootDir + _lang.toLowerCase() + File.separatorChar + subdir;
    return FileWriter.GetFilePointer(path, hdlComponentName, isMif, isEntity, _err, _lang);
	}

  // Return a suitable stem for naming instances of this component within a
  // circuit, so we can form names like "i_Add_1", "i_Add_2", etc., for
  // instances of BusAdder and BitAdder. These need not be unique: the
  // CircuitHDLGeneratorFactory will add suffixes to ensure all the instance
  // names within a circuit are unique.
	public final String getInstanceNamePrefix() { return hdlInstanceNamePrefix; }

  // Return an instance name by combining the prefix with a unique ID.
	public final String getInstanceName(long id) { return hdlInstanceNamePrefix + "_" + id; }


  // Returns mappings of all input, inout, and output ports to signal names
  // or expressions for the "port map" of the given component instantiation.
  protected ArrayList<String> getPortMappings(NetlistComponent comp) {
    ArrayList<String> assn = new ArrayList<>();
    for (PortInfo port : inPorts)
      getPortMappings(assn, comp, port);
    for (PortInfo port : inOutPorts)
      getPortMappings(assn, comp, port);
    for (PortInfo port : outPorts)
      getPortMappings(assn, comp, port);
    if (clockPort != null)
      getClockPortMappings(assn, comp);
    if (hiddenPort != null)
      getHiddenPortMappings(assn, comp);
    return assn;
  }

todo: add clock info to ports and params

  protected void getClockPortMappings(ArrayList<String> assn, NetlistComponent comp) {
    if (clockPort.index < 0 || clockPort.index >= comp.NrOfEnds()) {
      _err.AddFatalError("INTERNAL ERROR: Clock port %d of '%s' is missing.",
          clockPort.index, hdlComponentName);
      assn.add(String.format(_hdl.map, clockPort.ckPortName, _hdl.zero));
      assn.add(String.format(_hdl.map, clockPort.enPortName, _hdl.zero));
      return;
    }
    if (!comp.EndIsConnected(clockPort.index)) {
      _err.AddSevereWarning("ERROR: Clock port of '%s' in circuit '%s' is not connected."
          hdlComponentName, _nets.getCircuitName()
      _err.AddSevereWarning("  The component will likely malfunction without a clock.");
      assn.add(String.format(_hdl.map, clockPort.ckPortName, _hdl.zero));
      assn.add(String.format(_hdl.map, clockPort.enPortName, _hdl.zero));
      return;
    }
    ConnectionEnd end = comp.getEnd(clockPort.index);
    if (end.NrOfBits() != 1) {
      _err.AddFatalError("ERROR: Clock port of '%s' in '%s' is connected to a bus.",
          hdlComponentName, _nets.getCircuitName());
      assn.add(String.format(_hdl.map, clockPort.ckPortName, _hdl.zero));
      assn.add(String.format(_hdl.map, clockPort.enPortName, _hdl.zero));
      return;
    }
    Net net = end.GetConnection((byte) 0).GetParrentNet();
    if (net == null) {
      _err.AddFatalError("INTERNAL ERROR: Unexpected unconnected clock port of '%s' in circuit '%s'.",
          hdlComponentName, _nets.getCircuitName());
      assn.add(String.format(_hdl.map, clockPort.ckPortName, _hdl.zero));
      assn.add(String.format(_hdl.map, clockPort.enPortName, _hdl.zero));
      return;
    }

    byte bit = end.GetConnection((byte) 0).GetParrentNetBitIndex();
    int clkId = _nets.GetClockSourceId(net, bit);
    if (clkId < 0) {
      // In this case, we use the potentially noisy logic signal (or it's
      // inverse, for TRIG_LOW or TRIG_FALLING) that the user connected as the
      // clock signal, and set the enable signal to one. Note: currently only
      // Ram, Register, and some FlipFlops can be level-sensitive in Logisim,
      // but even here, only only Register and the FlipFlops support it in HDL).
      // All other cases are edge triggered.
      if (edgeTriggered()) {
        _err.AddSevereWarning("WARNING: Non-clock signal (or \"gated clock\") connected to clock port for edge-triggered '%s' in circuit '%s'",
            hdlComponentName, _nets.getCircuitName());
        _err.AddSevereWarning("         Expect functional differences between Logisim simulation and FPGA behavior.");
      }
      if (edgeTriggered() && clockPort.ckPortName.equals("FPGAClock")) {
        _err.AddSevereWarning("         Actually, this component is almost certain to malfunction "
            + "unless it's clock input is connected to a proper clock signal, "
            + "due to explicit timing requirements in the synthesized HDL and, "
            + "the need for signals to traverse multiple clock domains.");
      }
      String clkSignal = _nets.signalForEndBit(end, 0, _hdl);
      if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING 
          || _attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
          || _attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) {
        clkSignal = _hdl.not + " " + clkSignal;
      }
      assn.add(String.format(_hdl.map, clockPort.ckPortName, clkSignal));
      assn.add(String.format(_hdl.map, clockPort.enPortName, _hdl.one));
    } else {
      // Here we have a proper connection to one of the clock buses that come
      // from a Clock component. We just need to select the right bits from it.
      String[] clkSignals = ClockHDLGenerator.clkSignalFor(this, clkId);
      assn.add(String.format(_hdl.map, clockPort.ckPortName, clkSignals[0]));
      assn.add(String.format(_hdl.map, clockPort.enPortName, clkSignals[1]));
    }
  }

  protected void getHiddenPortMappings(ArrayList<String> assn, NetlistComponent comp) {
    int i, b;
    if (hiddenPort.GetMainMapType() == IOComponentInformationContainer.TickGenerator) {
      // Special case: tick generator isn't actually a hidden input, it is just a
      // reference to the primary ticker clock net.
      assn.add(String.format(_hdl.map, comp.GetInportLabel(0), TickComponentHDLGeneratorFactory.FPGAClock));
      assn.add(String.format(_hdl.map, comp.GetInportLabel(1), TickComponentHDLGeneratorFactory.FPGATick));
      return;
    }
    i = 0;
    b = comp.GetLocalBubbleInputStartId();
    for (String name : hiddenPort.GetInportLabels())
      assn.add(String.format(_hdl.map, name, "LOGISIM_HIDDEN_FPGA_INPUT" + (i++)));
    i = 0;
    b = comp.GetLocalBubbleInOutStartId();
    for (String name : hiddenPort.GetInOutLabels())
      assn.add(String.format(_hdl.map, name, "LOGISIM_HIDDEN_FPGA_INOUT" + (i++)));
    i = 0;
    b = comp.GetLocalBubbleOutputStartId();
    for (String name : hiddenPort.GetOutputLabels())
      assn.add(String.format(_hdl.map, name, "LOGISIM_HIDDEN_FPGA_OUTPUT" + (i++)));
  }

  protected void getUnconnectedPortMappings(ArrayList<String> assn, PortInfo p) {
    if (_hdl.isVhdl) {
      if (p.defaultValue == null)
        assn.add(String.format("%s => open", p.name));
      else if (p.width.equals("1"))
        assn.add(String.format("%s => %s", p.name, _hdl.bit(p.defaultValue)));
      else
        assn.add(String.format("%s => (others => %s)", p.name, _hdl.bit(p.defaultValue)));
    } else {
      if (p.defaultValue == null)
        assn.add(String.format(".%s()", p.name));
      else if (p.width.equals("1"))
        assn.add(String.format(".%s(%s)", p.name, _hdl.bit(p.defaultValue)));
      else if (p.defaultValue)
        assn.add(String.format(".%s(~0)", p.name));
      else
        assn.add(String.format(".%s(0)", p.name));
    }
  }

  protected void getPortMappings(ArrayList<String> assn, NetlistComponent comp, PortInfo p) {
    if (p.index == UNCONNECTED || p.index < 0 || p.index >= comp.NrOfEnds()) {
      // For output and inout ports, default value *should* be null, making
      // those ones be open. Sadly, we can't do a sanity check for those here.
      getUnconnectedPortMappings(assn, p);
    } else {
      ConnectionEnd end = comp.getEnd(endIdx);
      // Sanity check to ensure default value for output ports is null.
      if (end.IsOutputEnd() && p.defaultValue != null) {
        err.AddSevereWarning("INTERNAL ERROR: ignoring default value for output pin '%s' of '%s' in circuit '%s'.",
            p.name, hdlComponentName, _nets.getCircuitName());
        p.defaultValue = null;
      }
      int w = end.NrOfBits();
      if (w == 1 && !p.width.equals("1")) {
        // Special VHDL case: When port is anything other than width "1", it
        // will be a std_logic_vector. And if the end is 1 bit, then the port
        // vector must be of type std_logic_vector(0 downto 0), but the signal
        // being assigned is of type std_logic. These can't be directly assigned
        // using "portname => signal" because of the mismatched types. We need
        // to do "portname(0) => signal" instead.
        assn.add(String.format(_hdl.map0, p.name, _nets.signalForEndBit(end, 0, _hdl)));
      } else if (w == 1) {
        assn.add(String.format(_hdl.map, p.name, _nets.signalForEndBit(end, 0, _hdl)));
      } else {
        int status = _nets.busConnectionStatus(end);
        if (status == Netlist.BUS_MIXCONNECTED) {
          // Neither Verilog nor VHDL can handle this case properly without
          // dummy signals. Hopefully it doesn't ever happen, since Logisim
          // would have noticed any width mismatch error. Just in case this is
          // part of some circuit that doesn't matter, let's just issue a severe
          // warning and treat this as entirely unconnected.
          // FIXME: We might want to instead introduce dummy signals to make
          // this work, if the Logisim circuit could possibly have been doing
          // something useful even with the strange port connection.
          _err.AddSevereWarning("BUS WIDTH MISMATCH: Some bits of pin '%s' of '%s' in circuit '%s' are connected, but others are not.",
              p.name, hdlComponentName, _nets.getCircuitName());
          _err.AddSevereWarning("                    All bits will be treated as disconnected.");
          getUnconnectedPortMappings(assn, p);
        } else if (status == Netlist.BUS_UNCONNECTED) {
          getUnconnectedPortMappings(assn, p);
        } else if (status == Netlist.BUS_SIMPLYCONNECTED ) {
          assn.add(String.format(_hdl.map, p.name, _nets.signalForEndBus(end, 0, _hdl)));
        } else {
          // Port has a bus connection to multiple separate signals. For VHDL,
          // we need something like:
          //    portName(7 downto 4) => foo(6 downto 2)
          //    portName(3 downto 1) => bar(7 downto 5)
          //    portName(0) => foo(9)
          // For verilog, we need something like:
          //    .portName({foo[6:2], bar[7:5], foo[9]})
          ArrayList<String> signals = new ArrayList<>();
          Net prevnet = end.getConnection((byte)(w-1)).GetParrentNet();
          byte prevbit = end.GetConnection((byte)(w-1)).GetParrentNetBitIndex();
          int n = 1;
          for (int i = w-2; i >= -1; i--) {
            Net net = i < 0 ? null : end.getConnection((byte)i).GetParrentNet();
            byte bit = i < 0 ? 0 : end.GetConnection((byte)i).GetParrentNetBitIndex();
            if (net == prevnet && bit == prevbit-n) {
              n++;
              continue;
            }
            // emit the n-bit slice, or accumulate it
            String signal;
            if (n == 1 && prevnet.BitWidth() == 1)
              signal = String.format("S_LOGISIM_NET_%d", _nets.GetNetId(prevnet));
            else if (n == 1)
              signal = String.format("S_LOGISIM_BUS_%d"+_hdl.idx, _nets.GetNetId(prevnet), prevbit);
            else
              signal = String.format("S_LOGISIM_BUS_%d"+_hdl.range, _nets.GetNetId(prevnet), prevbit, prevbit-n+1);
            if (_hdl.isVerilog)
              signals.add(signal);
            else if (n == 1)
              assn.add(String.format(_hdl.map1, p.name, i+1, signal));
            else
              assn.add(String.format(_hdl.mapN, p.name, i+n, i+1, signal));
          }
          if (_hdl.isVerilog)
            assn.add(String.format(_hdl.map, p.name, "{" + String.join(", ", signals + "}"));
        }
      }
    }
  }

}
