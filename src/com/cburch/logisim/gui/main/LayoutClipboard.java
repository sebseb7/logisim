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

class LayoutClipboard implements ClipboardOwner, FlavorListener, PropertyChangeWeakSupport.Producer {

  // LayoutClipboard holds a set of Components (wires, pins, subcircuits, etc.).
  // It also needs to copy the Circuits/VhdlEntities underlying any of those
  // subcircuits (in case the target doesn't have any suitably-named circuits
  // and the user wants to import them). And it needs a list of any non-builtin
  // library references (Jar or Logisim) used by any of the components as well.
  
  public static final String contentsProperty = "contents";

  public static class Data {
    public Collection<Component> components;
    public Collection<Circuit> circuits;
    public Collection<VhdlContent> vhdl;
    public Collection<Library> libraries;
    CircuitTransaction circuitTransaction;

    Data(XmlClipReader.ReadClipContext ctx) {
      components = ctx.getComponents();
      circuits = ctx.getCircuits();
      vhdl = ctx.getVhdl();
      libraries = ctx.getLibraries();
      if (!circuits.isEmpty())
        circuitTransaction = ctx.getCircuitTransaction();
    }
  }

  private static Data decode(Project proj, Transferable incoming) {
    try {
      String xml = (String)incoming.getTransferData(XmlData.dnd.dataFlavor);
      LogisimFile srcFile = proj.getLogisimFile();
      Loader loader = srcFile.getLoader();
      XmlClipReader.ReadClipContext ctx =
          XmlClipReader.parseSelection(srcFile, loader, xml);
      if (ctx == null || ctx.getComponents().isEmpty())
        return null;
      return new Data(ctx);
    } catch (Exception e) {
      proj.showError("Error parsing clipboard data", e);
      return null;
    }
  }

  private static XmlData encode(Selection sel) {
    Project proj = sel.getProject();
    LogisimFile srcFile = proj.getLogisimFile();
    Loader loader = srcFile.getLoader();
    String xml = XmlWriter.encodeSelection(srcFile, loader, sel.getComponents());
    return xml == null ? null : new XmlData(xml);
  }

  static class XmlData implements DragDrop.Support {
    String xml;

    XmlData(String xml) { this.xml = xml; }

    public static final String mimeTypeBase = "application/vnd.kwalsh.logisim-evolution-hc";
    public static final String mimeTypeClip = mimeTypeBase + ".components;class=java.lang.String";
    // todo: add image
    public static final DragDrop dnd = new DragDrop(mimeTypeClip);
    public DragDrop getDragDrop() { return dnd; }
    public Object convertTo(String mimetype) { return xml; }
  }

  public static final Clipboard sysclip = Toolkit.getDefaultToolkit().getSystemClipboard();
  public static final LayoutClipboard SINGLETON = new LayoutClipboard();
  private XmlData current; // the owned system clip
  private boolean external; // not owned, but system clip is compatible
  private boolean available; // current != null || external

  private LayoutClipboard() {
    sysclip.addFlavorListener(this);
    flavorsChanged(null);
  }

  public void flavorsChanged(FlavorEvent e) {
    boolean oldAvail = available;
    external = sysclip.isDataFlavorAvailable(XmlData.dnd.dataFlavor);
    available = current != null || external;
    if (oldAvail != available)
      LayoutClipboard.this.firePropertyChange(contentsProperty, oldAvail, available);
  }

  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    current = null;
    flavorsChanged(null);
  }

  public void set(Selection value) {
    current = encode(value);
    if (current != null)
      sysclip.setContents(current, this); 
  }

  public Data get(Project proj) {
    if (current != null)
      return decode(proj, current);
    else if (external) 
      return decode(proj, sysclip.getContents(XmlData.dnd.dataFlavor));
    else
      return null;
  }

  public boolean isEmpty() {
    return current == null && !external;
  }

  PropertyChangeWeakSupport propListeners = new PropertyChangeWeakSupport(LayoutClipboard.class);
  public PropertyChangeWeakSupport getPropertyChangeListeners() { return propListeners; }
}
