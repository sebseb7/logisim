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

package com.bfh.logisim.gui;

import java.awt.EventQueue;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.bfh.logisim.netlist.CorrectLabel;
import com.bfh.logisim.netlist.Netlist;
import com.bfh.logisim.download.FPGADownload;
import com.bfh.logisim.fpga.Board;
import com.bfh.logisim.fpga.BoardReader;
import com.bfh.logisim.fpga.Chipset;
import com.bfh.logisim.fpga.PinBindings;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGenerator;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.gui.menu.MenuSimulate;
import com.cburch.logisim.gui.generic.ComboBox;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.proj.ProjectEvent;

public class Commander extends JFrame {

  private static final String SLASH = File.separator;
  private static final String SANDBOX_DIR = "sandbox" + SLASH;
  private static final String SCRIPT_DIR = "scripts" + SLASH;
  private static final String UCF_DIR = "ucf" + SLASH;
  private static final String[] HDL_PATHS = { "verilog", "vhdl", "scripts", "sandbox", "ucf" };
  private static final String OTHER_BOARD = "Other";

  private final Project proj;
  private Board board;
  private String lang;
  private int boardsListSelectedIndex;
  private PinBindings pinBindings;
  private final FPGAReport err = new FPGAReport(this);
  private final Settings settings = new Settings();

  private static final String MAX_SPEED = "Maximum Speed";
  private static final String DIV_SPEED = "Reduced Speed";
  private static final String DYN_SPEED = "Dynamic Speed";

  // Three steps: (1) Generate, (2) Synthesize, (3 Downlaod.
  // User can do choose to do just first step, just last step, or all three steps.
  private static final String HDL_GEN_ONLY = "Generate HDL only";
  private static final String HDL_GEN_AND_DOWNLOAD = "Synthesize and Download";
  private static final String HDL_DOWNLOAD_ONLY = "Download only";

  private final JLabel textMainCircuit = new JLabel("Choose main circuit ");
  private final JLabel textTargetBoard = new JLabel("Choose target board ");
  private final JLabel textTargetFreq = new JLabel("Choose tick frequency ");
  private final JLabel textTargetDiv = new JLabel("Divide clock by...");
  private final JLabel textAnnotation = new JLabel("Annotation method");
  private final BoardIcon boardIcon = new BoardIcon();
  private final JButton annotateButton = new JButton("Annotate");
  private final JButton validateButton = new JButton("Download");
  private final JCheckBox writeToFlash = new JCheckBox("Write to flash?");
  private final ComboBox<String> boardsList = new ComboBox<>();
  private final ComboBox<Circuit> circuitsList = new ComboBox<>();
  private final ComboBox<String> clockOption = new ComboBox<>();
  private final ComboBox<Object> clockDivRate = new ComboBox<>();
  private final ComboBox<Object> clockDivCount = new ComboBox<>();
  private final ComboBox<String> annotationList = new ComboBox<>();
  private final ComboBox<String> HDLType = new ComboBox<>();
  private final ComboBox<String> HDLAction = new ComboBox<>();
  private final JButton toolSettings = new JButton("Settings");
  private final Console messages = new Console("Messages");
  private final ArrayList<Console> consoles = new ArrayList<>();
  private final JTabbedPane tabbedPane = new JTabbedPane();

  public Commander(Project p) {
    super("FPGA Commander : " + p.getLogisimFile().getName());
    proj = p;
    lang = settings.GetHDLType();

    board = BoardReader.read(settings.GetSelectedBoardFileName());
    boardIcon.setImage(board == null ? null : board.image);

    setResizable(true);
    setAlwaysOnTop(false);
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

    setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;

    // listen for project changes
    proj.addProjectListener(e -> {
      if (e.getAction() == ProjectEvent.ACTION_SET_CURRENT && e.getCircuit() != null)
        circuitsList.setSelectedItem(e.getCircuit());
      else if (e.getAction() == ProjectEvent.ACTION_SET_FILE)
        updateCircuitList();
    });
    proj.getLogisimFile().addLibraryListener(e -> {
      if (e.getAction() == LibraryEvent.ADD_TOOL || e.getAction() == LibraryEvent.REMOVE_TOOL)
        updateCircuitList();
    });

    // listen for simulator changes
    proj.getSimulator().addSimulatorListener(new Simulator.Listener() {
      public void simulatorReset(Simulator.Event e) { }
      public void propagationCompleted(Simulator.Event e) { }
      public void simulatorStateChanged(Simulator.Event e) { useTickSpeedFromSimulator(); }
    });

    // configure circuit list
    circuitsList.setRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(JList list, Object v, int i, boolean s, boolean f) {
        return super.getListCellRendererComponent(list, ((Circuit)v).getName(), i, s, f);
      }
    });
    updateCircuitList();

    // configure board list
    for (String boardname : settings.GetBoardNames())
      boardsList.addItem(boardname);
    boardsList.addItem(OTHER_BOARD);
    boardsList.setSelectedItem(settings.GetSelectedBoard());
    boardsListSelectedIndex = boardsList.getSelectedIndex();
    boardsList.addActionListener(e -> setBoard());

    // configure clock speed options
    clockOption.addItem(MAX_SPEED);
    clockOption.addItem(DIV_SPEED);
    clockOption.addItem(DYN_SPEED);
    clockOption.setSelectedItem(DIV_SPEED);
    clockDivRate.setEditable(true);
    clockDivCount.setEditable(true);
    clockOption.addActionListener(e -> setClockOption());
    clockDivRate.addActionListener(e -> setClockDivRate());
    clockDivCount.addActionListener(e -> setClockDivCount());
    populateClockDivOptions();
    updateClockOptions();

    // configure annotation options
    annotationList.addItem("Relabel all components");
    annotationList.addItem("Add missing labels");
    annotationList.setSelectedIndex(1);

    // circuit selection
    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 2;
    add(textMainCircuit, c);
    c.gridx = 1;
    c.gridwidth = 2;
    add(circuitsList, c);

    // target board selection
    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 3;
    add(textTargetBoard, c);
    c.gridx = 1;
    c.gridwidth = 2;
    add(boardsList, c);

    // clock speed options
    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 4;
    add(textTargetFreq, c);
    c.gridy = 4;
    c.gridx = 1;
    add(clockOption, c);
    c.gridx = 2;
    add(clockDivRate, c);
    c.gridy = 5;
    c.gridx = 1;
    add(textTargetDiv, c);
    c.gridx = 2;
    add(clockDivCount, c);

    // annotation options
    c.gridx = 0;
    c.gridy = 6;
    add(textAnnotation, c);
    c.gridwidth = 2;
    c.gridx = 1;
    add(annotationList, c);

    // board picture
    c.gridx = 3;
    c.gridy = 2;
    c.gridheight = 6;
    add(boardIcon.label, c);
    c.gridheight = 1;

    // validate button
    validateButton.addActionListener(e -> doDownloadPrep());
    c.gridwidth = 1;
    c.gridx = 1;
    c.gridy = 7;
    add(validateButton, c);

    // write to flash
    writeToFlash.setVisible(board != null && board.fpga.FlashDefined);
    writeToFlash.setSelected(false);
    c.gridx = 2;
    c.gridy = 7;
    add(writeToFlash, c);

    // annotate button
    annotateButton.addActionListener(e -> annotate(annotationList.getSelectedIndex() == 0));
    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 7;
    add(annotateButton, c);

    // HDL type button
    HDLType.addItem(Settings.VHDL);
    HDLType.addItem(Settings.VERILOG);
    HDLType.setSelectedItem(lang);
    HDLType.addActionListener(e -> setHDLType());
    c.gridx = 0;
    c.gridy = 0;
    add(HDLType, c);

    // HDL action selection
    HDLAction.addItem(HDL_GEN_ONLY);
    HDLAction.addItem(HDL_GEN_AND_DOWNLOAD);
    HDLAction.addItem(HDL_DOWNLOAD_ONLY);
    HDLAction.addActionListener(e -> setHDLAction());
    populateHDLAction();
    c.gridwidth = 2;
    c.gridx = 1;
    c.gridy = 0;
    add(HDLAction, c);

    // settings button
    toolSettings.setText("Settings");
    toolSettings.addActionListener(e -> doToolSettings());
    c.gridwidth = 2;
    c.gridx = 3;
    c.gridy = 0;
    add(toolSettings, c);

    // console panels
    tabbedPane.add(messages); // tab index 0 is for console messages
    tabbedPane.setPreferredSize(new Dimension(700, 20 * Console.FONT_SIZE));
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 8;
    c.gridwidth = 5;
    c.weightx = 1;
    c.weighty = 1;
    add(tabbedPane, c);

    // setup
    pack();
    Dimension size = getSize();
    size.height -= 10 * Console.FONT_SIZE;
    setMinimumSize(size);
    setLocation(Projects.getCenteredLoc(getWidth(), getHeight()));

    // show window
    validateButton.requestFocus();
    setVisible(true);
  }

  public String getProjectName() {
    return proj.getLogisimFile().getName();
  }

  private void updateCircuitList() {
    circuitsList.removeAllItems();
    for (Circuit circ : proj.getLogisimFile().getCircuits())
      circuitsList.addItem(circ);
    Circuit circ = proj.getCurrentCircuit();
    if (circ == null)
      circ = proj.getLogisimFile().getMainCircuit();
    circuitsList.setSelectedItem(circ);
  }

  private void updateClockOptions() {
    // Circuit root = circuitsList.getSelectedValue();
    // int nClocks = root.getNetlist().NumberOfClockTrees();
    // clockOption.setEnabled(nClocks > 0);
    // clockDivRate.setEnabled(nClocks > 0);
    // clockDivCount.setEnabled(nClocks > 0);
  }

  boolean updatingClockMenus = false;
  private void populateClockDivOptions() {
    updatingClockMenus = true;
    clockDivCount.removeAllItems();
    clockDivRate.removeAllItems();
    long base = board == null ? 50000000 : board.fpga.ClockFrequency;
    ArrayList<Integer> counts = new ArrayList<>();
    ArrayList<Double> freqs = new ArrayList<>();
    double ff = (double)base;
    while (ff >= MenuSimulate.SupportedTickFrequencies[0]*2) {
      freqs.add(ff);
      ff /= 2;
    }
    for (double f : MenuSimulate.SupportedTickFrequencies)
      freqs.add(f);
    for (double f : freqs) {
      int count = countForFreq(base, f);
      if (counts.contains(count))
        continue;
      counts.add(count);
      String rate = rateForCount(base, count);
      clockDivCount.addItem(count);
      clockDivRate.addItem(new ExactRate(base, count));
      if (Math.abs((proj.getSimulator().getTickFrequency() - f)/f) < 0.0001) {
        clockDivCount.setSelectedItem(count);
        clockDivRate.setSelectedItem(new ExactRate(base, count));
      }
    }
    if (clockDivCount.getSelectedValue() == null && clockDivCount.getItemCount() > 0)
      clockDivCount.setSelectedIndex(0);
    if (clockDivRate.getSelectedValue() == null && clockDivRate.getItemCount() > 0)
      clockDivRate.setSelectedIndex(0);
    updatingClockMenus = false;
    setClockDivCount();
    setClockDivRate();
  }

  private void setClockOption() {
    boolean div = clockOption.getSelectedValue().equals(DIV_SPEED);
    boolean max = clockOption.getSelectedValue().equals(MAX_SPEED);
    clockDivRate.setEnabled(div);
    clockDivCount.setEnabled(div);
    long base = board == null ? 50000000 : board.fpga.ClockFrequency;
    if (max) {
      clockDivRate.setSelectedItem(new ExactRate(base, 0));
      clockDivCount.setSelectedItem("undivided");
    } else if (div) {
      if (prevSelectedDivCount > 0 && prevSelectedDivRate != null) {
        clockDivCount.setSelectedItem(prevSelectedDivCount);
        clockDivRate.setSelectedItem(prevSelectedDivRate);
      } else {
        useTickSpeedFromSimulator();
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
    @Override
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
    @Override
    public int hashCode() {
      return (int)(39 * (base + 27) + count);
    }
  }

  private void setClockDivRate() {
    if (updatingClockMenus)
      return;
    if (!clockOption.getSelectedValue().equals(DIV_SPEED))
      return;
    long base = board == null ? 50000000 : board.fpga.ClockFrequency;
    Object o = clockDivRate.getSelectedValue();
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
          useTickSpeedFromSimulator();
        }
        return;
      }
      String rate = rateForCount(base, i);
      clockDivRate.setSelectedItem(rate); // rounds to nearest acceptable value
    }
    if (clockDivCount.getSelectedValue() == null || !clockDivCount.getSelectedValue().equals(i))
      clockDivCount.setSelectedItem(i);
    prevSelectedDivRate = clockDivRate.getSelectedValue();
    prevSelectedDivCount = (Integer)clockDivCount.getSelectedValue();
  }

  private void setClockDivCount() {
    if (updatingClockMenus)
      return;
    if (!clockOption.getSelectedValue().equals(DIV_SPEED))
      return;
    long base = board == null ? 50000000 : board.fpga.ClockFrequency;
    Object item = clockDivCount.getSelectedValue();
    String s = item == null ? "-1" : item.toString();
    int count = -1;
    try { count = Integer.parseInt(s); }
    catch (NumberFormatException e) { }
    if (count <= 0) {
      if (prevSelectedDivCount > 0 && prevSelectedDivRate != null) {
        clockDivCount.setSelectedItem(prevSelectedDivCount);
        clockDivRate.setSelectedItem(prevSelectedDivRate);
      } else {
        useTickSpeedFromSimulator();
      }
    } else {
      clockDivRate.setSelectedItem(new ExactRate(base, count));
      prevSelectedDivRate = clockDivRate.getSelectedValue();
      prevSelectedDivCount = count;
    }
  }

  private void useTickSpeedFromSimulator() {
    long base = board.fpga.ClockFrequency;
    for (double f : MenuSimulate.SupportedTickFrequencies) {
      int count = countForFreq(base, f);
      if (Math.abs((proj.getSimulator().getTickFrequency() - f)/f) < 0.0001) {
        clockDivCount.setSelectedItem(count);
        clockDivRate.setSelectedItem(new ExactRate(base, count));
      }
    }
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

  private boolean settingBoard = false;
  private void setBoard() {
    if (settingBoard)
      return;
    settingBoard = true;
    Board old = board;
    String boardName = boardsList.getSelectedValue();
    if (boardName == OTHER_BOARD) {
      doLoadOtherBoard();
    } else {
      settings.SetSelectedBoard(boardName);
      settings.UpdateSettingsFile();
      board = BoardReader.read(settings.GetSelectedBoardFileName());
    }
    if (board == null || board == old) {
      // load failed, cancelled, or pointless... go back to previous selection
      board = old;
      boardsList.setSelectedIndex(boardsListSelectedIndex);
      settingBoard = false;
      annotateButton.setEnabled(board != null);
      validateButton.setEnabled(board != null);
      return;
    }
    cachedPinBindings = null;
    annotateButton.setEnabled(true);
    validateButton.setEnabled(true);
    boardsListSelectedIndex = boardsList.getSelectedIndex();
    settingBoard = false;
    boardIcon.setImage(board == null ? null : board.image);
    writeToFlash.setSelected(false);
    writeToFlash.setVisible(board != null && board.fpga.FlashDefined);
    populateClockDivOptions();
  }

  private void doLoadOtherBoard() {
    String filename = doBoardFileSelect();
    if (filename == null || filename.isEmpty())
      return; // cancelled
    board = BoardReader.read(filename);
    if (board == null)
      return; // failed to load
    if (settings.GetBoardNames().contains(board.name)) {
      AddErrors("A board with the name \""+board.name+"\" already exists. "
          + "Either rename your board file, or edit Logisim's XML settings file by "
          + "hand to remove the existing board.");
      board = null;
      return;
    }
    settings.AddExternalBoard(filename);
    settings.SetSelectedBoard(board.name);
    settings.UpdateSettingsFile();
    boardsList.addItem(board.name);
    boardsList.setSelectedItem(board.name);
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

  private void annotate(boolean clearExistingLabels) {
    clearAllMessages();
    if (board == null) {
      err.AddError("Please select a valid FPGA board before annotation.");
      return;
    }
    Circuit root = circuitsList.getSelectedValue();
    if (root == null)
      return; // huh?
    if (clearExistingLabels)
      root.recursiveResetNetlists();
    root.autoHdlAnnotate(clearExistingLabels, err, lang, board.fpga.Vendor);
    err.AddInfo("Annotation done");
    // TODO: Fix this dirty hack, see Circuit.Annotate() for details.
    proj.repaintCanvas();
    proj.getLogisimFile().setDirty(true);
  }

  private boolean cleanDirectory(String dirname) {
    try {
      File dir = new File(dirname);
      if (!dir.exists())
        return true;
      for (File f : dir.listFiles()) {
        if (f.isDirectory()) {
          if (!cleanDirectory(f.getPath()))
            return false;
        } else {
          if (!f.delete()) {
            err.AddFatalError("Unable to remove old project file: %s", f.getPath());
            return false;
          }
        }
      }
      if (!dir.delete()) {
        err.AddFatalError("Unable to remove old project directory: %s", dirname);
        return false;
      }
      return true;
    } catch (Exception e) {
      err.AddFatalError("Error removing directory tree: %s\n   detail: %s", dirname,
          e.getMessage());
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

  private boolean justDownload() {
    return HDLAction.getSelectedValue().equals(HDL_DOWNLOAD_ONLY);
  }

  private void doDownloadPrep() {
    ClearConsoles();
    if (board == null) {
      AddErrors("No FPGA board is selected. Please select an FPGA board.");
      return;
    }
    Circuit root = circuitsList.getSelectedValue();
    if (root == null) {
      AddErrors("INTERNAL ERROR: no circuit selected.");
      return;
    }
    String path = proj.getLogisimFile().getName();
    if (path.indexOf(" ") != -1) {
      AddErrors("The file '" + path + "' contains a space.");
      AddErrors("Spaces are not permitted by the HDL synthesis engine. Please");
      AddErrors("rename your files and directories to not have any spaces.");
      return;
    }
    path = proj.getLogisimFile().getLoader().getMainFile().toString();
    if (path.indexOf(" ") != -1) {
      AddErrors("The directory '" + path + "' contains a space.");
      AddErrors("Spaces are not permitted by the HDL synthesis engine. Please");
      AddErrors("rename your files and directories to not have any spaces.");
      return;
    }
    boolean badTools = settings.GetToolsAreDisabled();
    if (justDownload() && !badTools) {
      AddInfo("*** NOTE *** Skipping both HDL generation and synthesis.");
      AddInfo("*** NOTE *** Recent changes to circuits will not take effect.");
      doSynthesisAndDownload(null);
    } else if (!justDownload()) {
      AddInfo("Performing design rule checks (DRC)");
      if (!performDRC()) {
        AddErrors("DRC failed, synthesis can't continue.");
        return;
      }
      AddInfo("Performing pin assignment");
      PinBindings pinBindings = performPinAssignments();
      if (pinBindings == null) {
        AddErrors("Pin assignment failed or is incomplete, synthesis can't continue.");
        return;
      }
      AddInfo("Generating HDL files");
      if (!writeHDL(pinBindings)) {
        AddErrors("HDL file generation failed, synthesis can't continue.");
        return;
      }
      AddInfo("HDL files are ready for synthesis.");
      doSynthesisAndDownload(pinBindings);
    }
  }

  private String workspacePath() {
    File projFile = proj.getLogisimFile().getLoader().getMainFile();
    return settings.GetWorkspacePath(projFile);
  }

  private String projectWorkspace() {
    return workspacePath() + SLASH
        + proj.getLogisimFile().getName() + SLASH;
  }

  private String circuitWorkspace() {
    String rootname = circuitsList.getSelectedValue().getName();
    return projectWorkspace() +
        CorrectLabel.getCorrectLabel(rootname) + SLASH;
  }

  private void doSynthesisAndDownload(PinBindings pinBindings) {
    if (board == null)
      return;
    String basedir = projectWorkspace();
    String circdir = circuitWorkspace() + lang.toLowerCase() + SLASH;

    FPGADownload downloader = FPGADownload.forVendor(board.fpga.Vendor);
    downloader.err = err;
    downloader.lang = lang;
    downloader.board = board;
    downloader.settings = settings;
    downloader.projectPath = basedir;
    downloader.circuitPath = circdir;
    downloader.scriptPath = basedir + SCRIPT_DIR;
    downloader.sandboxPath = basedir + SANDBOX_DIR;
    downloader.ucfPath = basedir + UCF_DIR;
    downloader.writeToFlash = writeToFlash.isSelected();

    if (!justDownload()) {
      // sanity check pin bindings
      if (pinBindings == null || !pinBindings.allPinsAssigned()) {
        err.AddError("Not all pins have been assigned, synthesis can't continue.");
        return;
      }
      boolean badTools = settings.GetToolsAreDisabled();
      // generate scripts and synthesize
      if (downloader.generateScripts(pinBindings) && !badTools) {
        err.AddError("Can't generate Tool-specific download scripts");
        return;
      }
    } else {
      // don't generate or synthesize, just sanity-check that project is ready
      if (!downloader.readyForDownload()) {
        err.AddError("HDL files are not ready for download. "
            + "Try selecting \"Synthesize and Download\" instead.");
        return;
      }
    }
    // download
    downloader.initiateDownload();
  }

  private boolean mkdirs(String dirname) {
    try {
      File dir = new File(dirname);
      if (dir.exists())
        return true;
      if (!dir.mkdirs()) {
        err.AddFatalError("Unable to create directory: %s", dirname);
        return false;
      }
      return true;
    } catch (Exception e) {
      err.AddFatalError("Error creating directory: %s\n  detail: %s", dirname, e.getMessage());
      return false;
    }
  }

  void populateHDLAction() {
    if (board == null) {
      AddInfo("Please select an FPGA board.");
      HDLAction.setEnabled(false);
    }
    if ((board.fpga.Vendor == Chipset.ALTERA
          && settings.GetAlteraToolPath().equals(Settings.Unknown))
        || (board.fpga.Vendor == Chipset.XILINX
          && settings.GetXilinxToolPath().equals(Settings.Unknown))) {
      // Synthesis/download not possible.
      if (!settings.GetToolsAreDisabled()) {
        settings.SetToolsAreDisabled(true);
        settings.UpdateSettingsFile();
      }
      HDLAction.setSelectedItem(HDL_GEN_ONLY);
      HDLAction.setEnabled(false);
      AddInfo("Tool path is not set correctly. " +
          "Synthesis and download will not be available.");
      AddInfo("Please set the path to " + board.fpga.Vendor
          + "tools using the \"Settings\" button");
    } else if (settings.GetToolsAreDisabled()) {
      // Synthesis/download possible, but user selected to only generate HDL.
      HDLAction.setSelectedItem(HDL_GEN_ONLY);
      HDLAction.setEnabled(true);
    } else {
      // Synthesis/download possible, user elects to do so.
      HDLAction.setSelectedItem(HDL_GEN_AND_DOWNLOAD);
      HDLAction.setEnabled(true);
    }
  }

  private void setHDLAction() {
    if (HDLAction.getSelectedValue().equals(HDL_GEN_ONLY)) {
      settings.SetToolsAreDisabled(true);
    } else {
      settings.SetToolsAreDisabled(false);
    }
  }

  private void setHDLType() {
    if (HDLType.getSelectedIndex() == 0)
      lang = Settings.VHDL;
    else
      lang = Settings.VERILOG;
    if (lang.equals(settings.GetHDLType()))
      return;
    settings.SetHDLType(lang);
    if (!settings.UpdateSettingsFile())
      AddErrors("***SEVERE*** Could not update XML settings file");
    Circuit root = circuitsList.getSelectedValue();
    if (root != null)
      root.recursiveResetNetlists();
  }

  private PinBindings cachedPinBindings;
  private Circuit cachedPinBindingsCircuit;
  private PinBindings performPinAssignments() {
    board.printStats(err);

    Circuit root = circuitsList.getSelectedValue();
    Netlist netlist = root.getNetlist();
   
    if (cachedPinBindings == null || cachedPinBindingsCircuit != root) {
      cachedPinBindings = new PinBindings(err, board, netlist.getMappableComponents());
      cachedPinBindingsCircuit = root;
    } else {
      cachedPinBindings.setComponents(netlist.getMappableComponents());
    }

    File f = proj.getLogisimFile().getLoader().getMainFile();
    String path = f == null ? "" : f.getAbsolutePath();
    BindingsDialog dlg = new BindingsDialog(board, cachedPinBindings, this, path);
    setVisible(false);
    dlg.setVisible(true);
    setVisible(true);
    if (cachedPinBindings.allPinsAssigned()) {
      cachedPinBindings.finalizeMappings();
      return cachedPinBindings;
    }
    err.AddError("Some I/O-related components have not been assigned to I/O "
        + "resources on the FPGA board. All components must be assigned.");
    return null;
  }

  private boolean performDRC() {
    clearAllMessages();
    Circuit root = circuitsList.getSelectedValue();
    return root.getNetlist().validate(err, lang, board.fpga.Vendor,
        board.fpga.ClockFrequency, getClkPeriod());
  }

  private void doToolSettings() {
    FPGASettingsDialog dlg = new FPGASettingsDialog(this, settings);
    dlg.SetVisible(true);

    if (board.fpga.Vendor == Chipset.ALTERA) {
      if (!settings.GetAlteraToolPath().equals(Settings.Unknown)) {
        HDLAction.setEnabled(true);
        settings.SetToolsAreDisabled(false);
        HDLAction.setSelectedItem(HDL_GEN_AND_DOWNLOAD);
        if (!settings.UpdateSettingsFile()) {
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
    } else if (board.fpga.Vendor == Chipset.XILINX) {
      if (!settings.GetXilinxToolPath().equals(Settings.Unknown)) {
        HDLAction.setEnabled(true);
        settings.SetToolsAreDisabled(false);
        HDLAction.setSelectedItem(HDL_GEN_AND_DOWNLOAD);
        if (!settings.UpdateSettingsFile()) {
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

  private String doBoardFileSelect() {
    JFileChooser fc = new JFileChooser(workspacePath());
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Board files", "xml", "xml");
    fc.setFileFilter(filter);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    File test = new File(workspacePath());
    if (test.exists()) {
      fc.setSelectedFile(test);
    }
    fc.setDialogTitle("Board description selection");
    int retval = fc.showOpenDialog(null);
    if (retval != JFileChooser.APPROVE_OPTION)
      return null;
    File file = fc.getSelectedFile();
    return file.getPath();
  }

  private void selectWorkSpace() {
    JFileChooser fc = new JFileChooser(workspacePath());
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    File test = new File(workspacePath());
    if (test.exists()) {
      fc.setSelectedFile(test);
    }
    fc.setDialogTitle("Workspace Directory Selection");
    int retval = fc.showOpenDialog(null);
    if (retval == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      if (file.getPath().endsWith(SLASH)) {
        settings.SetStaticWorkspacePath(file.getPath());
      } else {
        settings.SetStaticWorkspacePath(file.getPath() + SLASH);
      }
      if (!settings.UpdateSettingsFile()) {
        AddErrors("***SEVERE*** Could not update the FPGACommander settings file");
      } else {
        AddInfo("Updated the FPGACommander settings file");
      }
    }
  }

  public void reactivate() {
    if (!isVisible()) {
      // clear();
      setVisible(true);
    }
    toFront();
  }

  private int getClkPeriod() {
    if (clockOption.getSelectedValue().equals(MAX_SPEED))
      return 0;
    else if (clockOption.getSelectedValue().equals(DYN_SPEED))
      return -1;
    Object item = clockDivCount.getSelectedValue();
    if (item == null)
      return 1;
    return Integer.parseInt(item.toString());
  }

  private boolean writeHDL(PinBindings pinBindings) {
    Circuit root = circuitsList.getSelectedValue();
    String basedir = projectWorkspace();
    basedir += CorrectLabel.getCorrectLabel(root.getName()) + SLASH;
    if (!cleanDirectory(basedir))
      return false;
    for (String subdir : HDL_PATHS)
      if (!mkdirs(basedir + subdir))
        return false;

    // Generate HDL for top-level module and everything it contains, including
    // the root circuit (and all its subcircuits and components), the top-level
    // ticker (if needed), any clocks lifted to the top-level, etc.
    ToplevelHDLGenerator g = new ToplevelHDLGenerator(lang, err,
        board.fpga.Vendor, root, pinBindings);

    // if (g.hdlDependsOnCircuitState()) { // for NVRAM
    //   CircuitState cs = getCircuitState(root);
    //   if (!g.writeAllHDLThatDependsOn(cs, null, null, basedir))
    //     return false;
    // }
    return g.writeAllHDLFiles(basedir);
  }

  private CircuitState getCircuitState(Circuit circ) {
    ArrayList<CircuitState> list = new ArrayList<>();
    for (CircuitState cs : proj.getRootCircuitStates())
      if (cs.getCircuit() == circ)
        list.add(cs);
    if (list.isEmpty()) {
      err.AddSevereWarning("Circuit %s contains non-volatile RAM or other components that depend "
          + "on the current simulator state, but there are no running simulations with this "
          + "circuit as the root. These components will be initialized using default values.",
          circ.getName());
      return null;
    }
    if (list.size() > 0) {
      err.AddSevereWarning("Circuit %s contains non-volatile RAM or other components that depend "
          + "on the current simulator state, but there are multiple running simulations with this "
          + "circuit as the root. The first maching simulator state will be used for HDL synthesis.",
          circ.getName());
    }
    return list.get(0);
  }

}
