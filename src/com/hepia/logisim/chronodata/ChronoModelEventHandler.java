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
package com.hepia.logisim.chronodata;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Model;
import com.cburch.logisim.gui.log.Selection;
import com.cburch.logisim.gui.log.SelectionItem;
import com.hepia.logisim.chronogui.ChronoPanel;

public class ChronoModelEventHandler implements Model.Listener {

	private ChronoPanel chronoPanel;
  private Model model;

	public ChronoModelEventHandler(ChronoPanel chronoPanel, Model model) {
		this.chronoPanel = chronoPanel;
    this.model = model;

    ChronoData data = chronoPanel.getChronoData();
    setModel(model);
  }

  public void setModel(Model newModel) {
    ChronoData data = chronoPanel.getChronoData();
    if (model != null)
      model.removeModelListener(this);
    data.clear();
    model = newModel;
    if (model == null)
      return;

		Selection sel = model.getSelection();
		int columns = sel.size();
		for (int i = 0; i < columns; i++) {
      SelectionItem id = sel.get(i);
			Value value = id.fetchValue(model.getCircuitState());
      data.addSignal(id, value);
    }

		model.addModelListener(this);
	}

	@Override
	public void entryAdded(Model.Event event, Value[] values) {
    ChronoData data = chronoPanel.getChronoData();
    data.addSignalValues(values);
    // data.updateRealTimeExpandedBus();
    chronoPanel.repaintAll(false);
	}

	@Override
	public void filePropertyChanged(Model.Event event) {
	}

	@Override
	public void selectionChanged(Model.Event event) {
    // todo: update ChronoData and panel with new signals, removed signals
	}
}
