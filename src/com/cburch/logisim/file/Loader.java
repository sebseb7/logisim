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

package com.cburch.logisim.file;
import static com.cburch.logisim.file.Strings.S;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.cburch.hdl.HdlFile;
import com.cburch.logisim.std.Builtin;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.Errors;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.ZipClassLoader;

public class Loader implements LibraryLoader {
  private static class JarFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".jar");
    }

    @Override
    public String getDescription() {
      return S.get("jarFileFilter");
    }
  }

  private static class LogisimFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory()
          || f.getName().endsWith(LogisimFile.LOGISIM_EXTENSION)
          || f.getName().endsWith(LogisimFile.LOGISIM_EXTENSION_ALT);
    }

    @Override
    public String getDescription() {
      return S.get("logisimFileFilter");
    }
  }

  private static class TclFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".tcl");
    }

    @Override
    public String getDescription() {
      return S.get("tclFileFilter");
    }
  }

  private static class TxtFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".txt");
    }

    @Override
    public String getDescription() {
      return S.get("txtFileFilter");
    }
  }

  private static class VhdlFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".vhd") || f.getName().endsWith(".vhdl");
    }

    @Override
    public String getDescription() {
      return S.get("vhdlFileFilter");
    }
  }

  public static final FileFilter LOGISIM_FILTER = new LogisimFileFilter();

  public static final FileFilter JAR_FILTER = new JarFileFilter();
  public static final FileFilter TXT_FILTER = new TxtFileFilter();
  public static final FileFilter TCL_FILTER = new TclFileFilter();
  public static final FileFilter VHDL_FILTER = new VhdlFileFilter();

  private Component parent;
  private Builtin builtin = new Builtin();
  private File mainFile = null; // to be cleared with each new file
  private Stack<File> filesOpening = new Stack<>();
  private Map<File, File> substitutions = new HashMap<File, File>();

  public Loader(Component parent) {
    this.parent = parent;
  }

  public JFileChooser createChooser() {
    return JFileChoosers.createAt(getCurrentDirectory());
  }

  public Builtin getBuiltin() {
    return builtin;
  }

  // used here and in LibraryManager only, also in MemMenu
  public File getCurrentDirectory() {
    File ref = filesOpening.empty() ? mainFile : filesOpening.peek();
    return ref == null ? null : ref.getParentFile();
  }

  File getFileFor(String name, FileFilter filter) throws LoadCanceledByUser {
    // Determine the actual file name.
    File file = new File(name);
    if (!file.isAbsolute()) {
      File currentDirectory = getCurrentDirectory();
      if (currentDirectory != null)
        file = new File(currentDirectory, name);
    }
    while (!file.canRead()) {
      // It doesn't exist. Figure it out from the user.
      // todo: allow cancel in first dialog
      Errors.title("Missing File").show(S.fmt("fileLibraryMissingError", file.getName()));
      JFileChooser chooser = createChooser();
      chooser.setFileFilter(filter);
      chooser.setDialogTitle(S.fmt("fileLibraryMissingTitle", file.getName()));
      int action = chooser.showDialog(parent, S.get("fileLibraryMissingButton"));
      if (action != JFileChooser.APPROVE_OPTION)
        throw new LoadCanceledByUser();
      file = chooser.getSelectedFile();
    }
    return file;
  }

  public File getMainFile() {
    return mainFile;
  }

  private File getSubstitution(File source) {
    File ret = substitutions.get(source);
    return ret == null ? source : ret;
  }

  Library loadJarFile(File request, String className)
      throws LoadFailedException {
    File actual = getSubstitution(request);
    // Up until 2.1.8, this was written to use a URLClassLoader, which
    // worked pretty well, except that the class never releases its file
    // handles. For this reason, with 2.2.0, it's been switched to use
    // a custom-written class ZipClassLoader instead. The ZipClassLoader
    // is based on something downloaded off a forum, and I'm not as sure
    // that it works as well. It certainly does more file accesses.

    // Anyway, here's the line for this new version:
    ZipClassLoader loader = new ZipClassLoader(actual);

    // And here's the code that was present up until 2.1.8, and which I
    // know to work well except for the closing-files bit. If necessary, we
    // can revert by deleting the above declaration and reinstating the
    // below.
    /*
     * URL url; try { url = new URL("file", "localhost",
     * file.getCanonicalPath()); } catch (MalformedURLException e1) { throw
     * new LoadFailedException("Internal error: Malformed URL"); } catch
     * (IOException e1) { throw new
     * LoadFailedException(S.get("jarNotOpenedError")); }
     * URLClassLoader loader = new URLClassLoader(new URL[] { url });
     */

    // load library class from loader
    Class<?> retClass;
    try {
      retClass = loader.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new LoadFailedException(S.fmt("jarClassNotFoundError", className));
    }
    if (!(Library.class.isAssignableFrom(retClass))) {
      throw new LoadFailedException(S.fmt("jarClassNotLibraryError", className));
    }

    // instantiate library
    Library ret;
    try {
      ret = (Library) retClass.newInstance();
    } catch (Exception e) {
      throw new LoadFailedException(S.fmt("jarLibraryNotCreatedError", className));
    }
    return ret;
  }

  public Library loadJarLibrary(File file, String className) {
    File actual = getSubstitution(file);
    return LibraryManager.instance.loadJarLibrary(this, actual, className);
  }

  //
  // Library methods
  //
  public Library loadLibrary(String desc) throws LoadCanceledByUser {
    return LibraryManager.instance.loadLibrary(this, desc);
  }

  //
  // methods for LibraryManager
  //
  LogisimFile loadLogisimFile(File request) throws LoadFailedException {
    File actual = getSubstitution(request);
    for (File fileOpening : filesOpening) {
      if (fileOpening.equals(actual)) {
        throw new LoadFailedException(
            S.fmt("logisimCircularError", LogisimFile.toProjectName(actual)));
      }
    }

    LogisimFile ret = null;
    filesOpening.push(actual);
    try {
      ret = LogisimFile.load(actual, this);
    } catch (IOException e) {
      throw new LoadFailedException(
            S.fmt("logisimLoadError", LogisimFile.toProjectName(actual), e.toString()));
    } finally {
      filesOpening.pop();
    }
    if (ret != null)
      ret.setName(LogisimFile.toProjectName(actual));
    return ret;
  }

  public Library loadLogisimLibrary(File file) {
    File actual = getSubstitution(file);
    LoadedLibrary ret = LibraryManager.instance.loadLogisimLibrary(this, actual);
    if (ret != null) {
      LogisimFile retBase = (LogisimFile) ret.getBase();
      showMessages(retBase);
    }
    return ret;
  }

  public LogisimFile openLogisimFile(File file) throws LoadFailedException {
      LogisimFile ret = loadLogisimFile(file);
      if (ret == null)
        throw new LoadFailedException("File could not be opened"); // fixme i18n
      setMainFile(file);
      showMessages(ret);
      return ret;
  }

  public LogisimFile openLogisimFile(File file, Map<File, File> substitutions)
      throws LoadFailedException {
    this.substitutions = substitutions;
    try {
      return openLogisimFile(file);
    } finally {
      this.substitutions = Collections.emptyMap();
    }
  }

  public LogisimFile openLogisimFile(File srcFile, InputStream reader)
      throws /* LoadFailedException, */ IOException, LoadCanceledByUser {
    LogisimFile ret = LogisimFile.load(srcFile, reader, this);
    showMessages(ret);
    return ret;
  }

  public void reload(LoadedLibrary lib) {
    LibraryManager.instance.reload(this, lib);
  }

  public void setMainFile(File value) {
    if (!value.isAbsolute())
      throw new IllegalArgumentException("must be absolute");
    mainFile = value;
  }

  public void setParent(Component value) {
    parent = value;
  }

  private void showMessages(LogisimFile source) {
    if (source == null)
      return;
    File circFile = filesOpening.empty() ? null : filesOpening.peek();
    for (String m = source.getMessage(); m != null; m = source.getMessage())
      Errors.project(circFile).warn(m);
  }

  public String vhdlImportChooser(Component window) {
    JFileChooser chooser = createChooser();
    chooser.setFileFilter(Loader.VHDL_FILTER);
    chooser.setDialogTitle(S.get("hdlOpenDialog"));
    int returnVal = chooser.showOpenDialog(window);
    if (returnVal != JFileChooser.APPROVE_OPTION)
      return null;
    File selected = chooser.getSelectedFile();
    if (selected == null)
      return null;
    try {
      String vhdl = HdlFile.load(selected);
      return vhdl;
    } catch (IOException e) {
      Errors.title(S.get("hexOpenErrorTitle")).show(e.getMessage(), e);
      return null;
    }
  }

}
