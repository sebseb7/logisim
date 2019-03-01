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

package com.bfh.logisim.fpgaboardeditor;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.util.Errors;

public class BoardDialog implements ComponentListener {

	public final JFrame panel;
	private JButton save, load;

	private JTextField name;
	private BoardPanel image;
  private Chipset fpga;
	public LinkedList<BoardIO> ioComponents = new LinkedList<>();

	public BoardDialog() {
		final GridBagConstraints c = new GridBagConstraints();

		panel = new JFrame(Strings.get("FPGABoardEditor"));
		panel.setResizable(false);
		panel.addComponentListener(this);
		panel.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		panel.setLayout(new GridBagLayout());

		// Set an empty board picture
		image = new BoardPanel(this);
		panel.add(image);

		JPanel buttons = new JPanel() {
      @Override
      public void add(Component comp) {
        super.add(comp, c);
        c.gridx++;
      }
    }
		buttons.setLayout(new GridBagLayout());

		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;

		buttons.add(new JLabel("Board Name:"));

		name = new JTextField(32);
		name.setEnabled(true);
		buttons.add(name);

		JButton chipset = new JButton("Configure FPGA Chipset");
		cancel.addActionListener(e -> doChipsetDialog());
		cancel.setEnabled(true);
		buttons.add(cancel);

		load = new JButton("Load");
		loadButton.addActionListener(e -> doLoad());
		loadButton.setEnabled(true);
		buttons.add(load);

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> { panel.setVisible(false); clear(); });
		cancel.setEnabled(true);
		buttons.add(cancel);

		save = new JButton("Done");
		save.setActionCommand("save");
		save.addActionListener(e -> doSave());
		save.setEnabled(false);
		buttons.add(save);

		c.gridx = 0;
		c.gridy = 1;
		panel.add(buttons, c);

		panel.pack();
		panel.setLocationRelativeTo(null);
		panel.setVisible(true);
	}

	private void doSave() {
    Board board = new Board(name.getText(), fpga, image.getImage());
    String dir = getSaveDirectory();
    if (dir == null)
      return;
    String filename += dir + board.name + ".xml";
    if (!BoardWriter.write(filename, board))
      return;
    panel.setVisible(false);
    clear();
  }

  private void doLoad() {
    JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fc.setDialogTitle("Choose XML board description");
    fc.setFileFilter(Loader.XML_FILTER);
    fc.setAcceptAllFileFilterUsed(false);
    int retval = fc.showOpenDialog(null);
    if (retval != JFileChooser.APPROVE_OPTION)
      return;
    String path = fc.getSelectedFile().getPath();
    Board board = BoardReader.read(path);
    if (board == null)
      return;

    name.setText(board.name);
    fpga = board.fpga;
    image.setImage(board.image);
    ioComponents.clear();
    ioComponents.addAll(board);

    setEnables();
	}

  private void setEnables() {
    save.setEnabled(!name.getText().isEmpty()
        && fpga != null
        && image.getImage() != null
        && !ioComponents.isEmpty());
  }

	public void clear() {
		if (panel.isVisible())
			panel.setVisible(false);
		image.clear();
		ioComponents.clear();
		fpga = null;
		name.setText("");
		save.setEnabled(false);
	}

	private String getSaveDirectory() {
		JFileChooser fc = new JFileChooser(old);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setDialogTitle("Choose directory to save XML board description:");
		int retval = fc.showOpenDialog(null);
		if (retval != JFileChooser.APPROVE_OPTION)
      return null;
    String dir = fc.getSelectedFile().getPath();
    if (!dir.endsWith("/"))
      dir += "/";
    return dir;
	}

	public boolean isActive() {
		return panel.isVisible();
	}

	public void setActive() {
		this.clear();
		panel.setVisible(true);
	}

  private static void add(JComponent dlg, GridBagConstraints c,
      String caption, JComponent input) {
      dlg.add(new JLabel(caption + " "), c);
      c.gridx++;
      dlg.add(input, c);
      c.gridx--;
      c.gridy++;
  }

	private void doChipsetDialog() {
		final JDialog dlg = new JDialog(panel, "FPGA Chipset Properties");
		GridBagConstraints c = new GridBagConstraints();
		dlg.setLayout(new GridBagLayout());
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;

    final boolean save[] = new boolean[] { false; }
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> {
      save[0] = false;
      dlg.setVisible(false)
    };
		JButton save = new JButton("Done and Store");
		save.addActionListener(e -> {
      save[0] = true;
      dlg.setVisible(false); 
    });


		JTextField rate = new JTextField(10);
		JComboBox<String> hz = new JComboBox<>(new String[] { "Hz", "kHz", "MHz" });
		hz.setSelectedIndex(2);

		JTextField clkLoc = new JTextField();

		JComboBox<PullBehavior> clkPull = new JComboBox<>(PullBehavior.OPTIONS);
		clkPull.setSelectedIndex(0);
		JComboBox<IoStandard> clkStandard = new JComboBox<>(IoStandard.OPTIONS);
		clkStandard.setSelectedIndex(0);
		JComboBox<IoStandard> unusedPull = new JComboBox<>(PullBehavior.OPTIONS);
		unusedPull.setSelectedIndex(0);
		JTextField jtagPos = new JTextField("1");
		JComboBox<String> vendor = new JComboBox<>(Chipset.VENDORS);
		vendor.setSelectedIndex(0);
		JTextField family = new JTextField();
		JTextField part = new JTextField();
		JTextField pkg = new JTextField();
		JTextField speed = new JTextField();
		JTextField flashName = new JTextField();
		JTextField flashPos = new JTextField("2");
		JCheckBox usbTmc = new JCheckBox("USBTMC Download");
		usbTmc.setSelected(false);

		JPanel freqPanel = new JPanel();
		freqPanel.setLayout(new GridBagLayout());
		freqPanel.add(rate, c);
		c.gridx++;
		freqPanel.add(hz, c);

		JPanel clockPanel = new JPanel();
		clockPanel.setLayout(new GridBagLayout());
    c.gridx = 0;
    c.gridy = 0;
    add(clockPanel, c, "Clock frequency:", freqPanel);
    add(clockPanel, c, "Clock pin location:", clkLoc);
    add(clockPanel, c, "Clock pin pull behavior:", clkPull);
    add(clockPanel, c, "Clock pin I/O standard:", clkStandard);
    add(clockPanel, c, "Unused FPGA pin behavior:", unusedPull);
    add(clockPanel, c, "FPGA position in JTAG chain:", jtagPos);

		JPanel devPanel = new JPanel();
		devPanel.setLayout(new GridBagLayout());
    c.gridx = 0;
    c.gridy = 0;
    add(devPanel, c, "FPGA vendor:", vendor);
    add(devPanel, c, "FPGA family:", family);
    add(devPanel, c, "FPGA part:", part);
    add(devPanel, c, "FPGA package:", pkg);
    add(devPanel, c, "FPGA speed grade:", speed);
    add(devPanel, c, "Flash name:", flashName);
    add(devPanel, c, "Flash position in JTAG chain:", flashPos);

		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.NORTH;
		dlg.add(clockPanel, c);

		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.NORTH;
		dlg.add(devPanel, c);

		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		dlg.add(usbTmc, c);

		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		dlg.add(cancel, c);

		c.gridx = 1;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		dlg.add(save, c);

		dlg.pack();
		dlg.setLocation(Projects.getCenteredLoc(dlg.getWidth(), dlg.getHeight()));
		dlg.setModal(true);
		dlg.setResizable(false);
		dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dlg.setAlwaysOnTop(false);

    for (;;) {
      dlg.setVisible(true);
      if (!save[0])
        break;
      long freq = getFrequency(rate.getText(), hz.getSelectedItem();
      if (freq == 0) {
        Errors.title("Error").show("Please specify a clock frequency.");
      } else if (freq == -1) {
        Errors.title("Error").show("Clock frequency must be a multiple of 1 Hz.");
      } else if (freq < 0) {
        Errors.title("Error").show("Invalid clock frequency.");
      } else if (clkLoc.getText().isEmpty()) {
        Errors.title("Error").show("Please specify clock pin location.");
      } else if (family.getText().isEmpty()) {
        Errors.title("Error").show("Please specify FPGA family.");
      } else if (part.getText().isEmpty()) {
        Errors.title("Error").show("Please specify FPGA part.");
      } else if (pkg.getText().isEmpty()) {
        Errors.title("Error").show("Please specify FPGA package.");
      } else if (speed.getText().isEmpty()) {
        Errors.title("Error").show("Please specify FPGA speed grade.");
      } else {
        HashMap<String, String> params;
        params.put("ClockInformation/Frequency", freq);
        params.put("ClockInformation/FPGApin", clkLoc.getText());
        params.put("ClockInformation/PullBehavior", clkPull.getSelectedItem());
        params.put("ClockInformation/IOStandard", clkStandard.getSelectedItem());
        params.put("FPGAInformation/Family", family.getText() );
        params.put("FPGAInformation/Part", part.getText());
        params.put("FPGAInformation/Package", pkg.getText());
        params.put("FPGAInformation/Speedgrade", speed.getText());
        params.put("FPGAInformation/Vendor", vendor.getText());
        params.put("FPGAInformation/USBTMC", usbTmc.isSelected());
        params.put("FPGAInformation/JTAGPos", jtagPos.getText());
        params.put("FPGAInformation/FlashPos", flashPos.getText());
        params.put("FPGAInformation/FlashName", flashName.getText());
        params.put("UnusedPins/PullBehavior", unusedPull.getSelectedItem());
        try {
          fpga = new Chipset(params);
          break;
        } catch (Exception e) {
          Errors.title("Error").show("Invalid chipset parameters: " + e.getMessage(), e);
        }
      }
    }
		dlg.dispose();
    save.setEnabled(true);
	}

	private long getFrequency(String str, String speed) {
		long num = 0;
		long multiplier = 1;
		boolean dec_mult = false;

		if (speed.equals("kHz"))
			multiplier = 1000;
		if (speed.equals("MHz"))
			multiplier = 1000000;
		for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
			if (c >= '0' && c <= '9') {
				num *= 10;
				num += (c - '0');
				if (dec_mult) {
					multiplier /= 10;
					if (multiplier == 0)
						return -1;
				}
			} else if (!dec_mult && c == '.') {
					dec_mult = true;
      } else {
					return -2;
			}
		}
		return num * multiplier;
	}

  // public void doRectClickDialog(BoardRectangle rect) {
  // TODO
  // }

	public void doRectSelectDialog(BoardRectangle rect) {
    for (BoardIO io : ioComponents) {
      if (io.rect.overlaps(rect)) {
        Errors.title("Error").show("Please ensure rectangles do not overlap.");
        return;
      }
    }
		final JDialog dlg = new JDialog(panel, "Add FPGA Board I/O Resource");
		dlg.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = -1;
		c.fill = GridBagConstraints.HORIZONTAL;
		for (BoardIO.Type type : BoardIO.KnownTypes) {
      JButton button = new JButton("Define " + type + " Component");
			button.addActionListener(e -> {
        dlg.setVisible(false);
        BoardIO io = BoardIO.makeUserDefined(type, rect, this);
        if (io != null) {
          ioComponents.add(io);
          setEnables();
        }
      });
			c.gridy++;
			dlg.add(button, c);
		}
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dlg.setVisible(false));
		c.gridy++;
		dlg.add(cancel, c);
		dlg.pack();
		dlg.setLocation(Projects.getCenteredLoc(dlg.getWidth(), dlg.getHeight()));
		dlg.setModal(true);
		dlg.setResizable(false);
		dlg.setAlwaysOnTop(false);
		dlg.setVisible(true);
		dlg.dispose();
	}
}
