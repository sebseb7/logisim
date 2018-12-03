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
package com.cburch.logisim.gui.log;
import static com.cburch.logisim.gui.log.Strings.S;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.util.JDialogOk;
import com.cburch.logisim.util.StringGetter;

public class ClockSource extends JDialogOk {

  private ComponentSelector selector;
  private JLabel msgLabel = new JLabel();
  private StringGetter msg;
  SignalInfo item;

  public ClockSource(StringGetter msg, Circuit circ, boolean requireDriveable) {
    super("Clock Source Selection", true);
    this.msg = msg;

    selector = new ComponentSelector(circ,
        requireDriveable
        ? ComponentSelector.DRIVEABLE_CLOCKS
        : ComponentSelector.OBSERVEABLE_CLOCKS);

    JScrollPane explorerPane = new JScrollPane(selector,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    explorerPane.setPreferredSize(new Dimension(120, 200));

    msgLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    explorerPane.setBorder(
        BorderFactory.createCompoundBorder(
          BorderFactory.createEmptyBorder(0, 10, 10, 10),
          explorerPane.getBorder()));
    getContentPane().add(msgLabel, BorderLayout.NORTH);
    getContentPane().add(explorerPane, BorderLayout.CENTER);

    localeChanged();

    setMinimumSize(new Dimension(200, 300));
    setPreferredSize(new Dimension(300, 400));
    pack();
  }
  
  public void localeChanged() {
    selector.localeChanged();
    msgLabel.setText("<html>" + msg.toString() + "</html>"); // for line breaking
  }

  public void okClicked() {
    SignalInfo.List list = selector.getSelectedItems();
    if (list.size() != 1)
      return;
    item = list.get(0);
  }

  public static SignalInfo doClockDriverDialog(Circuit circ) {
    ClockSource dialog = new ClockSource(
        S.getter("selectClockDriverMessage"),
        circ, true);
    dialog.show();
    return dialog.item;
  }

  public static SignalInfo doClockMissingObserverDialog(Circuit circ) {
    ClockSource dialog = new ClockSource(
        S.getter("selectClockMissingMessage"),
        circ, false);
    dialog.show();
    return dialog.item;
  }

  public static SignalInfo doClockMultipleObserverDialog(Circuit circ) {
    ClockSource dialog = new ClockSource(
        S.getter("selectClockMultipleMessage"),
        circ, false);
    dialog.show();
    return dialog.item;
  }

  public static SignalInfo doClockObserverDialog(Circuit circ) {
    ClockSource dialog = new ClockSource(
        S.getter("selectClockObserverMessage"),
        circ, false);
    dialog.show();
    return dialog.item;
  }
}
