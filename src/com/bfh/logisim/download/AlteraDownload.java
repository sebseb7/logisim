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

import java.io.File;
import java.util.ArrayList;

import com.bfh.logisim.fpga.Chipset;
import com.bfh.logisim.fpga.PinBindings;
import com.bfh.logisim.fpga.PullBehavior;
import com.bfh.logisim.gui.Commander;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.cburch.logisim.hdl.Hdl;

public class AlteraDownload extends FPGADownload {

  public AlteraDownload() { super("Altera"); }

  @Override
  public boolean readyForDownload() {
    return new File(sandboxPath + TOP_HDL + ".sof").exists()
        || new File(sandboxPath + TOP_HDL + ".pof").exists();
  }

  private ArrayList<String> cmd(String prog, String ...args) {
    ArrayList<String> command = new ArrayList<>();
    command.add(settings.GetAlteraToolPath() + File.separator + prog);
    if (settings.GetAltera64Bit())
      command.add("--64bit");
    for (String arg: args)
      command.add(arg);
    return command;
  }

  private String bitfile;
  private String cablename;

  @Override
  public ArrayList<Stage> initiateDownload(Commander cmdr) {
    ArrayList<Stage> stages = new ArrayList<>();

    if (!readyForDownload()) {
      String script = scriptPath.replace(projectPath, ".." + File.separator) + "AlteraDownload.tcl";
      stages.add(new Stage(
            "init", "Creating Quartus Project",
            cmd(ALTERA_QUARTUS_SH, "-t", script),
            "Failed to create Quartus project, cannot download"));
      stages.add(new Stage(
            "optimize", "Optimizing for Minimal Area",
            cmd(ALTERA_QUARTUS_MAP, TOP_HDL, "--optimize=area"),
            "Failed to optimize design, cannot download"));
      stages.add(new Stage(
            "synthesize", "Synthesizing (may take a while)",
            cmd(ALTERA_QUARTUS_SH, "--flow", "compile", TOP_HDL),
            "Failed to synthesize design, cannot download"));
    }

    // Typical output for FPGA enumerate command:
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
    stages.add(new Stage(
          "scan", "Searching for FPGA Devices",
          cmd(ALTERA_QUARTUS_PGM, "--list"),
          "Could not find any FPGA devices. Did you connect the FPGA board?") {
      @Override
      protected boolean prep() {
        // if there is no .sof generated, we can use a .pof instead
        if (new File(sandboxPath + TOP_HDL + ".sof").exists()) {
          bitfile = "P;" + TOP_HDL + ".sof";
        } else if (new File(sandboxPath + TOP_HDL + ".pof").exists()) {
          bitfile = "P;" + TOP_HDL + ".pof";
        } else {
          console.printf(console.ERROR, "Error: Design must be synthesized before download.");
          return false;
        }
        if (!cmdr.confirmDownload()) {
          cancelled = true;
          return false;
        }
        return true;
      }
      @Override
      protected boolean post() {
        ArrayList<String> dev = new ArrayList<>();
        for (String line: console.getText()) {
          int n  = dev.size() + 1;
          if (!line.matches("^" + n + "\\) .*" ))
            continue;
          line = line.replaceAll("^" + n + "\\) ", "");
          dev.add(line.trim());
        }
        if (dev.size() == 0) {
          console.printf(console.ERROR, "No USB-Blaster cable detected");
          return false;
        } else if (dev.size() == 1) {
          cablename = "usb-blaster"; // why not dev.get(0)?
        } else if (dev.size() > 1) {
          console.printf("%d FPGA devices detected:", dev.size());
          int i = 1;
          for (String d : dev)
            console.printf("   %d) %s", i++, d);
          cablename = cmdr.chooseDevice(dev);
          if (cablename == null) {
            cancelled = true;
            return false;
          }
        }
        return true;
      }
    });
    stages.add(new Stage(
          "download", "Downloading to FPGA", null,
          "Failed to download design; did you connect the board?") {
      @Override
      protected boolean prep() {
        cmd = cmd(ALTERA_QUARTUS_PGM, "-c", cablename, "-m", "jtag", "-o", bitfile);
        return true;
      }
    });
    return stages;
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
 
}
