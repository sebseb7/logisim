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

package com.cburch.logisim.analyze.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.MenuListener;

public class AnalyzerMenuListener extends MenuListener {

  protected class FileListener implements ActionListener {
    // todo: print, export_image, maybe others
    public void actionPerformed(ActionEvent event) {
      Object src = event.getSource();
      if (src == LogisimMenuBar.EXPORT_IMAGE) {
        System.out.println("export image");
        //ExportImage.doExport(proj);
      } else if (src == LogisimMenuBar.PRINT) {
        System.out.println("print");
        //Print.doPrint(proj);
      }
    }

    public void register() {
      menubar.addActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
      menubar.addActionListener(LogisimMenuBar.PRINT, this);
    }
  }

  private FileListener fileListener = new FileListener();

  public AnalyzerMenuListener(LogisimMenuBar menubar) {
    super(menubar);
    fileListener.register();
    editListener.register();
  }

}
