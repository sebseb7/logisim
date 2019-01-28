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

import java.awt.BorderLayout;

import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.gui.generic.ProjectExplorer;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.tools.Library;

class Toolbox extends JPanel {
  private static final long serialVersionUID = 1L;
  private ProjectExplorer explorer;

  Toolbox(Project proj, Frame frame, MenuListener menu) {
    super(new BorderLayout());

    ToolboxToolbarModel toolbarModel = new ToolboxToolbarModel(frame, menu);
    Toolbar toolbar = new Toolbar(toolbarModel);
    add(toolbar, BorderLayout.NORTH);

    explorer = new ProjectExplorer(proj);
    explorer.setListener(new ToolboxManip(proj, explorer));
    add(new JScrollPane(explorer), BorderLayout.CENTER);

    toolbarModel.menuEnableChanged(menu);
  }

  void setHaloedTool(Tool value) {
    explorer.setHaloedTool(value);
  }

  public void updateStructure() {
    explorer.updateStructure();
  }

  public void clearExplorerSelection() {
    explorer.clearExplorerSelection();
  }

  public Library getSelectedLibrary() {
    return explorer.getSelectedLibrary();
  }

  public Tool getSelectedTool() {
    return explorer.getSelectedTool();
  }

  public void revealLibrary(List<Library> libPath, Library lib) {
    explorer.revealLibrary(libPath, lib);
  }
}
