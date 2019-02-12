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

import com.bfh.logisim.designrulecheck.BubbleInformationContainer;
import com.bfh.logisim.designrulecheck.ConnectionEnd;
import com.bfh.logisim.designrulecheck.ConnectionPoint;
import com.bfh.logisim.designrulecheck.Net;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;

public class AbstractHDLGeneratorFactory extends HDLGeneratorFactory {

  public static class HDLCTX { // fixme - temporary hack
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
  
  protected AbstractHDLGeneratorFactory(String lang, FPGAReport err, Netlist nets, AttributeSet attrs, char vendor, String hdlComponentName, String hdlInstanceNamePrefix) {
    super(lang, err, nets, attrs, vendor, hdlComponentName, hdlInstanceNamePrefix);
  }

  protected AbstractHDLGeneratorFactory(HDLCTX ctx, String hdlComponentName, String hdlInstanceNamePrefix) {
    super(ctx.lang, ctx.err, ctx.nets, ctx.attrs, ctx.vendor, hdlComponentName, hdlInstanceNamePrefix);
  }

  /*
	public String GetBusNameContinues(NetlistComponent comp, int EndIndex,
			String HDLType, Netlist TheNets) {
		String Result;
		String BracketOpen = (HDLType.equals("VHDL")) ? "(" : "[";
		String BracketClose = (HDLType.equals("VHDL")) ? ")" : "]";
		String VectorLoopId = (HDLType.equals("VHDL")) ? " DOWNTO "
				: ":";
		if ((EndIndex < 0) || (EndIndex >= comp.NrOfEnds())) {
			return "";
		}
		ConnectionEnd ConnectionInformation = comp.getEnd(EndIndex);
		int NrOfBits = ConnectionInformation.NrOfBits();
		if (NrOfBits == 1) {
			return "";
		}
		if (!TheNets.IsContinuesBus(comp, EndIndex)) {
			return "";
		}
		Net ConnectedNet = ConnectionInformation.GetConnection((byte) 0)
				.GetParrentNet();
		Result = BusName
				+ Integer.toString(TheNets.GetNetId(ConnectedNet))
				+ BracketOpen
				+ Integer.toString(ConnectionInformation.GetConnection(
						(byte) (ConnectionInformation.NrOfBits() - 1))
						.GetParrentNetBitIndex())
				+ VectorLoopId
				+ Integer.toString(ConnectionInformation.GetConnection(
						(byte) (0)).GetParrentNetBitIndex()) + BracketClose;
		return Result;
	}
  */

  //    s/GetNetName(...)/nets.signalForEnd1(...)/
  //    s/GetNetMap(...)/nets.signalForEnd(...)/
  //    s/GetBusEntryName(...)/nets.signalForEndBit(...)/
  //    s/GetClockNetName/nets.clockForEnd(...)/

  // XXX - here - move this to netlist, or netlistcomponent?
  // Determine the HDL name corresponding to whichever Logisim wire is connected
  // to the given "end" (i.e. port) of this instance. If the end is not
  // connected to a wire, a fatal error is reported.
  // protected String getNetName(NetlistComponent comp, int endIdx) {
  //   return getNetNameOrElse(comp, endIdx, null /* no default */, _nets);
  // }

  // XXX - here - move this to netlist, or netlistcomponent?
  // Determine the HDL name corresponding to whichever Logisim wire is connected
  // to the given "end" (i.e. port) of this instance. If the end is not
  // connected to a wire, the given default value is returned instead, sized to
  // the appropriate width.
  // protected String getNetNameOrElse(NetlistComponent comp, int endIdx, boolean defaultValue) {
  //   return getNetNameOrElse(comp, endIdx, defaultValue, _nets);
  // }

  // XXX - here - move this to netlist, or netlistcomponent? Same as above, but
  // using the given Netlist instead of the usual Netlist (which is the one for
  // the circuit in which this component is embedded).

  // Determine the HDL name corresponding to whichever Logisim wire is connected
  // to the given "end" (i.e. port) of this component.
	// public String getNetName(NetlistComponent comp, int endIdx, Boolean defaultValue, Netlist nets) {
	// 	
  //   if (endIdx < 0 || endIdx >= comp.NrOfEnds()) {
  //     _err.AddFatalError("INTERNAL ERROR: Invalid end/port '%d' for component '%s'", endIdx, this);
  //     return "???";
  //   }

  //   ConnectionEnd end = comp.getEnd(endIdx);
  //   int n = end.NrOfBits();
  //   if (n != 1) {
  //     _err.AddFatalError("INTERNAL ERROR: Unexpected %d-bit end/port '%d' for component '%s'", n, endIdx, this);
  //     return "???";
  //   }

  //   Hdl hdl = new Hdl(_lang, _err);

  //   ConnectionPoint solderPoint = end.GetConnection((byte)0);
  //   Net net = solderPoint.GetParrentNet();
  //   if (net == null) { // unconnected net
  //     if (defaultValue == null) {
  //       _err.AddFatalError("INTERNAL ERROR: Unexpected unconnected net to end/port '%d' for component '%s'", endIdx, this);
  //       return "???";
  //     }
  //     return end.IsOutputEnd() ? hdl.unconnected : hdl.bit(defaultValue);
  //   } else if (net.BitWidth() == 1) { // connected to a single-bit net
  //     return "s_LOGISIM_NET_" + nets.GetNetId(net);
  //   } else { // connected to one bit of a multi-bit bus
  //     return String.format("S_LOGISIM_NET_%d"+hdl.idx,
  //         nets.GetNetId(net),
  //         solderPoint.GetParrentNetBitIndex());
  //   }
	// }

  // DONE
  // Generate and write all necessary HDL files for this component, using the
  // given root directory and hdlName. Depeding on the specific subclass
  // involved, this may include entity, architecture, and several memory init
  // files.
  public boolean writeHDLFiles(String rootDir, String hdlName) {
    HashMap<String, File> memInitFiles = writeMemInitFiles(rootDir, hdlName);
    return writeEntity(rootDir, hdlName)
        && writeArchitecture(rootDir, hdlName, memInitFiles);
  }

  // DONE
  // Generate and write all "memory init" files for this component, using the
  // given root directory and hdlName, returning the list of memory names and
  // corresponding memory init files.
  protected HashMap<String, File> writeMemInitFiles(String rootDir, String hdlName) {
    MemInitData memInits = getMemInitData();
    HashMap<String, File> files = new HashMap<>();
    for (String m : memInits.keySet()) {
      Hdl data = memInits.get(m);
      if (data.isEmpty()) {
        _err.AddFatalError("INTERNAL ERROR: Empty memory initialization file for memory '%s:%s'.",
            hdlName, memName);
        continue;
      }
      File f = openFile(rootDir, hdlName + "_" + memName, true, false);
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
  // given root directory, hdlName, and NVRAM initialization files, where:
  //   hdlName = getHDLNameWithinCircuit(containingCircuitName);
  protected boolean writeArchitecture(String rootDir,
			String hdlName, HashMap<String, File> memInitFiles) { // fixme: meminitfiles from where?
    Hdl hdl = getArchitecture(hdlName, memInitFiles);
		if (hdl == null || hdl.isEmpty()) {
			_err.AddFatalError("INTERNAL ERROR: Generated empty architecture for HDL `%s'.", hdlName);
			return false;
		}
		File f = openFile(rootDir, hdlName, false, false);
		if (f == null)
			return false;
		return FileWriter.WriteContents(f, hdl, _err);
	}

  /////////// DONE
  // Generate the full HDL code for the "architecture" file.
	protected Hdl getArchitecture(String hdlName, HashMap<String, File> memInitFiles) {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out, hdlName);

    SignalList inPorts = getInPorts();
    // fixme: if logisim circuits can do bidirectional ports, add support for in/out ports here
    SignalList outPorts = getOutPorts();
    Generics params = getGenericParameters();
		SignalList wires = getWires();
		SignalList registers = getRegisters();
		MemoryList mems = getMemories();
    TypeList types = getTypes();

		if (out.isVhdl) {

			out.stmt("architecture logisim_generated of " + hdlName + " is ");
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
      out.stmt("module %s(\t %s );", hdlName, String.join(",\n\t ", ports));

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
  // the given root directory and hdlName, where:
  //   hdlName = getHDLNameWithinCircuit(containingCircuitName);
  protected boolean writeEntity(String rootDir, String hdlName) {
    if (!_lang.equals("VHDL"))
      return true;
    Hdl hdl = getVhdlEntity(hdlName);
		if (hdl == null || hdl.isEmpty()) {
			_err.AddFatalError("INTERNAL ERROR: Generated empty entity for VHDL `%s'.", hdlName);
			return false;
		}
		File f = openFile(rootDir, hdlName, false, true);
		if (f == null)
			return false;
		return FileWriter.WriteContents(f, hdl, _err);
	}

  /////////// DONE
	protected Hdl getVhdlEntity(String hdlName) {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out, hdlName);
    generateVhdlLibraries(out, IEEE_LOGIC, IEEE_NUMERIC);
    generateVhdlBlackBox(out, hdlName, true);
    return out;
	}

  /////////// DONE
	protected void generateComponentInstantiation(Hdl out, String hdlName) {
		if (_lang.equals("VHDL"))
      generateVhdlBlackBox(out, hdlName, false);
	}

  /////////// DONE
	private void generateVhdlBlackBox(Hdl out, String hdlName, boolean isEntity) {
    SignalList inPorts = getInPorts();
    SignalList inOutPorts = getInOutPorts();
    SignalList outPorts = getOutPorts();
    Generics params = getGenericParameters();

    if (isEntity)
      out.stmt("entity %s is", hdlName);
    else
      out.stmt("component %s", hdlName);

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
      out.stmt("end %s;", hdlName);
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
	protected void generateComponentMap(Hdl out, long id,
      NetlistComponent comp, String containingCircuitName) {

		String hdlName = getHDLNameWithinCircuit(containingCircuitName);
		String instName = getInstanceName(id);

    ParameterAssignments paramVals = getParameterAssignments(comp);
    PortAssignments portVals = getPortAssignments(comp);

    ArrayList<String> generics = new ArrayList<>();
    ArrayList<String> values = new ArrayList<>();

		if (out.isVhdl) {

      out.stmt("%s : %s", instName, hdlName);
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
        out.stmt("%s %s;", hdlName, instName);
      else if (generics.isEmpty())
        out.stmt("%s %s (\t %s );", hdlName, instName, v);
      else if (values.isEmpty())
        out.stmt("%s #(\t %s ) %s;", hdlName, g, instName);
      else
        out.stmt("%s #(\t %s ) %s ( %s );", hdlName, g, instName, v);
		}

    out.stmt();
	}



  /////////// DONE
	protected void generateFileHeader(Hdl out, String hdlName) {
    out.comment(" === Logisim-Evolution Holy Cross Edition auto-generated %s code", _lang);
    out.comment(" === Project: %s", _projectName);
    out.comment(" === Component: %s", hdlName);
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
	protected static void generateVhdlLibraries(Hdl out, String ...libs) {
    if (libs.length == 0)
      return;
    out.stmt("library ieee;");
    for (String lib : libs)
      out.stmt("use ieee.%s.all;", lib);
    out.stmt();
  }

  /////////// DONE
  // Returns a file opened for writing.
	protected File openFile(String rootDir, String hdlName, boolean isMif, boolean isEntity) {
    if (!rootDir.endsWith(File.separator))
      rootDir += File.separatorChar;
    String subdir = subdir();
    if (!subdir.endsWith(File.separator) && !subdir.isEmpty())
      subdir += File.separatorChar;
    String path = rootDir + _lang.toLowerCase() + File.separatorChar + subdir;
    return FileWriter.GetFilePointer(path, hdlName, isMif, isEntity, _err, _lang);
	}

  /////////// DONE
  // Returns a suitable subdirectory for storing HDL files.
	protected abstract String subdir();

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
    final int width; // constant integer
    WireInfo(String n, int w) {
      name = n;
      width = w;
    }
  }

  protected final ArrayList<PortInfo> inPorts = new ArrayList<>();
  // protected final ArrayList<PortInfo> inOutPorts = new ArrayList<>();
  protected final ArrayList<PortInfo> outPorts = new ArrayList<>();
  protected final ArrayList<WireInfo> wires = new ArrayList<>();
  protected final ArrayList<String> parameters = new ArrayList<>();

}
