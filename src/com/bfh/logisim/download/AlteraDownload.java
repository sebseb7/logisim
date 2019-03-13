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

package com.bfh.logisim.download;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import com.bfh.logisim.fpga.Chipset;
import com.bfh.logisim.fpga.PinBindings;
import com.bfh.logisim.fpga.PullBehavior;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.TickHDLGenerator;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGenerator;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.hdl.Hdl;

public class AlteraDownload extends FPGADownload implements Runnable {

  static final String TOP_HDL = ToplevelHDLGenerator.HDL_NAME;
  static final String CLK_PORT = TickHDLGenerator.FPGA_CLK_NET;

  public AlteraDownload() { }

  private JFrame panel;
  private JLabel text;
  private JProgressBar progress;
  private boolean stopRequested = false;

  private void showProgress() {
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;

    panel = new JFrame("Altera Downloading");
    panel.setLayout(new GridBagLayout());
    panel.setResizable(false);
    panel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    panel.addWindowListener(new WindowAdapter() {
      public void windowClosed(WindowEvent e) { cancel(); }
    });

    text = new JLabel("Altera Downloader");
    text.setMinimumSize(new Dimension(600, 30));
    text.setPreferredSize(new Dimension(600, 30));

    progress = new JProgressBar(0, 5);
    progress.setValue(1);
    progress.setStringPainted(true);

    c.gridx = 0;
    c.gridy = 0;
    panel.add(text, c);

    c.gridx = 0;
    c.gridy = 1;
    panel.add(progress, c);

    panel.pack();
    panel.setLocation(Projects.getCenteredLoc(panel.getWidth(), panel.getHeight() * 4));
    panel.setVisible(true);
  }

  private void setStatus(String msg) {
    text.setText(msg);
    Rectangle r = text.getBounds();
    r.x = r.y = 0;
    text.repaint(r);
  }

  private void setProgress(int val) {
    progress.setValue(val);
    Rectangle r = progress.getBounds();
    r.x = r.y = 0;
    progress.repaint(r);
  }

  public void cancel() {
    setStatus("Cancelling... please wait");
    stopRequested = true;
    synchronized(lock) {
      if (altera != null) {
        altera.destroy();
      }
    }
  }

  public void run() {
    String fatal = null;
    Throwable failure = null;
    try { fatal = download(); }
    catch (Throwable e) { failure = e; }
    if (failure != null) {
      StringWriter buf = new StringWriter();
      failure.printStackTrace(new PrintWriter(buf));
      err.AddFatalError("Altera failed to download with an unexpected error.\n %s\n %s",
        failure.getMessage(), buf.toString());
    }
    if (fatal != null)
      err.AddFatalError("%s", fatal);
    panel.setVisible(false);
    panel.dispose();
  }

  private Process altera = null;
  private Object lock = new Object();

  private String shell(ArrayList<String> cmd) {
    String s = "";
    for (String c : cmd) {
      if (!c.matches("[a-zA-Z0-9-+_=:,.]*")) {
        c = c.replaceAll("\\\\", "\\\\");
        c = c.replaceAll("`", "\\`");
        c = c.replaceAll("\\$", "\\$");
        c = c.replaceAll("!", "\\!");
        c = c.replaceAll("'", "'\\''");
        c = "'" + c + "'";
      }
      if (s.length() > 0)
        s += " ";
      s += c;
    }
    return s;
  }

  private boolean alteraCommand(String title, ArrayList<String> out, int progid, String... args)
      throws IOException, InterruptedException {
    ArrayList<String> command = new ArrayList<>();
    command.add(settings.GetAlteraToolPath() + File.separator + Settings.AlteraPrograms[progid]);
    if (settings.GetAltera64Bit())
      command.add("--64bit");
    for (String arg: args)
      command.add(arg);
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.directory(new File(sandboxPath));
    synchronized(lock) {
      altera = builder.start();
    }
    InputStream is = altera.getInputStream();
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);
    String line;
    if (out == null) {
      err.NewConsole(title);
      err.printf("Command: %s\n", shell(command));
    }
    while ((line = br.readLine()) != null) {
      if (out != null)
        out.add(line);
      else
        err.printf("%s", line);
    }
    altera.waitFor();
    if (altera.exitValue() != 0) {
      if (out != null) {
        err.NewConsole(title);
        err.printf("Command: %s\n", shell(command));
        for (String msg : out)
          err.printf("%s", msg);
      }
      return false;
    }
    return true;
  }

  private String download() throws IOException, InterruptedException {
    setStatus("Generating FPGA files and performing download; this may take a (very long) while...");
    boolean sofFileExists = new File(sandboxPath + TOP_HDL + ".sof").exists();

    setProgress(1);
    if (stopRequested)
      return null;
    if (!sofFileExists) {
      setStatus("Creating Project");
      if (!alteraCommand("init", null, 0, "-t", scriptPath.replace(projectPath, ".." + File.separator) + "AlteraDownload.tcl"))
        return "Failed to Create a Quartus Project, cannot download";
    }

    setProgress(2);
    if (stopRequested)
      return null;
    if (!sofFileExists) {
      setStatus("Optimize Project");
      if (!alteraCommand("optimize", null, 2, TOP_HDL, "--optimize=area"))
        return "Failed to optimize (AREA) Project, cannot download";
    }

    setProgress(3);
    if (stopRequested)
      return null;
    if (!sofFileExists) {
      setStatus("Synthesizing and creating configuration file (this may take a while)");
      if (!alteraCommand("synthesize", null, 0, "--flow", "compile", TOP_HDL))
        return "Failed to synthesize design and to create the configuration files, cannot download";
    }

    setStatus("Downloading");
    if (stopRequested)
      return null;
    Object[] options = { "Yes, download" };
    if (JOptionPane.showOptionDialog(
          progress,
          "Verify that your board is connected and you are ready to download.",
          "Ready to download ?", JOptionPane.YES_OPTION,
          JOptionPane.WARNING_MESSAGE, null, options, options[0]) == JOptionPane.CLOSED_OPTION) {
      err.AddSevereWarning("Download aborted.");
      return null;
    }

    setProgress(4);
    if (stopRequested)
      return null;
    // if there is no .sof generated, try with the .pof
    String bin;
    if (new File(sandboxPath
          + TOP_HDL + ".sof")
        .exists()) {
      bin = "P;" + TOP_HDL + ".sof";
    } else if (new File(sandboxPath
          + TOP_HDL + ".pof")
        .exists()) {
      bin = "P;" + TOP_HDL + ".pof";
    } else {
      err.AddFatalError("File not found: " +
          sandboxPath + TOP_HDL + ".sof");
      return "Error: Design must be synthesized before download.";
    }
    // Info: *******************************************************************
    // Info: Running Quartus II 32-bit Programmer
    //     Info: Version 13.0.0 Build 156 04/24/2013 SJ Web Edition
    //     ...
    //     Info: Processing started: Wed Feb 24 17:39:18 2016
    // Info: Command: quartus_pgm --list
    // 1) USB-Blaster [1-1.3]
    // 2) USB-Blaster on grace.holycross.edu [1-1.3]
    // Info: Quartus II 32-bit Programmer was successful. 0 errors, 0 warnings
    //     Info: Peak virtual memory: 126 megabytes
    //     ...

    ArrayList<String> lines = new ArrayList<>();
    if (!alteraCommand("download", lines, 1, "--list"))
      return "Failed to list devices; did you connect the board?";
    ArrayList<String> dev = new ArrayList<>();
    for (String line: lines) {
      int n  = dev.size() + 1;
      if (!line.matches("^" + n + "\\) .*" ))
        continue;
      line = line.replaceAll("^" + n + "\\) ", "");
      dev.add(line.trim());
    }
    String cablename = "usb-blaster";
    if (dev.size() == 0) {
      err.AddSevereWarning("No USB-Blaster cable detected");
    } else if (dev.size() > 1) {
      err.printf("%d FPGA devices detected:", dev.size());
      int i = 1;
      for (String d : dev)
        err.printf("   %d) %s", i++, d);
      cablename = chooseDevice(dev);
      if (cablename == null) {
        err.printf("Download cancelled.");
        return null;
      }
    }

    err.printf("Downloading to FPGA device: %s", cablename);

    if (!alteraCommand("download", null, 1, "-c", cablename, "-m", "jtag", "-o", bin))
      return "Failed to download design; did you connect the board?";

    return null;
  }

  String chooseDevice(ArrayList<String> names) {
    String[] choices = new String[names.size()];
    for (int i = 0; i < names.size(); i++)
      choices[i] = names.get(i);
    String choice = (String) JOptionPane.showInputDialog(progress,
        "Multiple FPGA devices detected. Select one to be programmed...",
        "Select FPGA Device",
        JOptionPane.QUESTION_MESSAGE, null,
        choices,
        choices[0]);
    return choice;
  }

  @Override
  public boolean generateScripts(PinBindings ioResources, ArrayList<String> hdlFiles) {

    Hdl out = new Hdl(lang, err);

    Chipset chip = board.fpga;
    String[] pkg = board.fpga.Package.split(" ");
    String hdltype = out.isVhdl ? "VHDL_FILE" : "VERILOG_FILE";

    out.stmt("# Quartus II Tcl Project package loading script for Logisim");
    out.stmt("package require ::quartus::project");
    out.stmt();
    out.stmt("set need_to_close_project 0");
    out.stmt("set make_assignments 1");
    out.stmt();
    out.stmt("# Check that the right project is open");
    out.stmt("if {[is_project_open]} {");
    out.stmt("    if {[string compare $quartus(project) \"%s\"]} {", TOP_HDL);
    out.stmt("        puts \"Project %s is not open\"", TOP_HDL);
    out.stmt("        set make_assignments 0");
    out.stmt("    }");
    out.stmt("} else {");
    out.stmt("    # Only open if not already open");
    out.stmt("    if {[project_exists %s]} {", TOP_HDL);
    out.stmt("        project_open -revision %s %s", TOP_HDL, TOP_HDL);
    out.stmt("    } else {");
    out.stmt("        project_new -revision %s %s", TOP_HDL, TOP_HDL);
    out.stmt("    }");
    out.stmt("    set need_to_close_project 1");
    out.stmt("}");
    out.stmt();
    out.stmt("# Make assignments");
    out.stmt("if {$make_assignments} {");
    out.stmt("    set_global_assignment -name FAMILY \"%s\"", chip.Technology);
    out.stmt("    set_global_assignment -name DEVICE %s", chip.Part);
    out.stmt("    set_global_assignment -name DEVICE_FILTER_PACKAGE %s", pkg[0]);
    out.stmt("    set_global_assignment -name DEVICE_FILTER_PIN_COUNT %s", pkg[1]);
    if (chip.UnusedPinsBehavior != PullBehavior.UNKNOWN)
      out.stmt("    set_global_assignment -name RESERVE_ALL_UNUSED_PINS \"AS INPUT %s\"", chip.UnusedPinsBehavior.altera);
    out.stmt("    set_global_assignment -name FMAX_REQUIREMENT \"%s\"", chip.Speed);
    out.stmt("    set_global_assignment -name RESERVE_NCEO_AFTER_CONFIGURATION \"USE AS REGULAR IO\"");
    out.stmt("    set_global_assignment -name CYCLONEII_RESERVE_NCEO_AFTER_CONFIGURATION \"USE AS REGULAR IO\"");
    out.stmt();
    out.stmt("    # Include all entities and gates");
    out.stmt();
    for (String f : hdlFiles)
      out.stmt("    set_global_assignment -name %s \"%s\"", hdltype, f);
    out.stmt();
    out.stmt("    # Map fpga_clk and ionets to fpga pins");
    if (ioResources.requiresOscillator)
      out.stmt("    set_location_assignment %s -to %s", board.fpga.ClockPinLocation, CLK_PORT);
    ioResources.forEachPhysicalPin((pin, net, io, label) -> {
      out.stmt("    set_location_assignment %s -to %s  ;# %s", pin, net, label);
      if (io.pull == PullBehavior.PULL_UP)
        out.stmt("    set_instance_assignment -name WEAK_PULL_UP_RESISTOR ON -to %s", net);
    });
    out.stmt("    # Commit assignments");
    out.stmt("    export_assignments");
    out.stmt();
    out.stmt("    # Close project");
    out.stmt("    if {$need_to_close_project} {");
    out.stmt("        project_close");
    out.stmt("    }");
    out.stmt("}");

    File f = FileWriter.GetFilePointer(scriptPath, "AlteraDownload.tcl", err);
    return f != null && FileWriter.WriteContents(f, out, err);
  }
 
  @Override
  public boolean readyForDownload() {
    return new File(sandboxPath + TOP_HDL + ".sof").exists();
  }

  @Override
  public boolean initiateDownload() {
    showProgress();
    new Thread(this).start();
    return true;
  }

}
