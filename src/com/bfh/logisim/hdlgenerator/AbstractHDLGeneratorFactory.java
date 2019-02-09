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


  // TODO
	public String GetBusEntryName(NetlistComponent comp, int EndIndex,
			boolean FloatingNetTiedToGround, int bitindex, String HDLType,
			Netlist TheNets) {
    Hdl out = new Hdl(HDLType, null);
		StringBuffer Contents = new StringBuffer();
		String BracketOpen = (HDLType.equals("VHDL")) ? "(" : "[";
		String BracketClose = (HDLType.equals("VHDL")) ? ")" : "]";
		if ((EndIndex >= 0) && (EndIndex < comp.NrOfEnds())) {
			ConnectionEnd ThisEnd = comp.getEnd(EndIndex);
			boolean IsOutput = ThisEnd.IsOutputEnd();
			int NrOfBits = ThisEnd.NrOfBits();
			if ((NrOfBits > 1) && (bitindex >= 0) && (bitindex < NrOfBits)) {
				if (ThisEnd.GetConnection((byte) bitindex).GetParrentNet() == null) {
					/* The net is not connected */
					if (IsOutput) {
						if (HDLType.equals("VHDL")) {
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

  // TODO
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

  // TODO
	public String GetClockNetName(NetlistComponent comp, int EndIndex, Netlist TheNets) {
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

  // TODO
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
					if (HDLType.equals("VHDL")) {
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
					if (HDLType.equals("VHDL")) {
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


  // TODO
	public String GetNetName(NetlistComponent comp, int EndIndex,
			boolean FloatingNetTiedToGround, String HDLType, Netlist MyNetlist) {
    Hdl out = new Hdl(HDLType, null);
		StringBuffer Contents = new StringBuffer();
		String BracketOpen = (HDLType.equals("VHDL")) ? "(" : "[";
		String BracketClose = (HDLType.equals("VHDL")) ? ")" : "]";
		String Unconnected = (HDLType.equals("VHDL")) ? "OPEN" : "";
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


  // TODO
  protected void generateInlinedCode(Hdl out, NetlistComponent comp) { }

  // TODO
	public void generateInlinedCodeForTopLevelIO(Hdl out,
      ArrayList<String> ComponentIdentifier, MappableResourcesContainer MapInfo) {

		ArrayList<String> Contents = new ArrayList<String>();
		String Preamble = (_lang.equals("VHDL")) ? "" : "assign ";
		String AssignOperator = (_lang.equals("VHDL")) ? " <= " : " = ";
		String OpenBracket = (_lang.equals("VHDL")) ? "(" : "[";
		String CloseBracket = (_lang.equals("VHDL")) ? ")" : "]";
		String Inversion = (_lang.equals("VHDL")) ? "NOT " : "~";
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

  // TODO:
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

  // TODO
	public Map<String, ArrayList<String>> GetMemInitData() { return null; }


  /////////// DONE
  // Generate and write an "architecture" file for this component, using the
  // given root directory, hdlName, and NVRAM initialization files, where:
  //   hdlName = getHDLNameWithinCircuit(containingCircuitName);
  public boolean writeArchitecture(String rootDir,
			String hdlName, Map<String, File> memInitFiles) { // fixme: meminitfiles from where?
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
	protected Hdl getArchitecture(String hdlName, Map<String, File> memInitFiles) {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out, hdlName);

    SignalList inPorts = getInPorts();
    // fixme: support in/out ports here?
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
            String filepath = memInitFiles.get(Mem).getPath();
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

      // fixme: add support for memories here and in Ram/Rom HDL generators.

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
  public boolean writeEntity(String directory, String hdlName) {
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
  // A list of memory blocks used internally within this component.
	protected MemoryList getMemories() { return new MemoryList(); }


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
    String subdir = getSubDir();
    if (!subdir.endsWith(File.separator) && !subdir.isEmpty())
      subdir += File.separatorChar;
    String path = rootDir + _lang.toLowerCase() + File.separatorChar + subdir;
    return FileWriter.GetFilePointer(path, hdlName, isMif, isEntity, _err, _lang);
	}

  /////////// DONE
  // Returns a suitable subdirectory for storing HDL files.
	protected abstract String getSubDir();

}
