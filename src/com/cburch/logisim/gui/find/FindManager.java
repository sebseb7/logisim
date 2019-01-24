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

package com.cburch.logisim.gui.find;
import static com.cburch.logisim.gui.find.Strings.S;

import javax.swing.JFrame;

import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenuItemManager;

public class FindManager extends WindowMenuItemManager
  implements LocaleListener {
  public static FindFrame getFindFrame(java.awt.Component parent) {
    if (findWindow == null) {
      findWindow = new FindFrame();
      findWindow.pack();
      findWindow.setLocationRelativeTo(parent);
      if (findManager != null)
        findManager.frameOpened(findWindow);
    }
    return findWindow;
  }

  public static void initialize() {
    findManager = new FindManager();
  }

  private static FindFrame findWindow = null;
  private static FindManager findManager = null;

  private FindManager() {
    super(S.get("findFrameTitle"), true);
    LocaleManager.addLocaleListener(this);
  }

  @Override
  public JFrame getJFrame(boolean create, java.awt.Component parent) {
    if (create)
      return getFindFrame(parent);
    else
      return findWindow;
  }

  public void localeChanged() {
    setText(S.get("findFrameTitle"));
  }
}
