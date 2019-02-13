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
import java.util.HashMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.cburch.logisim.hdl.Hdl;

public class HDLGenerator extends HDLSupport {

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
  // include entity, architecture, and several memory init files.
  public boolean writeHDLFiles(String rootDir) {
    HashMap<String, File> memInitFiles = writeMemInitFiles(rootDir);
    return writeEntity(rootDir)
        && writeArchitecture(rootDir, memInitFiles);
  }

  // DONE
  // Generate and write all "memory init" files for this component, using the
  // given root directory, returning the list of memory names and corresponding
  // memory init files.
  protected HashMap<String, File> writeMemInitFiles(String rootDir) {
    MemInitData memInits = getMemInitData();
    HashMap<String, File> files = new HashMap<>();
    for (String m : memInits.keySet()) {
      Hdl data = memInits.get(m);
      if (data.isEmpty()) {
        _err.AddFatalError("INTERNAL ERROR: Empty memory initialization file for memory '%s:%s'.",
            hdlComponentName, memName);
        continue;
      }
      File f = openFile(rootDir, hdlComponentName + "_" + memName, true, false);
      if (f == null)
        continue;
      if (!FileWriter.WriteContents(f, data, _err))
        continue;
      files.put(m, f);
    }
    return files;
  }

  /////////// DONE
  // Generate and write an "architecture" file for this component, using the
  // given root directory and NVRAM initialization files.
  protected boolean writeArchitecture(String rootDir, HashMap<String, File> memInitFiles) {
    Hdl hdl = getArchitecture(memInitFiles);
		if (hdl == null || hdl.isEmpty()) {
			_err.AddFatalError("INTERNAL ERROR: Generated empty architecture for HDL `%s'.", hdlComponentName);
			return false;
		}
		File f = openFile(rootDir, false, false);
		if (f == null)
			return false;
		return FileWriter.WriteContents(f, hdl, _err);
	}

  /////////// DONE
  // Generate the full HDL code for the "architecture" file.
	protected Hdl getArchitecture(HashMap<String, File> memInitFiles) {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out);

    SignalList inPorts = getInPorts();
    SignalList inOutPorts = getInPorts(); ... // fixme: PortIO can do bidirectional ports, so add support for in/out ports here
    SignalList outPorts = getOutPorts();
    Generics params = getGenericParameters();
		SignalList wires = getWires();
		SignalList registers = getRegisters();
		MemoryList mems = getMemories();
    TypeList types = getTypes();

		if (out.isVhdl) {

			out.stmt("architecture logisim_generated of " + hdlComponentName + " is ");
      out.indent();

			if (!types.isEmpty()) {
        out.comment("type definitions");
				for (String t : types.keySet())
					out.stmt("type %s is %s;", t, types.get(t);
				out.stmt();
			}

      generateVhlComponentDeclarations(out);

      if (!wires.isEmpty() || !registers.isEmpty()) {
        out.comment("signal definitions");
        for (String s : wires.keySet())
          out.stmt("signal %s : %s;", s, out.typeForWidth(wires.get(s), params));
        for (String s : registers.keySet())
          out.stmt("signal %s : %s;", s, out.typeForWidth(registers.get(s), params));
        out.stmt();
			}

      if (!mems.isEmpty()) {
        out.comment("memory definitions");
        if (memInitFiles != null && !memInitFiles.isEmpty())
          out.stmt("attribute nvram_init_file : string;");
        for (String m : mems.keySet()) {
          out.stmt("signal %s : %s;", m, mems.get(m));
          // fixme: memInitFiles comes from where? name probably won't match!
          if (memInitFiles != null && memInitFiles.get(m) != null) {
            String filepath = memInitFiles.get(m).getPath();
            Contents.add("attribute nvram_init_file of %s : signal is \"%s\";", m, filepath);
          }
				}
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

      ArrayList<String> ports = new ArrayList<>();
      ports.addall(inPorts.keySet());
      ports.addall(outPorts.keySet());
      out.stmt("module %s(\t %s );", hdlComponentName, String.join(",\n\t ", ports));

      out.indent();
      for (int p : params.keySet())
        out.stmt("parameter %s = 1;", params.get(p));
			for (String s : inPorts.keySet())
				out.stmt("input %s%s;", out.typeForWidth(inPorts.get(s), params), s);
			for (String s : outPorts.keySet())
				out.stmt("output %s%s;", out.typeForWidth(outPorts.get(s), params), s);

      out.stmt();

      if (!wires.isEmpty()) {
        for (String s : wires.keySet())
          out.stmt("wire %s%s;", out.typeForWidth(wires.get(s), params), s);
        out.stmt();
      }

      if (!registers.isEmpty()) {
        for (String s : registers.keySet())
          out.stmt("reg %s%s;", out.typeForWidth(registers.get(s), params), s);
        out.stmt();
      }

      // fixme: verilog support for memories here and in Ram/Rom HDL generators.

			out.stmt();
      generateBehavior(out);
			out.stmt();

      out.dedent();
			out.stmt("endmodule");

		}
		return out;
	}




  /////////// DONE
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

  /////////// DONE
	protected Hdl getVhdlEntity() {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out);
    generateVhdlLibraries(out);
    generateVhdlBlackBox(out, true);
    return out;
	}

  /////////// DONE
	protected void generateComponentDeclaration(Hdl out) {
		if (_lang.equals("VHDL"))
      generateVhdlBlackBox(out, false);
	}

  /////////// DONE
	private void generateVhdlBlackBox(Hdl out, boolean isEntity) {
    SignalList inPorts = getInPorts();
    SignalList inOutPorts = getInOutPorts();
    SignalList outPorts = getOutPorts();
    Generics params = getGenericParameters();

    if (isEntity)
      out.stmt("entity %s is", hdlComponentName);
    else
      out.stmt("component %s", hdlComponentName);

		if (!params.isEmpty()) {
      ArrayList<String> generics = new ArrayList<>();
      for (int p : params.keySet()) {
        String s = params.get(p);
        if (s.startsWith("="))
          continue;
        generics.add(s + " : integer");
      }
      out.stmt("generic(\t %s );", String.join(";\n\t ", generics));
    }

		if (!inPorts.isEmpty() || !outPorts.isEmpty() || !inOutPorts.isEmpty()) {
      ArrayList<String> ports = new ArrayList<>();
      for (String s : inPorts.keySet())
        ports.add(s + " : in " + out.typeForWidth(inPorts.get(s), params));
      for (String s : inOutPorts.keySet())
        ports.add(s + " : inout " + out.typeForWidth(inOutPorts.get(s), params));
      for (String s : outPorts.keySet())
        ports.add(s + " : out " + out.typeForWidth(outPorts.get(s), params));
      out.stmt("port(\t %s );", String.join(";\n\t ", generics));
		}

    if (isEntity)
      out.stmt("end %s;", hdlComponentName);
    else
      out.stmt("end component;");
		out.stmt();
	}

  /////////// DONE
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
  //
	protected void generateComponentInstance(Hdl out, long id, NetlistComponent comp) {

		String instName = getInstanceName(id);

    ParameterAssignments paramVals = getParameterAssignments(comp);
    PortAssignments portVals = getPortAssignments(comp);

    ArrayList<String> generics = new ArrayList<>();
    ArrayList<String> values = new ArrayList<>();

		if (out.isVhdl) {

      out.stmt("%s : %s", instName, hdlComponentName);
			if (!paramVals.isEmpty()) {
        for (String s : paramVals.keySet())
          generics.add(s + " => " +  paramVals.get(s));
        out.stmt("generic map(\t %s )", String.join(",\n\t ", generics));
			}

			if (!portVals.isEmpty()) {
        for (String s : portVals.keySet())
          values.add(s + " => " +  portVals.get(s));
        out.stmt("port map(\t %s )", String.join(",\n\t ", values));
      }

		} else {

      for (String s : paramVals.keySet())
        generics.add(".%s(%d)", s, paramVals.get(s));
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



  /////////// DONE
	protected void generateFileHeader(Hdl out) {
    out.comment(" === Logisim-Evolution Holy Cross Edition auto-generated %s code", _lang);
    out.comment(" === Project: %s", _projectName);
    out.comment(" === Component: %s", hdlComponentName);
    out.stmt();
	}

  /////////// DONE
	protected void generateVhdlComponentDeclarations(Hdl out) { }

  /////////// DONE
  // A list of signal or port names and corresponding widths/IDs. If width=n>0,
  // the signal has a fixed size of n bits. If width=ID<0, then the width is
  // taken from the generic parameter value map corresponding to that ID. In
  // VHDL, width=1 signals are represented using std_logic, others using
  // std_logic_vector(n-1 downto 0), where n is the value taken from the generic
  // parameter value map.
  protected static class SignalList extends TreeMap<String, Integer> { }

  /////////// DONE
  // Return a list of this component's input ports.
	protected SignalList getInPorts() { return new SignalList(); }

  /////////// DONE
  // Return a list of this component's output ports.
	protected SignalList getOutPorts() { return new SignalList(); }

  /////////// DONE
  // Return a list of this component's in/out ports.
	protected SignalList getInOutPorts() { return new SignalList(); }

  /////////// DONE
  // A list of generic parameter IDs and corresponding generic parameter names
  // or derived parameter expressions. The IDs should all be negative. If the
  // name starts with "=", it what follows is a derived parameter expression:
  // the ID can be used for signals but it won't actually be included in the HDL
  // code list of parameters. For example:
  //    -1 --> "N"
  //    -2 --> "=N+1"
  //    -3 --> "=2*(N+1
  // Only "N" is a real generic parameter, but signals can be defined using any
  // of these as the width.
  protected static class Generics extends TreeMap<Integer, String> { }

  /////////// DONE
  // Return a list of this component's in/out ports.
	protected Generics getGenericParameters() { return new Generics(); }


  /////////// DONE
  // Return a list of wires used internally within this component.
	protected SignalList getWires() { return new SignalList(); }

  /////////// DONE
  // Return a list of registers used internally within this component. In
  // Verilog, wires and registers are different. In VHDL they are the same.
	protected SignalList getRegisters() { return new SignalList(); }


  /////////// DONE
  // A list of memory names and corresponding type names.
  protected static class MemoryList extends TreeMap<String, String> { }

  /////////// DONE
  // Return a list of memory blocks used internally within this component.
	protected MemoryList getMemories() { return new MemoryList(); }

  /////////// DONE
  // A list of memory names and their corresponding initialization data file
  // contents (e.g. for RAM components marked as "non-volatile").
  protected static class MemInitData extends TreeMap<String, Hdl> { }

  // Return a list of memory init data used internally within this component.
	protected MemInitData getMemInitData() { return new MemInitData(); }

  /////////// DONE
  // A list of type names and corresponding definitions, e.g. to use for
  // getMemories().
  protected static class TypeList extends TreeMap<String, String> { }

  /////////// DONE
  // Returns a list of types defined and used internally within this component.
	protected TypeList getTypes() { return new TypeList(); }

  /////////// DONE
  // An assignment, from each real generic parameter name, to a constant value.
  protected static class ParameterAssignments extends TreeMap<String, Integer> { }

  /////////// DONE
  // Returns assignments of real generic parameters to constant values
  // appropriate for the given component instantiation. Derived generic
  // parameters in the Generics list do not need assignments.
  protected ParameterAssignments getParameterAssignments(NetlistComponent ComponentInfo) {
    return new ParameterAssignments();
  }

  /////////// DONE
  // An assignment, from each port name, to a signal name or expression.
  protected static class PortAssignments extends TreeMap<String, String> { }

  /////////// DONE
  // Returns assignments of all input and output ports to signal names or
  // expressions appropriate for the given component instantiation.
  protected PortAssignments getPortAssignments(NetlistComponent ComponentInfo) {
    return new PortAssignments();
  }

  /////////// DONE
  // Generate HDL code for the component, i.e. the actual behavioral RTL code.
  protected void generateBehavior(Hdl out) { }

  /////////// DONE
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

  /////////// DONE
  // Returns a file opened for writing.
	protected File openFile(String rootDir, boolean isMif, boolean isEntity) {
    if (!rootDir.endsWith(File.separator))
      rootDir += File.separatorChar;
    if (!subdir.endsWith(File.separator) && !subdir.isEmpty())
      subdir += File.separatorChar;
    String path = rootDir + _lang.toLowerCase() + File.separatorChar + subdir;
    return FileWriter.GetFilePointer(path, hdlComponentName, isMif, isEntity, _err, _lang);
	}

  protected static class PortInfo {
    final String name;
    final String width; // generic param (but not an expression) or constant integer
    final int index;
    Boolean defaultValue; // only for inputs, null means port is required
    PortInfo(String n, String w, int i, Boolean v) {
      name = n;
      width = w;
      index = i;
      defaultValue = v;
    }
  }

  protected static class WireInfo {
    final String name;
    final String width; // generic param or expression or constant integer
    WireInfo(String n, String w) {
      name = n;
      width = w;
    }
  }

  protected static class ParameterInfo {
    final String name;
    final String type; // integer, natural, or positive
    final int width; // constant integer
    final int defaultValue;
    ParameterInfo(String n, String t, int w, int v) {
      name = n;
      type = t;
      width = w;
      defaultValue = v;
    }
  }

  protected final ArrayList<PortInfo> inPorts = new ArrayList<>();
  protected final ArrayList<PortInfo> inOutPorts = new ArrayList<>();
  protected final ArrayList<PortInfo> outPorts = new ArrayList<>();
  protected final ArrayList<WireInfo> wires = new ArrayList<>();
  protected final ArrayList<ParameterInfo> parameters = new ArrayList<>();


  // Return a suitable stem for naming instances of this component within a
  // circuit, so we can form names like "i_Add_1", "i_Add_2", etc., for
  // instances of BusAdder and BitAdder. These need not be unique: the
  // CircuitHDLGeneratorFactory will add suffixes to ensure all the instance
  // names within a circuit are unique.
	public final String getInstanceNamePrefix() { return hdlInstanceNamePrefix; }

  // Return an instance name by combining the prefix with a unique ID.
	public final String getInstanceName(long id) { return hdlInstanceNamePrefix + "_" + id; }

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
  public final IOComponentInformationContainer getHiddenPort() { return hiddenPort; }
  
  // From Keyboard
  // public SortedMap<String, String> GetPortMap(Netlist Nets,
  //     NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
  //   SortedMap<String, String> PortMap = new TreeMap<String, String>();
  //   String ZeroBit = (HDLType.equals(Settings.VHDL)) ? "'0'" : "1'b0";
  //   String SetBit = (HDLType.equals(Settings.VHDL)) ? "'1'" : "1'b1";
  //   String BracketOpen = (HDLType.equals(Settings.VHDL)) ? "(" : "[";
  //   String BracketClose = (HDLType.equals(Settings.VHDL)) ? ")" : "]";
  //   AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
  //   if (!ComponentInfo.EndIsConnected(Keyboard.CK)) {
  //     Reporter.AddSevereWarning("Component \"KBD\" in circuit \""
  //         + Nets.getCircuitName() + "\" has no clock connection");
  //     PortMap.put("GlobalClock", ZeroBit);
  //     PortMap.put("ClockEnable", ZeroBit);
  //   } else {
  //     String ClockNetName = GetClockNetName(ComponentInfo, Keyboard.CK, Nets);
  //     if (ClockNetName.isEmpty()) {
  //       Reporter.AddSevereWarning("Component \"KBD\" in circuit \""
  //           + Nets.getCircuitName()
  //           + "\" has a none-clock-component forced clock!\n"
  //           + "        Functional differences between Logisim simulation and hardware can be expected!");
  //       PortMap.putAll(GetNetMap("GlobalClock", true, ComponentInfo,
  //             Tty.CK, Reporter, HDLType, Nets));
  //       PortMap.put("ClockEnable", SetBit);
  //     } else {
  //       int ClockBusIndex = ClockHDLGeneratorFactory.DerivedClockIndex;
  //       if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_RISING)
  //         ClockBusIndex = ClockHDLGeneratorFactory.PositiveEdgeTickIndex;
  //       else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING)
  //         ClockBusIndex = ClockHDLGeneratorFactory.InvertedDerivedClockIndex;
  //       PortMap.put("GlobalClock",
  //           ClockNetName
  //           + BracketOpen
  //           + Integer
  //           .toString(ClockHDLGeneratorFactory.GlobalClockIndex)
  //           + BracketClose);
  //       PortMap.put("ClockEnable",
  //           ClockNetName + BracketOpen
  //           + Integer.toString(ClockBusIndex)
  //           + BracketClose);
  //     }
  //   }
  //   String pin = LocalInOutBubbleBusname;
  //   int offset = ComponentInfo.GetLocalBubbleInOutStartId();
  //   int start = offset;
  //   int end = offset + 4 - 1;
  //   PortMap.put("ps2kbd", pin + "(" + end + " DOWNTO " + start + ")");
  //   return PortMap;
  // }
  
  // From Tty
  // @Override
  // public SortedMap<String, String> GetPortMap(Netlist Nets,
  //     NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
  //   SortedMap<String, String> PortMap = new TreeMap<String, String>();
  //   String ZeroBit = (HDLType.equals(Settings.VHDL)) ? "'0'" : "1'b0";
  //   String SetBit = (HDLType.equals(Settings.VHDL)) ? "'1'" : "1'b1";
  //   String BracketOpen = (HDLType.equals(Settings.VHDL)) ? "(" : "[";
  //   String BracketClose = (HDLType.equals(Settings.VHDL)) ? ")" : "]";
  //   AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
  //   if (!ComponentInfo.EndIsConnected(Tty.CK)) {
  //     Reporter.AddSevereWarning("Component \"TTY\" in circuit \""
  //         + Nets.getCircuitName() + "\" has no clock connection");
  //     PortMap.put("GlobalClock", ZeroBit);
  //     PortMap.put("ClockEnable", ZeroBit);
  //   } else {
  //     String ClockNetName = GetClockNetName(ComponentInfo, Tty.CK, Nets);
  //     if (ClockNetName.isEmpty()) {
  //       Reporter.AddSevereWarning("Component \"TTY\" in circuit \""
  //           + Nets.getCircuitName()
  //           + "\" has a none-clock-component forced clock!\n"
  //           + "        Functional differences between Logisim simulation and hardware can be expected!");
  //       PortMap.putAll(GetNetMap("GlobalClock", true, ComponentInfo,
  //             Tty.CK, Reporter, HDLType, Nets));
  //       PortMap.put("ClockEnable", SetBit);
  //     } else {
  //       int ClockBusIndex = ClockHDLGeneratorFactory.DerivedClockIndex;
  //       if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_RISING)
  //         ClockBusIndex = ClockHDLGeneratorFactory.PositiveEdgeTickIndex;
  //       else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING)
  //         ClockBusIndex = ClockHDLGeneratorFactory.InvertedDerivedClockIndex;
  //       PortMap.put("GlobalClock",
  //           ClockNetName
  //           + BracketOpen
  //           + Integer
  //           .toString(ClockHDLGeneratorFactory.GlobalClockIndex)
  //           + BracketClose);
  //       PortMap.put("ClockEnable",
  //           ClockNetName + BracketOpen
  //           + Integer.toString(ClockBusIndex)
  //           + BracketClose);
  //     }
  //   }
  //   PortMap.putAll(GetNetMap("Data", true, ComponentInfo, Tty.IN,
  //         Reporter, HDLType, Nets));
  //   PortMap.putAll(GetNetMap("Clear", true, ComponentInfo, Tty.CLR,
  //         Reporter, HDLType, Nets));
  //   PortMap.putAll(GetNetMap("Enable", true, ComponentInfo, Tty.WE,
  //         Reporter, HDLType, Nets));
  //   String pin = LocalInOutBubbleBusname;
  //   int offset = ComponentInfo.GetLocalBubbleInOutStartId();
  //   int start = offset;
  //   int end = offset + 12 - 1;
  //   PortMap.put("lcd_rs_rw_en_db_bl", pin + "(" + end + " DOWNTO " + start + ")");
  //   return PortMap;
  // }



  // From PortIO
//   @Override
//   public SortedMap<String, String> GetPortMap(Netlist Nets,
//       NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
//     String ComponentName = "PORTIO_" + ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.LABEL);
// 
//     SortedMap<String, String> PortMap = new TreeMap<String, String>();
//     for (InOutMap io : getPorts(ComponentInfo.GetComponent().getAttributeSet())) {
//       switch (io.type) {
//       case ALWAYSINPUT:
//         PortMap.putAll(GetNetMap(io.name, false, ComponentInfo, io.endNr, Reporter, HDLType, Nets));
//         break;
//       case TRISTATEINPUT_1:
//       case TRISTATEINPUT_N:
//         PortMap.putAll(GetNetMap(io.name, true, ComponentInfo, io.endNr, Reporter, HDLType, Nets));
//         break;
//       case ENABLE:
//         PortMap.putAll(GetNetMap(io.name, true, ComponentInfo, io.endNr, Reporter, HDLType, Nets));
//         break;
//       case OUTPUT:
//         PortMap.putAll(GetNetMap(io.name, false, ComponentInfo, io.endNr, Reporter, HDLType, Nets));
//         break;
//       case BUS:
//         String pin = LocalInOutBubbleBusname;
//         int offset = ComponentInfo.GetLocalBubbleInOutStartId();
//         int start = offset + io.start + io.busNr * 32;
//         int end = offset + io.end + io.busNr * 32;
//         if (io.size == 1)
//           PortMap.put(io.name, pin + "(" + end + ")");
//         else
//           PortMap.put(io.name, pin + "(" + end + " DOWNTO " + start + ")");
//         break;
//       }
//     }
//     return PortMap;
//   }

// HexDigit
//   @Override
//   public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
//     list.putAll(GetNetMap("Hex", true, info, HexDigit.HEX, err, lang, nets));
//     list.putAll(GetNetMap("DecimalPoint", true, info, HexDigit.DP, err, lang, nets));
//     String pin = LocalOutputBubbleBusname;
//     int b = info.GetLocalBubbleOutputStartId();
//     list.put("Segment_A", pin + "(" + (b+0) + ")");
//     list.put("Segment_B", pin + "(" + (b+1) + ")");
//     list.put("Segment_C", pin + "(" + (b+2) + ")");
//     list.put("Segment_D", pin + "(" + (b+3) + ")");
//     list.put("Segment_E", pin + "(" + (b+4) + ")");
//     list.put("Segment_F", pin + "(" + (b+5) + ")");
//     list.put("Segment_G", pin + "(" + (b+6) + ")");
//     list.put("Segment_DP", pin + "(" + (b+7) + ")");
//   }

}
