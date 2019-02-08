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
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.bfh.logisim.designrulecheck.BubbleInformationContainer;
import com.bfh.logisim.designrulecheck.ConnectionEnd;
import com.bfh.logisim.designrulecheck.ConnectionPoint;
import com.bfh.logisim.designrulecheck.Net;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.settings.Settings;
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
  
  protected AbstractHDLGeneratorFactory(String lang, FPGAReport err, Netlist nets, AttributeSet attrs, char vendor) {
    super(lang, err, nets, attrs, vendor);
  }

  protected AbstractHDLGeneratorFactory(HDLCTX ctx) {
    super(ctx.lang, ctx.err, ctx.nets, ctx.attrs, ctx.vendor);
  }

  // protected AbstractHDLGeneratorFactory(String lang, FPGAReport err, AttributeSet attrs) {
  //   super(lang, err, null /* nets, fixme */, attrs);
  // }
  // 
  // public void initHDLGen(Netlist nets) { // fixme
  //   if (_nets != null) throw new IllegalStateException();
  //   _nets = nets;
  // }
	
	public static File WriteMemInitFile(String TargetDirectory,
			ArrayList<String> Contents, String ComponentName,
			String MemName,
			FPGAReport Reporter, String HDLType) {
		if (Contents.isEmpty()) {
			Reporter.AddFatalError("INTERNAL ERROR: Empty memory initialization file for memory '"
					+ ComponentName + ":"+MemName+"' received!");
			return null;
		}
		File OutFile = FileWriter.GetFilePointer(TargetDirectory,
				ComponentName+"_"+MemName, true, false, Reporter, HDLType);
		if (OutFile == null) {
			return null;
		}
		if (!FileWriter.WriteContents(OutFile, Contents, Reporter)) {
			return null;
		}
		return OutFile;
	}

	public static boolean WriteArchitecture(String TargetDirectory,
			ArrayList<String> Contents, String ComponentName,
			FPGAReport Reporter, String HDLType) {
		if (Contents.isEmpty()) {
			Reporter.AddFatalError("INTERNAL ERROR: Empty behavior description for Component '"
					+ ComponentName + "' received!");
			return false;
		}
		File OutFile = FileWriter.GetFilePointer(TargetDirectory,
				ComponentName, false, false, Reporter, HDLType);
		if (OutFile == null) {
			return false;
		}
		return FileWriter.WriteContents(OutFile, Contents, Reporter);
	}

	public static boolean WriteEntity(String TargetDirectory,
			ArrayList<String> Contents, String ComponentName,
			FPGAReport Reporter, String HDLType) {
		if (HDLType.endsWith(Settings.VERILOG)) {
			return true;
		}
		if (Contents == null || Contents.isEmpty()) {
			Reporter.AddFatalError("INTERNAL ERROR: Empty entity description received!");
			return false;
		}
		File OutFile = FileWriter.GetFilePointer(TargetDirectory,
				ComponentName, false, true, Reporter, HDLType);
		if (OutFile == null) {
			return false;
		}
		return FileWriter.WriteContents(OutFile, Contents, Reporter);
	}

	/* Here the common predefined methods are defined */

	public ArrayList<String> GetArchitecture(/*Netlist TheNetlist,*/
			/*AttributeSet attrs,*/ Map<String, File> memInitFiles,
			String ComponentName /*, FPGAReport Reporter, String HDLType */) {
    if (_nets == null) throw new IllegalStateException();
    return GetArchitectureWithNetlist(_nets, /*attrs,*/ memInitFiles, ComponentName /*, Reporter, HDLType*/);
  }

	protected ArrayList<String> GetArchitectureWithNetlist(Netlist TheNetlist,
			/*AttributeSet attrs,*/ Map<String, File> memInitFiles,
			String ComponentName /*, FPGAReport Reporter, String HDLType*/) {
		ArrayList<String> Contents = new ArrayList<String>();
		Map<String, Integer> InputsList = GetInputList(TheNetlist, _attrs); // For
																			// verilog
		// Map<String, Integer> InOutsList = GetInOutList(TheNetlist, _attrs);
		// //For verilog
		Map<String, Integer> OutputsList = GetOutputList(TheNetlist, _attrs); // For
																				// verilog
		Map<Integer, String> ParameterList = GetParameterList(_attrs);
		Map<String, Integer> WireList = GetWireList(_attrs, TheNetlist);
		Map<String, Integer> RegList = GetRegList(_attrs, _lang);
		Map<String, Integer> MemList = GetMemList(_attrs, _lang);
		StringBuffer OneLine = new StringBuffer();
		Contents.addAll(FileWriter.getGenerateRemark(ComponentName, _lang,
				TheNetlist.projName()));
		if (_lang.equals(Settings.VHDL)) {
			ArrayList<String> libs = GetExtraLibraries();
			if (!libs.isEmpty()) {
				Contents.addAll(libs);
				Contents.add("");
			}
			Contents.add("ARCHITECTURE PlatformIndependent OF " + ComponentName + " IS ");
			Contents.add("");
			int NrOfTypes = GetNrOfTypes(TheNetlist, _attrs, _lang);
			if (NrOfTypes > 0) {
        Hdl out = new Hdl(_lang, _err);
        out.comment("definitions for private types");
				Contents.addAll(out);
				for (String ThisType : GetTypeDefinitions(TheNetlist, _attrs, _lang)) {
					Contents.add("   " + ThisType + ";");
				}
				Contents.add("");
			}
			ArrayList<String> Comps = GetComponentDeclarationSection( TheNetlist, _attrs);
			if (!Comps.isEmpty()) {
        Hdl out = new Hdl(_lang, _err);
        out.comment("definitions for components");
				Contents.addAll(out);
				Contents.addAll(Comps);
				Contents.add("");
			}
      Hdl out = new Hdl(_lang, _err);
      out.comment("definitions for signals");
			Contents.addAll(out);
			for (String Wire : WireList.keySet()) {
				OneLine.append(Wire);
				while (OneLine.length() < SallignmentSize) {
					OneLine.append(" ");
				}
				OneLine.append(": std_logic");
				if (WireList.get(Wire) == 1) {
					OneLine.append(";");
				} else {
					OneLine.append("_vector( ");
					if (WireList.get(Wire) < 0) {
						if (!ParameterList.containsKey(WireList.get(Wire))) {
							_err.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
							Contents.clear();
							return Contents;
						}
						OneLine.append("("
								+ ParameterList.get(WireList.get(Wire)) + "-1)");
					} else {
						if (WireList.get(Wire) == 0) {
							OneLine.append("0");
						} else {
							OneLine.append(Integer.toString(WireList.get(Wire) - 1));
						}
					}
					OneLine.append(" DOWNTO 0 );");
				}
				Contents.add("   SIGNAL " + OneLine.toString());
				OneLine.setLength(0);
			}
			for (String Reg : RegList.keySet()) {
				OneLine.append(Reg);
				while (OneLine.length() < SallignmentSize) {
					OneLine.append(" ");
				}
				OneLine.append(": std_logic");
				if (RegList.get(Reg) == 1) {
					OneLine.append(";");
				} else {
					OneLine.append("_vector( ");
					if (RegList.get(Reg) < 0) {
						if (!ParameterList.containsKey(RegList.get(Reg))) {
							_err.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
							Contents.clear();
							return Contents;
						}
						OneLine.append("("
								+ ParameterList.get(RegList.get(Reg)) + "-1)");
					} else {
						if (RegList.get(Reg) == 0) {
							OneLine.append("0");
						} else {
							OneLine.append(Integer.toString(RegList.get(Reg) - 1));
						}
					}
					OneLine.append(" DOWNTO 0 );");
				}
				Contents.add("   SIGNAL " + OneLine.toString());
				OneLine.setLength(0);
			}
			if (memInitFiles != null && memInitFiles.size() > 0) {
				Contents.add("    ATTRIBUTE ram_init_file : string;");
			}
			for (String Mem : MemList.keySet()) {
				OneLine.append(Mem);
				while (OneLine.length() < SallignmentSize) {
					OneLine.append(" ");
				}
				OneLine.append(": ");
				OneLine.append(GetType(MemList.get(Mem)));
				OneLine.append(";");
				Contents.add("    SIGNAL " + OneLine.toString());
				OneLine.setLength(0);
				if (memInitFiles != null) {
					String mif = memInitFiles.get(Mem).getPath();
					Contents.add("    ATTRIBUTE ram_init_file of " + Mem + " :");
					Contents.add("      SIGNAL is \"" + mif + "\";");
				}
			}
			Contents.add("");
			Contents.add("BEGIN");
			Contents.addAll(GetModuleFunctionality(TheNetlist, _attrs, _err, _lang));
			Contents.add("END PlatformIndependent;");
		} else {
			String Preamble = "module " + ComponentName + "( ";
			StringBuffer Indenting = new StringBuffer();
			while (Indenting.length() < Preamble.length()) {
				Indenting.append(" ");
			}
			if (InputsList.isEmpty() && OutputsList.isEmpty()) {
				Contents.add(Preamble + " );");
			} else {
				StringBuffer ThisLine = new StringBuffer();
				for (String inp : InputsList.keySet()) {
					if (ThisLine.length() == 0) {
						ThisLine.append(Preamble.toString() + inp);
					} else {
						Contents.add(ThisLine.toString() + ",");
						ThisLine.setLength(0);
						ThisLine.append(Indenting.toString() + inp);
					}
				}
				for (String outp : OutputsList.keySet()) {
					if (ThisLine.length() == 0) {
						ThisLine.append(Preamble.toString() + outp);
					} else {
						Contents.add(ThisLine.toString() + ",");
						ThisLine.setLength(0);
						ThisLine.append(Indenting.toString() + outp);
					}
				}
				if (ThisLine.length() != 0) {
					Contents.add(ThisLine.toString() + ");");
				} else {
					_err.AddError("Internale Error in Verilog Architecture generation!");
				}
			}
			if (!ParameterList.isEmpty()) {
				Contents.add("");
        Hdl out = new Hdl(_lang, _err);
        out.comment("definitions for module parameters (with dummy values)");
				Contents.addAll(out);
				for (int param : ParameterList.keySet()) {
					Contents.add("   parameter "
							+ ParameterList.get(param).toString() + " = 1;");
				}
				Contents.add("");
			}
			boolean firstline = true;
			int nr_of_bits;
			for (String inp : InputsList.keySet()) {
				OneLine.setLength(0);
				OneLine.append("   input");
				nr_of_bits = InputsList.get(inp);
				if (nr_of_bits < 0) {
					/* we have a parameterized array */
					if (!ParameterList.containsKey(nr_of_bits)) {
						_err.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
						Contents.clear();
						return Contents;
					}
					OneLine.append("["
							+ ParameterList.get(nr_of_bits).toString()
							+ "-1:0]");
				} else {
					if (nr_of_bits > 1) {
						OneLine.append("[" + Integer.toString(nr_of_bits - 1)
								+ ":0]");
					} else {
						if (nr_of_bits == 0) {
							OneLine.append("[0:0]");
						}
					}
				}
				OneLine.append("  " + inp + ";");
				if (firstline) {
					firstline = false;
					Contents.add("");
          Hdl out = new Hdl(_lang, _err);
          out.comment("definitions for inputs");
					Contents.addAll(out);
				}
				Contents.add(OneLine.toString());
			}
			firstline = true;
			for (String outp : OutputsList.keySet()) {
				OneLine.setLength(0);
				OneLine.append("   output");
				nr_of_bits = OutputsList.get(outp);
				if (nr_of_bits < 0) {
					/* we have a parameterized array */
					if (!ParameterList.containsKey(nr_of_bits)) {
						_err.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
						Contents.clear();
						return Contents;
					}
					OneLine.append("["
							+ ParameterList.get(nr_of_bits).toString()
							+ "-1:0]");
				} else {
					if (nr_of_bits > 1) {
						OneLine.append("[" + Integer.toString(nr_of_bits - 1)
								+ ":0]");
					} else {
						if (nr_of_bits == 0) {
							OneLine.append("[0:0]");
						}
					}
				}
				OneLine.append(" " + outp + ";");
				if (firstline) {
					firstline = false;
					Contents.add("");
          Hdl out = new Hdl(_lang, _err);
          out.comment("definitions for outputs");
					Contents.addAll(out);
				}
				Contents.add(OneLine.toString());
			}
			firstline = true;
			for (String wire : WireList.keySet()) {
				OneLine.setLength(0);
				OneLine.append("   wire");
				nr_of_bits = WireList.get(wire);
				if (nr_of_bits < 0) {
					/* we have a parameterized array */
					if (!ParameterList.containsKey(nr_of_bits)) {
						_err.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
						Contents.clear();
						return Contents;
					}
					OneLine.append("["
							+ ParameterList.get(nr_of_bits).toString()
							+ "-1:0]");
				} else {
					if (nr_of_bits > 1) {
						OneLine.append("[" + Integer.toString(nr_of_bits - 1)
								+ ":0]");
					} else {
						if (nr_of_bits == 0) {
							OneLine.append("[0:0]");
						}
					}
				}
				OneLine.append(" " + wire + ";");
				if (firstline) {
					firstline = false;
					Contents.add("");
          Hdl out = new Hdl(_lang, _err);
          out.comment("definitions for internal wires");
					Contents.addAll(out);
				}
				Contents.add(OneLine.toString());
			}
			for (String reg : RegList.keySet()) {
				OneLine.setLength(0);
				OneLine.append("   reg");
				nr_of_bits = RegList.get(reg);
				if (nr_of_bits < 0) {
					/* we have a parameterized array */
					if (!ParameterList.containsKey(nr_of_bits)) {
						_err.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
						Contents.clear();
						return Contents;
					}
					OneLine.append("["
							+ ParameterList.get(nr_of_bits).toString()
							+ "-1:0]");
				} else {
					if (nr_of_bits > 1) {
						OneLine.append("[" + Integer.toString(nr_of_bits - 1)
								+ ":0]");
					} else {
						if (nr_of_bits == 0) {
							OneLine.append("[0:0]");
						}
					}
				}
				OneLine.append(" " + reg + ";");
				if (firstline) {
					firstline = false;
					Contents.add("");
          Hdl out = new Hdl(_lang, _err);
          out.comment("definitions for internal registers");
					Contents.addAll(out);
				}
				Contents.add(OneLine.toString());
			}
			/* TODO: Add memlist */
			if (!firstline) {
				Contents.add("");
			}
			Contents.addAll(GetModuleFunctionality(TheNetlist, _attrs, _err, _lang));
			Contents.add("");
			Contents.add("endmodule");
		}
		return Contents;
	}

	public String GetBusEntryName(NetlistComponent comp, int EndIndex,
			boolean FloatingNetTiedToGround, int bitindex, String HDLType,
			Netlist TheNets) {
    Hdl out = new Hdl(HDLType, null);
		StringBuffer Contents = new StringBuffer();
		String BracketOpen = (HDLType.equals(Settings.VHDL)) ? "(" : "[";
		String BracketClose = (HDLType.equals(Settings.VHDL)) ? ")" : "]";
		if ((EndIndex >= 0) && (EndIndex < comp.NrOfEnds())) {
			ConnectionEnd ThisEnd = comp.getEnd(EndIndex);
			boolean IsOutput = ThisEnd.IsOutputEnd();
			int NrOfBits = ThisEnd.NrOfBits();
			if ((NrOfBits > 1) && (bitindex >= 0) && (bitindex < NrOfBits)) {
				if (ThisEnd.GetConnection((byte) bitindex).GetParrentNet() == null) {
					/* The net is not connected */
					if (IsOutput) {
						if (HDLType.equals(Settings.VHDL)) {
							Contents.append("OPEN");
						} else {
							Contents.append("'bz");
						}
					} else {
						Contents.append(out.bit(!FloatingNetTiedToGround));
					}
				} else {
					Net ConnectedNet = ThisEnd.GetConnection((byte) bitindex)
							.GetParrentNet();
					int ConnectedNetBitIndex = ThisEnd.GetConnection(
							(byte) bitindex).GetParrentNetBitIndex();
					if (!ConnectedNet.isBus()) {
						Contents.append(NetName
								+ Integer.toString(TheNets
										.GetNetId(ConnectedNet)));
					} else {
						Contents.append(BusName
								+ Integer.toString(TheNets
										.GetNetId(ConnectedNet)) + BracketOpen
								+ Integer.toString(ConnectedNetBitIndex)
								+ BracketClose);
					}
				}
			}
		}
		return Contents.toString();
	}

	public String GetBusNameContinues(NetlistComponent comp, int EndIndex,
			String HDLType, Netlist TheNets) {
		String Result;
		String BracketOpen = (HDLType.equals(Settings.VHDL)) ? "(" : "[";
		String BracketClose = (HDLType.equals(Settings.VHDL)) ? ")" : "]";
		String VectorLoopId = (HDLType.equals(Settings.VHDL)) ? " DOWNTO "
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

	public String GetClockNetName(NetlistComponent comp, int EndIndex,
			Netlist TheNets) {
		StringBuffer Contents = new StringBuffer();
		if ((TheNets.GetCurrentHierarchyLevel() != null) && (EndIndex >= 0)
				&& (EndIndex < comp.NrOfEnds())) {
			ConnectionEnd EndData = comp.getEnd(EndIndex);
			if (EndData.NrOfBits() == 1) {
				Net ConnectedNet = EndData.GetConnection((byte) 0)
						.GetParrentNet();
				byte ConnectedNetBitIndex = EndData.GetConnection((byte) 0)
						.GetParrentNetBitIndex();
				/* Here we search for a clock net Match */
				int clocksourceid = TheNets.GetClockSourceId(
						TheNets.GetCurrentHierarchyLevel(), ConnectedNet,
						ConnectedNetBitIndex);
				if (clocksourceid >= 0) {
					Contents.append(ClockTreeName
							+ Integer.toString(clocksourceid));
				}
			}
		}
		return Contents.toString();
	}

	public ArrayList<String> GetComponentDeclarationSection(Netlist TheNetlist,
			AttributeSet attrs) {
		/*
		 * This method returns all the component definitions used as component
		 * in the circuit. This method is only called in case of VHDL-code
		 * generation.
		 */
		ArrayList<String> Components = new ArrayList<String>();
		return Components;
	}

	public ArrayList<String> GetComponentInstantiation(/*Netlist TheNetlist,*/
			/*AttributeSet attrs,*/ String ComponentName/*, String HDLType*/) {
    if (_nets == null) throw new IllegalStateException();
		ArrayList<String> Contents = new ArrayList<String>();
		if (_lang.equals(Settings.VHDL)) {
			Contents.addAll(GetVHDLBlackBox(_nets, _attrs, ComponentName, false));
		}
		return Contents;
	}

	public ArrayList<String> GetComponentMap(/*Netlist Nets,*/ Long ComponentId,
			NetlistComponent ComponentInfo/* , FPGAReport Reporter,*/
			/* String CircuitName */ /*, String HDLType*/) {
    if (_nets == null) throw new IllegalStateException();
		ArrayList<String> Contents = new ArrayList<String>();
		Map<String, Integer> ParameterMap = GetParameterMap(_nets, ComponentInfo, _err);
		Map<String, String> PortMap = GetPortMap(_nets, ComponentInfo, _err, _lang);
		String CompName = (ComponentInfo == null)
        ? this.getComponentStringIdentifier()
        : ComponentInfo.GetComponent().getFactory().getHDLName(ComponentInfo.GetComponent().getAttributeSet());
		String ThisInstanceIdentifier = GetInstanceIdentifier(ComponentInfo,
				ComponentId);
		StringBuffer OneLine = new StringBuffer();
		int TabLength;
		boolean first;
		if (_lang.equals(Settings.VHDL)) {
			Contents.add("   " + ThisInstanceIdentifier + " : " + CompName);
			if (!ParameterMap.isEmpty()) {
				OneLine.append("      GENERIC MAP ( ");
				TabLength = OneLine.length();
				first = true;
				for (String generic : ParameterMap.keySet()) {
					if (!first) {
						OneLine.append(",");
						Contents.add(OneLine.toString());
						OneLine.setLength(0);
						while (OneLine.length() < TabLength) {
							OneLine.append(" ");
						}
					} else {
						first = false;
					}
					OneLine.append(generic);
					for (int i = generic.length(); i < SallignmentSize; i++) {
						OneLine.append(" ");
					}
					OneLine.append("=> "
							+ Integer.toString(ParameterMap.get(generic)));
				}
				OneLine.append(")");
				Contents.add(OneLine.toString());
				OneLine.setLength(0);
			}
			if (!PortMap.isEmpty()) {
				OneLine.append("      PORT MAP ( ");
				TabLength = OneLine.length();
				first = true;
				for (String port : PortMap.keySet()) {
					if (!first) {
						OneLine.append(",");
						Contents.add(OneLine.toString());
						OneLine.setLength(0);
						while (OneLine.length() < TabLength) {
							OneLine.append(" ");
						}
					} else {
						first = false;
					}
					OneLine.append(port);
					for (int i = port.length(); i < SallignmentSize; i++) {
						OneLine.append(" ");
					}
					OneLine.append("=> " + PortMap.get(port));
				}
				OneLine.append(");");
				Contents.add(OneLine.toString());
				OneLine.setLength(0);
			}
			Contents.add("");
		} else {
			OneLine.append("   " + CompName);
			if (!ParameterMap.isEmpty()) {
				OneLine.append(" #(");
				TabLength = OneLine.length();
				first = true;
				for (String parameter : ParameterMap.keySet()) {
					if (!first) {
						OneLine.append(",");
						Contents.add(OneLine.toString());
						OneLine.setLength(0);
						while (OneLine.length() < TabLength) {
							OneLine.append(" ");
						}
					} else {
						first = false;
					}
					OneLine.append("." + parameter + "("
							+ Integer.toString(ParameterMap.get(parameter))
							+ ")");
				}
				OneLine.append(")");
				Contents.add(OneLine.toString());
				OneLine.setLength(0);
			}
			OneLine.append("      " + ThisInstanceIdentifier + " (");
			if (!PortMap.isEmpty()) {
				TabLength = OneLine.length();
				first = true;
				for (String port : PortMap.keySet()) {
					if (!first) {
						OneLine.append(",");
						Contents.add(OneLine.toString());
						OneLine.setLength(0);
						while (OneLine.length() < TabLength) {
							OneLine.append(" ");
						}
					} else {
						first = false;
					}
					OneLine.append("." + port + "(");
					String MappedSignal = PortMap.get(port);
					if (!MappedSignal.contains(",")) {
						OneLine.append(MappedSignal);
					} else {
						String[] VectorList = MappedSignal.split(",");
						OneLine.append("{");
						int TabSize = OneLine.length();
						for (int vectorentries = 0; vectorentries < VectorList.length; vectorentries++) {
							String Entry = VectorList[vectorentries];
							if (Entry.contains("{")) {
								Entry.replaceAll("{", "");
							}
							if (Entry.contains("}")) {
								Entry.replaceAll("}", "");
							}
							OneLine.append(Entry);
							if (vectorentries < VectorList.length - 1) {
								Contents.add(OneLine.toString() + ",");
								OneLine.setLength(0);
								while (OneLine.length() < TabSize) {
									OneLine.append(" ");
								}
							} else {
								OneLine.append("}");
							}
						}
					}
					OneLine.append(")");
				}
			}
			OneLine.append(");");
			Contents.add(OneLine.toString());
			Contents.add("");
		}
		return Contents;
	}

	public String getComponentStringIdentifier() {
		return "AComponent";
	}

	@Override
	public ArrayList<String> GetEntity(/*Netlist TheNetlist,*/ /*AttributeSet attrs,*/
			String ComponentName /*, FPGAReport Reporter, String HDLType*/) {
    if (_nets == null) throw new IllegalStateException();
    return GetEntityWithNetlist(_nets, /*attrs,*/ ComponentName);
  }

	protected ArrayList<String> GetEntityWithNetlist(Netlist nets, /*AttributeSet attrs,*/ String ComponentName) {
		ArrayList<String> Contents = new ArrayList<String>();
		if (_lang.equals(Settings.VHDL)) {
			Contents.addAll(FileWriter.getGenerateRemark(ComponentName,
					Settings.VHDL, nets.projName()));
			Contents.addAll(FileWriter.getExtendedLibrary());
			Contents.addAll(GetVHDLBlackBox(nets, _attrs, ComponentName, true));
		}
		return Contents;
	}

	/* Here all public entries for HDL generation are defined */
	public ArrayList<String> GetExtraLibraries() {
		/*
		 * this method returns extra VHDL libraries required for simulation
		 * and/or synthesis
		 */
		return new ArrayList<String>();
	}

  // CircuitHDLGeneratorFactory calls this for NormalComponents, when nets
  // defined, if IsOnlyInlined returns true.
	public ArrayList<String> GetInlinedCode3(/*Netlist Nets,*/ Long ComponentId,
			NetlistComponent ComponentInfo, /*FPGAReport Reporter, */
			String  CircuitName /*, String HDLType*/) {
		ArrayList<String> Contents = new ArrayList<String>();
		return Contents;
	}

  // ToplevelHDLGeneratorFactory calls this for components that are not Pin,
  // PortIO, Tty, or Keyboard.
	public ArrayList<String> GetInlinedCode2(/*String HDLType,*/
			ArrayList<String> ComponentIdentifier, /*FPGAReport Reporter,*/
			MappableResourcesContainer MapInfo) {
		ArrayList<String> Contents = new ArrayList<String>();
		String Preamble = (_lang.equals(Settings.VHDL)) ? "" : "assign ";
		String AssignOperator = (_lang.equals(Settings.VHDL)) ? " <= " : " = ";
		String OpenBracket = (_lang.equals(Settings.VHDL)) ? "(" : "[";
		String CloseBracket = (_lang.equals(Settings.VHDL)) ? ")" : "]";
		String Inversion = (_lang.equals(Settings.VHDL)) ? "NOT " : "~";
		StringBuffer Temp = new StringBuffer();
		NetlistComponent comp = MapInfo.GetComponent(ComponentIdentifier);
		if (comp == null) {
			_err.AddFatalError("Component not found, bizar");
			return Contents;
		}
		ArrayList<String> bla = new ArrayList<String>();
		bla.addAll(ComponentIdentifier);
		bla.remove(0);
		BubbleInformationContainer BubbleInfo = comp.GetGlobalBubbleId(bla);
		if (BubbleInfo == null) {
			_err.AddFatalError("Component has no bubble information, bizar! " + bla.toString());
			return Contents;
		}
		/* The button is simple as it has only 1 pin */
		/*
		 * The bubble information presents the internal pin location, we now
		 * need to know the input pin index
		 */
		ArrayList<String> MyMaps = MapInfo.GetMapNamesList(ComponentIdentifier);
		if (MyMaps == null) {
			_err.AddFatalError("Component has no map information, bizar! "
					+ ComponentIdentifier.toString());
			return Contents;
		}
		int BubbleOffset = 0;
		for (int MapOffset = 0; MapOffset < MyMaps.size(); MapOffset++) {
			String map = MyMaps.get(MapOffset);
			int InputId = MapInfo.GetFPGAInputPinId(map);
			int OutputId = MapInfo.GetFPGAOutputPinId(map);
			int NrOfPins = MapInfo.GetNrOfPins(map);
			boolean Invert = MapInfo.RequiresToplevelInversion(
					ComponentIdentifier, map);
			for (int PinId = 0; PinId < NrOfPins; PinId++) {
				Temp.setLength(0);
				Temp.append("   " + Preamble);
				if (InputId >= 0
						&& ((BubbleInfo.GetInputStartIndex() + BubbleOffset) <= BubbleInfo
								.GetInputEndIndex())) {
					Temp.append("s_"
							+ HDLGeneratorFactory.LocalInputBubbleBusname
							+ OpenBracket);
					Temp.append(BubbleInfo.GetInputStartIndex() + BubbleOffset);
					BubbleOffset++;
					Temp.append(CloseBracket + AssignOperator);
					if (Invert) {
						Temp.append(Inversion);
					}
					Temp.append(HDLGeneratorFactory.FPGAInputPinName);
					Temp.append("_" + Integer.toString(InputId + PinId) + ";");
					Contents.add(Temp.toString());
				}
				Temp.setLength(0);
				Temp.append("   " + Preamble);
				if (OutputId >= 0
						&& ((BubbleInfo.GetOutputStartIndex() + BubbleOffset) <= BubbleInfo
								.GetOutputEndIndex())) {
					Temp.append(HDLGeneratorFactory.FPGAOutputPinName);
					Temp.append("_" + Integer.toString(OutputId + PinId)
							+ AssignOperator);
					if (Invert) {
						Temp.append(Inversion);
					}
					Temp.append("s_"
							+ HDLGeneratorFactory.LocalOutputBubbleBusname
							+ OpenBracket);
					Temp.append(BubbleInfo.GetOutputStartIndex() + BubbleOffset);
					BubbleOffset++;
					Temp.append(CloseBracket + ";");
					Contents.add(Temp.toString());
				}
			}
		}
		return Contents;
	}

	public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist,
			AttributeSet attrs) {
		/*
		 * This method returns a map list of all the INOUT of a black-box. The
		 * String Parameter represents the Name, and the Integer parameter
		 * represents: >0 The number of bits of the signal <0 A parameterized
		 * vector of bits where the value is the "key" of the parameter map 0 Is
		 * an invalid value and must not be used
		 */
		SortedMap<String, Integer> InOuts = new TreeMap<String, Integer>();
		return InOuts;
	}
	
  public void inputs(SortedMap<String, Integer> list, Netlist TheNetlist, AttributeSet attrs) { }

	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
		/*
		 * This method returns a map list of all the inputs of a black-box. The
		 * String Parameter represents the Name, and the Integer parameter
		 * represents: >0 The number of bits of the signal <0 A parameterized
		 * vector of bits where the value is the "key" of the parameter map 0 Is
		 * an invalid value and must not be used
		 */
		SortedMap<String, Integer> Inputs = new TreeMap<>();
    inputs(Inputs, TheNetlist, attrs);
		return Inputs;
	}

	public String GetInstanceIdentifier(NetlistComponent ComponentInfo,
			Long ComponentId) {
		/*
		 * this method returns the Name of this instance of an used component,
		 * e.g. "GATE_1"
		 */
		return getComponentStringIdentifier() + "_" + ComponentId.toString();
	}

	public SortedMap<String, Integer> GetMemList(AttributeSet attrs,
			String HDLType) {
		/*
		 * This method returns a map list of all the memory contents signals
		 * used in the black-box. The String Parameter represents the Name, and
		 * the Integer parameter represents the type definition.
		 */
		SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
		return Regs;
	}

	public Map<String, ArrayList<String>> GetMemInitData(/*AttributeSet attrs*/) {
		return null;
	}

  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) { }

	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		/*
		 * In this method the functionality of the black-box is described. It is
		 * used for both VHDL and VERILOG.
		 */
		ArrayList<String> Contents = new ArrayList<String>();
    behavior(new Hdl(HDLType, Reporter), TheNetlist, attrs);
		return Contents;
	}

	public Map<String, String> GetNetMap(String SourceName,
			boolean FloatingPinTiedToGround, NetlistComponent comp,
			int EndIndex, FPGAReport Reporter, String HDLType, Netlist TheNets) {
    Hdl out = new Hdl(HDLType, null);
		Map<String, String> NetMap = new HashMap<String, String>();
		if ((EndIndex < 0) || (EndIndex >= comp.NrOfEnds())) {
			Reporter.AddFatalError("INTERNAL ERROR: Component tried to index non-existing SolderPoint");
			return NetMap;
		}
		ConnectionEnd ConnectionInformation = comp.getEnd(EndIndex);
		boolean IsOutput = ConnectionInformation.IsOutputEnd();
		int NrOfBits = ConnectionInformation.NrOfBits();
		if (NrOfBits == 1) {
			/* Here we have the easy case, just a single bit net */
			NetMap.put(
					SourceName,
					GetNetName(comp, EndIndex, FloatingPinTiedToGround,
							HDLType, TheNets));
		} else {
			/*
			 * Here we have the more difficult case, it is a bus that needs to
			 * be mapped
			 */
			/* First we check if the bus has a connection */
			boolean Connected = false;
			for (int i = 0; i < NrOfBits; i++) {
				if (ConnectionInformation.GetConnection((byte) i)
						.GetParrentNet() != null) {
					Connected = true;
				}
			}
			if (!Connected) {
				/* Here is the easy case, the bus is unconnected */
				if (IsOutput) {
					if (HDLType.equals(Settings.VHDL)) {
						NetMap.put(SourceName, "OPEN");
					} else {
						NetMap.put(SourceName, "");
					}
				} else {
					NetMap.put(
							SourceName,
							out.fill(!FloatingPinTiedToGround, NrOfBits));
				}
			} else {
				/*
				 * There are connections, we detect if it is a continues bus
				 * connection
				 */
				if (TheNets.IsContinuesBus(comp, EndIndex)) {
					/* Another easy case, the continues bus connection */
					NetMap.put(
							SourceName,
							GetBusNameContinues(comp, EndIndex, HDLType,
									TheNets));
				} else {
					/* The last case, we have to enumerate through each bit */
					if (HDLType.equals(Settings.VHDL)) {
						StringBuffer SourceNetName = new StringBuffer();
						for (int i = 0; i < NrOfBits; i++) {
							/* First we build the Line information */
							SourceNetName.setLength(0);
							SourceNetName.append(SourceName + "("
									+ Integer.toString(i) + ") ");
							ConnectionPoint SolderPoint = ConnectionInformation
									.GetConnection((byte) i);
							if (SolderPoint.GetParrentNet() == null) {
								/* The net is not connected */
								if (IsOutput) {
									NetMap.put(SourceNetName.toString(), "OPEN");
								} else {
									NetMap.put(
											SourceNetName.toString(),
                      out.bit(!FloatingPinTiedToGround));
								}
							} else {
								/*
								 * The net is connected, we have to find out if
								 * the connection is to a bus or to a normal net
								 */
								if (SolderPoint.GetParrentNet().BitWidth() == 1) {
									/* The connection is to a Net */
									NetMap.put(
											SourceNetName.toString(),
											NetName
													+ Integer.toString(TheNets
															.GetNetId(SolderPoint
																	.GetParrentNet())));
								} else {
									/* The connection is to an entry of a bus */
									NetMap.put(
											SourceNetName.toString(),
											BusName
													+ Integer.toString(TheNets
															.GetNetId(SolderPoint
																	.GetParrentNet()))
													+ "("
													+ Integer
															.toString(SolderPoint
																	.GetParrentNetBitIndex())
													+ ")");
								}
							}
						}
					} else {
						ArrayList<String> SeperateSignals = new ArrayList<String>();
						/*
						 * First we build an array with all the signals that
						 * need to be concatenated
						 */
						for (int i = 0; i < NrOfBits; i++) {
							ConnectionPoint SolderPoint = ConnectionInformation
									.GetConnection((byte) i);
							if (SolderPoint.GetParrentNet() == null) {
								/* this entry is not connected */
								if (IsOutput) {
									SeperateSignals.add("1'bz");
								} else {
									SeperateSignals.add(out.bit(!FloatingPinTiedToGround));
								}
							} else {
								/*
								 * The net is connected, we have to find out if
								 * the connection is to a bus or to a normal net
								 */
								if (SolderPoint.GetParrentNet().BitWidth() == 1) {
									/* The connection is to a Net */
									SeperateSignals.add(NetName
											+ Integer.toString(TheNets
													.GetNetId(SolderPoint
															.GetParrentNet())));
								} else {
									/* The connection is to an entry of a bus */
									SeperateSignals.add(BusName
											+ Integer.toString(TheNets
													.GetNetId(SolderPoint
															.GetParrentNet()))
											+ "["
											+ Integer.toString(SolderPoint
													.GetParrentNetBitIndex())
											+ "]");
								}
							}
						}
						/* Finally we can put all together */
						StringBuffer Vector = new StringBuffer();
						Vector.append("{");
						for (int i = NrOfBits; i > 0; i++) {
							Vector.append(SeperateSignals.get(i - 1));
							if (i != 1) {
								Vector.append(",");
							}
						}
						Vector.append("}");
						NetMap.put(SourceName, Vector.toString());
					}
				}
			}
		}
		return NetMap;
	}

	public String GetNetName(NetlistComponent comp, int EndIndex,
			boolean FloatingNetTiedToGround, String HDLType, Netlist MyNetlist) {
    Hdl out = new Hdl(HDLType, null);
		StringBuffer Contents = new StringBuffer();
		String BracketOpen = (HDLType.equals(Settings.VHDL)) ? "(" : "[";
		String BracketClose = (HDLType.equals(Settings.VHDL)) ? ")" : "]";
		String Unconnected = (HDLType.equals(Settings.VHDL)) ? "OPEN" : "";
		String FloatingValue = out.bit(!FloatingNetTiedToGround);
		if ((EndIndex >= 0) && (EndIndex < comp.NrOfEnds())) {
			ConnectionEnd ThisEnd = comp.getEnd(EndIndex);
			boolean IsOutput = ThisEnd.IsOutputEnd();
			if (ThisEnd.NrOfBits() == 1) {
				ConnectionPoint SolderPoint = ThisEnd.GetConnection((byte) 0);
				if (SolderPoint.GetParrentNet() == null) {
					/* The net is not connected */
					if (IsOutput) {
						Contents.append(Unconnected);
					} else {
						Contents.append(FloatingValue);
					}
				} else {
					/*
					 * The net is connected, we have to find out if the
					 * connection is to a bus or to a normal net
					 */
					if (SolderPoint.GetParrentNet().BitWidth() == 1) {
						/* The connection is to a Net */
						Contents.append(NetName
								+ Integer.toString(MyNetlist
										.GetNetId(SolderPoint.GetParrentNet())));
					} else {
						/* The connection is to an entry of a bus */
						Contents.append(BusName
								+ Integer.toString(MyNetlist
										.GetNetId(SolderPoint.GetParrentNet()))
								+ BracketOpen
								+ Integer.toString(SolderPoint
										.GetParrentNetBitIndex())
								+ BracketClose);
					}
				}
			}
		}
		return Contents.toString();
	}

	public int GetNrOfTypes(Netlist TheNetlist, AttributeSet attrs,
			String HDLType) {
		/* In this method you can specify the number of own defined Types */
		return 0;
	}
	
  public void outputs(SortedMap<String, Integer> list, Netlist TheNetlist, AttributeSet attrs) { }

	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
		/*
		 * This method returns a map list of all the outputs of a black-box. The
		 * String Parameter represents the Name, and the Integer parameter
		 * represents: >0 The number of bits of the signal <0 A parameterized
		 * vector of bits where the value is the "key" of the parameter map 0 Is
		 * an invalid value and must not be used
		 */
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    outputs(Outputs, TheNetlist, attrs);
		return Outputs;
	}

	public void params(SortedMap<Integer, String> list, AttributeSet attrs) { }

	public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
		/*
		 * This method returns a map list of all parameters/generic. The integer
		 * parameter represents the key that can be used for the parameterized
		 * input and/or output vectors. The String is the name of the parameter.
		 * In VHDL all parameters are assumed to be INTEGER.
		 */
		SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    params(Parameters, attrs);
		return Parameters;
	}
	
  public void paramValues(SortedMap<String, Integer> list, Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter) { }

	public SortedMap<String, Integer> GetParameterMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter) {
		/*
		 * This method returns the assigned parameter/generic values used for
		 * the given component, the key is the name of the parameter/generic,
		 * and the Integer its assigned value
		 */
		SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    paramValues(ParameterMap, Nets, ComponentInfo, Reporter);
		return ParameterMap;
	}
	
  public void portValues(SortedMap<String, String> list, Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) { }

	public SortedMap<String, String> GetPortMap(Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		/*
		 * This method returns the assigned input/outputs of the component, the
		 * key is the name of the input/output (bit), and the value represent
		 * the connected net.
		 */
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
    portValues(PortMap, Nets, ComponentInfo, Reporter, HDLType);
		return PortMap;
	}

	public void registers(SortedMap<String, Integer> list, AttributeSet attrs, String HDLType) { }

	public SortedMap<String, Integer> GetRegList(AttributeSet attrs, String HDLType) {
		/*
		 * This method returns a map list of all the registers/flipflops used in
		 * the black-box. The String Parameter represents the Name, and the
		 * Integer parameter represents: >0 The number of bits of the signal <0
		 * A parameterized vector of bits where the value is the "key" of the
		 * parameter map 0 Is an invalid value and must not be used In VHDL
		 * there is no distinction between wire and reg. You can put them in
		 * both GetRegList or GetWireList
		 */
		SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
    registers(Regs, attrs, HDLType);
		return Regs;
	}

	public String GetRelativeDirectory(/*String HDLType*/) {
		String Subdir = GetSubDir();
		if (!Subdir.endsWith(File.separator) & !Subdir.isEmpty()) {
			Subdir += File.separatorChar;
		}
		return _lang.toLowerCase() + File.separatorChar + Subdir;
	}

	public String GetSubDir() {
		/*
		 * this method returns the module sub-directory where the HDL code is
		 * placed
		 */
		return "";
	}

	public String GetType(int TypeNr) {
		/* This method returns the type name indicated by TypeNr */
		return "";
	}

	protected SortedSet<String> GetTypeDefinitions(Netlist TheNetlist,
			AttributeSet attrs, String HDLType) {
		/*
		 * This method returns all the type definitions used without the ending
		 * ;
		 */
		return new TreeSet<String>();
	}

	private ArrayList<String> GetVHDLBlackBox(Netlist TheNetlist,
			AttributeSet attrs, String ComponentName, Boolean IsEntity) {
		ArrayList<String> Contents = new ArrayList<String>();
		Map<String, Integer> InputsList = GetInputList(TheNetlist, attrs);
		Map<String, Integer> InOutsList = GetInOutList(TheNetlist, attrs);
		Map<String, Integer> OutputsList = GetOutputList(TheNetlist, attrs);
		Map<Integer, String> ParameterList = GetParameterList(attrs);
		StringBuffer OneLine = new StringBuffer();
		int IdentSize;
		String CompTab = (IsEntity) ? "" : "   ";
		boolean first;
		if (IsEntity) {
			Contents.add("ENTITY " + ComponentName + " IS");
		} else {
			Contents.add("   COMPONENT " + ComponentName);
		}
		if (!ParameterList.isEmpty()) {
			OneLine.append(CompTab + "   GENERIC ( ");
			IdentSize = OneLine.length();
			first = true;
			for (int generic : ParameterList.keySet()) {
				if (!first) {
					OneLine.append(";");
					Contents.add(OneLine.toString());
					OneLine.setLength(0);
					while (OneLine.length() < IdentSize) {
						OneLine.append(" ");
					}
				} else {
					first = false;
				}
				OneLine.append(ParameterList.get(generic));
				for (int i = ParameterList.get(generic).length(); i < PallignmentSize; i++) {
					OneLine.append(" ");
				}
				OneLine.append(": INTEGER");
			}
			OneLine.append(");");
			Contents.add(OneLine.toString());
			OneLine.setLength(0);
		}
		if (!InputsList.isEmpty() || !OutputsList.isEmpty()
				|| !InOutsList.isEmpty()) {
			int nr_of_bits;
			OneLine.append(CompTab + "   PORT ( ");
			IdentSize = OneLine.length();
			first = true;
			for (String input : InputsList.keySet()) {
				if (!first) {
					OneLine.append(";");
					Contents.add(OneLine.toString());
					OneLine.setLength(0);
					while (OneLine.length() < IdentSize) {
						OneLine.append(" ");
					}
				} else {
					first = false;
				}
				OneLine.append(input);
				for (int i = input.length(); i < PallignmentSize; i++) {
					OneLine.append(" ");
				}
				OneLine.append(": IN  std_logic");
				nr_of_bits = InputsList.get(input);
				if (nr_of_bits < 0) {
					/* we have a parameterized input */
					if (!ParameterList.containsKey(nr_of_bits)) {
						Contents.clear();
						return Contents;
					}
					OneLine.append("_vector( (" + ParameterList.get(nr_of_bits)
							+ "-1) DOWNTO 0 )");
				} else {
					if (nr_of_bits > 1) {
						/* we have a bus */
						OneLine.append("_vector( "
								+ Integer.toString(nr_of_bits - 1)
								+ " DOWNTO 0 )");
					} else {
						if (nr_of_bits == 0) {
							OneLine.append("_vector( 0 DOWNTO 0 )");
						}
					}
				}
			}
			for (String inout : InOutsList.keySet()) {
				if (!first) {
					OneLine.append(";");
					Contents.add(OneLine.toString());
					OneLine.setLength(0);
					while (OneLine.length() < IdentSize) {
						OneLine.append(" ");
					}
				} else {
					first = false;
				}
				OneLine.append(inout);
				for (int i = inout.length(); i < PallignmentSize; i++) {
					OneLine.append(" ");
				}
				OneLine.append(": INOUT  std_logic");
				nr_of_bits = InOutsList.get(inout);
				if (nr_of_bits < 0) {
					/* we have a parameterized input */
					if (!ParameterList.containsKey(nr_of_bits)) {
						Contents.clear();
						return Contents;
					}
					OneLine.append("_vector( (" + ParameterList.get(nr_of_bits)
							+ "-1) DOWNTO 0 )");
				} else {
					if (nr_of_bits > 1) {
						/* we have a bus */
						OneLine.append("_vector( "
								+ Integer.toString(nr_of_bits - 1)
								+ " DOWNTO 0 )");
					} else {
						if (nr_of_bits == 0) {
							OneLine.append("_vector( 0 DOWNTO 0 )");
						}
					}
				}
			}
			for (String output : OutputsList.keySet()) {
				if (!first) {
					OneLine.append(";");
					Contents.add(OneLine.toString());
					OneLine.setLength(0);
					while (OneLine.length() < IdentSize) {
						OneLine.append(" ");
					}
				} else {
					first = false;
				}
				OneLine.append(output);
				for (int i = output.length(); i < PallignmentSize; i++) {
					OneLine.append(" ");
				}
				OneLine.append(": OUT std_logic");
				nr_of_bits = OutputsList.get(output);
				if (nr_of_bits < 0) {
					/* we have a parameterized output */
					if (!ParameterList.containsKey(nr_of_bits)) {
						Contents.clear();
						return Contents;
					}
					OneLine.append("_vector( (" + ParameterList.get(nr_of_bits)
							+ "-1) DOWNTO 0 )");
				} else {
					if (nr_of_bits > 1) {
						/* we have a bus */
						OneLine.append("_vector( "
								+ Integer.toString(nr_of_bits - 1)
								+ " DOWNTO 0 )");
					} else {
						if (nr_of_bits == 0) {
							OneLine.append("_vector( 0 DOWNTO 0 )");
						}
					}
				}
			}
			OneLine.append(");");
			Contents.add(OneLine.toString());
		}
		if (IsEntity) {
			Contents.add("END " + ComponentName + ";");
		} else {
			Contents.add("   END COMPONENT;");
		}
		Contents.add("");
		return Contents;
	}

	public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist Nets) { }

	public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
		/*
		 * This method returns a map list of all the wires/signals used in the
		 * black-box. The String Parameter represents the Name, and the Integer
		 * parameter represents: >0 The number of bits of the signal <0 A
		 * parameterized vector of bits where the value is the "key" of the
		 * parameter map 0 Is an invalid value and must not be used In VHDL a
		 * single bit "wire" is transformed to std_logic, all the others are
		 * std_logic_vectors
		 */
		SortedMap<String, Integer> Wires = new TreeMap<>();
    wires(Wires, attrs, Nets);
		return Wires;
	}

}
