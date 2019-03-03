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

package com.bfh.logisim.gui;

public class FPGAReport {
	private FPGACommanderGui myCommander;

  public String getProjectName() {
    return myCommander.getProjectName();
  }

	public FPGAReport(FPGACommanderGui parent) {
		myCommander = parent;
	}

	public void AddError(String Message) {
		myCommander.AddErrors(Message);
	}

	public void AddFatalError(String Message) {
		myCommander.AddErrors("***FATAL*** " + Message);
	}

	public void AddInfo(String Message) {
		myCommander.AddInfo(Message);
	}

	public void AddSevereError(String Message, Object ...args) {
		myCommander.AddErrors("**SEVERE** " + String.format(Message, args));
	}

	public void AddSevereWarning(String Message, Object ...args) {
		myCommander.AddWarning("**SEVERE** " + String.format(Message, args));
	}

	public void AddWarning(String Message, Object ...args) {
		myCommander.AddWarning(String.format(Message, String.format(Message, args)));
	}

	public void NewConsole(String title) {
		myCommander.NewConsole(title);
	}

	public void print(String Message, Object ...args) {
		myCommander.AddConsole(String.format(Message, args));
	}
}
