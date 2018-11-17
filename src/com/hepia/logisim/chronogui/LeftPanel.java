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
package com.hepia.logisim.chronogui;
import static com.hepia.logisim.chronogui.Strings.S;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.hepia.logisim.chronodata.ChronoData;

// Left panel containing signal names
public class LeftPanel extends JPanel {

	private ChronoPanel chronoPanel;
	private JTable table;
	private Object[][] tableData;

	public LeftPanel(ChronoPanel chronoPanel) {
		this.chronoPanel = chronoPanel;

		setLayout(new BorderLayout());
		setBackground(Color.WHITE);

    ChronoData data = chronoPanel.getChronoData();
    int n = data.getSignalCount();

    // icon, name, current_value
		String[] colNames = { "" , S.get("SignalName"), S.get("SignalValue") };

		tableData = new Object[n][3];
    for (int i = 0; i < n; i++) {
      ChronoData.Signal s = data.getSignal(i);
      tableData[i] = new Object[] { s.getIcon(), s.getName(), "-" };
		}

		table = new JTable(new DefaultTableModel(tableData, colNames)) {
			@Override
			public Class getColumnClass(int column) {
        return (column == 0) ? ImageIcon.class : String.class;
			}
			@Override
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};
    

		table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "tick");
		table.getActionMap().put("tick", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// todo
			}
		});

		table.addKeyListener(chronoPanel);
		table.setRowHeight(ChronoPanel.SIGNAL_HEIGHT);
		table.getColumnModel().getColumn(0).setMaxWidth(10);
    table.getColumnModel().getColumn(2).setPreferredWidth(50);

		table.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				if (row >= 0 && e.getComponent() instanceof JTable) {
					table.clearSelection();
					table.setRowSelectionInterval(row, row);
          ChronoData.Signal s = chronoPanel.getChronoData().getSignal(row);
					chronoPanel.mouseEntered(s);
				}
			}
		});

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int row = table.getSelectedRow();
				if (row >= 0 && SwingUtilities.isRightMouseButton(e)
						&& e.getComponent() instanceof JTable) {
          ChronoData.Signal s = chronoPanel.getChronoData().getSignal(row);
					PopupMenu m = new PopupMenu(chronoPanel, s);
					m.doPop(e);
				}
			}
		});

		JTableHeader header = table.getTableHeader();
		Dimension d = header.getPreferredSize();
		d.height = ChronoPanel.HEADER_HEIGHT;
		header.setPreferredSize(d);

		add(header, BorderLayout.NORTH);
		add(table, BorderLayout.CENTER);
	}

	public void highlight(ChronoData.Signal s) {
		table.setRowSelectionInterval(s.idx, s.idx);
	}

	public void refreshSignalsValues() {
		int t = chronoPanel.getRightPanel().getCurrentPosition();
		setSignalsValues(t);
	}

	public void setSignalsValues(int t) {
    ChronoData data = chronoPanel.getChronoData();
    int n = data.getSignalCount();
    for (int i = 0; i < n; i++) {
      ChronoData.Signal s = data.getSignal(i);
      String v = s.getFormattedValue(t);
      table.setValueAt(v, i, 2);
		}
	}

}
