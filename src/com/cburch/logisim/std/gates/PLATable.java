/* This file is adopted from the MIPS.jar library by
 * Martin Dybdal <dybber@dybber.dk> and
 * Anders Boesen Lindbo Larsen <abll@diku.dk>.
 * It was developed for the computer architecture class at the Department of
 * Computer Science, University of Copenhagen.
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.util.JFileChoosers;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class PLATable {
	private ArrayList<Row> rows = new ArrayList<>();
	private int inSize, outSize;
	int pendingInSize, pendingOutSize;
	private String label = "";

	public PLATable(int inSz, int outSz, String l) {
		inSize = pendingInSize = inSz;
		outSize = pendingOutSize = outSz;
		label = l;
	}

	public PLATable(PLATable other) {
		this(other.inSize, other.outSize, other.label);
		this.copyFrom(other);
	}

	public void setLabel(String l) {
		label = l;
	}

	public void pendingInputSize(int sz) { pendingInSize = sz; }
	public void pendingOutputSize(int sz) { pendingOutSize = sz; }

	public void copyFrom(PLATable other) {
		rows.clear();
		inSize = other.inSize;
		outSize = other.outSize;
		for (Row otherRow : other.rows) {
			Row r = addTableRow();
			r.copyFrom(otherRow);
		}
	}

	public void resize(int newInSize, int newOutSize) {
		inSize = newInSize;
		outSize = newOutSize;
		for (Row r: rows)
			r.truncate(inSize, outSize);
	}

	public Row addTableRow() {
		Row r = new Row(inSize, outSize); 
		rows.add(r);
		return r;
	}

	public void deleteTableRow(Row row) {
		rows.remove(row);
	}

	public String toString() { return toStandardString(); }

	public String toStandardString() {
		String ret = "";
		for (Row r: rows)
			ret += r.toStandardString() + "\n";
		return ret;
	}

	public static PLATable parse(String str) {
		PLATable tt = null;
		for (String line : str.split("\n")) {
			try {
				tt = parseOneLine(tt, line);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(),
						"Error in PLA Table", JOptionPane.ERROR_MESSAGE);
			}
		}
		if (tt == null)
			tt = new PLATable(2, 2, "PLA");
		return tt;
	}

	private static PLATable parseOneLine(PLATable tt, String line) throws IOException {
		line = line.trim();
		int jj = line.indexOf("#");
		String andBits, orBits, comment = "";
		if (jj >= 0) {
			comment = line.substring(jj+1).trim();
			line = line.substring(0, jj).trim();
		}
		if (line.equals(""))
			return tt;
		int ii = line.indexOf(" ");
		if (ii <= 0)
			throw new IOException("PLA row '"+line+"' is missing outputs.");
		andBits = line.substring(0, ii).trim();
		orBits = line.substring(ii+1).trim();
		if (tt == null)
			tt = new PLATable(andBits.length(), orBits.length(), "PLA");
		else if (andBits.length() != tt.inSize)
			throw new IOException("PLA row '"+line+"' must have exactly " + tt.inSize + " input bits.");
		else if (orBits.length() != tt.outSize)
			throw new IOException("PLA row '"+line+"' must have exactly " + tt.outSize + " output bits.");
		Row r = tt.addTableRow();
		for (int i = 0; i < andBits.length(); i++) {
			char s = andBits.charAt(i);
			if (s != ONE && s != ZERO && s != DONTCARE)
				throw new IOException("PLA row '"+line+"' contains invalid input bit '"+s+"'.");
			r.inBits[andBits.length() - i - 1] = s;
		}
		for (int i = 0; i < orBits.length(); i++) {
			char s = orBits.charAt(i);
			if (s != ONE && s != ZERO)
				throw new IOException("PLA row '"+line+"' contains invalid output bit '"+s+"'.");
			r.outBits[orBits.length() - i - 1] = s;
		}
		r.comment = comment;
		return tt;
	}

	public static PLATable parse(File src) throws IOException {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(src));
		} catch (IOException e) {
			throw new IOException(Strings.get("plaFileOpenError"));
		}
		PLATable tt = null;
		try {
			String line = in.readLine();
			while (line != null) {
				tt = parseOneLine(tt, line);
				line = in.readLine();
			}
		} finally {
			try { in.close(); }
			catch (IOException e) { }
		}
		return tt;
	}

	public void save(File dst) throws IOException {
		FileWriter out;
		try {
			out = new FileWriter(dst);
		} catch (IOException e) {
			throw new IOException(Strings.get("plaFileCreateError"));
		}
		try {
			out.write("# Logisim PLA program table\n");
			out.write(toStandardString());
		} finally {
			try { out.close(); }
			catch (IOException e) { }
		}
	}


	private static final char ONE = '1';
	private static final char ZERO = '0';
	private static final char DONTCARE = 'x';

	public static class Row {
		private char[] inBits, outBits;
		private String comment = "";

		public Row(int inSize, int outSize) {
			inBits = new char[inSize];
			outBits = new char[outSize];
			for (int i = 0; i < inSize; i++) inBits[i] = ZERO;
			for (int i = 0; i < outSize; i++) outBits[i] = ZERO;
		}

		public void copyFrom(Row other) {
			for (int i = 0; i < inBits.length; i++) inBits[i] = other.inBits[i];
			for (int i = 0; i < outBits.length; i++) outBits[i] = other.outBits[i];
			comment = other.comment;
		}

		public char changeInBit(int i) {
			if (inBits[i] == ZERO) inBits[i] = ONE;
			else if (inBits[i] == ONE) inBits[i] = DONTCARE;
			else inBits[i] = ZERO;
			return inBits[i];
		}
		public char changeOutBit(int i) {
			if (outBits[i] == ZERO) outBits[i] = ONE;
			else if (outBits[i] == ONE) outBits[i] = ZERO;
			return outBits[i];
		}

		void truncate(int newInSize, int newOutSize) {
			inBits = truncate(inBits, newInSize);
			outBits = truncate(outBits, newOutSize);
		}

		static char[] truncate(char b[], int n) {
			if (b.length == n)
				return b;
			char a[] = new char[n];
			for (int i = 0; i < n && i < b.length; i++) a[i] = b[i];
			for (int i = b.length; i < n; i++) a[i] = ZERO;
			return a;
		}

		public String toString() { return toStandardString(); }

		public String toStandardString() {
			String i = "";
			for (char inBit: inBits)
				i = inBit + i;
			String o = "";
			for (char outBit: outBits)
				o = outBit + o;
			String ret = i + " " + o;
			if (!comment.trim().equals(""))
				ret += " # " + comment.trim();
			return ret;
		}

		boolean matches(int input) {
			int i = input;
			for (char bit : inBits) {
				int b = input & 1;
				if ((bit == ONE && b != 1) || (bit == ZERO && b != 0))
					return false;
				input = (input >> 1);
			}
			return true;
		}

		int getOutput() {
			int out = 0;
			int bit = 1;
			for (char c : outBits) {
				if (c == ONE)
					out |= bit;
				bit = bit << 1;
			}
			return out;
		}
	}

	public int valueFor(int input) {
		for (Row row : rows)
			if (row.matches(input))
				return row.getOutput();
		return 0;
	}

	public String commentFor(int input) {
		for (Row row : rows)
			if (row.matches(input))
				return row.comment;
		return "n/a";
	}

	public static class EditorDialog extends JDialog {
		private final float smallFont = 9.5f;
		private final float tinyFont = 8.8f;
		private HeaderPanel hdrPanel;
		private TablePanel ttPanel;
		private PLATable oldTable, newTable;

		public EditorDialog(Frame parent) {
			super(parent, "", true);
			setLocation(300, 200);
			setResizable(true);
			Container cPane = super.getContentPane();
			cPane.setLayout(new BorderLayout(5, 5));

			hdrPanel = new HeaderPanel();
			JScrollPane header = new JScrollPane(hdrPanel,
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			header.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
				protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) { }
				protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) { }
				protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
				protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
				private JButton createZeroButton() {
					JButton jbutton = new JButton();
					jbutton.setPreferredSize(new Dimension(0, 0));
					jbutton.setMinimumSize(new Dimension(0, 0));
					jbutton.setMaximumSize(new Dimension(0, 0));
					return jbutton;
				}
			});
			
			ttPanel = new TablePanel();
			JScrollPane table = new JScrollPane(ttPanel,
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			header.getHorizontalScrollBar().setModel(table.getHorizontalScrollBar().getModel());

			cPane.add(header, "North");
			cPane.add(table, "Center");
			cPane.add(new ButtonPanel(this), "South");
			cPane.setPreferredSize(new Dimension(440, 300));
			setMinimumSize(new Dimension(300, 200));
		}

		public void showAndResize(PLATable t) {
			oldTable = t;
			newTable = new PLATable(oldTable);
			newTable.resize(t.pendingInSize, t.pendingOutSize);
			reset();
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			setLocation(dim.width/2-getWidth()/2, dim.height/3-getHeight()/2);
			setVisible(true);
		}

		void reset() {
			hdrPanel.reset();
			ttPanel.reset();
			repack();
		}

		void repack() {
			Dimension d = getSize();
			setMinimumSize(d);
			setMaximumSize(d);
			setPreferredSize(d);
			pack();
			setSize(d);
			setMinimumSize(new Dimension(300, 200));
			setMaximumSize(null);
		}

		void close(boolean ok) {
			if (ok)
				oldTable.copyFrom(newTable);
			setVisible(false);
		}

		static String normalizeName(String s) {
			if (s == null)
				return "pla.txt";
			s = s.trim();
			s = s.replace("[^a-zA-Z0-9().-]", " ");
			s = s.replace("\\s+", "_");
			if (s.equals("") || s.equals("_"))
				return "pla.txt";
			return s + ".txt";
		}

		void read() {
			JFileChooser chooser = JFileChoosers.create();
			chooser.setSelectedFile(new File(normalizeName(oldTable.label)));
			chooser.setDialogTitle(Strings.get("plaLoadDialogTitle"));
			chooser.setFileFilter(Loader.TXT_FILTER);
			int choice = chooser.showOpenDialog(null);
			if (choice == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				try {
					PLATable loaded = parse(f);
					newTable.copyFrom(loaded);
					reset();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, e.getMessage(),
							Strings.get("plaLoadErrorTitle"),
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}

		void write() {
			JFileChooser chooser = JFileChoosers.create();
			chooser.setSelectedFile(new File(normalizeName(oldTable.label)));
			chooser.setDialogTitle(Strings.get("plaSaveDialogTitle"));
			chooser.setFileFilter(Loader.TXT_FILTER);
			int choice = chooser.showSaveDialog(null);
			if (choice == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				try {
					newTable.save(f);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, e.getMessage(),
							Strings.get("plaSaveErrorTitle"),
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		
		class ButtonPanel extends JPanel {
			public ButtonPanel(JDialog parent) {
				JButton write = new JButton("Export");
				write.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) { write(); }
				});
				add(write);

				JButton read = new JButton("Import");
				read.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) { read(); }
				});
				add(read);

				JButton ok = new JButton("OK");
				ok.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) { close(true); }
				});
				parent.getRootPane().setDefaultButton(ok);
				add(ok);

				JButton cancel = new JButton("Cancel");
				cancel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) { close(false); }
				});
				add(cancel);

				parent.getRootPane().registerKeyboardAction(new ActionListener() {
					public void actionPerformed(ActionEvent e) { close(false); }
				}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
		}

		class HeaderPanel extends JPanel {
			HeaderPanel() {
				super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			}
			void reset() {
				removeAll();
				add(new TopLabelPanel(newTable.inSize, newTable.outSize));
				add(new TopNumberPanel(newTable.inSize, newTable.outSize));
			}
		}

		class TablePanel extends JPanel {
			TablePanel() {
				super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			}

			void reset() {
				removeAll();
				for (Row r : newTable.rows)
					add(new RowPanel(r));
				add(new InsertRowPanel());
				add(Box.createVerticalGlue());
			}

			void addRow() {
				add(new RowPanel(newTable.addTableRow()), getComponentCount()-2);
				Dimension d = getSize();
				repack();
			}

			void deleteRow(RowPanel rp) {
				newTable.deleteTableRow(rp.row);
				this.remove(rp);
				repack();
			}

			class RowPanel extends JPanel {
				private final Row row;
				RowPanel(Row r) {
					super(new FlowLayout(FlowLayout.CENTER, 0, 0));
					this.row = r;

					JButton rm = new JButton("Remove");
					rm.setFont(rm.getFont().deriveFont(smallFont));
					rm.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							deleteRow(RowPanel.this);
						}
					});
					rm.setMargin(new Insets(0, 0, 0, 0));
					rm.setPreferredSize(new Dimension(75, 17));
					add(rm);

					int inSz = row.inBits.length;
					int outSz = row.outBits.length;

					GridLayout layout = new GridLayout(1, 1 + inSz + 1 + Math.max(outSz, 2) + 1);
					layout.setHgap(buttonHgap);
					JPanel bitPanel = new JPanel(layout);

					bitPanel.add(new Box(BoxLayout.X_AXIS));

					for (int i = inSz-1; i >= 0; i--) {
						final int ii = i;
						bitPanel.add(new BitStateButton(row.inBits[ii]) {
							public char clicked() { return row.changeInBit(ii); }
						});
					}

					bitPanel.add(new Box(BoxLayout.X_AXIS));

					for (int i = outSz; i < 2; i++)
						bitPanel.add(new Box(BoxLayout.X_AXIS));
					for (int i = outSz-1; i >= 0; i--) {
						final int ii = i;
						bitPanel.add(new BitStateButton(row.outBits[ii]) {
							public char clicked() { return row.changeOutBit(ii); }
						});
					}

					bitPanel.add(new Box(BoxLayout.X_AXIS));

					add(bitPanel);

					final JTextField txt = new JTextField(null, row.comment, 10);
					txt.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent e) {
							row.comment = txt.getText();
						}
					});
					txt.setPreferredSize(new Dimension(125, 20));
					add(txt);
					pack();
					setMaximumSize(getPreferredSize());
				}
			}

			class InsertRowPanel extends JPanel {
				public InsertRowPanel() {
					super(new FlowLayout(FlowLayout.CENTER));
					JButton more = new JButton("Add Row");
					more.setFont(more.getFont().deriveFont(smallFont));
					more.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							addRow();
						}
					});
					more.setMargin(new Insets(1, 20, 1, 20));
					add(more);
				}
			}
		}

		class TopNumberPanel extends JPanel {
			TopNumberPanel(int inSz, int outSz) {
				super(new FlowLayout(FlowLayout.CENTER, 0, 0));
				add(Box.createRigidArea(new Dimension(75+bs, 11))); // space for remove button
				Dimension dim = new Dimension(bs, 11);

				for (int i = inSz - 1; i >= 0; i--) {
					JLabel l = new JLabel("" + i, SwingConstants.CENTER);
					l.setFont(l.getFont().deriveFont(tinyFont));
					l.setPreferredSize(dim);
					add(l);
				}

				add(Box.createRigidArea(dim));

				for (int i = outSz; i < 2; i++)
					add(Box.createRigidArea(dim));
				for (int i = outSz - 1; i >= 0; i--) {
					JLabel l = new JLabel("" + i, SwingConstants.CENTER);
					l.setFont(l.getFont().deriveFont(tinyFont));
					l.setPreferredSize(dim);
					add(l);
				}

				add(Box.createRigidArea(dim));

				JLabel c = new JLabel("comments");
				c.setFont(c.getFont().deriveFont(smallFont));
				c.setPreferredSize(new Dimension(125 - 8, 11)); // mystery 8
				add(c);

				pack();
				setMinimumSize(getPreferredSize());
				setMaximumSize(getPreferredSize());
			}
		}

		class TopLabelPanel extends JPanel {
			TopLabelPanel(int inSz, int outSz) {
				super(new FlowLayout(FlowLayout.CENTER, 0, 0));
				add(Box.createRigidArea(new Dimension(75+bs-Math.max(3-inSz, 0)*bs, 15))); // space for remove button

				JLabel i = new JLabel("input", SwingConstants.RIGHT);
				i.setFont(i.getFont().deriveFont(smallFont));
				i.setPreferredSize(new Dimension(Math.max(inSz, 3)*bs, 15));
				add(i);

				JLabel o = new JLabel("output", SwingConstants.RIGHT);
				o.setFont(o.getFont().deriveFont(smallFont));
				o.setPreferredSize(new Dimension(Math.max(outSz+1, 3)*bs, 15));
				add(o);

				add(Box.createRigidArea(new Dimension(bs+125 - 8, 15))); // space for comment, mystery 8

				pack();
				setMinimumSize(getPreferredSize());
				setMaximumSize(getPreferredSize());
			}
		}

		private static final int bs = 20;
		private static final int buttonHgap = 2;
		private static final int edgeThickness = 3;
		private static final Border stdBorder = BorderFactory.createEtchedBorder(); 
		private static final Border clickBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		private static Dimension buttonSize = new Dimension(bs - 2*edgeThickness - buttonHgap, bs - 2*edgeThickness);
		static abstract class BitStateButton extends JPanel {
			private final JLabel text;
			public abstract char clicked();
			BitStateButton(char s) {
				super(new FlowLayout(FlowLayout.CENTER, 0, 0));
				setBorder(stdBorder);
				text = new JLabel("" + s, SwingConstants.CENTER);
				text.setPreferredSize(buttonSize);
				add(text);
				addMouseListener(new MouseAdapter() {
					public void mousePressed(MouseEvent e) { setBorder(clickBorder); }
					public void mouseReleased(MouseEvent e) { setBorder(stdBorder); }
					public void mouseClicked(MouseEvent e) { text.setText("" + clicked()); }
				});
			}
		}
	}
}
