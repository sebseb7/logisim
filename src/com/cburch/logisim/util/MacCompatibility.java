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

package com.cburch.logisim.util;

import java.io.File;
import java.io.IOException;

// import javax.swing.JMenuBar;

// import net.roydesign.mac.MRJAdapter;

public class MacCompatibility {
  public static boolean isAboutAutomaticallyPresent() {
    // try {
      return true; // return MRJAdapter.isAboutAutomaticallyPresent();
    // } catch (Exception t) {
    //   return false;
    // }
  }

  public static boolean isPreferencesAutomaticallyPresent() {
    // try {
      return true; // return MRJAdapter.isPreferencesAutomaticallyPresent();
    // } catch (Exception t) {
    //   return false;
    // }
  }

  public static boolean isQuitAutomaticallyPresent() {
    // try {
      return true; // return MRJAdapter.isQuitAutomaticallyPresent();
    // } catch (Exception t) {
    //   return false;
    // }
  }

  public static void setFileCreatorAndType(File dest, String app, String type)
      throws IOException {
    // IOException ioExcept = null;
    // try {
    //   try {
    //     // MRJAdapter.setFileCreatorAndType(dest, app, type);
    //   } catch (IOException e) {
    //     ioExcept = e;
    //   }
    // } catch (Exception t) {
    // }
    // if (ioExcept != null)
    //   throw ioExcept;
  }

  public static boolean quitAfterLastWindowClosed() {
	  return true;
  }

  public static boolean alwaysUseScrollbars() {
	  return true;
  }
  private MacCompatibility() { }

}
