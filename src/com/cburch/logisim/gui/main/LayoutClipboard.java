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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;
import java.util.Collection;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.XmlClipReader;
import com.cburch.logisim.file.XmlWriter;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.hdl.VhdlContent;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.DragDrop;
import com.cburch.logisim.util.PropertyChangeWeakSupport;

public class LayoutClipboard<T>
  implements ClipboardOwner, FlavorListener, PropertyChangeWeakSupport.Producer {

  // LayoutClipboard<T> holds an object of type T, which can be:
  //   Circuit, 
  //   VhdlContent,
  //   Collection<Component> (a set of wires, pins, subcircuits, etc.),
  //   or Library
  // It also copies all the
  // Circuits/VhdlEntities underlying any of those (in case the target doesn't
  // have any suitably-named circuits and the user wants to import them). And it
  // copies a list of any non-builtin library references (Jar or Logisim) used
  // by any of the components as well.

  public static final String contentsProperty = "contents";

  public static class Clip<T> {
    public T selection;
    public Collection<Circuit> circuits;
    public Collection<VhdlContent> vhdl;
    public Collection<Library> libraries;
    CircuitTransaction circuitTransaction;

    Clip(XmlClipReader.ReadClipContext ctx, T sel) {
      selection = sel;
      circuits = ctx.getCircuits();
      vhdl = ctx.getVhdl();
      libraries = ctx.getLibraries();
      circuitTransaction = ctx.getCircuitTransaction();
    }
  }

  private interface XmlClipExtractor<T> {
    T extractSelection(XmlClipReader.ReadClipContext ctx);
  }

  private Clip<T> decode(Project proj, Transferable incoming, XmlClipExtractor<T> x) {
    try {
      String xml = (String)incoming.getTransferData(dnd.dataFlavor);
      if (xml == null || xml.length() == 0)
        return null;
      LogisimFile srcFile = proj.getLogisimFile();
      Loader loader = srcFile.getLoader();
      XmlClipReader.ReadClipContext ctx =
          XmlClipReader.parseSelection(srcFile, loader, xml);
      if (ctx == null)
        return null;
      T sel = x.extractSelection(ctx);
      if (sel == null)
        return null;
      return new Clip<T>(ctx, sel);
    } catch (Exception e) {
      proj.showError("Error parsing clipboard data", e);
      return null;
    }
  }

  private XmlData encode(Project proj, T value) {
    LogisimFile srcFile = proj.getLogisimFile();
    Loader loader = srcFile.getLoader();
    String xml = XmlWriter.encodeSelection(srcFile, loader, value);
    return xml == null ? null : new XmlData(xml);
  }

  class XmlData implements DragDrop.Support {
    String xml;
    XmlData(String xml) { this.xml = xml; }
    public DragDrop getDragDrop() { return dnd; }
    public Object convertTo(String mimetype) { return xml; }
  }

  // todo: add image flavor, text flavor, etc. for export
  public static final String mimeTypeBase = "application/vnd.kwalsh.logisim-evolution-hc";
  public static final String mimeTypeComponentsClip =
      mimeTypeBase + ".components;class=java.lang.String";
  public static final String mimeTypeCircuitClip =
      mimeTypeBase + ".circuit;class=java.lang.String";
  public static final String mimeTypeVhdlClip =
      mimeTypeBase + ".vhdl;class=java.lang.String";
  public static final String mimeTypeLibraryClip =
      mimeTypeBase + ".library;class=java.lang.String";

  public static final Clipboard sysclip = Toolkit.getDefaultToolkit().getSystemClipboard();

  public static final LayoutClipboard<Collection<Component>> forComponents =
      new LayoutClipboard<Collection<Component>>(mimeTypeComponentsClip,
          ctx -> ctx.getSelectedComponents().isEmpty() ? null : ctx.getSelectedComponents());
  public static final LayoutClipboard<Circuit> forCircuit =
      new LayoutClipboard<>(mimeTypeCircuitClip,
          ctx -> ctx.getSelectedCircuit());
  public static final LayoutClipboard<VhdlContent> forVhdl =
      new LayoutClipboard<>(mimeTypeVhdlClip,
          ctx -> ctx.getSelectedVhdl());
  public static final LayoutClipboard<Library> forLibrary =
      new LayoutClipboard<>(mimeTypeLibraryClip,
          ctx -> ctx.getSelectedLibrary());

  private final DragDrop dnd; // flavor of this clipboard
  private XmlClipExtractor<T> extractor;
  private XmlData current; // the owned system clip, if any, for this flavor
  private boolean external; // not owned, but system clip is compatible with this flavor
  private boolean available; // current != null || external

  private LayoutClipboard(String mimeType, XmlClipExtractor<T> x) {
    dnd = new DragDrop(mimeType);
    extractor = x;
    sysclip.addFlavorListener(this);
    flavorsChanged(null);
  }

  public void flavorsChanged(FlavorEvent e) {
    boolean oldAvail = available;
    external = sysclip.isDataFlavorAvailable(dnd.dataFlavor);
    available = current != null || external;
    if (oldAvail != available)
      LayoutClipboard.this.firePropertyChange(contentsProperty, oldAvail, available);
  }

  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    current = null;
    flavorsChanged(null);
  }

  public void set(Project proj, T value) {
    current = encode(proj, value);
    if (current != null)
      sysclip.setContents(current, this); 
  }

  public Clip<T> get(Project proj) {
    if (current != null)
      return decode(proj, current, extractor);
    else if (external) 
      return decode(proj, sysclip.getContents(dnd.dataFlavor), extractor);
    else
      return null;
  }

  public boolean isEmpty() {
    return current == null && !external;
  }

  PropertyChangeWeakSupport propListeners = new PropertyChangeWeakSupport(LayoutClipboard.class);
  public PropertyChangeWeakSupport getPropertyChangeListeners() { return propListeners; }
}
