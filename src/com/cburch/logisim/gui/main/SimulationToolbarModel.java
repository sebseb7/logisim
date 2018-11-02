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

package com.cburch.logisim.gui.main;
import static com.cburch.logisim.gui.main.Strings.S;

import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.UnmodifiableList;

class SimulationToolbarModel extends AbstractToolbarModel
  implements ChangeListener {
  private Project project;
  private LogisimToolbarItem simRunToggle;
  private LogisimToolbarItem simStep;
  private LogisimToolbarItem tickEnable;
  private LogisimToolbarItem tickHalf;
  private LogisimToolbarItem tickFull;
  private List<ToolbarItem> items;

  public SimulationToolbarModel(Project project, MenuListener menu) {
    this.project = project;

    simRunToggle = new LogisimToolbarItem(menu, "simrun.png",
        LogisimMenuBar.SIMULATE_RUN_TOGGLE, S.getter("simulateRunTip"));
    simStep = new LogisimToolbarItem(menu, "simstep.png",
        LogisimMenuBar.SIMULATE_STEP, S.getter("simulateStepTip"));
    tickEnable = new LogisimToolbarItem(menu, "simtplay.png",
        LogisimMenuBar.TICK_ENABLE, S.getter("simulateEnableTicksTip"));
    tickHalf = new LogisimToolbarItem(menu, "tickhalf.png",
        LogisimMenuBar.TICK_HALF, S.getter("simulateTickHalfTip"));
    tickFull = new LogisimToolbarItem(menu, "tickfull.png",
        LogisimMenuBar.TICK_FULL, S.getter("simulateTickFullTip"));

    items = UnmodifiableList.create(new ToolbarItem[] {
      simRunToggle, simStep, tickEnable, tickHalf, tickFull, });

    menu.getMenuBar().addEnableListener(this);
    stateChanged(null);
  }

  @Override
  public List<ToolbarItem> getItems() {
    return items;
  }

  @Override
  public boolean isSelected(ToolbarItem item) {
    return false;
  }

  @Override
  public void itemSelected(ToolbarItem item) {
    if (item instanceof LogisimToolbarItem) {
      ((LogisimToolbarItem) item).doAction();
    }
  }

  public void stateChanged(ChangeEvent e) {
    Simulator sim = project.getSimulator();
    boolean running = sim != null && sim.isRunning();
    boolean ticking = sim != null && sim.isTicking();
    simRunToggle.setIcon(running ? "simstop.png" : "simrun.png");
    simRunToggle.setToolTip(S.getter(running ? "simulateStopTip" : "simulateRunTip"));
    tickEnable.setIcon(ticking ? "simtstop.png" : "simtplay.png");
    tickEnable.setToolTip(S.getter(ticking ? "simulateDisableTicksTip" : "simulateEnableTicksTip"));
    fireToolbarAppearanceChanged();
  }
}
