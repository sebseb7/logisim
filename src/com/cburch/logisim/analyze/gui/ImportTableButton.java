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

package com.cburch.logisim.analyze.gui;

import java.util.Scanner;
import java.util.Date;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.PrintStream;
import java.io.IOException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.VariableList;

class ImportTableButton extends JButton {
	private static final long serialVersionUID = 1L;

	private JFrame parent;
	private AnalyzerModel model;

	ImportTableButton(JFrame parent, AnalyzerModel model) {
		this.parent = parent;
		this.model = model;
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doLoad();
			}
		});
	}

	void localeChanged() {
		setText(Strings.get("importTableButton"));
	}

	static final Pattern NAME_FORMAT = Pattern.compile("([a-zA-Z][a-zA-Z_0-9])\\[([1-9][0-9]*)\\.\\.([0-9][0-9]*)\\]");

	int lineno = 0;

	void validateHeader(String line, VariableList inputs, VariableList outputs) throws IOException {
		String s[] = line.split("\\s+");
		VariableList cur = inputs;
		for (int i = 0; i < s.length; i++) {
			if (s[i].equals("|")) {
				if (cur == inputs)
					cur = outputs;
				else
					throw new IOException(String.format("Line %d: Separator '|' must appear only once.", lineno));
				continue;
			}
			String name = s[i];
			if (name.matches("[a-zA-Z][a-zA-Z_0-9]")) {
				cur.add(new Var(name, 1));
			} else {
				Matcher m = NAME_FORMAT.matcher(line);
				if (!m.matches())
					throw new IOException(String.format("Line %d: Invalid variable name '%s'.", lineno, name));
				String n = m.group(1);
				int a, b;
				try {
					a = Integer.parseInt(m.group(2));
					b = Integer.parseInt(m.group(3));
				} catch (NumberFormatException e) {
					throw new IOException(String.format("Line %d: Invalid bit range in '%s'.", lineno, name));
				}
				if (a < 1 || b != 0)
					throw new IOException(String.format("Line %d: Invalid bit range in '%s'.", lineno, name));
				try {
					cur.add(new Var(n, b-a+1));
				} catch (IllegalArgumentException e) {
					throw new IOException(String.format("Line %d: Too many bits in %s for truth table.", lineno, (cur == inputs ? "input" : "output")));
				}
			}
		}
		if (inputs.vars.size() == 0)
			throw new IOException(String.format("Line %d: Truth table has no inputs.", lineno));
		if (outputs.vars.size() == 0)
			throw new IOException(String.format("Line %d: Truth table has no outputs.", lineno));
	}

	void validateBreak(String line) throws IOException {
		if (!line.matches("\\s*[~_=-][ ~_=-|]*"))
			throw new IOException(String.format("Line %d: Expecting separator line after header, instead found '%s'.", lineno, line));
	}

	Entry parseBit(char c, Var var) throws IOException {
		if (c == 'x' || c == 'X')
			return Entry.DONT_CARE;
		else if (c == '0')
			return Entry.ZERO;
		else if (c == '1')
			return Entry.ONE;
		else
			throw new IOException(String.format("Line %d: Bit value '%c' must be one of '0', '1', or 'x'.", lineno, c));
	}

	void validateRow(String line, VariableList inputs, VariableList outputs, ArrayList<Entry[]> rows) throws IOException {
		Entry[] row = new Entry[inputs.bits.size() + outputs.bits.size()];
		int col = 0;
		String s[] = line.split("\\s+");
		int ix = 0;
		for (Var var : inputs.vars) {
			if (ix >= s.length || s[ix].equals("|"))
				throw new IOException(String.format("Line %d: Not enough input columns.", lineno));
			String sval = s[ix++];
			if (sval.length() != var.width)
				throw new IOException(String.format("Line %d: Column value \"%s\" for %s must have exactly %d bits.", lineno, sval, var.name, var.width));
			for (int i = 0; i < var.width; i++)
				row[col++] = parseBit(sval.charAt(var.width-i-1), var);
		}
		if (ix >= s.length)
			throw new IOException(String.format("Line %d: Missing '|' column separator.", lineno));
		else if (!s[ix].equals("|"))
			throw new IOException(String.format("Line %d: Too many input columns.", lineno));
		ix++;
		for (Var var : inputs.vars) {
			if (ix >= s.length)
				throw new IOException(String.format("Line %d: Not enough output columns.", lineno));
			else if (s[ix].equals("|"))
				throw new IOException(String.format("Line %d: Column separator '|' must appear only once.", lineno));
			String sval = s[ix++];
			if (sval.length() != var.width)
				throw new IOException(String.format("Line %d: Column value \"%s\" for %s must have exactly %d bits.", lineno, sval, var.name, var.width));
			for (int i = 0; i < var.width; i++)
				row[col++] = parseBit(sval.charAt(var.width-i-1), var);
		}
		if (ix != s.length)
			throw new IOException(String.format("Line %d: Too many output columns.", lineno));
		rows.add(row);
	}

	void doLoad(File file) throws IOException {
		lineno = 0;
		Scanner sc = new Scanner(file);
		VariableList inputs = new VariableList(AnalyzerModel.MAX_INPUTS);
		VariableList outputs = new VariableList(AnalyzerModel.MAX_OUTPUTS);
		ArrayList<Entry[]> rows = new ArrayList<>();
		try {
			while (sc.hasNextLine()) {
				lineno++;
			    String line = sc.nextLine();
				int ix = line.indexOf('#');
				if (ix >= 0)
					line = line.substring(0, ix);
				line = line.trim();
				if (line.equals(""))
					continue;
				if (rows.size() == 0) {
					validateHeader(line, inputs, outputs);
				} else if (rows.size() == 1) {
					validateBreak(line);
				} else {
					validateRow(line, inputs, outputs, rows);
				}
			}
			if (rows.size() == 0)
				throw new IOException("End of file: Truth table has no rows.");
			try {
				model.setVariables(inputs, outputs);
				TruthTable table = model.getTruthTable();
				table.setVisibleRows(rows);
		} finally {
			sc.close();
		}
	}

	private File lastFile = null;
	void doLoad() {
		if (lastFile == null) {
			Circuit c = model.getCurrentCircuit();
			if (c != null)
				lastFile = new File(c.getName() + ".txt");
			else
				lastFile = new File("truthtable.txt");
		}
		JFileChooser chooser = JFileChoosers.createSelected(lastFile);
		chooser.setDialogTitle(Strings.get("openButton"));
		chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
		chooser.addChoosableFileFilter(ExportTableButton.FILE_FILTER);
		chooser.setFileFilter(ExportTableButton.FILE_FILTER);
		int choice = chooser.showOpenDialog(parent);
		if (choice == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			if (file.isDirectory()) {
				JOptionPane.showMessageDialog(parent,
						StringUtil.format(Strings.get("notFileMessage"), file.getName()),
						Strings.get("openErrorTitle"), JOptionPane.OK_OPTION);
				return;
			}
			if (!file.exists() || !file.canRead()) {
				JOptionPane.showMessageDialog(parent,
						StringUtil.format(Strings.get("cantReadMessage"), file.getName()),
						Strings.get("openErrorTitle"), JOptionPane.OK_OPTION);
				return;
			}
			try {
				doLoad(file);
				lastFile = file;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(parent,
						e.getMessage(),
						Strings.get("openErrorTitle"),
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

}
