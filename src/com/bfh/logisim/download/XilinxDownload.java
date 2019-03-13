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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import com.bfh.logisim.fpga.Chipset;
import com.bfh.logisim.fpga.DriveStrength;
import com.bfh.logisim.fpga.IoStandard;
import com.bfh.logisim.fpga.PinBindings;
import com.bfh.logisim.fpga.PullBehavior;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.proj.Projects;

public class XilinxDownload extends FPGADownload {

  public XilinxDownload() { }

  @Override
  public boolean readyForDownload() {
    return new File(scriptPath + script_file).exists()
        && new File(scriptPath + vhdl_list_file).exists()
        && new File(ucfPath + ucf_file).exists()
        && new File(scriptPath + download_file).exists();
  }

  @Override
	public boolean initiateDownload() {
    // FIXME: cleaup this method, combine with code in AlteraDownload
		boolean isCPLD = isCPLD(board.fpga);
		String bitFileExt = isCPLD ? ".jed" : ".bit";
		boolean BitFileExists = new File(sandboxPath + TOP_HDL + bitFileExt).exists();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		JFrame panel = new JFrame("Xilinx Downloading");
		panel.setLayout(new GridBagLayout());
		panel.setResizable(false);
		panel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		JLabel text = new JLabel(
				"Generating FPGA files and performing download; this may take a while");
		c.gridx = 0;
		c.gridy = 0;
		panel.add(text, c);
		JProgressBar progress = new JProgressBar(0, Settings.XilinxPrograms.length);
		progress.setValue(0);
		progress.setStringPainted(true);
		c.gridx = 0;
		c.gridy = 1;
		panel.add(progress, c);
		panel.pack();
		panel.setLocation(Projects.getCenteredLoc(panel.getWidth(),
				panel.getHeight() * 4));
		panel.setVisible(true);
		Rectangle labelRect = text.getBounds();
		labelRect.x = 0;
		labelRect.y = 0;
		text.paintImmediately(labelRect);
		List<String> command = new ArrayList<String>();
		if (!BitFileExists) {
			try {
				text.setText("Synthesizing Project");
				labelRect = text.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				text.paintImmediately(labelRect);
				Rectangle ProgRect = progress.getBounds();
				ProgRect.x = 0;
				ProgRect.y = 0;
				progress.paintImmediately(ProgRect);
				command.clear();
				command.add(settings.GetXilinxToolPath() + File.separator + Settings.XilinxPrograms[0]);
				command.add("-ifn");
				command.add(scriptPath.replace(projectPath, "../") + script_file);
				command.add("-ofn");
				command.add("logisim.log");
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(sandboxPath));
				final Process CreateProject = Xilinx.start();
				InputStream is = CreateProject.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				err.NewConsole("synthesize");
				while ((line = br.readLine()) != null) {
					err.printf("%s", line);
				}
				CreateProject.waitFor();
				if (CreateProject.exitValue() != 0) {
					err.AddFatalError("Failed to Synthesize Xilinx project; cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				err.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				err.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			}
		}
		if (!BitFileExists) {
			try {
				text.setText("Adding contraints");
				labelRect = text.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				text.paintImmediately(labelRect);
				progress.setValue(1);
				Rectangle ProgRect = progress.getBounds();
				ProgRect.x = 0;
				ProgRect.y = 0;
				progress.paintImmediately(ProgRect);
				command.clear();
				command.add(settings.GetXilinxToolPath() + File.separator + Settings.XilinxPrograms[1]);
				command.add("-intstyle");
				command.add("ise");
				command.add("-uc");
				command.add(ucfPath.replace(projectPath, "../") + ucf_file);
				command.add("logisim.ngc");
				command.add("logisim.ngd");
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(sandboxPath));
				final Process CreateProject = Xilinx.start();
				InputStream is = CreateProject.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				err.NewConsole("constrain");
				while ((line = br.readLine()) != null) {
					err.printf("%s", line);
				}
				CreateProject.waitFor();
				if (CreateProject.exitValue() != 0) {
					err.AddFatalError("Failed to add Xilinx constraints; cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				err.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				err.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			}
		}
		if (!BitFileExists && !isCPLD) {
			try {
				text.setText("Mapping Design");
				labelRect = text.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				text.paintImmediately(labelRect);
				progress.setValue(2);
				Rectangle ProgRect = progress.getBounds();
				ProgRect.x = 0;
				ProgRect.y = 0;
				progress.paintImmediately(ProgRect);
				command.clear();
				command.add(settings.GetXilinxToolPath() + File.separator + Settings.XilinxPrograms[2]);
				command.add("-intstyle");
				command.add("ise");
				command.add("-o");
				command.add("logisim_map");
				command.add("logisim.ngd");
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(sandboxPath));
				final Process CreateProject = Xilinx.start();
				InputStream is = CreateProject.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				err.NewConsole("mapping");
				while ((line = br.readLine()) != null) {
					err.printf("%s", line);
				}
				CreateProject.waitFor();
				if (CreateProject.exitValue() != 0) {
					err.AddFatalError("Failed to map Xilinx design; cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				err.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				err.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			}
		}
		if (!BitFileExists) {
			try {
				text.setText("Place and routing Design");
				labelRect = text.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				text.paintImmediately(labelRect);
				progress.setValue(3);
				Rectangle ProgRect = progress.getBounds();
				ProgRect.x = 0;
				ProgRect.y = 0;
				progress.paintImmediately(ProgRect);
				command.clear();
				if (!isCPLD) {
					command.add(settings.GetXilinxToolPath() + File.separator + Settings.XilinxPrograms[3]);
					command.add("-w");
					command.add("-intstyle");
					command.add("ise");
					command.add("-ol");
					command.add("high");
					command.add("logisim_map");
					command.add("logisim_par");
					command.add("logisim_map.pcf");
				} else {
					command.add(settings.GetXilinxToolPath() + File.separator + Settings.XilinxPrograms[6]);
					command.add("-p");
					command.add(board.fpga.Part.toUpperCase() + "-"
							+ board.fpga.SpeedGrade + "-"
							+ board.fpga.Package.toUpperCase());
					command.add("-intstyle");
					command.add("ise");
					/* TODO: do correct termination type */
					command.add("-terminate");
          command.add(board.fpga.UnusedPinsBehavior.xilinx);
					command.add("-loc");
					command.add("on");
					command.add("-log");
					command.add("logisim_cpldfit.log");
					command.add("logisim.ngd");
				}
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(sandboxPath));
				final Process CreateProject = Xilinx.start();
				InputStream is = CreateProject.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				err.NewConsole("place & route");
				while ((line = br.readLine()) != null) {
					err.printf("%s", line);
				}
				CreateProject.waitFor();
				if (CreateProject.exitValue() != 0) {
					err.AddFatalError("Failed to P&R Xilinx design; cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				err.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				err.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			}
		}
		if (!BitFileExists) {
			try {
				text.setText("Generating Bitfile");
				labelRect = text.getBounds();
				labelRect.x = 0;
				labelRect.y = 0;
				text.paintImmediately(labelRect);
				progress.setValue(4);
				Rectangle ProgRect = progress.getBounds();
				ProgRect.x = 0;
				ProgRect.y = 0;
				progress.paintImmediately(ProgRect);
				command.clear();
				if (!isCPLD) {
					command.add(settings.GetXilinxToolPath() + File.separator + Settings.XilinxPrograms[4]);
					command.add("-w");
          PullBehavior dir = board.fpga.UnusedPinsBehavior;
					if (dir == PullBehavior.PULL_UP || dir == PullBehavior.PULL_DOWN) {
						command.add("-g");
						command.add("UnusedPin:"+dir.xilinx.toUpperCase());
					}
					command.add("-g");
					command.add("StartupClk:CCLK");
					command.add("logisim_par");
					command.add(TOP_HDL + ".bit");
				} else {
					command.add(settings.GetXilinxToolPath() + File.separator + Settings.XilinxPrograms[7]);
					command.add("-i");
					command.add("logisim.vm6");
				}
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(sandboxPath));
				final Process CreateProject = Xilinx.start();
				InputStream is = CreateProject.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				err.NewConsole("generate");
				while ((line = br.readLine()) != null) {
					err.printf("%s", line);
				}
				CreateProject.waitFor();
				if (CreateProject.exitValue() != 0) {
					err.AddFatalError("Failed generate bitfile; cannot download");
					panel.dispose();
					return false;
				}
			} catch (IOException e) {
				err.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			} catch (InterruptedException e) {
				err.AddFatalError("Internal Error during Xilinx download");
				panel.dispose();
				return false;
			}
		}
		try {
			text.setText("Downloading Bitfile");
			labelRect = text.getBounds();
			labelRect.x = 0;
			labelRect.y = 0;
			text.paintImmediately(labelRect);
			progress.setValue(5);
			Rectangle ProgRect = progress.getBounds();
			ProgRect.x = 0;
			ProgRect.y = 0;
			progress.paintImmediately(ProgRect);
			Object[] options = { "Yes, download" };
			if (JOptionPane
					.showOptionDialog(
							progress,
							"Verify that your board is connected and you are ready to download.",
							"Ready to download ?", JOptionPane.YES_OPTION,
							JOptionPane.WARNING_MESSAGE, null, options,
							options[0]) == JOptionPane.CLOSED_OPTION) {
				err.AddSevereWarning("Download aborted.");
				panel.dispose();
				return false;
			}
			/* Until here update of status window */
			if (!board.fpga.USBTMCDownload) {
				command.clear();
				command.add(settings.GetXilinxToolPath() + File.separator + Settings.XilinxPrograms[5]);
				command.add("-batch");
				command.add(scriptPath.replace(projectPath, "../") + download_file);
				ProcessBuilder Xilinx = new ProcessBuilder(command);
				Xilinx.directory(new File(sandboxPath));
				final Process CreateProject = Xilinx.start();
				InputStream is = CreateProject.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				err.NewConsole("download");
				while ((line = br.readLine()) != null) {
					err.printf("%s", line);
				}
				CreateProject.waitFor();
				if (CreateProject.exitValue() != 0) {
					err.AddFatalError("Failed in downloading");
					panel.dispose();
					return false;
				}
				/* Until here is the standard download with programmer */
			} else {
				err.NewConsole("download");
				/* Here we do the USBTMC Download */
				boolean usbtmcdevice = new File("/dev/usbtmc0").exists();
				if (!usbtmcdevice) {
					err.AddFatalError("Could not find usbtmc device");
					panel.dispose();
					return false;
				}
				File bitfile = new File(sandboxPath + TOP_HDL + bitFileExt);
				byte[] bitfile_buffer = new byte[BUFFER_SIZE];
				int bitfile_buffer_size;
				BufferedInputStream bitfile_in = new BufferedInputStream(
						new FileInputStream(bitfile));
				File usbtmc = new File("/dev/usbtmc0");
				BufferedOutputStream usbtmc_out = new BufferedOutputStream(
						new FileOutputStream(usbtmc));
				usbtmc_out.write("FPGA ".getBytes());
				bitfile_buffer_size = bitfile_in.read(bitfile_buffer, 0,
						BUFFER_SIZE);
				while (bitfile_buffer_size > 0) {
					usbtmc_out.write(bitfile_buffer, 0, bitfile_buffer_size);
					bitfile_buffer_size = bitfile_in.read(bitfile_buffer, 0,
							BUFFER_SIZE);
				}
				usbtmc_out.close();
				bitfile_in.close();
			}
		} catch (IOException e) {
			err.AddFatalError("Internal Error during Xilinx download");
			panel.dispose();
			return false;
		} catch (InterruptedException e) {
			err.AddFatalError("Internal Error during Xilinx download");
			panel.dispose();
			return false;
		}

		panel.dispose();
		return true;
	}

  private boolean generateVhdlListFile(ArrayList<String> hdlFiles) {
    Hdl out = new Hdl(lang, err);
    String kind = lang.toUpperCase();
		for (String f : hdlFiles)
			out.stmt("%s work \"%s\"", kind, f);
		File f = FileWriter.GetFilePointer(scriptPath, vhdl_list_file, err);
		return f != null && FileWriter.WriteContents(f, out, err);
  }

  private boolean generateRunScript() {
    Chipset chip = board.fpga;
    String dev = String.format("%s-%s-%s", chip.Part, chip.Package, chip.SpeedGrade);
    String vhdlListPath = scriptPath.replace(projectPath, "../") + vhdl_list_file;
    Hdl out = new Hdl(lang, err);
		out.stmt("run -top %s -ofn logisim.ngc -ofmt NGC -ifn %s -ifmt mixed -p %s",
        TOP_HDL, vhdlListPath, dev);
		File f = FileWriter.GetFilePointer(scriptPath, script_file, err);
		return f != null && !FileWriter.WriteContents(f, out, err);
  }

  private boolean generateDownloadScript() {
		boolean isCPLD = isCPLD(board.fpga);
		String bitFileExt = isCPLD ? ".jed" : ".bit";
    int jtagPos = board.fpga.JTAGPos;
    Hdl out = new Hdl(lang, err);
		out.stmt("setmode -bscan");
		if (writeToFlash && board.fpga.FlashDefined) {
			String mcsFile = scriptPath + mcs_file;
			out.stmt("setmode -pff");
			out.stmt("setSubMode -pffserial");
			out.stmt("addPromDevice -p %s -size 0 -name %s", jtagPos, board.fpga.FlashName);
			out.stmt("addDesign -version 0 -name \"0\"");
			out.stmt("addDeviceChain -index 0");
			out.stmt("addDevice -p %s -file %s", jtagPos, TOP_HDL + bitFileExt);
			out.stmt("generate -format mcs -fillvalue FF -output %s", mcsFile);
			out.stmt("setMode -bs");
			out.stmt("setCable -port auto");
			out.stmt("identify");
			out.stmt("assignFile -p %s -file %s", board.fpga.FlashPos, mcsFile);
			out.stmt("program -p %s -e -v", board.fpga.FlashPos);
		} else if (isCPLD) {
			out.stmt("setcable -p auto");
			out.stmt("identify");
      out.stmt("assignFile -p %s -file %s", jtagPos, "logisim"+ bitFileExt);
      out.stmt("program -p %s -e", jtagPos);
    } else {
      out.stmt("setcable -p auto");
      out.stmt("identify");
      out.stmt("assignFile -p %s -file %s", jtagPos, TOP_HDL + bitFileExt);
      out.stmt("program -p %s -onlyFpga", jtagPos);
		}
		out.stmt("quit");
		File f = FileWriter.GetFilePointer(scriptPath, download_file, err);
		return f != null && FileWriter.WriteContents(f, out, err);
  }

  private boolean generateUcfFile(PinBindings ioResources) {
    Hdl out = new Hdl(lang, err);
		if (ioResources.requiresOscillator) {
			out.stmt("NET \"%s\" %s ;", CLK_PORT, xilinxClockSpec(board.fpga));
			out.stmt("NET \"%s\" TNM_NET = \"%s\" ;", CLK_PORT, CLK_PORT);
			out.stmt("TIMESPEC \"TS_%s\" = PERIOD \"%s\" %s HIGH 50 % ;",
          CLK_PORT, CLK_PORT, board.fpga.Speed);
			out.stmt();
		}
    ioResources.forEachPhysicalPin((pin, net, io, label) -> {
      String spec = String.format("LOC = \"%s\"", pin);
      if (io.pull == PullBehavior.PULL_UP || io.pull == PullBehavior.PULL_DOWN)
        spec += " | " + io.pull.xilinx;
      if (io.strength != DriveStrength.UNKNOWN && io.strength != DriveStrength.DEFAULT)
        spec += " | DRIVE = " + io.strength.ma;
      if (io.standard != IoStandard.UNKNOWN && io.standard != IoStandard.DEFAULT)
        spec += " | IOSTANDARD = " + io.standard;
      out.stmt("NET \"%s\" %s  ;# %s", net, spec, label);
    });
		File f = FileWriter.GetFilePointer(ucfPath, ucf_file, err);
		return FileWriter.WriteContents(f, out, err);
  }

  @Override
  public boolean generateScripts(PinBindings ioResources, ArrayList<String> hdlFiles) {
    return generateVhdlListFile(hdlFiles)
        && generateRunScript()
        && generateDownloadScript()
        && generateUcfFile(ioResources);
	}

	private static String xilinxClockSpec(Chipset chip) {
    String spec = String.format("LOC = \"%s\"", chip.ClockPinLocation);
    PullBehavior pull = chip.ClockPullBehavior;
		if (pull == PullBehavior.PULL_UP || pull == PullBehavior.PULL_DOWN)
			spec += " | " + pull.xilinx;
    IoStandard std = chip.ClockIOStandard;
		if (std != IoStandard.DEFAULT && std != IoStandard.UNKNOWN)
			spec += " | IOSTANDARD = " + std;
		return spec;
	}

  private static boolean isCPLD(Chipset chip) {
    String part = chip.Part.toUpperCase();
    return part.startsWith("XC2C")
        || part.startsWith("XA2C")
        || part.startsWith("XCR3")
        || part.startsWith("XC9500")
        || part.startsWith("XA9500");
  }

	private final static String vhdl_list_file = "XilinxVHDLList.prj";
	private final static String script_file = "XilinxScript.cmd";
	private final static String ucf_file = "XilinxConstraints.ucf";
	private final static String download_file = "XilinxDownload";
	private final static String mcs_file = "XilinxProm.mcs";
	private final static Integer BUFFER_SIZE = 16 * 1024;

}
