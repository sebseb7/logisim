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

package com.cburch.draw.actions;
import static com.cburch.draw.Strings.S;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;

public class ModelTranslateAction extends ModelAction {
	private Set<CanvasObject> moved;
	private int dx;
	private int dy;

	public ModelTranslateAction(CanvasModel model,
			Collection<CanvasObject> moved, int dx, int dy) {
		super(model);
		this.moved = new HashSet<CanvasObject>(moved);
		this.dx = dx;
		this.dy = dy;
	}

	@Override
	void doSub(CanvasModel model) {
		model.translateObjects(moved, dx, dy);
	}

	@Override
	public String getName() {
		return S.fmt("actionTranslate", getShapesName(moved));
	}

	@Override
	public Collection<CanvasObject> getObjects() {
		return Collections.unmodifiableSet(moved);
	}

	@Override
	void undoSub(CanvasModel model) {
		model.translateObjects(moved, -dx, -dy);
	}
}
