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
package com.cburch.logisim.gui.chrono;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Selection;
import com.cburch.logisim.gui.log.SelectionItem;
import com.cburch.logisim.util.Icons;

/**
 * Contains all data to be plotted
 */
public class ChronoData {

  // This class is mostly needed because drag-and-drop DataFlavor works easiest
  // with a regular class (not an inner or generic class).
  public static class Signals extends ArrayList<Signal> implements Transferable {
    public static final DataFlavor dataFlavor;
    static {
      DataFlavor f = null;
      try {
        f = new DataFlavor(
            String.format("%s;class=\"%s\"",
              DataFlavor.javaJVMLocalObjectMimeType,
              Signals.class.getName()));
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      dataFlavor = f;
    }
    public static final DataFlavor[] dataFlavors = new DataFlavor[] { dataFlavor };

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if(!isDataFlavorSupported(flavor))
        throw new UnsupportedFlavorException(flavor);
      return this;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return dataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return dataFlavor.equals(flavor);
    }

  }

  public static class Signal {
    public int idx;
    public final SelectionItem info;
    private final ArrayList<Value> vals;
    private int offset;
    private int width;

    private Signal(int idx, SelectionItem info, int offset, Value initialValue) {
      this.idx = idx;
      this.info = info;
      this.offset = offset;
      this.width = initialValue.getWidth();
      this.vals = new ArrayList<>();
      vals.add(initialValue);
    }

    private void extend(Value v) {
      vals.add(v);
    }

    private void reset(Value v) {
      offset = 0;
      vals.clear();
      vals.add(v);
    }

    // todo: this doesn't belong here
    public ImageIcon getIcon() {
      if (width > 1)
        return (ImageIcon)Icons.getIcon("chronoBus.gif");
      else
        return (ImageIcon)Icons.getIcon("chronoSignal.gif");
    }

    public Value getValue(int t) {
      int idx = t - offset;
      return (idx < 0 || idx >= vals.size()) ? null : vals.get(idx);
    }

    public String getFormattedValue(int t) {
      int idx = t - offset;
      return (idx < 0 || idx >= vals.size()) ? "-" : info.format(vals.get(idx));
    }

    public String format(Value v) {
      return info.format(v);
    }

    public String getFormattedMaxValue() {
      // well, signed decimal should maybe use a large positive value?
      return format(Value.createKnown(BitWidth.create(width), -1));
    }

    public String getFormattedMinValue() {
      // well, signed decimal should maybe use a large negative value?
      return format(Value.createKnown(BitWidth.create(width), 0));
    }

    public String getName() {
      return info.getChronoDisplayName();
    }

    public int getWidth() {
      return width;
    }

    private boolean expanded; // todo: this doesn't belong here
    public boolean isExpanded() { return expanded; }
    public void toggleExpanded() { expanded = !expanded; }
  }

  private ArrayList<Signal> signals = new ArrayList<>();
  private int count = 1;

	public ChronoData() {
	}

  public int getSignalCount() {
    return signals.size();
  }

  public int getValueCount() { // todo: uniform spacing for clock ticks
    return count;
  }

  public Signal getSignal(int idx) {
    return signals.get(idx);
  }

  public void clear() {
    spotlight = null;
    count = 1;
    signals.clear();
  }

  // really: add, remove, and/or move as needed
  public void setSignals(Selection sel, CircuitState circuitState) {
		int n = sel.size();
		for (int i = 0; i < n; i++) {
      SelectionItem id = sel.get(i);
			Value value = id.fetchValue(circuitState);
      addSignal(id, value, i);
    }
    if (spotlight != null && signals.indexOf(spotlight)  >= n)
      spotlight = null;
    if (signals.size() > n)
      signals.subList(n, signals.size()).clear();
  }

  // really: add or move
  private void addSignal(SelectionItem info, Value initialValue, int idx) {
    for (Signal s : signals) {
      if (s.info.equals(info)) {
        if (s.idx == idx)
          return;
        signals.remove(s.idx);
        signals.add(idx, s);
        int a = Math.min(idx, s.idx);
        int b = Math.max(idx, s.idx);
        for (int i = a; i <= b; i++)
          signals.get(i).idx = i;
        return;
      }
    }
    Signal s = new Signal(idx, info, count - 1, initialValue);
    signals.add(idx, s);
  }

	public void addSignalValues(Value[] vals) {
    for (int i = 0; i < signals.size() && i < vals.length; i++)
      signals.get(i).extend(vals[i]);
    count++;
	}

	public void resetSignalValues(Value[] vals) {
    count = 0;
    for (int i = 0; i < signals.size() && i < vals.length; i++)
      signals.get(i).reset(vals[i]);
    count++;
	}

  // The (at most one) signal under the mouse is highlighted.
  // Multiple other rows can be selected by clicking.
  private Signal spotlight;
  public Signal getSpotlight() {
    return spotlight;
  }
  public Signal setSpotlight(Signal s) {
    Signal old = spotlight;
    spotlight = s;
    return old;
  }

	// /**
	//  * Hide all signals that compose busName
	//  */
	// public void contractBus(SignalDataBus sd) {
	// 	if (sd.getSignalValues().size() > 0) {
	// 		int signalNbr = sd.getSignalValues().get(0).length();
	// 		int busNamePos = (mSignalOrder.indexOf(sd.getName()));

	// 		for (int signalI = 0; signalI < signalNbr; ++signalI) {
	// 			String name = sd.getName() + "__s__" + signalI;
	// 			remove(name);
	// 			mSignalOrder.remove(busNamePos + 1);
	// 		}
	// 		sd.setExpanded(false);
	// 	}
	// }

	// /**
	//  * Display all signals that compose busName
	//  */
	// public void expandBus(SignalDataBus sd) {
	// 	if (sd.getSignalValues().size() > 0) {
	// 		int signalNbr = sd.getSignalValues().get(0).length();
	// 		int busNamePos = (mSignalOrder.indexOf(sd.getName()));
	// 		// for each signal that defines the bus

	// 		for (int signalI = 0; signalI < signalNbr; ++signalI) {
	// 			int bitPos = signalNbr - signalI - 1;
	// 			ArrayList<String> sig = new ArrayList<String>();
	// 			String name = sd.getName() + "__s__" + signalI;
	// 			for (String s : sd.getSignalValues()) {
	// 				sig.add(s.substring(bitPos, bitPos + 1));
	// 			}
	// 			// add signalData
	// 			put(name, new SignalData(name, sig));
	// 			// insert new signal in name signal order
	// 			mSignalOrder.add(busNamePos + signalI + 1, name);
	// 		}
	// 		sd.setExpanded(true);
	// 	}
	// }

	/**
	 * Remove if the sysclk has 2 or more identical states
	 */
	//private void normalize() {
    // todo: huh?
    /*
		try {
			ArrayList<String> vClk = get("sysclk").getSignalValues();
			int i = 0;
			while (i < vClk.size() - 1) {
				if (vClk.get(i).equals(vClk.get(i + 1))) {
					for (SignalData sd : values()) {
						sd.getSignalValues().remove(i);
					}
				} else {
					i++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} */
	// }

  // public int getRowCount() {
  //   return get(0)
  // }

	// public void setSignalNames(List<String> names) {
	// 	signalNames = new ArrayList<>(names);
	// }

	// public List<String> getSignalNames() {
	// 	return signalNames;
	// }

	// /**
	//  * In real time mode, if a bus is expanded we need to add the new data to
	//  * every signal
	//  */
	// public void updateRealTimeExpandedBus() {
	// 	for (java.util.Map.Entry<String, SignalData> entry : entrySet()) {
	// 		if (entry.getValue() instanceof SignalDataBus) {
	// 			SignalDataBus sdb = (SignalDataBus) entry.getValue();
	// 			if (sdb.isExpanded()) {
	// 				int signalNbr = sdb.getSignalValues().get(0).length();
	// 				for (int signalI = 0; signalI < signalNbr; ++signalI) {
	// 					int bitPos = signalNbr - signalI - 1;
	// 					String name = sdb.getName() + "__s__" + signalI;
	// 					get(name)
	// 							.getSignalValues()
	// 							.add(sdb.getSignalValues()
	// 									.get(sdb.getSignalValues().size() - 1)
	// 									.substring(bitPos, bitPos + 1));
	// 				}
	// 			}
	// 		}
	// 	}
	// }

// 	public ChronoData(String logisimLogFile, ChronoPanel panel) throws IOException {
//     LineNumberReader lineReader =
//         new LineNumberReader(new FileReader(logisimLogFile));
//     try {
// 
//       // Tab delimited, first line has all signal names
//       String line = lineReader.readLine();
//       signalNames.addAll(line.split("\\t"));
// 
//       // second line has tick frequency // todo: huh?
//       line = lineReader.readLine();
//       try {
//         panel.setTimelineParam(new TimelineParam(line));
//       } catch (Exception e) {
//         panel.setTimelineParam(null);
//       }
// 
//       ArrayList<String>[] rawData = new ArrayList<>[signalNames.size()];
//       for (int i = 0; i < signalNames.size(); i++)
//         rawData[i] = new ArrayList<>();
// 
//       // this will crash if row has too many items, and is buggy if row has too
//       // few items
// 
//       // rest of the file has values
//       while ((line = lineReader.readLine()) != null) {
//         String[] vals = line.split("\\t");
//         for (int i = 0; i < vals.length && i < signalNames.size(); i++) {
//           String val = vals[i].replaceAll("\\s", "");
//           // log file may have spaces (e.g. in bus with >4 bits)
//           rawData[i].add(val);
//         }
//       }
//     } finally {
//       lineReader.close();
//     }
// 
//     for (int i = 0; i < signalNames.size(); i++) {
//       String name = signalNames[i];
//       ArrayList<String> vs = rawData[i];
//       // todo: this crashes with empty vector
// 			if (vs.get(0).length() > 1)
// 				put(name, new SignalDataBus(name, vs));
//       else
// 				put(name, new SignalData(name, vs));
// 		}
// 
// 		// normalize();
// 	}

//  // Export all chronogram data into a specified file.
//	public static void export(String filePath, TimelineParam timeLineParam,
//			ChronoData chronoData) {
//    BufferedWriter writer = null;
//		try {
//      writer = new BufferedWriter(new FileWriter(filePath));
//
//      // todo: fix hack with __s__
//
//			// header
//			for (String name : chronoData.getSignalOrder()) {
//				if (!name.contains("__s__"))
//					writer.write(name + "\t");
//			}
//			writer.newLine();
//
//			// freqency // todo: huh?
//			if (timeLineParam != null)
//				writer.write(timeLineParam.toString());
//			else
//				writer.write("noclk");
//			writer.newLine();
//
//			// content
//      int n = chronoData.getRowCount();
//			for (int row = 0; row < n; row++) {
//        int first = true;
//				for (String signalName : chronoData.getSignalNames()) {
//					if (!signalName.contains("__s__")) {
//            if (!first)
//              writer.write("\t");
//            first = false;
//						writer.write(chronoData.getSignalValue(signalName, row));
//					}
//				}
//				writer.newLine();
//			}
//
//			writer.flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//      try {
//        if (writer != null)
//          writer.close();
//      } catch (IOException e) {
//      }
//    }
//	}

}
