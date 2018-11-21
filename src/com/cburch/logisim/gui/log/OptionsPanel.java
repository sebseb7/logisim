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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

class OptionsPanel extends LogPanel {

  JButton selectionButton;
  JPanel selectionPanel = new JPanel();

  JRadioButton realTime = new JRadioButton();
  JRadioButton stepTime = new JRadioButton();
  JRadioButton clockedSimple = new JRadioButton();
  JRadioButton clockedDetail = new JRadioButton();

  JPanel timingPanel = new JPanel();

  OptionsPanel(LogFrame frame) {
    super(frame);

    selectionButton = frame.makeSelectionButton();
    selectionPanel.add(selectionButton);

    ButtonGroup g = new ButtonGroup();
    g.add(realTime);
    g.add(stepTime);
    g.add(clockedSimple);
    g.add(clockedDetail);

    // todo: listners, further options, text with explanations, tooltips
    timingPanel.setLayout(new BoxLayout(timingPanel, BoxLayout.Y_AXIS));
    timingPanel.add(realTime);
    timingPanel.add(stepTime);
    timingPanel.add(clockedSimple);
    timingPanel.add(clockedDetail);

    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    setLayout(gb);

    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = gc.weighty = 0.0;
    gc.gridx = gc.gridy = 0;
    gb.setConstraints(selectionPanel, gc);
    add(selectionPanel);

    gc.gridy = 1;
    gb.setConstraints(timingPanel, gc);
    add(timingPanel);
   
    localeChanged();
  }

  @Override
  public String getHelpText() {
    return S.get("optionsHelp");
  }

  @Override
  public String getTitle() {
    return S.get("optionsTab");
  }

  @Override
  public void localeChanged() {
    selectionPanel.setBorder(BorderFactory.createTitledBorder(S.get("selectionLabel")));
    timingPanel.setBorder(BorderFactory.createTitledBorder(S.get("timingLabel")));
    realTime.setText(S.get("realTime"));
    stepTime.setText(S.get("stepTime"));
    clockedSimple.setText(S.get("clockedSimple"));
    clockedDetail.setText(S.get("clockedDetail"));
  }

  @Override
  public void modelChanged(Model oldModel, Model newModel) { }

}
