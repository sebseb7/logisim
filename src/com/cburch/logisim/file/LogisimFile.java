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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.UniquelyNamedThread;
import com.cburch.logisim.std.hdl.VhdlContent;
import com.cburch.logisim.std.hdl.VhdlEntity;

public class LogisimFile extends Library implements LibraryEventSource {

  private static class WritingThread extends UniquelyNamedThread {
    OutputStream out;
    LogisimFile file;

    WritingThread(OutputStream out, LogisimFile file) {
      super("WritingThread");
      this.out = out;
      this.file = file;
    }

    @Override
    public void run() {
      try {
        file.write(out, file.loader);
      } catch (IOException e) {
        file.loader.showError(S.fmt("fileDuplicateError", e.toString()));
      }
      try {
        out.close();
      } catch (IOException e) {
        file.loader.showError(S.fmt("fileDuplicateError", e.toString()));
      }
    }
  }

  //
  // creation methods
  //
  public static LogisimFile createNew(Loader loader) {
    LogisimFile ret = new LogisimFile(loader);
    ret.main = new Circuit("main", ret);
    // The name will be changed in LogisimPreferences
    ret.tools.add(new AddTool(null, ret.main.getSubcircuitFactory()));
    return ret;
  }

  private static String getFirstLine(BufferedInputStream in)
      throws IOException {
    byte[] first = new byte[512];
    in.mark(first.length - 1);
    in.read(first);
    in.reset();

    int lineBreak = first.length;
    for (int i = 0; i < lineBreak; i++) {
      if (first[i] == '\n') {
        lineBreak = i;
      }
    }
    return new String(first, 0, lineBreak, "UTF-8");
  }

  public static LogisimFile load(File file, Loader loader) throws IOException {
    InputStream in = new FileInputStream(file);
    Throwable firstExcept = null;
    try {
      return loadSub(in, loader, file);
    } catch (Throwable t) {
      firstExcept = t;
    } finally {
      in.close();
    }

    if (firstExcept != null) {
      // We'll now try to do it using a reader. This is to work around
      // Logisim versions prior to 2.5.1, when files were not saved using
      // UTF-8 as the encoding (though the XML file reported otherwise).
      try {
        in = new ReaderInputStream(new FileReader(file), "UTF8");
        return loadSub(in, loader, file);
      } catch (Exception t) {
        firstExcept.printStackTrace();
        loader.showError(S.fmt("xmlFormatError", firstExcept.toString()));
      } finally {
        try {
          in.close();
        } catch (Exception t) {
        }
      }
    }

    return null;
  }

  public static LogisimFile load(InputStream in, Loader loader)
      throws IOException, LoadCanceledByUser {
    try {
      return loadSub(in, loader, null);
    } catch (SAXException e) {
      e.printStackTrace();
      loader.showError(S.fmt("xmlFormatError", e.toString()));
      throw new IOException("huh?", e);
      // return null;
    }
  }

  private static LogisimFile loadSub(InputStream in, Loader loader, File file)
      throws IOException, SAXException, LoadCanceledByUser {
    // fetch first line and then reset
    BufferedInputStream inBuffered = new BufferedInputStream(in);
    String firstLine = getFirstLine(inBuffered);

    if (firstLine == null) {
      throw new IOException("File is empty");
    } else if (firstLine.equals("Logisim v1.0")) {
      // if this is a 1.0 file, then set up a pipe to translate to
      // 2.0 and then interpret as a 2.0 file
      throw new IOException("Version 1.0 files no longer supported");
    }

    XmlReader xmlReader = new XmlReader(loader, file);
    LogisimFile ret = xmlReader.readLibrary(inBuffered);
    ret.loader = loader;
    return ret;
  }

  private EventSourceWeakSupport<LibraryListener> listeners = new EventSourceWeakSupport<LibraryListener>();
  private Loader loader;
  private LinkedList<String> messages = new LinkedList<String>();
  private Options options = new Options();

  private LinkedList<AddTool> tools = new LinkedList<AddTool>();

  private LinkedList<Library> libraries = new LinkedList<Library>();

  private Circuit main = null;

  private String name;

  private boolean dirty = false;

  LogisimFile(Loader loader) {
    this.loader = loader;

    // Creates the default project name, adding an underscore if needed
    name = S.get("defaultProjectName");
    if (Projects.windowNamed(name)) {
      for (int i = 2; true; i++) {
        if (!Projects.windowNamed(name + "_" + i)) {
          name += "_" + i;
          break;
        }
      }
    }

  }

  public void addCircuit(Circuit circuit) {
    addCircuit(circuit, tools.size());
  }

  public void addCircuit(Circuit circuit, int index) {
    AddTool tool = new AddTool(null, circuit.getSubcircuitFactory());
    tools.add(index, tool);
    if (tools.size() == 1)
      setMainCircuit(circuit);
    fireEvent(LibraryEvent.ADD_TOOL, tool);
  }

  public void addVhdlContent(VhdlContent content) {
    addVhdlContent(content, tools.size());
  }

  public void addVhdlContent(VhdlContent content, int index) {
    AddTool tool = new AddTool(null, new VhdlEntity(content));
    tools.add(index, tool);
    fireEvent(LibraryEvent.ADD_TOOL, tool);
    com.cburch.logisim.tools.FactoryAttributes s =
        (com.cburch.logisim.tools.FactoryAttributes) tool.getAttributeSet();
  }

  public void addLibrary(Library lib) {
    libraries.add(lib);
    fireEvent(LibraryEvent.ADD_LIBRARY, lib);
  }

  //
  // listener methods
  //
  public void addLibraryListener(LibraryListener what) {
    listeners.add(what);
  }

  //
  // modification actions
  //
  public void addMessage(String msg) {
    messages.addLast(msg);
  }

  @SuppressWarnings("resource")
  public LogisimFile cloneLogisimFile(Loader newloader) {
    PipedInputStream reader = new PipedInputStream();
    PipedOutputStream writer = new PipedOutputStream();
    try {
      reader.connect(writer);
    } catch (IOException e) {
      newloader.showError(S.fmt("fileDuplicateError", e.toString()));
      return null;
    }
    new WritingThread(writer, this).start();
    try {
      return LogisimFile.load(reader, newloader);
    } catch (IOException e) {
      newloader.showError(S.fmt("fileDuplicateError", e.toString()));
    } catch (LoadCanceledByUser e) {
      newloader.showError(S.fmt("fileDuplicateError", e.toString()));
    } finally {
      try { reader.close();
      } catch (IOException e1) { }
    }
    return null;
  }

  public boolean contains(Circuit circ) {
    for (AddTool tool : tools) {
      if (tool.getFactory() instanceof SubcircuitFactory) {
        SubcircuitFactory factory = (SubcircuitFactory) tool.getFactory();
        if (factory.getSubcircuit() == circ)
          return true;
      }
    }
    return false;
  }

  public boolean containsFactory(String name) {
    for (AddTool tool : tools) {
      if (tool.getFactory() instanceof VhdlEntity) {
        VhdlEntity factory = (VhdlEntity) tool.getFactory();
        if (factory.getContent().getName().equals(name))
          return true;
      } else if (tool.getFactory() instanceof SubcircuitFactory) {
        SubcircuitFactory factory = (SubcircuitFactory) tool.getFactory();
        if (factory.getSubcircuit().getName().equals(name))
          return true;
      }
    }
    return false;
  }

  public boolean contains(VhdlContent content) {
    for (AddTool tool : tools) {
      if (tool.getFactory() instanceof VhdlEntity) {
        VhdlEntity factory = (VhdlEntity) tool.getFactory();
        if (factory.getContent() == content)
          return true;
      }
    }
    return false;
  }

  private Tool findTool(Library lib, Tool query) {
    for (Tool tool : lib.getTools()) {
      if (tool.equals(query))
        return tool;
    }
    return null;
  }

  public Tool findTool(Tool query) {
    for (Library lib : getLibraries()) {
      Tool ret = findTool(lib, query);
      if (ret != null)
        return ret;
    }
    return null;
  }

  private void fireEvent(int action, Object data) {
    LibraryEvent e = new LibraryEvent(this, action, data);
    for (LibraryListener l : listeners) {
      l.libraryChanged(e);
    }
  }

  public AddTool getAddTool(Circuit circ) {
    for (AddTool tool : tools) {
      if (tool.getFactory() instanceof SubcircuitFactory) {
        SubcircuitFactory factory = (SubcircuitFactory) tool.getFactory();
        if (factory.getSubcircuit() == circ) {
          return tool;
        }
      }
    }
    return null;
  }

  public AddTool getAddTool(VhdlContent content) {
    for (AddTool tool : tools) {
      if (tool.getFactory() instanceof VhdlEntity) {
        VhdlEntity factory = (VhdlEntity) tool.getFactory();
        if (factory.getContent() == content) {
          return tool;
        }
      }
    }
    return null;
  }

  public Circuit getCircuit(String name) {
    if (name == null)
      return null;
    for (AddTool tool : tools) {
      if (tool.getFactory() instanceof SubcircuitFactory) {
        SubcircuitFactory factory = (SubcircuitFactory) tool.getFactory();
        if (name.equals(factory.getName()))
          return factory.getSubcircuit();
      }
    }
    return null;
  }

  public VhdlContent getVhdlContent(String name) {
    if (name == null)
      return null;
    for (AddTool tool : tools) {
      if (tool.getFactory() instanceof VhdlEntity) {
        VhdlEntity factory = (VhdlEntity) tool.getFactory();
        if (name.equals(factory.getName()))
          return factory.getContent();
      }
    }
    return null;
  }

  public int getCircuitCount() {
    return getCircuits().size();
  }

  public List<Circuit> getCircuits() {
    List<Circuit> ret = new ArrayList<Circuit>(tools.size());
    for (AddTool tool : tools) {
      if (tool.getFactory() instanceof SubcircuitFactory) {
        SubcircuitFactory factory = (SubcircuitFactory) tool.getFactory();
        ret.add(factory.getSubcircuit());
      }
    }
    return ret;
  }

  public int indexOfCircuit(Circuit circ) {
    for (int i = 0; i < tools.size(); i++) {
      AddTool tool = tools.get(i);
      if (tool.getFactory() instanceof SubcircuitFactory) {
        SubcircuitFactory factory = (SubcircuitFactory) tool.getFactory();
        if (factory.getSubcircuit() == circ) {
          return i;
        }
      }
    }
    return -1;
  }

  public List<VhdlContent> getVhdlContents() {
    List<VhdlContent> ret = new ArrayList<VhdlContent>(tools.size());
    for (AddTool tool : tools) {
      if (tool.getFactory() instanceof VhdlEntity) {
        VhdlEntity factory = (VhdlEntity) tool.getFactory();
        ret.add(factory.getContent());
      }
    }
    return ret;
  }

  public int indexOfVhdl(VhdlContent vhdl) {
    for (int i = 0; i < tools.size(); i++) {
      AddTool tool = tools.get(i);
      if (tool.getFactory() instanceof VhdlEntity) {
        VhdlEntity factory = (VhdlEntity) tool.getFactory();
        if (factory.getContent() == vhdl) {
          return i;
        }
      }
    }
    return -1;
  }

  @Override
  public List<Library> getLibraries() {
    return libraries;
  }

  public Loader getLoader() {
    return loader;
  }

  public Circuit getMainCircuit() {
    return main;
  }

  public String getMessage() {
    if (messages.size() == 0)
      return null;
    return messages.removeFirst();
  }

  //
  // access methods
  //
  @Override
  public String getName() {
    return name;
  }

  public Options getOptions() {
    return options;
  }

  @Override
  public List<AddTool> getTools() {
    return tools;
  }

  public String getUnloadLibraryMessage(Library lib) {
    HashSet<ComponentFactory> factories = new HashSet<ComponentFactory>();
    for (Tool tool : lib.getTools()) {
      if (tool instanceof AddTool) {
        factories.add(((AddTool) tool).getFactory());
      }
    }

    for (Circuit circuit : getCircuits()) {
      for (Component comp : circuit.getNonWires()) {
        if (factories.contains(comp.getFactory())) {
          return S.fmt("unloadUsedError", circuit.getName());
        }
      }
    }

    ToolbarData tb = options.getToolbarData();
    MouseMappings mm = options.getMouseMappings();
    for (Tool t : lib.getTools()) {
      if (tb.usesToolFromSource(t)) {
        return S.get("unloadToolbarError");
      }
      if (mm.usesToolFromSource(t)) {
        return S.get("unloadMappingError");
      }
    }

    return null;
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  public void moveCircuit(AddTool tool, int index) {
    int oldIndex = tools.indexOf(tool);
    if (oldIndex < 0) {
      tools.add(index, tool);
      fireEvent(LibraryEvent.ADD_TOOL, tool);
    } else {
      AddTool value = tools.remove(oldIndex);
      tools.add(index, value);
      fireEvent(LibraryEvent.MOVE_TOOL, tool);
    }
  }

  public void removeCircuit(Circuit circuit) {
    if (getCircuitCount() <= 1) {
      throw new RuntimeException("Cannot remove last circuit");
    }

    int index = indexOfCircuit(circuit);
    if (index >= 0) {
      Tool circuitTool = tools.remove(index);

      if (main == circuit) {
        AddTool dflt_tool = tools.get(0);
        SubcircuitFactory factory = (SubcircuitFactory) dflt_tool
            .getFactory();
        setMainCircuit(factory.getSubcircuit());
      }
      fireEvent(LibraryEvent.REMOVE_TOOL, circuitTool);
    }
  }

  public void removeVhdl(VhdlContent vhdl) {
    int index = indexOfVhdl(vhdl);
    if (index >= 0) {
      Tool vhdlTool = tools.remove(index);
      fireEvent(LibraryEvent.REMOVE_TOOL, vhdlTool);
    }
  }

  public void removeLibrary(Library lib) {
    libraries.remove(lib);
    fireEvent(LibraryEvent.REMOVE_LIBRARY, lib);
  }

  public void removeLibraryListener(LibraryListener what) {
    listeners.remove(what);
  }

  public void setDirty(boolean value) {
    if (dirty != value) {
      dirty = value;
      fireEvent(LibraryEvent.DIRTY_STATE, value ? Boolean.TRUE
          : Boolean.FALSE);
    }
  }

  public void setMainCircuit(Circuit circuit) {
    if (circuit == null)
      return;
    this.main = circuit;
    fireEvent(LibraryEvent.SET_MAIN, circuit);
  }

  public void setName(String name) {
    this.name = name;
    fireEvent(LibraryEvent.SET_NAME, name);
  }

  //
  // other methods
  //
  void write(OutputStream out, LibraryLoader loader) throws IOException {
    write(out, loader, null);
  }

  void write(OutputStream out, LibraryLoader loader, File dest)
      throws IOException {
    try {
      XmlWriter.write(this, out, loader, dest);
    } catch (TransformerConfigurationException e) {
      loader.showError("internal error configuring transformer");
    } catch (ParserConfigurationException e) {
      loader.showError("internal error configuring parser");
    } catch (TransformerException e) {
      String msg = e.getMessage();
      String err = S.get("xmlConversionError");
      if (msg != null)
        err += ": " + msg;
      loader.showError(err);
    }
  }
}
