/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.bfh.logisim.fpgagui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.BoardRectangle;
import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.fpgaboardeditor.Strings;
import com.bfh.logisim.settings.Settings;

public class FPGASettingsDialog implements ActionListener {

	private JDialog panel;
	private Settings settings;
	private JTextField alteraPath, xilinxPath, workPath;
	private JRadioButton altera32Choice, altera64Choice;

	public FPGASettingsDialog(JFrame parentFrame, Settings settings) {
		this.settings = settings;

		panel = new JDialog(parentFrame, ModalityType.APPLICATION_MODAL);
		panel.setTitle("FPGA Compiler and Toolchain Settings");
		panel.setResizable(false);
		panel.setAlwaysOnTop(false);
		panel.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		String apath = settings.GetAlteraToolPath();
		if ("Unknown".equals(apath)) apath = "";
		String xpath = settings.GetXilinxToolPath();
		if ("Unknown".equals(xpath)) xpath = "";
		String wpath = settings.GetStaticWorkspacePath();
		if (wpath == null) wpath = "";

		JLabel globalSection = new JLabel("Global Settings");
		JLabel alteraSection = new JLabel("Altera Settings");
		JLabel xilinxSection = new JLabel("Xilinx Settings");
		Font font = globalSection.getFont();
		Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
		globalSection.setFont(boldFont);
		alteraSection.setFont(boldFont);
		xilinxSection.setFont(boldFont);

		JLabel workLabel = new JLabel("Temporary directory for compilation:");
		JLabel workStatic = new JLabel("(leave blank to use default)");
		workPath = new JTextField(wpath);
		workPath.setPreferredSize(new Dimension(400, 10));
		JButton workPicker = new JButton("Choose");
		workPicker.setActionCommand("workPicker");
		workPicker.addActionListener(this);

		JLabel alteraLabel = new JLabel("Altera tools path (directory containing " + settings.AlteraPrograms[0]+"):");
		alteraPath = new JTextField(apath);
		alteraPath.setPreferredSize(new Dimension(400, 10));
		JButton alteraPicker = new JButton("Choose");
		alteraPicker.setActionCommand("alteraPicker");
		alteraPicker.addActionListener(this);

		altera32Choice = new JRadioButton("32-bit (faster, less memory, small projects)");
		altera64Choice = new JRadioButton("64-bit (slower, more memory, large projects)");
		ButtonGroup group = new ButtonGroup();
		group.add(altera32Choice);
		group.add(altera64Choice);
		if (settings.GetAltera64Bit())
			altera64Choice.setSelected(true);
		else
			altera32Choice.setSelected(true);

		JLabel xilinxLabel = new JLabel("Xilinx tools path (directory containing " + settings.XilinxPrograms[0]+"):");
		xilinxPath = new JTextField(xpath);
		xilinxPath.setPreferredSize(new Dimension(400, 10));
		JButton xilinxPicker = new JButton("Choose");
		xilinxPicker.setActionCommand("xilinxPicker");
		xilinxPicker.addActionListener(this);

		JButton ok = new JButton("OK");
		ok.setActionCommand("OK");
		ok.addActionListener(this);
		JButton cancel = new JButton("Cancel");
		cancel.setActionCommand("Cancel");
		cancel.addActionListener(this);

		int y = -1;
		
		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(15, 0, 10, 0);
		panel.add(globalSection, c);

		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(5, 10, 0, 0);
		panel.add(workLabel, c);
		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(2, 10, 2, 0);
		panel.add(workStatic, c);
		c.gridx = 0; c.gridy = ++y; c.gridwidth = 1; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(2, 10, 5, 0);
		panel.add(workPath, c);
		c.gridx = 1; c.gridy = y; c.gridwidth = 1; c.fill = GridBagConstraints.NONE; c.insets = new Insets(2, 10, 5, 0);
		panel.add(workPicker, c);

		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(10, 0, 10, 0);
		panel.add(alteraSection, c);

		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(5, 10, 2, 0);
		panel.add(alteraLabel, c);
		c.gridx = 0; c.gridy = ++y; c.gridwidth = 1; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(2, 10, 5, 0);
		panel.add(alteraPath, c);
		c.gridx = 1; c.gridy = y; c.gridwidth = 1; c.fill = GridBagConstraints.NONE; c.insets = new Insets(2, 10, 5, 0);
		panel.add(alteraPicker, c);

		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(5, 10, 5, 0);
		panel.add(new JLabel(), c);

		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(5, 10, 2, 0);
		panel.add(altera32Choice, c);
		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(2, 10, 5, 0);
		panel.add(altera64Choice, c);

		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(10, 0, 10, 0);
		panel.add(xilinxSection, c);

		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(5, 10, 2, 0);
		panel.add(xilinxLabel, c);
		c.gridx = 0; c.gridy = ++y; c.gridwidth = 1; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(2, 10, 5, 0);
		panel.add(xilinxPath, c);
		c.gridx = 1; c.gridy = y; c.gridwidth = 1; c.fill = GridBagConstraints.NONE; c.insets = new Insets(2, 10, 5, 0);
		panel.add(xilinxPicker, c);

		c.gridx = 0; c.gridy = ++y; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(5, 10, 5, 0);
		panel.add(new JLabel(), c);

		c.gridx = 1; c.gridy = ++y; c.gridwidth = 1; c.fill = GridBagConstraints.NONE; c.insets = new Insets(15, 10, 10, 0);
		panel.add(cancel, c);
		c.gridx = 2; c.gridy = y; c.gridwidth = 1; c.fill = GridBagConstraints.NONE; c.insets = new Insets(15, 10, 10, 0);
		panel.add(ok, c);

		panel.pack();
		panel.setMinimumSize(new Dimension(600, 400));
		panel.setLocationRelativeTo(parentFrame);
		panel.setVisible(false);
	}

	public void SetVisible(boolean show) {
		if (!show) {
			panel.setVisible(false);
		} else if (!panel.isVisible()) {
			panel.setVisible(true);
		} else {
			panel.setVisible(false);
			panel.setVisible(true);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("workPicker")) {
			pick(null, workPath.getText());
		} else if (e.getActionCommand().equals("alteraPicker")) {
			pick("Altera", alteraPath.getText());
		} else if (e.getActionCommand().equals("xilinxPicker")) {
			pick("Xilinx", xilinxPath.getText());
		} else if (e.getActionCommand().equals("Cancel")) {
			panel.setVisible(false);
		} else if (e.getActionCommand().equals("OK")) {
			panel.setVisible(false);
			save();
		}
	}

	private void save() {
		String apath = alteraPath.getText();
		if (!"".equals(apath) && !settings.SetAlteraToolPath(apath)) {
			String names = settings.AlteraPrograms[0];
			for (int i = 1; i < settings.AlteraPrograms.length; i++) {
				names += ", " + (i == settings.AlteraPrograms.length - 1 ? "and " :""); 
				names += settings.AlteraPrograms[i];
			}
			JOptionPane.showMessageDialog(null,
					"Error setting Altera tool path.\n" +
					"Please select a directory containing " + names + ".");
		}
		settings.SetAltera64Bit(altera64Choice.isSelected());
		String xpath = xilinxPath.getText();
		if (!"".equals(xpath) && !settings.SetXilinxToolPath(xpath)) {
			String names = settings.XilinxPrograms[0];
			for (int i = 1; i < settings.XilinxPrograms.length; i++) {
				names += ", " + (i == settings.XilinxPrograms.length - 1 ? "and " :""); 
				names += settings.XilinxPrograms[i];
			}
			JOptionPane.showMessageDialog(null,
					"Error setting Xilinx tool path.\n" +
					"Please select a directory containing " + names + ".");
		}
		if (!settings.SetStaticWorkspacePath(workPath.getText())) {
			JOptionPane.showMessageDialog(null,
					"Error setting temporary compilation path.");
		}
		if (!settings.UpdateSettingsFile()) {
			JOptionPane.showMessageDialog(null, "Error saving settings file.\n");
		}
	}

	private void pick(String vendor, String path) {
		JFileChooser fc = new JFileChooser(path);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (!"".equals(path)) {
			File file = new File(path);
			if (file.exists()) {
				fc.setSelectedFile(file);
			}
		}
		if (vendor != null)
			fc.setDialogTitle(vendor + " Design Suite Path Selection");
		else
			fc.setDialogTitle("Temporary directory for compilation");
		int retval = fc.showOpenDialog(null);
		if (retval != JFileChooser.APPROVE_OPTION)
			return;
		File file = fc.getSelectedFile();
		path = file.getPath();
		if (vendor.equals("Altera")) {
			alteraPath.setText(path);
		} else if (vendor.equals("Xilinx")) {
			xilinxPath.setText(path);
		} else {
			workPath.setText(path);
		}
	}
}
