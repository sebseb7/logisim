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

package com.cburch.logisim.gui.appear;
import static com.cburch.logisim.gui.menu.Strings.S;

import java.util.HashMap;
import java.util.Map;

import com.cburch.logisim.gui.generic.PopupMenu;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.LogisimMenuItem;

public class AppearanceEditPopup extends PopupMenu
  implements EditHandler.Listener {

  private AppearanceCanvas canvas;
  private EditHandler handler;
  private Map<Object, Boolean> enabled = new HashMap<>();

  public AppearanceEditPopup(AppearanceCanvas canvas) {
    this.canvas = canvas;
    handler = new AppearanceEditHandler(canvas);
    handler.setListener(this);
    handler.computeEnabled();
    initialize();
  }

  private void initialize() {
    add(LogisimMenuBar.CUT, S.get("editCutItem"), e -> handler.cut());
    add(LogisimMenuBar.COPY, S.get("editCopyItem"), e -> handler.copy());
    needSep = true;
    add(LogisimMenuBar.DELETE, S.get("editClearItem"), e -> handler.delete());
    add(LogisimMenuBar.DUPLICATE, S.get("editDuplicateItem"), e-> handler.duplicate());
    needSep = true;
    add(LogisimMenuBar.RAISE, S.get("editRaiseItem"), e -> handler.raise());
    add(LogisimMenuBar.LOWER, S.get("editLowerItem"), e -> handler.lower());
    add(LogisimMenuBar.RAISE_TOP, S.get("editRaiseTopItem"), e -> handler.raiseTop());
    add(LogisimMenuBar.LOWER_BOTTOM, S.get("editLowerBottomItem"), e -> handler.lowerBottom());
    needSep = true;
    add(LogisimMenuBar.ADD_CONTROL, S.get("editAddControlItem"), e -> handler.addControlPoint());
    add(LogisimMenuBar.REMOVE_CONTROL, S.get("editRemoveControlItem"), e -> handler.removeControlPoint());
  }

  @Override
  public void enableChanged(EditHandler handler, LogisimMenuItem tag, boolean value) {
    enabled.put(tag, value);
  }

  @Override
  protected boolean shouldShow(Object tag) {
    if (tag == LogisimMenuBar.ADD_CONTROL
        || tag == LogisimMenuBar.REMOVE_CONTROL) {
      return canvas.getSelection().getSelectedHandle() != null;
    } else {
      return true;
    }
  }

  @Override
  protected boolean shouldEnable(Object tag) {
    return enabled.getOrDefault(tag, false);
  }
}
