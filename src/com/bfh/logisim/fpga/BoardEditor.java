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

package com.bfh.logisim.fpga;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.gui.generic.ComboBox;
import com.cburch.logisim.gui.main.ExportImage;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.Errors;
import com.cburch.logisim.util.JDialogOk;

public class BoardEditor extends JFrame {

	private JButton save, load, builtin;

	private JTextField name;
	private BoardPanel image;
  private Chipset fpga;
	private LinkedList<BoardIO> ioComponents = new LinkedList<>();
  public HashMap<BoardIO, Rect> rects = new HashMap<>();

	public BoardEditor() {
    super(Strings.get("FPGABoardEditor"));
		final GridBagConstraints c = new GridBagConstraints();

		setResizable(false);
		// addComponentListener(this);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setLayout(new BorderLayout());

		// Set an empty board picture
		image = new BoardPanel(this);
    image.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		add(image, BorderLayout.CENTER);

		JPanel buttons = new JPanel() {
      @Override
      public Component add(Component comp) {
        super.add(comp, c);
        c.gridx++;
        return comp;
      }
    };
		// buttons.setLayout(new GridBagLayout());

		// c.gridx = 0;
		// c.gridy = 0;
		// c.fill = GridBagConstraints.HORIZONTAL;

		buttons.add(new JLabel("Board Name:"));

		name = new JTextField(20);
		name.setEnabled(true);
		buttons.add(name);

		JButton chipset = new JButton("Configure FPGA Chipset");
		chipset.addActionListener(e -> doChipsetDialog());
		buttons.add(chipset);

		builtin = new JButton("Built-in FPGA Boards");
		builtin.addActionListener(e -> doBuiltin());
		buttons.add(builtin);

		load = new JButton("Load");
		load.addActionListener(e -> doLoad());
		buttons.add(load);

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> { setVisible(false); clear(); });
		buttons.add(cancel);

		save = new JButton("Done");
		save.setActionCommand("save");
		save.addActionListener(e -> doSave());
		save.setEnabled(false);
		buttons.add(save);

		add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void doSave() {
    Board board = new Board(name.getText(), fpga, image.getImage());
    String dir = getSaveDirectory();
    if (dir == null)
      return;
    String filename = dir + board.name + ".xml";
    if (!BoardWriter.write(filename, board))
      return;
    setVisible(false);
    clear();
  }

  private void doBuiltin() {
    Settings settings = new Settings();
    ComboBox<String> boardsList = new ComboBox<>();
    for (String boardname : settings.GetBoardNames())
      boardsList.addItem(boardname);
    boardsList.setSelectedItem(settings.GetSelectedBoard());
    JDialogOk dlg = new JDialogOk("Select Built-in FPGA Board") {
      public void okClicked() {
        String name = boardsList.getSelectedValue();
        settings.SetSelectedBoard(name);
        settings.UpdateSettingsFile();
        setBoard(BoardReader.read(settings.GetSelectedBoardFileName()));
      }
    };
    JPanel p = new JPanel();
    p.add(new JLabel("Select a built-in FPGA board:"));
    p.add(boardsList);
    dlg.getContentPane().add(p, BorderLayout.CENTER);
    dlg.pack();
    dlg.setVisible(true);
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
    setBoard(BoardReader.read(path));
  }

  private void setBoard(Board board) {
    if (board == null)
      return;

    name.setText(board.name);
    fpga = board.fpga;
    image.setImage(board.image);
    ioComponents.clear();
    rects.forEach((io, rect) -> remove(rect));
    rects.clear();

    ioComponents.addAll(board);
    for (BoardIO io : ioComponents) {
      Rect r = new Rect(io);
      rects.put(io, r);
      add(r);
    }

    setEnables();
	}

  private void setEnables() {
    save.setEnabled(!name.getText().isEmpty()
        && fpga != null
        && image.getImage() != null
        && !ioComponents.isEmpty());
  }

	public void clear() {
		if (isVisible())
			setVisible(false);
		image.clear();
		ioComponents.clear();
    rects.forEach((io, rect) -> remove(rect));
    rects.clear();
		fpga = null;
		name.setText("");
		save.setEnabled(false);
	}

	private String getSaveDirectory() {
		JFileChooser fc = new JFileChooser();
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

	public void reactivate() {
    if (!isVisible()) {
      clear();
      setVisible(true);
    }
    toFront();
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
		final JDialog dlg = new JDialog(this, "FPGA Chipset Properties");
		GridBagConstraints c = new GridBagConstraints();
		dlg.setLayout(new GridBagLayout());
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;

    final boolean ok[] = new boolean[] { false };
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener((e) -> {
      ok[0] = false;
      dlg.setVisible(false);
    });
		JButton save = new JButton("Done and Store");
		save.addActionListener((e) -> {
      ok[0] = true;
      dlg.setVisible(false); 
    });

		JTextField rate = new JTextField(10);
		JComboBox<String> hz = new JComboBox<>(new String[] { "Hz", "kHz", "MHz" });
		JTextField clkLoc = new JTextField();
		JComboBox<PullBehavior> clkPull = new JComboBox<>(PullBehavior.OPTIONS);
		JComboBox<IoStandard> clkStandard = new JComboBox<>(IoStandard.OPTIONS);
		JComboBox<PullBehavior> unusedPull = new JComboBox<>(PullBehavior.OPTIONS);
		JTextField jtagPos = new JTextField("1");
		JComboBox<String> vendor = new JComboBox<>(Chipset.VENDORS);
		JTextField family = new JTextField();
		JTextField part = new JTextField();
		JTextField pkg = new JTextField();
		JTextField speed = new JTextField();
		JTextField flashName = new JTextField();
		JTextField flashPos = new JTextField("2");
		JCheckBox usbTmc = new JCheckBox("USBTMC Download");

    if (fpga == null) {
      // sensible default values
      rate.setText("50");
      hz.setSelectedIndex(2);
      clkPull.setSelectedIndex(0);
      clkStandard.setSelectedIndex(0);
      unusedPull.setSelectedIndex(0);
      vendor.setSelectedIndex(0);
      usbTmc.setSelected(false);
    } else {
      rate.setText(fpga.Speed.split(" ")[0]);
      hz.setSelectedItem(fpga.Speed.split(" ")[1]);
      clkLoc.setText(fpga.ClockPinLocation);
      clkPull.setSelectedItem(fpga.ClockPullBehavior);
      clkStandard.setSelectedItem(fpga.ClockIOStandard);
      unusedPull.setSelectedItem(fpga.UnusedPinsBehavior);
      jtagPos.setText(""+fpga.JTAGPos);
      vendor.setSelectedItem(fpga.VendorName);
      family.setText(fpga.Technology);
      part.setText(fpga.Part);
      pkg.setText(fpga.Package);
      speed.setText(fpga.SpeedGrade);
      flashName.setText(fpga.FlashName);
      flashPos.setText(""+fpga.FlashPos);
      usbTmc.setSelected(fpga.USBTMCDownload);
    }

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
      if (!ok[0])
        break;
      long freq = getFrequency(rate.getText(), hz.getSelectedItem().toString());
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
        HashMap<String, String> params = new HashMap<>();
        params.put("ClockInformation/Frequency", ""+freq);
        params.put("ClockInformation/FPGApin", clkLoc.getText());
        params.put("ClockInformation/PullBehavior", ""+clkPull.getSelectedItem());
        params.put("ClockInformation/IOStandard", ""+clkStandard.getSelectedItem());
        params.put("FPGAInformation/Family", family.getText() );
        params.put("FPGAInformation/Part", part.getText());
        params.put("FPGAInformation/Package", pkg.getText());
        params.put("FPGAInformation/Speedgrade", speed.getText());
        params.put("FPGAInformation/Vendor", ""+vendor.getSelectedItem());
        params.put("FPGAInformation/USBTMC", ""+usbTmc.isSelected());
        params.put("FPGAInformation/JTAGPos", jtagPos.getText());
        params.put("FPGAInformation/FlashPos", flashPos.getText());
        params.put("FPGAInformation/FlashName", flashName.getText());
        params.put("UnusedPins/PullBehavior", ""+unusedPull.getSelectedItem());
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

  private void doChangeImage() {
    JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fc.setDialogTitle("Choose FPGA board picture to use");
    fc.setFileFilter(ExportImage.PNG_FILTER);
    fc.setAcceptAllFileFilterUsed(false);
    int retval = fc.showOpenDialog(null);
    if (retval == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      try {
        image.setImage(file);
      } catch (IOException ex) {
        Errors.title("Error").show("Error loading image", ex);
      }
    }
  }

  public void doClickDialog(int x, int y) {
    if (image.getImage() == null) {
      doChangeImage();
    } else {
      for (BoardIO io : ioComponents) {
        if (io.rect.contains(x, y)) {
          doBoardIODialog(io, rects.get(io));
          break;
        }
      }
    }
  }
	
  public void doRectSelectDialog(Bounds rect) {
    for (BoardIO io : ioComponents) {
      if (io.rect.overlaps(rect)) {
        Errors.title("Error").show("Please ensure rectangles do not overlap.");
        return;
      }
    }
		final JDialog dlg = new JDialog(this, "Add FPGA Board I/O Resource");
		dlg.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = -1;
		c.fill = GridBagConstraints.HORIZONTAL;
		for (BoardIO.Type type : BoardIO.PhysicalTypes) {
      JButton button = new JButton("Define " + type + " Component");
			button.addActionListener(e -> {
        dlg.setVisible(false);
        BoardIO io = BoardIO.makeUserDefined(type, rect, this);
        if (io != null) {
          ioComponents.add(io);
          Rect r = new Rect(io);
          rects.put(io, r);
          add(r);
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

  private void doBoardIODialog(BoardIO io, Rect r) {
    BoardIO redo = BoardIO.redoUserDefined(io, this);
    if (io == null) {
      remove(r);
      rects.remove(io);
    } else {
      r.set(redo);
      rects.remove(io);
      rects.put(redo, r);
    }
  }

  private static final Color REGULAR = new Color(1f, 0f, 0f, 0.4f);
  private static final Color HILIGHT = new Color(1f, 0f, 0f, 0.6f);

  private class Rect extends JPanel implements MouseListener, MouseMotionListener {
    BoardIO io;

    Rect(BoardIO io) {
      set(io);
      setOpaque(false);
      setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.RED));
      setBackground(REGULAR);
      addMouseListener(this);
      addMouseMotionListener(this);
    }

    void set(BoardIO io) {
      this.io = io;
      this.setLocation(io.rect.x, io.rect.y);
      this.setSize(new Dimension(io.rect.width, io.rect.height));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (!SwingUtilities.isLeftMouseButton(e))
        return;
      doBoardIODialog(io, this);
    }

    public void mouseEntered(MouseEvent e) { setBackground(HILIGHT); }
    public void mouseExited(MouseEvent e) { setBackground(REGULAR); }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    public void mouseMoved(MouseEvent e) { }
    public void mouseDragged(MouseEvent e) { }
  }
}
