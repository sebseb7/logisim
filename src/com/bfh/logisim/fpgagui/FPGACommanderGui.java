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

package com.bfh.logisim.fpgagui;

import java.awt.EventQueue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.download.AlteraDownload;
import com.bfh.logisim.download.XilinxDownload;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.BoardReaderClass;
import com.bfh.logisim.fpgaboardeditor.FPGAClass;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.menu.MenuSimulate;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;

public class FPGACommanderGui implements ActionListener {

	private class MyLibraryListener implements LibraryListener {

		@Override
		public void libraryChanged(LibraryEvent event) {
			if (event.getAction() == LibraryEvent.ADD_TOOL
					|| event.getAction() == LibraryEvent.REMOVE_TOOL) {
				RebuildCircuitSelection();
			}
		}
	}

	private class MyProjListener implements ProjectListener {

		@Override
		public void projectChanged(ProjectEvent event) {
			if (event.getAction() == ProjectEvent.ACTION_SET_CURRENT) {
                                if (event.getCircuit() != null)
                                    SetCurrentSheet(event.getCircuit().getName());
			} else if (event.getAction() == ProjectEvent.ACTION_SET_FILE) {
				RebuildCircuitSelection();
			}
		}
	}

	private class MySimulatorListener implements SimulatorListener {
            public void propagationCompleted(SimulatorEvent e) { }
            public void simulatorStateChanged(SimulatorEvent e) { ChangeTickFrequency(); }
            public void tickCompleted(SimulatorEvent e) { }
	}

	private JFrame panel;
	private ComponentMapDialog MapPannel;
	private JLabel textMainCircuit = new JLabel("Choose main circuit ");
	private JLabel textTargetBoard = new JLabel("Choose target board ");
	private JLabel textTargetFreq = new JLabel("Choose tick frequency ");
	private JLabel textTargetDiv = new JLabel("Divide clock by...");
	private JLabel textAnnotation = new JLabel("Annotation method");
	private JLabel boardPic = new JLabel();
	private BoardIcon boardIcon = null;
	private JButton annotateButton = new JButton();
	private JButton validateButton = new JButton();
	private JCheckBox writeToFlash = new JCheckBox("Write to flash?");
	private JComboBox<String> boardsList = new JComboBox<>();
	private JComboBox<String> circuitsList = new JComboBox<>();
	private JComboBox<String> clockOption = new JComboBox<>();
	private static final String clockMax = "Maximum Speed";
	private static final String clockDiv = "Reduced Speed";
	private static final String clockDyn = "Dynamic Speed";
	private JComboBox<Object> clockDivRate = new JComboBox<>();
	private JComboBox<Object> clockDivCount = new JComboBox<>();
	private JComboBox<String> annotationList = new JComboBox<>();
	private JComboBox<String> HDLType = new JComboBox<>();
	private JComboBox<String> HDLOnly = new JComboBox<>();
	private JButton ToolSettings = new JButton();
	private Console messages = new Console("Messages");
	private ArrayList<Console> consoles = new ArrayList<>();
	private static final String OnlyHDLMessage = "Generate HDL only";
	private static final String HDLandDownloadMessage = "Synthesize and Download";
	private static final String OnlyDownloadMessage = "Download only";
	private JTabbedPane tabbedPane = new JTabbedPane();
	private Project MyProject;
	private Settings MySettings = new Settings();
	private BoardInformation MyBoardInformation = null;
	private MyProjListener myprojList = new MyProjListener();
	private MyLibraryListener myliblistener = new MyLibraryListener();
	private MySimulatorListener mysimlistener = new MySimulatorListener();
	private MappableResourcesContainer MyMappableResources;
	private String[] HDLPaths = { Settings.VERILOG.toLowerCase(),
			Settings.VHDL.toLowerCase(), "scripts", "sandbox", "ucf" };
	@SuppressWarnings("unused")
	private static final Integer VerilogSourcePath = 0;
	@SuppressWarnings("unused")
	private static final Integer VHDLSourcePath = 1;
	private static final Integer ScriptPath = 2;
	private static final Integer SandboxPath = 3;
	private static final Integer UCFPath = 4;
	private FPGAReport MyReporter = new FPGAReport(this);

        void HDLOnlyUpdate() {
		if ((MyBoardInformation.fpga.getVendor() == FPGAClass.VendorAltera && MySettings
				.GetAlteraToolPath().equals(Settings.Unknown))
				|| (MyBoardInformation.fpga.getVendor() == FPGAClass.VendorXilinx && MySettings
						.GetXilinxToolPath().equals(Settings.Unknown))) {
                        // Synthesis/download not possible.
			if (!MySettings.GetHDLOnly()) {
				MySettings.SetHdlOnly(true);
				MySettings.UpdateSettingsFile();
			}
                        HDLOnly.setSelectedItem(OnlyHDLMessage);
                        HDLOnly.setEnabled(false);
			AddInfo("Tool path is not set correctly. " +
				"Synthesis and download will not be available.");
			AddInfo("Please set the path to Altera or Xilinx tools using the \"Settings\" button");
		} else if (MySettings.GetHDLOnly()) {
                        // Synthesis/download possible, but user selected to only generate HDL.
                        HDLOnly.setSelectedItem(OnlyHDLMessage);
                        HDLOnly.setEnabled(true);
                } else {
                        // Synthesis/download possible, user elects to do so.
                        HDLOnly.setSelectedItem(HDLandDownloadMessage);
                        HDLOnly.setEnabled(true);
		}
        }

	public FPGACommanderGui(Project Main) {
		MyProject = Main;
		panel = new JFrame("FPGA Commander : "
				+ MyProject.getLogisimFile().getName());
		panel.setResizable(true);
		panel.setAlwaysOnTop(false);
		panel.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		GridBagLayout thisLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(thisLayout);
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// panel.setLocation(mlocation.x, mlocation.y);

		// change main circuit
		circuitsList.setEnabled(true);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 2;
		textMainCircuit.setEnabled(true);
		panel.add(textMainCircuit, c);
		c.gridx = 1;
		c.gridwidth = 2;
		// circuitsList.addActionListener(this);
		circuitsList.setActionCommand("mainCircuit");
		int i = 0;
		for (Circuit thisCircuit : MyProject.getLogisimFile().getCircuits()) {
			circuitsList.addItem(thisCircuit.getName());
			if (MyProject.getCurrentCircuit() != null &&
                                thisCircuit.getName().equals(
					MyProject.getCurrentCircuit().getName())) {
				circuitsList.setSelectedIndex(i);
			}
			i++;
		}
		MyProject.addProjectListener(myprojList);
		MyProject.getLogisimFile().addLibraryListener(myliblistener);
		circuitsList.setActionCommand("Circuit");
		circuitsList.addActionListener(this);
		panel.add(circuitsList, c);

		// Big TODO: add in all classes (Settings and this one) support for
		// board xmls stored on disc (rather than in the resources directory)
		// change target board
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 3;
		textTargetBoard.setEnabled(true);
		panel.add(textTargetBoard, c);
		c.gridx = 1;
		c.gridwidth = 2;
		boardsList.addItem("Other");
		i = 1;
		for (String boardname : MySettings.GetBoardNames()) {
			boardsList.addItem(boardname);
			if (boardname.equals(MySettings.GetSelectedBoard())) {
				boardsList.setSelectedIndex(i);
			}
			i++;
		}
		boardsList.setEnabled(true);
		boardsList.addActionListener(this);
		boardsList.setActionCommand("targetBoard");
		panel.add(boardsList, c);

		// select clock frequency
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 4;
		textTargetFreq.setEnabled(true);
		panel.add(textTargetFreq, c);
		clockOption.setEnabled(true);
                clockOption.addItem(clockMax);
                clockOption.addItem(clockDiv);
                clockOption.addItem(clockDyn);
                clockOption.setSelectedItem(clockDiv);
                clockDivRate.setEnabled(true);
                clockDivCount.setEnabled(true);
                clockDivRate.setEditable(true);
                clockDivCount.setEditable(true);
		clockOption.setActionCommand("ClockOption");
		clockDivRate.setActionCommand("ClockDivRate");
		clockDivCount.setActionCommand("ClockDivCount");
		clockOption.addActionListener(this);
		clockDivRate.addActionListener(this);
		clockDivCount.addActionListener(this);
		c.gridy = 4;
		c.gridx = 1;
		panel.add(clockOption, c);
		c.gridx = 2;
		panel.add(clockDivRate, c);
		c.gridy = 5;
		c.gridx = 1;
		textTargetDiv.setEnabled(true);
		panel.add(textTargetDiv, c);
		c.gridx = 2;
		panel.add(clockDivCount, c);


		// select annotation level
		c.gridx = 0;
		c.gridy = 6;
		textAnnotation.setEnabled(true);
		panel.add(textAnnotation, c);
		annotationList.addItem("Relabel all components");
		annotationList.addItem("Add missing labels");
		annotationList.setSelectedIndex(1);
		c.gridwidth = 2;
		c.gridx = 1;
		panel.add(annotationList, c);

		/* Read the selected board information to retrieve board picture */
		MyBoardInformation = new BoardReaderClass(
				MySettings.GetSelectedBoardFileName()).GetBoardInformation();
		MyBoardInformation
				.setBoardName(boardsList.getSelectedItem().toString());
		boardIcon = new BoardIcon(MyBoardInformation.GetImage());
		// set board image on panel creation
		boardPic.setIcon(boardIcon);
		c.gridx = 3;
		c.gridy = 2;
		c.gridheight = 6;
		// c.gridwidth = 2;
		panel.add(boardPic, c);

		c.gridheight = 1;
		// c.gridwidth = 1;

                populateClockDivOptions();
                updateClockOptions();
		MyProject.getSimulator().addSimulatorListener(mysimlistener);

		// validate button
		validateButton.setActionCommand("Download");
		validateButton.setText("Download");
		validateButton.addActionListener(this);
		c.gridwidth = 1;
		c.gridx = 1;
		c.gridy = 7;
		panel.add(validateButton, c);

		// write to flash
		writeToFlash.setVisible(MyBoardInformation.fpga.isFlashDefined());
		writeToFlash.setSelected(false);
		c.gridx = 2;
		c.gridy = 7;
		panel.add(writeToFlash, c);

		// annotate button
		annotateButton.setActionCommand("annotate");
		annotateButton.setText("Annotate");
		annotateButton.addActionListener(this);
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 7;
		panel.add(annotateButton, c);

		// HDL Type Button
		HDLType.addItem(Settings.VHDL);
		HDLType.addItem(Settings.VERILOG);
                HDLType.setSelectedItem(MySettings.GetHDLType());
		HDLType.setEnabled(true);
		HDLType.addActionListener(this);
		HDLType.setActionCommand("HDLType");
		c.gridx = 0;
		c.gridy = 0;
		panel.add(HDLType, c);

		// HDL Only Radio
                HDLOnly.addItem(OnlyHDLMessage);
                HDLOnly.addItem(HDLandDownloadMessage);
                HDLOnly.addItem(OnlyDownloadMessage);
                HDLOnlyUpdate();
		HDLOnly.setActionCommand("HDLOnly");
		HDLOnly.addActionListener(this);
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = 0;
		panel.add(HDLOnly, c);

		// Tool Settings
		ToolSettings.setText("Settings");
		ToolSettings.setActionCommand("ToolSettings");
		ToolSettings.addActionListener(this);
		c.gridwidth = 2;
		c.gridx = 3;
		c.gridy = 0;
		panel.add(ToolSettings, c);

		tabbedPane.add(messages); // index 0

		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 8;
		c.gridwidth = 5;
                c.weightx = 1;
                c.weighty = 1;

		tabbedPane.setPreferredSize(new Dimension(700, 20 * Console.FONT_SIZE));
		panel.add(tabbedPane, c);

		panel.pack();
		Dimension size = panel.getSize();
		size.height -= 10 * Console.FONT_SIZE;
		panel.setMinimumSize(size);

		panel.setLocation(Projects.getCenteredLoc(panel.getWidth(), panel.getHeight()));
		// panel.setLocationRelativeTo(null);
		
		panel.setVisible(false);
		if (MyProject.getLogisimFile().getLoader().getMainFile() != null) {
			MapPannel = new ComponentMapDialog(panel, MyProject
					.getLogisimFile().getLoader().getMainFile()
					.getAbsolutePath());
		} else {
			MapPannel = new ComponentMapDialog(panel, "");
		}
		MapPannel.SetBoardInformation(MyBoardInformation);

                HDLOnlyUpdate();
		validateButton.requestFocus();
	}

        private void updateClockOptions() {
            /*
            LogisimFile myfile = MyProject.getLogisimFile();
            String CircuitName = circuitsList.getSelectedItem().toString();
            Circuit RootSheet = myfile.getCircuit(CircuitName);
            int nClocks = RootSheet.getNetList().NumberOfClockTrees();
            clockOption.setEnabled(nClocks > 0);
            clockDivRate.setEnabled(nClocks > 0);
            clockDivCount.setEnabled(nClocks > 0);
            */
        }

		boolean updatingClockMenus = false;
        private void populateClockDivOptions() {
			updatingClockMenus = true;
            clockDivCount.removeAllItems();
            clockDivRate.removeAllItems();
            long base = MyBoardInformation.fpga.getClockFrequency();
            ArrayList<Integer> counts = new ArrayList<>();
            ArrayList<Double> freqs = new ArrayList<>();
            double ff = (double)base;
            while (ff >= MenuSimulate.SupportedTickFrequencies[0]*2) {
                freqs.add(ff);
                ff /= 2;
            }
            for (double f : MenuSimulate.SupportedTickFrequencies) {
                freqs.add(f);
            }
            for (double f : freqs) {
                int count = countForFreq(base, f);
                if (counts.contains(count))
                    continue;
                counts.add(count);
                String rate = rateForCount(base, count);
                clockDivCount.addItem(count);
                clockDivRate.addItem(new ExactRate(base, count));
                if (Math.abs((MyProject.getSimulator().getTickFrequency() - f)/f) < 0.0001) {
                    clockDivCount.setSelectedItem(count);
                    clockDivRate.setSelectedItem(new ExactRate(base, count));
                }
            }
			if (clockDivCount.getSelectedItem() == null && clockDivCount.getItemCount() > 0) {
				clockDivCount.setSelectedIndex(0);
			}
			if (clockDivRate.getSelectedItem() == null && clockDivRate.getItemCount() > 0)
				clockDivRate.setSelectedIndex(0);
			updatingClockMenus = false;
			setClockDivCount();
			setClockDivRate();
        }

        private void setClockOption() {
            boolean div = clockOption.getSelectedItem().equals(clockDiv);
            boolean max = clockOption.getSelectedItem().equals(clockMax);
            clockDivRate.setEnabled(div);
            clockDivCount.setEnabled(div);
            // textTargetDiv.setEnabled(div);
            long base = MyBoardInformation.fpga.getClockFrequency();
            if (max) {
                clockDivRate.setSelectedItem(new ExactRate(base, 0));
                clockDivCount.setSelectedItem("undivided");
            } else if (div) {
                if (prevSelectedDivCount > 0 && prevSelectedDivRate != null) {
                    clockDivCount.setSelectedItem(prevSelectedDivCount);
                    clockDivRate.setSelectedItem(prevSelectedDivRate);
                } else {
                    ChangeTickFrequency();
                }
            } else {
                clockDivRate.setSelectedItem(new ExactRate(base, -1));
                clockDivCount.setSelectedItem("set in circuit");
            }
        }

        private int prevSelectedDivCount = 0;
        private Object prevSelectedDivRate = null;

        private static class ExactRate {
            long base;
            int count;
            String rate;
            public ExactRate(long base, int count) {
                this.base = base;
                this.count = count;
                if (count < 0)
                    rate = "varies";
                else if (count == 0)
                    rate = rateForFreq(2.0*base);
                else
                    rate = rateForCount(base, count);
            }
            public String toString() {
                return rate;
            }
            @Override
            public boolean equals(Object other) {
                if (other instanceof ExactRate) {
                    ExactRate that = (ExactRate)other;
                    return (base == that.base && count == that.count);
                }
                return false;
            }
            @Override public int hashCode() {
                return (int)(39 * (base + 27) + count);
            }
        }

        private void setClockDivRate() {
			if (updatingClockMenus) {
				return;
			}
            if (!clockOption.getSelectedItem().equals(clockDiv))
                return;
            long base = MyBoardInformation.fpga.getClockFrequency();
            Object o = clockDivRate.getSelectedItem();
            Integer i;
            if (o instanceof ExactRate) {
                i = ((ExactRate)o).count;
            } else {
                // approximate
                i = countForRate(base, o.toString());
                if (i == null) {
                    if (prevSelectedDivCount > 0 && prevSelectedDivRate != null) {
                        clockDivCount.setSelectedItem(prevSelectedDivCount);
                        clockDivRate.setSelectedItem(prevSelectedDivRate);
                    } else {
                        ChangeTickFrequency();
                    }
                    return;
                }
                String rate = rateForCount(base, i);
                clockDivRate.setSelectedItem(rate); // rounds to nearest acceptable value
            }
            if (clockDivCount.getSelectedItem() == null || !clockDivCount.getSelectedItem().equals(i)) {
                clockDivCount.setSelectedItem(i);
			}
            prevSelectedDivRate = clockDivRate.getSelectedItem();
            prevSelectedDivCount = (Integer)clockDivCount.getSelectedItem();
        }

        private void setClockDivCount() {
			if (updatingClockMenus) {
				return;
			}

            if (!clockOption.getSelectedItem().equals(clockDiv))
                return;
            long base = MyBoardInformation.fpga.getClockFrequency();
			Object item = clockDivCount.getSelectedItem();
            String s = item == null ? "-1" : item.toString();
            int count = -1;
            try { count = Integer.parseInt(s); }
            catch (NumberFormatException e) { }
            if (count <= 0) {
                if (prevSelectedDivCount > 0 && prevSelectedDivRate != null) {
                    clockDivCount.setSelectedItem(prevSelectedDivCount);
                    clockDivRate.setSelectedItem(prevSelectedDivRate);
                } else {
                    ChangeTickFrequency();
                }
                return;
            }
            clockDivRate.setSelectedItem(new ExactRate(base, count));
            prevSelectedDivRate = clockDivRate.getSelectedItem();
            prevSelectedDivCount = count;
        }
	
        private static Integer countForRate(long base, String rate) {
            rate = rate.toLowerCase().trim();
            int multiplier = 1;
            if (rate.endsWith("khz")) {
                multiplier = 1000;
                rate = rate.substring(0, rate.length() - 3);
            } else if (rate.endsWith("mhz")) {
                multiplier = 1000000;
                rate = rate.substring(0, rate.length() - 3);
            } else if (rate.endsWith("hz")) {
                multiplier = 1;
                rate = rate.substring(0, rate.length() - 2);
            }
            double freq;
            try {
                freq = Double.parseDouble(rate) * multiplier;
            } catch (NumberFormatException e) {
                return null;
            }
            if (freq <= 0)
                return null;
            return countForFreq(base, freq);
        }

        // base=25mhz, actual=50mhz, count=1 --> 0 0 0 0 0 0 --> 25mhz = 25/1
        // base=25mhz, actual=50mhz, count=2 --> 1 0 1 0 1 0 --> 12.5mhz = 25/2
        // base=25mhz, actual=50mhz, count=3 --> 2 1 0 2 1 0 --> 8.3mhz = 25/3
        private static int countForFreq(long base, double freq) {
            long count = (long)((double)base / freq);
            if ((count > (long) 0x7FFFFFFF) | (count < 0))
                count = (long) 0x7FFFFFFF;
            else if (count == 0)
                count = 1;
            return (int)count;
        }

        private static String rateForCount(long base, int count) {
            double f = (double)base / count;
            return rateForFreq(f);
        }

        private static String rateForFreq(double f) {
            String suffix;
            if (f < 0.1) {
                return String.format("%g Hz", f);
            } else if (f < 1000) {
                suffix = "Hz";
            } else if (f < 1000000) {
                f /= 1000;
                suffix = "kHz";
            } else {
                suffix = "MHz";
                f /= 1000000;
            }
            if (Math.abs(f - Math.round(f)) < 0.1)
                return String.format("%.0f %s", f, suffix);
            else
                return String.format("%.2f %s", f, suffix);
        }

        private void RepaintConsoles() {
            Rectangle rect = tabbedPane.getBounds();
            rect.x = 0;
            rect.y = 0;
            if (EventQueue.isDispatchThread())
                tabbedPane.paintImmediately(rect);
            else
                tabbedPane.repaint(rect);
        }

        private void ClearConsoles() {
            synchronized(consoles) {
                consoles.clear();
                tabbedPane.setSelectedIndex(0);
                for (int i = tabbedPane.getTabCount() - 1; i > 0; i--) {
                    tabbedPane.removeTabAt(i);
                }
            }
            RepaintConsoles();
        }

        public void NewConsole(String title) {
            Console console = new Console(title);
            synchronized(consoles) {
                consoles.add(console);
		tabbedPane.add(console);
            }
        }

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("annotate")) {
			Annotate(annotationList.getSelectedIndex() == 0);
		} else if (e.getActionCommand().equals("ClockOption")) {
			setClockOption();
		} else if (e.getActionCommand().equals("ClockDivRate")) {
			setClockDivRate();
		} else if (e.getActionCommand().equals("ClockDivCount")) {
			setClockDivCount();
		} else if (e.getActionCommand().equals("HDLType")) {
			handleHDLType();
		} else if (e.getActionCommand().equals("ToolSettings")) {
			selectToolSettings();
		} else if (e.getActionCommand().equals("HDLOnly")) {
			handleHDLOnly();
		} else if (e.getActionCommand().equals("Download")) {
			DownLoad();
                } else if (e.getActionCommand().equals("mainCircuit")) {
                        updateClockOptions();
		} else if (e.getActionCommand().equals("targetBoard")) {
                        populateClockDivOptions();
			if (!boardsList.getSelectedItem().equals("Other")) {
				MySettings.SetSelectedBoard(boardsList.getSelectedItem()
						.toString());
				MySettings.UpdateSettingsFile();
				MyBoardInformation = new BoardReaderClass(
						MySettings.GetSelectedBoardFileName())
						.GetBoardInformation();
				MyBoardInformation.setBoardName(boardsList.getSelectedItem()
						.toString());
				MapPannel.SetBoardInformation(MyBoardInformation);
				boardIcon = new BoardIcon(MyBoardInformation.GetImage());
				boardPic.setIcon(boardIcon);
				boardPic.repaint();
                                HDLOnlyUpdate();
				writeToFlash.setSelected(false);
				writeToFlash.setVisible(MyBoardInformation.fpga.isFlashDefined());
			} else {
				String NewBoardFileName = GetBoardFile();
				MyBoardInformation = new BoardReaderClass(NewBoardFileName).GetBoardInformation();
				if (MyBoardInformation == null) {
					for (int index = 0 ; index < boardsList.getItemCount() ; index ++)
						if (boardsList.getItemAt(index).equals(MySettings.GetSelectedBoard()))
							boardsList.setSelectedIndex(index);
					this.AddErrors("\""+NewBoardFileName+"\" does not has the proper format for a board file!\n");
				} else {
					String[] Parts = NewBoardFileName.split(File.separator);
					String BoardInfo = Parts[Parts.length-1].replace(".xml", "");
					Boolean CanAdd = true;
					for (int index = 0 ; index < boardsList.getItemCount() ; index ++)
						if (boardsList.getItemAt(index).equals(BoardInfo)) {
							this.AddErrors("A board with the name \""+BoardInfo+"\" already exisits, cannot add new board descriptor\n");
							CanAdd = false;
						}
					if (CanAdd) {
						MySettings.AddExternalBoard(NewBoardFileName);
						MySettings.SetSelectedBoard(BoardInfo);
						MySettings.UpdateSettingsFile();
						boardsList.addItem(BoardInfo);
						for (int index = 0 ; index < boardsList.getItemCount() ; index ++)
							if (boardsList.getItemAt(index).equals(BoardInfo))
								boardsList.setSelectedIndex(index);
						MyBoardInformation.setBoardName(BoardInfo);
						MapPannel.SetBoardInformation(MyBoardInformation);
						boardIcon = new BoardIcon(MyBoardInformation.GetImage());
						boardPic.setIcon(boardIcon);
						boardPic.repaint();
                                                HDLOnlyUpdate();
						writeToFlash.setSelected(false);
						writeToFlash.setVisible(MyBoardInformation.fpga.isFlashDefined());
					} else {
						for (int index = 0 ; index < boardsList.getItemCount() ; index ++)
							if (boardsList.getItemAt(index).equals(MySettings.GetSelectedBoard()))
								boardsList.setSelectedIndex(index);
					}
				}
				
			}
		}
	}

	public void AddConsole(String Message) {
            Console area;
            synchronized(consoles) {
                int i = consoles.size() - 1;
                if (i == -1) {
                    NewConsole("Console");
                    i = 0;
                }
		tabbedPane.setSelectedIndex(1 + i);
                area = consoles.get(i);
            }
            area.append(Message);
            RepaintConsoles();
	}

	public void AddErrors(String Message) {
                messages.append(Message, Console.ERROR);
                RepaintConsoles();
        }

	public void AddInfo(String Message) {
                messages.append(Message, Console.INFO);
                RepaintConsoles();
	}

	public void AddWarning(String Message) {
                messages.append(Message, Console.WARNING);
                RepaintConsoles();
        }

	private void Annotate(boolean ClearExistingLabels) {
		clearAllMessages();
		String CircuitName = circuitsList.getSelectedItem().toString();
		Circuit root = MyProject.getLogisimFile().getCircuit(CircuitName);
		if (root != null) {
			if (ClearExistingLabels) {
				root.ClearAnnotationLevel();
			}
			root.Annotate(ClearExistingLabels, MyReporter);
			MyReporter.AddInfo("Annotation done");
			/* TODO: Dirty hack, see Circuit.java function Annotate for details */
			MyProject.repaintCanvas();
			MyProject.getLogisimFile().setDirty(true);
		}
	}

	private void ChangeTickFrequency() {
            long base = MyBoardInformation.fpga.getClockFrequency();
            for (double f : MenuSimulate.SupportedTickFrequencies) {
                int count = countForFreq(base, f);
                if (Math.abs((MyProject.getSimulator().getTickFrequency() - f)/f) < 0.0001) {
                    clockDivCount.setSelectedItem(count);
                    clockDivRate.setSelectedItem(new ExactRate(base, count));
                }
            }
	}

	private boolean CleanDirectory(String dir) {
		try {
			File thisDir = new File(dir);
			if (!thisDir.exists()) {
				return true;
			}
			for (File theFiles : thisDir.listFiles()) {
				if (theFiles.isDirectory()) {
					if (!CleanDirectory(theFiles.getPath())) {
						return false;
					}
				} else {
					if (!theFiles.delete()) {
						return false;
					}
				}
			}
			if (!thisDir.delete()) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			MyReporter.AddFatalError("Could not remove directory tree :" + dir);
			return false;
		}
	}

	private void clearAllMessages() {
		messages.clear();
                RepaintConsoles();
	}

	public void ClearConsole() {
            Console area;
            synchronized(consoles) {
                int i = consoles.size() - 1;
                if (i == -1) {
                    return;
                }
		tabbedPane.setSelectedIndex(1 + i);
                area = consoles.get(i);
            }
            area.clear();
            RepaintConsoles();
	}

        private boolean skipHdl() {
             return HDLOnly.getSelectedItem().toString().equals(OnlyDownloadMessage);
        }

	private void DownLoad() {
                ClearConsoles();
                String Name = MyProject.getLogisimFile().getName();
                if (Name.indexOf(" ") != -1) {
                    AddErrors("The file '" + Name + "' contains a space.");
                    AddErrors("Spaces are not permitted by the HDL synthesis engine. Please");
                    AddErrors("rename your file and directory to not have any spaces.");
                    return;
                }
                String Dir = MyProject.getLogisimFile().getLoader().getMainFile().toString();
                if (Dir.indexOf(" ") != -1) {
                    AddErrors("The directory '" + Dir + "' contains a space.");
                    AddErrors("Spaces are not permitted by the HDL synthesis engine. Please");
                    AddErrors("rename your file and directory to not have any spaces.");
                    return;
                }
		if (MySettings.GetHDLOnly() || !skipHdl()) {
                    AddInfo("Performing DRC");
                    if (!performDRC()) {
                        AddErrors("DRC Failed");
                        return;
                    }
                    AddInfo("Mapping Pins");
                    if (!MapDesign()) {
                        AddErrors("Design could not be mapped");
                        return;
                    }
                    AddInfo("Generating HDL");
                    if (!writeHDL()) {
                        AddErrors("Could not create HDL files");
                        return;
                    }
                    AddInfo("Successfully created HDL files");
                } else if (!MySettings.GetHDLOnly()) {
                    AddInfo("*** NOTE *** Skipping HDL file generation and synthesis.");
                    AddInfo("*** NOTE *** Recent changes to circuits will not take effect.");
                }
		DownLoadDesign(MySettings.GetHDLOnly());
	}

        private String GetWorkspacePath() {
                File projFile = MyProject.getLogisimFile().getLoader().getMainFile();
		return MySettings.GetWorkspacePath(projFile);
        }

	private boolean readyForDownload() {
	    String CircuitName = circuitsList.getSelectedItem().toString();
	    String ProjectDir = GetWorkspacePath() + File.separator
			    + MyProject.getLogisimFile().getName();
	    if (!ProjectDir.endsWith(File.separator)) {
		    ProjectDir += File.separator;
	    }
	    LogisimFile myfile = MyProject.getLogisimFile();
	    Circuit RootSheet = myfile.getCircuit(CircuitName);
	    ProjectDir += CorrectLabel.getCorrectLabel(RootSheet.getName())
			    + File.separator;
	    String SourcePath = ProjectDir + MySettings.GetHDLType().toLowerCase()
			    + File.separator;
	    if (MyBoardInformation.fpga.getVendor() == FPGAClass.VendorAltera) {
		return AlteraDownload.readyForDownload(ProjectDir + HDLPaths[SandboxPath] + File.separator);
	    } else {
		// todo: xilinx readyForDownload()
		return MapPannel.isDoneAssignment();
	    }
	}

	private void DownLoadDesign(boolean generateOnly) {
		String CircuitName = circuitsList.getSelectedItem().toString();
		String ProjectDir = GetWorkspacePath() + File.separator
				+ MyProject.getLogisimFile().getName();
		if (!ProjectDir.endsWith(File.separator)) {
			ProjectDir += File.separator;
		}
		LogisimFile myfile = MyProject.getLogisimFile();
		Circuit RootSheet = myfile.getCircuit(CircuitName);
		ProjectDir += CorrectLabel.getCorrectLabel(RootSheet.getName())
				+ File.separator;
		String SourcePath = ProjectDir + MySettings.GetHDLType().toLowerCase()
				+ File.separator;
		if (HDLOnly.getSelectedItem().toString().equals(OnlyDownloadMessage)) {
                    if (!readyForDownload()) {
                        MyReporter.AddError("HDL files are not ready for download. Use \"Synthesize and download\" instead.");
                        return;
                    }
                } else {
		    if (!MapPannel.isDoneAssignment()) {
			    MyReporter.AddError("Not all pins have been assigned. Download to board canceled.");
			    return;
		    }
		    ArrayList<String> Entities = new ArrayList<String>();
		    ArrayList<String> Behaviors = new ArrayList<String>();
		    GetVHDLFiles(ProjectDir, SourcePath, Entities, Behaviors,
				    MySettings.GetHDLType());
		    if (MyBoardInformation.fpga.getVendor() == FPGAClass.VendorAltera) {
			    if (!AlteraDownload.GenerateQuartusScript(MyReporter, ProjectDir
					    + HDLPaths[ScriptPath] + File.separator,
					    RootSheet.getNetList(), MyMappableResources,
					    MyBoardInformation, Entities, Behaviors,
					    MySettings.GetHDLType())) {
				MyReporter.AddError("Can't generate quartus script");
				return;
			    }
		    } else {
			    if (!XilinxDownload.GenerateISEScripts(MyReporter, ProjectDir,
					    ProjectDir + HDLPaths[ScriptPath] + File.separator,
					    ProjectDir + HDLPaths[UCFPath] + File.separator,
					    RootSheet.getNetList(), MyMappableResources,
					    MyBoardInformation, Entities, Behaviors,
					    MySettings.GetHDLType(),
					    writeToFlash.isSelected())
					    && !generateOnly) {
				MyReporter.AddError("Can't generate xilinx script");
				return;
			    }
		    }
		}
		if (MyBoardInformation.fpga.getVendor() == FPGAClass.VendorAltera) {
		    AlteraDownload.Download(MySettings, ProjectDir
				    + HDLPaths[ScriptPath] + File.separator, SourcePath,
				    ProjectDir + HDLPaths[SandboxPath] + File.separator,
				    MyReporter);
		} else {
		    XilinxDownload.Download(MySettings, MyBoardInformation,
				    ProjectDir + HDLPaths[ScriptPath] + File.separator,
				    ProjectDir + HDLPaths[UCFPath] + File.separator,
				    ProjectDir, ProjectDir + HDLPaths[SandboxPath]
						    + File.separator, MyReporter);
		}
	}

	private boolean GenDirectory(String dir) {
		try {
			File Dir = new File(dir);
			if (Dir.exists()) {
				return true;
			}
			return Dir.mkdirs();
		} catch (Exception e) {
			MyReporter
					.AddFatalError("Could not check/create directory :" + dir);
			return false;
		}
	}

	private void GetVHDLFiles(String SourcePath, String Path,
			ArrayList<String> Entities, ArrayList<String> Behaviors,
			String HDLType) {
		File Dir = new File(Path);
		File[] Files = Dir.listFiles();
		for (File thisFile : Files) {
			if (thisFile.isDirectory()) {
				if (Path.endsWith(File.separator)) {
					GetVHDLFiles(SourcePath, Path + thisFile.getName(),
							Entities, Behaviors, HDLType);
				} else {
					GetVHDLFiles(SourcePath,
							Path + File.separator + thisFile.getName(),
							Entities, Behaviors, HDLType);
				}
			} else {
				String EntityMask = (HDLType.equals(Settings.VHDL)) ? FileWriter.EntityExtension
						+ ".vhd"
						: ".v";
				String ArchitecturMask = (HDLType.equals(Settings.VHDL)) ? FileWriter.ArchitectureExtension
						+ ".vhd"
						: "#not_searched#";
				if (thisFile.getName().endsWith(EntityMask)) {
					Entities.add((Path + File.separator + thisFile.getName())
							.replace("\\", "/"));
					// Entities.add((Path+File.separator+thisFile.getName()).replace(SourcePath,
					// "../"));
				} else if (thisFile.getName().endsWith(ArchitecturMask)) {
					Behaviors.add((Path + File.separator + thisFile.getName())
							.replace("\\", "/"));
					// Behaviors.add((Path+File.separator+thisFile.getName()).replace(SourcePath,
					// "../"));
				}
			}
		}
	}

	private void handleHDLOnly() {
                /*
		boolean hdlonly = HDLOnly.getSelectedItem().toString().equals(OnlyHDLMessage);
		MySettings.SetHdlOnly(hdlonly);
		skipHDL.setSelected(false);
		skipHDL.setEnabled(!hdlonly && readyForDownload());
                */
		if (HDLOnly.getSelectedItem().toString().equals(OnlyHDLMessage)) {
			MySettings.SetHdlOnly(true);
		} else {
			MySettings.SetHdlOnly(false);
		}
	}

	private void handleHDLType() {
                if (HDLType.getSelectedIndex() == 0)
			MySettings.SetHDLType(Settings.VHDL);
		else
			MySettings.SetHDLType(Settings.VERILOG);
		if (!MySettings.UpdateSettingsFile()) {
			AddErrors("***SEVERE*** Could not update the FPGACommander settings file");
		} else {
			AddInfo("Updated the FPGACommander settings file");
		}
		String CircuitName = circuitsList.getSelectedItem().toString();
		Circuit root = MyProject.getLogisimFile().getCircuit(CircuitName);
		if (root != null) {
			root.ClearAnnotationLevel();
		}
	}

	private boolean MapDesign() {
		String CircuitName = circuitsList.getSelectedItem().toString();
		LogisimFile myfile = MyProject.getLogisimFile();
		Circuit RootSheet = myfile.getCircuit(CircuitName);
		Netlist RootNetlist = RootSheet.getNetList();
		if (MyBoardInformation == null) {
			MyReporter
					.AddError("INTERNAL ERROR: No board information available ?!?");
			return false;
		}

		Map<String, ArrayList<Integer>> BoardComponents = MyBoardInformation
				.GetComponents();
		MyReporter.AddInfo("The Board " + MyBoardInformation.getBoardName()
				+ " has:");
		for (String key : BoardComponents.keySet()) {
			MyReporter.AddInfo(BoardComponents.get(key).size() + " " + key
					+ "(s)");
		}
		/*
		 * At this point I require 2 sorts of information: 1) A hierarchical
		 * netlist of all the wires that needs to be bubbled up to the toplevel
		 * in order to connect the LEDs, Buttons, etc. (hence for the HDL
		 * generation). 2) A list with all components that are required to be
		 * mapped to PCB components. Identification can be done by a hierarchy
		 * name plus component/sub-circuit name
		 */
		MyMappableResources = new MappableResourcesContainer(
				MyBoardInformation, RootNetlist);
		if (!MyMappableResources.IsMappable(BoardComponents, MyReporter)) {
			return false;
		}

		MapPannel.SetBoardInformation(MyBoardInformation);
		MapPannel.SetMappebleComponents(MyMappableResources);
		panel.setVisible(false);
		MapPannel.SetVisible(true);
		panel.setVisible(true);
		if (MyMappableResources.UnmappedList().isEmpty()) {
			MyMappableResources.BuildIOMappingInformation();
			return true;
		}

		MyReporter
				.AddError("Not all IO components have been mapped to the board "
						+ MyBoardInformation.getBoardName()
						+ " please map all components to continue!");
		return false;
	}

	private boolean performDRC() {
		clearAllMessages();
		String CircuitName = circuitsList.getSelectedItem().toString();
		Circuit root = MyProject.getLogisimFile().getCircuit(CircuitName);
		ArrayList<String> SheetNames = new ArrayList<String>();
		int DRCResult;
		if (root == null) {
			DRCResult = Netlist.DRC_ERROR;
		} else {
			root.getNetList().ClearNetlist();
			DRCResult = root.getNetList().DesignRuleCheckResult(MyReporter,
					HDLType.getSelectedItem().toString(), true,
					MyBoardInformation.fpga.getVendor(), SheetNames);
		}
		return (DRCResult == Netlist.DRC_PASSED);
	}

	private void RebuildCircuitSelection() {
		circuitsList.removeAllItems();
		panel.setTitle("FPGA Commander : "
				+ MyProject.getLogisimFile().getName());
		int i = 0;
		for (Circuit thisone : MyProject.getLogisimFile().getCircuits()) {
			circuitsList.addItem(thisone.getName());
			if (MyProject.getCurrentCircuit() != null &&
                                thisone.getName().equals(
					MyProject.getCurrentCircuit().getName())) {
				circuitsList.setSelectedIndex(i);
			}
			i++;
		}
	}

	FPGASettingsDialog settings;
	private void selectToolSettings() {
		if (settings == null) 
			settings = new FPGASettingsDialog(panel, MySettings);
		settings.SetVisible(true);

		if (MyBoardInformation.fpga.getVendor() == FPGAClass.VendorAltera) {
			if (!MySettings.GetAlteraToolPath().equals(Settings.Unknown)) {
				HDLOnly.setEnabled(true);
				MySettings.SetHdlOnly(false);
				HDLOnly.setSelectedItem(HDLandDownloadMessage);
				if (!MySettings.UpdateSettingsFile()) {
					AddErrors("***SEVERE*** Could not update the FPGACommander settings file");
				} else {
					AddInfo("Updated the FPGACommander settings file");
				}
			} else {
				AddErrors("***FATAL*** Required programs of the Altera toolsuite not found!");
				String prgs = "";
				for (String p : Settings.AlteraPrograms) {
					prgs = prgs + "\n     " + p;
				}
				AddErrors("***INFO*** Please select a directory containing these Altera programs:" + prgs);
			}
		} else if (MyBoardInformation.fpga.getVendor() == FPGAClass.VendorXilinx) {
			if (!MySettings.GetXilinxToolPath().equals(Settings.Unknown)) {
				HDLOnly.setEnabled(true);
				MySettings.SetHdlOnly(false);
				HDLOnly.setSelectedItem(HDLandDownloadMessage);
				if (!MySettings.UpdateSettingsFile()) {
					AddErrors("***SEVERE*** Could not update the FPGACommander settings file");
				} else {
					AddInfo("Updated the FPGACommander settings file");
				}
			} else {
				AddErrors("***FATAL*** Required programs of the Xilinx toolsuite not found!");
				String prgs = "";
				for (String p : Settings.XilinxPrograms) {
					prgs = prgs + "\n     " + p;
				}
				AddErrors("***INFO*** Please select a directory containing these Xilinx programs:" + prgs);
			}
		}
	}
	
	private String GetBoardFile() {
		JFileChooser fc = new JFileChooser(GetWorkspacePath());
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Board files", "xml", "xml");
		fc.setFileFilter(filter);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		File test = new File(GetWorkspacePath());
		if (test.exists()) {
			fc.setSelectedFile(test);
		}
		fc.setDialogTitle("Board description selection");
		int retval = fc.showOpenDialog(null);
		if (retval == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			return file.getPath();
		} else return "";
	}

	private void selectWorkSpace() {
		JFileChooser fc = new JFileChooser(GetWorkspacePath());
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		File test = new File(GetWorkspacePath());
		if (test.exists()) {
			fc.setSelectedFile(test);
		}
		fc.setDialogTitle("Workspace Directory Selection");
		int retval = fc.showOpenDialog(null);
		if (retval == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			if (file.getPath().endsWith(File.separator)) {
				MySettings.SetStaticWorkspacePath(file.getPath());
			} else {
				MySettings.SetStaticWorkspacePath(file.getPath() + File.separator);
			}
			if (!MySettings.UpdateSettingsFile()) {
				AddErrors("***SEVERE*** Could not update the FPGACommander settings file");
			} else {
				AddInfo("Updated the FPGACommander settings file");
			}
		}
	}

	private void SetCurrentSheet(String Name) {
		for (int i = 0; i < circuitsList.getItemCount(); i++) {
			if (((String) circuitsList.getItemAt(i)).equals(Name)) {
				circuitsList.setSelectedIndex(i);
				circuitsList.repaint();
				return;
			}
		}
	}

	public void ShowGui() {
		if (!panel.isVisible()) {
			panel.setVisible(true);
		} else {
			panel.setVisible(false);
			panel.setVisible(true);
		}
	}

	private boolean writeHDL() {
		String CircuitName = circuitsList.getSelectedItem().toString();
		if (!GenDirectory(GetWorkspacePath() + File.separator
				+ MyProject.getLogisimFile().getName())) {
			MyReporter.AddFatalError("Unable to create directory: \""
					+ GetWorkspacePath() + File.separator
					+ MyProject.getLogisimFile().getName() + "\"");
			return false;
		}
		String ProjectDir = GetWorkspacePath() + File.separator
				+ MyProject.getLogisimFile().getName();
		if (!ProjectDir.endsWith(File.separator)) {
			ProjectDir += File.separator;
		}
		LogisimFile myfile = MyProject.getLogisimFile();
		Circuit RootSheet = myfile.getCircuit(CircuitName);
		ProjectDir += CorrectLabel.getCorrectLabel(RootSheet.getName())
				+ File.separator;
		if (!CleanDirectory(ProjectDir)) {
			MyReporter
					.AddFatalError("Unable to cleanup old project files in directory: \""
							+ ProjectDir + "\"");
			return false;
		}
		if (!GenDirectory(ProjectDir)) {
			MyReporter.AddFatalError("Unable to create directory: \""
					+ ProjectDir + "\"");
			return false;
		}
		for (int i = 0; i < HDLPaths.length; i++) {
			if (!GenDirectory(ProjectDir + HDLPaths[i])) {
				MyReporter.AddFatalError("Unable to create directory: \""
						+ ProjectDir + HDLPaths[i] + "\"");
				return false;
			}
		}
		Set<String> GeneratedHDLComponents = new HashSet<String>();
		HDLGeneratorFactory Worker = RootSheet.getSubcircuitFactory()
				.getHDLGenerator(MySettings.GetHDLType(),
						RootSheet.getStaticAttributes(),
						MyBoardInformation.fpga.getVendor());
		if (Worker == null) {
			MyReporter
					.AddFatalError("Internal error on HDL generation, null pointer exception");
			return false;
		}
		if (!Worker.GenerateAllHDLDescriptions(GeneratedHDLComponents,
				ProjectDir, null, MyReporter, MySettings.GetHDLType())) {
			return false;
		}
		/* Here we generate the top-level shell */
                int TickPeriod;
                if (clockOption.getSelectedItem().equals(clockMax))
                    TickPeriod = 0;
                else if (clockOption.getSelectedItem().equals(clockDyn))
                    TickPeriod = -1;
                else {
					Object item = clockDivCount.getSelectedItem();
					String s = item == null ? "1" : item.toString();
					TickPeriod = Integer.parseInt(s);
				}

		if (RootSheet.getNetList().NumberOfClockTrees() > 0) {
			TickComponentHDLGeneratorFactory Ticker = new TickComponentHDLGeneratorFactory(
					MyBoardInformation.fpga.getClockFrequency(),
					TickPeriod);
			if (!AbstractHDLGeneratorFactory.WriteEntity(
					ProjectDir
							+ Ticker.GetRelativeDirectory(MySettings
									.GetHDLType()), Ticker.GetEntity(
							RootSheet.getNetList(), null,
							Ticker.getComponentStringIdentifier(), MyReporter,
							MySettings.GetHDLType()), Ticker
							.getComponentStringIdentifier(), MyReporter,
					MySettings.GetHDLType())) {
				return false;
			}
			if (!AbstractHDLGeneratorFactory.WriteArchitecture(ProjectDir
					+ Ticker.GetRelativeDirectory(MySettings.GetHDLType()),
					Ticker.GetArchitecture(RootSheet.getNetList(), null, null,
							Ticker.getComponentStringIdentifier(), MyReporter,
							MySettings.GetHDLType()), Ticker
							.getComponentStringIdentifier(), MyReporter,
					MySettings.GetHDLType())) {
				return false;
			}
			HDLGeneratorFactory ClockGen = RootSheet
					.getNetList()
					.GetAllClockSources()
					.get(0)
					.getFactory()
					.getHDLGenerator(
							MySettings.GetHDLType(),
							RootSheet.getNetList().GetAllClockSources().get(0)
									.getAttributeSet(),
							MyBoardInformation.fpga.getVendor());
			String CompName = RootSheet.getNetList().GetAllClockSources()
					.get(0).getFactory().getHDLName(null);
			if (!AbstractHDLGeneratorFactory.WriteEntity(
					ProjectDir
							+ ClockGen.GetRelativeDirectory(MySettings
									.GetHDLType()), ClockGen.GetEntity(
							RootSheet.getNetList(), null, CompName, MyReporter,
							MySettings.GetHDLType()), CompName, MyReporter,
					MySettings.GetHDLType())) {
				return false;
			}
			if (!AbstractHDLGeneratorFactory.WriteArchitecture(ProjectDir
					+ ClockGen.GetRelativeDirectory(MySettings.GetHDLType()),
					ClockGen.GetArchitecture(RootSheet.getNetList(), null, null,
							CompName, MyReporter, MySettings.GetHDLType()),
					CompName, MyReporter, MySettings.GetHDLType())) {
				return false;
			}
		}
		Worker = new ToplevelHDLGeneratorFactory(
				MyBoardInformation.fpga.getClockFrequency(),
						TickPeriod, RootSheet, MyMappableResources,
                                                skipHdl());
		if (!AbstractHDLGeneratorFactory.WriteEntity(
				ProjectDir
						+ Worker.GetRelativeDirectory(MySettings.GetHDLType()),
				Worker.GetEntity(RootSheet.getNetList(), null,
						ToplevelHDLGeneratorFactory.FPGAToplevelName,
						MyReporter, MySettings.GetHDLType()), Worker
						.getComponentStringIdentifier(), MyReporter, MySettings
						.GetHDLType())) {
			return false;
		}
		if (!AbstractHDLGeneratorFactory.WriteArchitecture(
				ProjectDir
						+ Worker.GetRelativeDirectory(MySettings.GetHDLType()),
				Worker.GetArchitecture(RootSheet.getNetList(), null, null,
						ToplevelHDLGeneratorFactory.FPGAToplevelName,
						MyReporter, MySettings.GetHDLType()), Worker
						.getComponentStringIdentifier(), MyReporter, MySettings
						.GetHDLType())) {
			return false;
		}

		return true;
	}
}
