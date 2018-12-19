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

import java.io.File;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.file.LibraryManager;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;

public class AttrTableLibraryModel extends AttributeSetTableModel {
  private Project proj;
  private Library lib;

  public AttrTableLibraryModel(Project proj, Library lib) {
    super(makeAttributes(proj, lib));
    this.proj = proj;
    this.lib = lib;
  }

  public static final AttributeOption JAR_LIB = new AttributeOption(
      "jar", S.getter("libraryAttrJarType"));
  public static final AttributeOption BUILTIN_LIB = new AttributeOption(
      "builtin", S.getter("libraryAttrBuiltinType"));
  public static final AttributeOption LOGISIM_LIB = new AttributeOption(
      "logisim", S.getter("libraryAttrLogisimType"));
  public static final AttributeOption TOP_LIB = new AttributeOption(
      "top", S.getter("libraryAttrTopType"));
  public static final Attribute<AttributeOption> TYPE =
      Attributes.forOption("type", S.getter("libraryAttrTypeTitle"),
          new AttributeOption[] { JAR_LIB, BUILTIN_LIB, LOGISIM_LIB, TOP_LIB });

  public static final Attribute<String> PATH = Attributes.forString("path",
      S.getter("libraryAttrPathTitle"));

  public static final Attribute<String> LIB_NAME = Attributes.forString("libname",
      S.getter("libraryName"));
  public static final Attribute<String> PROJECT_NAME = Attributes.forString("projname",
      S.getter("projectName"));

  static AttributeSet makeAttributes(Project proj, Library lib) {
    AttributeOption libType;
    String path = null;
    LogisimFile file = proj.getLogisimFile();
    if (lib == file) {
      libType = TOP_LIB;
      File f = file.getLoader().getMainFile();
      if (f != null)
        path = f.toString();
      else
        path = "";
    } else {
      String desc = LibraryManager.instance.getDescriptor(file.getLoader(), lib);
      if (desc == null)
        desc = "#unknown";
      if (desc.startsWith("#")) {
        libType = BUILTIN_LIB;
      } else if (desc.startsWith("jar#")) {
        libType = JAR_LIB;
        path = desc.substring(4);
      } else if (desc.startsWith("circ#")) {
        libType = LOGISIM_LIB;
        path = desc.substring(5);
      } else {
        libType = BUILTIN_LIB; // ???
      }
    }
    AttributeSet attrs;
    if (path == null)
      attrs = AttributeSets.fixedSet(
          new Attribute<?>[] { LIB_NAME, TYPE },
          new Object[] { lib.getDisplayName(), libType });
    else if (libType == TOP_LIB)
      attrs = AttributeSets.fixedSet(
          new Attribute<?>[] { PROJECT_NAME, TYPE, PATH },
          new Object[] { lib.getDisplayName(), libType, path });
    else
      attrs = AttributeSets.fixedSet(
          new Attribute<?>[] { LIB_NAME, TYPE, PATH },
          new Object[] { lib.getDisplayName(), libType, path });
    attrs.setReadOnly((libType == TOP_LIB ? PROJECT_NAME : LIB_NAME), true);
    attrs.setReadOnly(TYPE, true);
    if (path != null)
      attrs.setReadOnly(PATH, true);
    return attrs;
  }

  @Override
  public String getTitle() {
    return S.fmt("libraryAttrTitle", lib.getDisplayName());
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value)
      throws AttrTableSetException {
    String msg = S.get("attributeChangeInvalidError");
    throw new AttrTableSetException(msg);
  }
}
