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
package com.cburch.logisim.gui.chrono;
import static com.cburch.logisim.gui.chrono.Strings.S;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

public class PopupMenu extends MouseAdapter {

  static class PopupContents extends JPopupMenu implements ActionListener {
    private static final long serialVersionUID = 1L;
    private ChronoPanel chronoPanel;
    private ChronoData.Signal signal;
    private JRadioButtonMenuItem expandBus;

    public PopupContents(ChronoPanel p, ChronoData.Signal s) {
      chronoPanel = p;
      signal = s;

      // Only for buses, for now
      if (signal.getWidth() > 1) {
        // todo: later
//        JMenu dataFormat;
//        JRadioButtonMenuItem[] formats;
//        // format choice
//        dataFormat = new JMenu(S.get("BusFormat"));
//        formats = new JRadioButtonMenuItem[5];
//        ButtonGroup group = new ButtonGroup();
//        for (int i = 0; i < SignalDataBus.signalFormat.length; ++i) {
//          formats[i] = new JRadioButtonMenuItem(
//              SignalDataBus.signalFormat[i]);
//          formats[i].setActionCommand(SignalDataBus.signalFormat[i]);
//          formats[i].addActionListener(this);
//          group.add(formats[i]);
//          dataFormat.add(formats[i]);
//        }
//
//        // default selection
//        for (int i = 0; i < SignalDataBus.signalFormat.length; ++i)
//          if (SignalDataBus.signalFormat[i].equals(signalDataBus
//                .getFormat()))
//            formats[i].setSelected(true);
//        add(dataFormat);
//
//        // expand
//        expandBus = new JRadioButtonMenuItem(S.get("BusExpand"));
//        expandBus.setSelected(signalDataBus.isExpanded());
//        expandBus.addActionListener(this);
//        add(expandBus);
      }
    }

    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == expandBus)
        signal.toggleExpanded();
      // else
        // chronoPanel.setCodingFormat(signalDataBus, e.getActionCommand());
    }
  }

	private ChronoData.Signal signal;
	private ChronoPanel chronoPanel;

	public PopupMenu(ChronoPanel p, ChronoData.Signal s) {
    chronoPanel = p;
		signal = s;
	}

	public void doPop(MouseEvent e) {
		PopupContents menu = new PopupContents(chronoPanel, signal);
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger())
			doPop(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
			doPop(e);
	}
}
