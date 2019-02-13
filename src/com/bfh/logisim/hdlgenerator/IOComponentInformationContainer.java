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

import static com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer.IOComponentTypes;


public class IOComponentInformationContainer {
	
  // These are here just for convenience in generator classes.
  public static final IOComponentTypes LED = IOComponentTypes.LED;
  public static final IOComponentTypes Button = IOComponentTypes.Button;
  public static final IOComponentTypes Pin = IOComponentTypes.Pin;
  public static final IOComponentTypes SevenSegment = IOComponentTypes.SevenSegment;
  public static final IOComponentTypes DIPSwitch = IOComponentTypes.DIPSwitch;
  public static final IOComponentTypes RGBLED = IOComponentTypes.RGBLED;
  public static final IOComponentTypes PortIO = IOComponentTypes.PortIO;
  public static final IOComponentTypes Bus = IOComponentTypes.Bus;
  public static final IOComponentTypes Unknown = IOComponentTypes.Unknown;

	private int NrOfInputBubbles;
	private int NrOfInOutBubbles;
	private int NrOfOutputBubbles;
	private ArrayList<String> InputBubbleLabels;
	private ArrayList<String> InOutBubbleLabels;
	private ArrayList<String> OutputBubbleLabels;
	private IOComponentTypes MainMapType;
	private ArrayList<IOComponentTypes> AlternateMapTypes;

	public IOComponentInformationContainer(int inports, int outports,
			int inoutports, ArrayList<String> inportLabels,
			ArrayList<String> outportLabels, ArrayList<String> inoutportLabels,
			IOComponentTypes MapType) {
		NrOfInputBubbles = inports;
		NrOfOutputBubbles = outports;
		NrOfInOutBubbles = inoutports;
		InputBubbleLabels = inportLabels;
		OutputBubbleLabels = outportLabels;
		InOutBubbleLabels = inoutportLabels;
		MainMapType = MapType;
		AlternateMapTypes = new ArrayList<IOComponentTypes>();
	}

	public IOComponentInformationContainer(int inports, int outports,
			int inoutport, IOComponentTypes MapType) {
		NrOfInputBubbles = inports;
		NrOfOutputBubbles = outports;
		NrOfInOutBubbles = inoutport;
		InputBubbleLabels = null;
		OutputBubbleLabels = null;
		InOutBubbleLabels = null;
		MainMapType = MapType;
		AlternateMapTypes = new ArrayList<IOComponentTypes>();
	}

	public void AddAlternateMapType(IOComponentTypes map) {
		AlternateMapTypes.add(map);
	}

	public IOComponentInformationContainer clone() {
		IOComponentInformationContainer Myclone = new IOComponentInformationContainer(
				NrOfInputBubbles, NrOfOutputBubbles, NrOfInOutBubbles,
				InputBubbleLabels, OutputBubbleLabels, InOutBubbleLabels,
				MainMapType);
		for (IOComponentTypes Alt : AlternateMapTypes) {
			Myclone.AddAlternateMapType(Alt);
		}
		return Myclone;
	}

	public IOComponentTypes GetAlternateMapType(
			int id) {
		if (id >= AlternateMapTypes.size()) {
			return Unknown;
		} else {
			return AlternateMapTypes.get(id);
		}
	}

	public String GetInOutportLabel(int inoutNr) {
		if (InOutBubbleLabels == null) {
			return Integer.toString(inoutNr);
		}
		if (InOutBubbleLabels.size() <= inoutNr) {
			return Integer.toString(inoutNr);
		}
		return InOutBubbleLabels.get(inoutNr);
	}

	public String GetInportLabel(int inputNr) {
		if (InputBubbleLabels == null) {
			return AlternateMapTypes.get(0).name() + Integer.toString(inputNr);
		}
		if (InputBubbleLabels.size() <= inputNr) {
			return Integer.toString(inputNr);
		}
		return InputBubbleLabels.get(inputNr);
	}

	public IOComponentTypes GetMainMapType() {
		return MainMapType;
	}

	public int GetNrOfInOutports() {
		return NrOfInOutBubbles;
	}

	public int GetNrOfInports() {
		return NrOfInputBubbles;
	}

	public int GetNrOfOutports() {
		return NrOfOutputBubbles;
	}

	public String GetOutportLabel(int outputNr) {
		if (OutputBubbleLabels == null) {
			return AlternateMapTypes.get(0).name() + Integer.toString(outputNr);
		}
		if (OutputBubbleLabels.size() <= outputNr) {
			return Integer.toString(outputNr);
		}
		return OutputBubbleLabels.get(outputNr);
	}

	public boolean HasAlternateMapTypes() {
		return !AlternateMapTypes.isEmpty();
	}

	public void setNrOfInOutports(int nb, ArrayList<String> labels) {
		NrOfInOutBubbles = nb;
		InOutBubbleLabels = labels;
	}

	public void setNrOfInports(int nb, ArrayList<String> labels) {
		NrOfInputBubbles = nb;
		InputBubbleLabels = labels;
	}

//   private ArrayList<IOComponentTypes> allMapTypes() {
//     ArrayList<IOComponentTypes> all = new ArrayList<>();
//     all.add(MainMapType);
//     all.addAll(AlternateMapTypes);
//     return all;
//   }

//     for (IOComponentTypes t : allMapTypes())
//       switch(t) {
//       case Pin:
//       case Bus:
//       case PortIO:
//       case Button:
//       case DIPSwitch:
//       }

  public boolean CanBeAllOnesInput() {
    return (NrOfInputBubbles > 0 || NrOfInOutBubbles > 0) && NrOfOutputBubbles == 0;
  }

  public boolean CanBeAllZerosInput() {
    return (NrOfInputBubbles > 0 || NrOfInOutBubbles > 0) && NrOfOutputBubbles == 0;
  }

  public boolean CanBeConstantInput() {
    return (NrOfInputBubbles > 0 || NrOfInOutBubbles > 0) && NrOfOutputBubbles == 0;
  }

  // public boolean CanBeUndefinedInput() {
  //   return (NrOfInputBubbles > 0 || NrOfInOutBubbles > 0) && NrOfOutputBubbles == 0;
  // }

  public boolean CanBeDisconnectedOutput() {
    return NrOfInputBubbles == 0 && (NrOfInOutBubbles > 0 || NrOfOutputBubbles > 0);
  }

}
