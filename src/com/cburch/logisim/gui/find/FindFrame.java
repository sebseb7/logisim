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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.cburch.hdl.HdlModel;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.generic.WrapLayout;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.std.hdl.VhdlContent;
import com.cburch.logisim.std.hdl.VhdlEntity;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

public class FindFrame extends LFrame.Dialog implements LocaleListener {
  // maybe use LFrame.SubWindow instead?

  private class TopPanel extends JPanel {
    TitledBorder findBorder = BorderFactory.createTitledBorder("");
    JTextField field = new JTextField();
    TitledBorder whereBorder = BorderFactory.createTitledBorder("");
    JRadioButton inSheet = new JRadioButton();
    JRadioButton inCircuit = new JRadioButton();
    JRadioButton inProject = new JRadioButton();
    JRadioButton inAll = new JRadioButton();
    JButton go = new JButton();

    TopPanel() {
      setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
      setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

      JPanel input = new JPanel(new BorderLayout());
      input.setBorder(findBorder);
      input.add(field, BorderLayout.CENTER);
      add(input);

      JPanel where = new JPanel(new WrapLayout());
      where.setBorder(whereBorder);
      where.add(inSheet);
      where.add(inCircuit);
      where.add(inProject);
      where.add(inAll);
      add(where);

      JPanel cmd = new JPanel(new WrapLayout());
      cmd.add(go);
      add(cmd);

      ButtonGroup g = new ButtonGroup();
      g.add(inSheet);
      g.add(inCircuit);
      g.add(inProject);
      g.add(inAll);
      inSheet.setSelected(true);
    }
  }

  private class ResultPanel extends JList<Result> {
    ResultPanel() {
      setModel(model);
      setCellRenderer(new ResultRenderer());
      setFont(getFont().deriveFont(Font.PLAIN));
      setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }
  }

  private TopPanel top;
  private ResultPanel results;
  private JScrollPane scrollPane;
  private Model model = new Model();

  public FindFrame() {
    super(null);
    top = new TopPanel();
    results = new ResultPanel();

    Container contents = getContentPane();

    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();

    JPanel boxes = new JPanel();
    boxes.setLayout(gb);
    gc.anchor = GridBagConstraints.NORTHWEST;
    gc.weightx = 1;
    gc.weighty = 0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.gridx = gc.gridy = 0;

    boxes.add(top, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.fill = GridBagConstraints.BOTH;
    gc.weighty = 1;
    scrollPane = new JScrollPane();
    scrollPane.setViewportView(results);
    // scrollPane.setPreferredSize(new Dimension(250, 300));
    boxes.add(scrollPane, gc);

    contents.add(boxes, BorderLayout.CENTER);
    // contents.setMinimumSize(new Dimension(250, 300));

    top.go.addActionListener(e -> model.update());
    top.field.addActionListener(e -> model.update());
    top.field.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e) { model.clearIfNoResults(); }
      public void removeUpdate(DocumentEvent e) { model.clearIfNoResults(); }
      public void changedUpdate(DocumentEvent e) { model.clearIfNoResults(); }
    });

    results.addListSelectionListener(e -> reveal(results.getSelectedValue()));

    LocaleManager.addLocaleListener(this);
    localeChanged();
  }

  @Override
  public void localeChanged() {
    setTitle(S.get("findFrameTitle"));
    top.findBorder.setTitle(S.get("findTextLabel"));
    top.whereBorder.setTitle(S.get("whereLabel"));
    top.inSheet.setText(S.get("inSheet"));
    top.inCircuit.setText(S.get("inCircuit"));
    top.inProject.setText(S.get("inProject"));
    top.inAll.setText(S.get("inAll"));
    top.inSheet.setToolTipText(S.get("inSheet"));
    top.inCircuit.setToolTipText(S.get("inCircuit"));
    top.inProject.setToolTipText(S.get("inProject"));
    top.inAll.setToolTipText(S.get("inAll"));
    top.go.setText(S.get("findButtonLabel"));
  }

  // Subcircuit and VHDL entities have an attribute for the underlying factory
  // name (i.e. the circuit or vhdl content). Other components may have a label,
  // like 'Foo', and the display name might be "Register 'Foo'", but nowhere
  // does the word "Register" appear in an attribute. So we use a bogus
  // string for that part.
  private static final String COMPONENT_TYPE = S.get("matchComponentName");

  private class Model extends AbstractListModel {
    ArrayList<Result> data = new ArrayList<>();

    void clearIfNoResults() {
      if (data.size() == 1) {
        data.clear();
        fireIntervalRemoved(this, 0, 1);
      }
    }

    @Override
    public Object getElementAt(int index) {
      return data.get(index);
    }

    @Override
    public int getSize() {
      return data.size();
    }

    void update() {
      int n = data.size();
      if (n > 0) {
        data.clear();
        fireIntervalRemoved(this, 0, n);
      }
      add(null); // null displays as result count
      List<Project> projects = Projects.getOpenProjects();
      if (projects.isEmpty())
        return;
      Project proj = projects.get(0);
      String text = top.field.getText();
      // todo: regexp here?
      if (text.equals(""))
        return;
      LibrarySource src = new LibrarySource(proj, new ArrayList<Library>(), proj.getLogisimFile());
      if (top.inSheet.isSelected() || top.inCircuit.isSelected()) {
        Circuit circ = proj.getCurrentCircuit();
        HdlModel hdl = proj.getCurrentHdl();
        if (circ != null) {
          String source = proj.getLogisimFile().getName() + ", " + circ.getName();
          CircuitSource circSource = src.forCircuit(circ);
          if (top.inCircuit.isSelected()) {
            searchAttributes(text, circ.getStaticAttributes(), source, circSource);
            searchCircuit(text, circ, source, new HashSet<Object>(), circSource);
          } else {
            searchCircuit(text, circ, source, null, circSource); // non recursive
          }
        } else if (hdl != null) {
          String source = proj.getLogisimFile().getName() + ", " + hdl.getName();
          searchHdl(text, hdl, source, src.forHdl(hdl));
        }
      } else if (top.inProject.isSelected()) {
        HashSet<Library> searched = new HashSet<>();
        searchLibrary(text, proj.getLogisimFile(), proj.getLogisimFile().getName(), searched, src);
      } else {
        HashSet<Library> searched = new HashSet<>();
        for (Project p : projects)
          searchLibrary(text, p.getLogisimFile(), p.getLogisimFile().getName(), searched,
              new LibrarySource(p, new ArrayList<Library>(), p.getLogisimFile()));
      }
    }

    void searchLibrary(String text, Library lib, String source, HashSet<Library> searched,
        LibrarySource src) {
      if (searched.contains(lib))
        return;
      searched.add(lib);
      searchText(text, lib.getDisplayName(), source, S.get("matchLibraryName"), src, null);
      for (Tool tool : lib.getTools()) {
        String subsource = source + ", " + tool.getDisplayName();
        searchAttributes(text, tool.getAttributeSet(), subsource, src.forTool(tool));
        if (!(tool instanceof AddTool))
          continue;
        AddTool t = (AddTool)tool;
        if (t.getFactory() instanceof SubcircuitFactory) {
          Circuit circ = ((SubcircuitFactory)t.getFactory()).getSubcircuit();
          searchCircuit(text, circ, subsource, null, src.forCircuit(circ)); // non-recursive
        } else if (t.getFactory() instanceof VhdlEntity) {
          VhdlContent vhdl = ((VhdlEntity)t.getFactory()).getContent();
          searchHdl(text, vhdl, subsource, src.forHdl(vhdl));
        }
      }
      for (Library sublib : lib.getLibraries()) {
        String subsource = source + ", " + lib.getDisplayName();
        searchLibrary(text, sublib, subsource, searched, src.forLibrary(sublib));
      }
    }

    void searchCircuit(String text, Circuit circ, String source, HashSet<Object> searched,
        CircuitSource src) {
      if (searched != null && searched.contains(circ))
        return;
      for (Component comp : circ.getNonWires()) {
        String compName = comp.getDisplayName();
        String subsource = source + "/" + compName;
        Source compSrc = src.forComponent(comp);
        searchAttributes(text, comp.getAttributeSet(), subsource, compSrc);
        if (!(comp.getFactory() instanceof SubcircuitFactory
              || comp.getFactory() instanceof VhdlEntity))
          searchText(text, comp.getFactory().getDisplayName(),
              subsource, COMPONENT_TYPE, compSrc, null);
      }
      if (searched == null)
        return; // non-recursive
      searched.add(circ);
      for (Component comp : circ.getNonWires()) {
        ComponentFactory factory = comp.getFactory();
        if (factory instanceof VhdlEntity) {
          VhdlContent vhdl = ((VhdlEntity)factory).getContent();
          if (searched.contains(vhdl))
            continue;
          searched.add(vhdl);
          String subsource = source + "/" + comp.getDisplayName();
          searchHdl(text, vhdl, subsource, src.findHdl(vhdl));
        } else if (factory instanceof SubcircuitFactory) {
          String subsource = source + "/" + comp.getDisplayName();
          Circuit subcirc = ((SubcircuitFactory)factory).getSubcircuit();
          searchCircuit(text, subcirc, subsource, searched, src.findCircuit(subcirc));
        }
      }
    }

    void searchHdl(String text, HdlModel hdl, String source, HdlSource src) {
      String code = hdl.getContent();
      searchText(text, code, source, null /*use line number*/, src, null);
    }

    void searchAttributes(String text, AttributeSet as, String source, Source src) {
      // if (as.contains(StdAttr.LABEL))
      //   search(text, as.getValue(StdAttr.LABEL), name + " label");
      // if (as.contains(CircuitAttributes.NAME_ATTR))
      //   search(text, as.getValue(CircuitAttributes.NAME_ATTR), name + " circuit name");
      // if (as.contains(CircuitAttributes.CIRCUIT_LABEL_ATTR))
      //   search(text, as.getValue(CircuitAttributes.CIRCUIT_LABEL_ATTR), name + " circuit shared label");
      // if (as.contains(CircuitAttributes.CIRCUIT_VHDL_PATH))
      //   search(text, as.getValue(CircuitAttributes.CIRCUIT_VHDL_PATH), name + " circuit vhdl path");
      if (as == null)
        return;
      for (Attribute<?> a : as.getAttributes()) {
        Object o = as.getValue(a);
        if (o instanceof String && a == Text.ATTR_TEXT)
          searchText(text, (String)o, source, null /* use line number */, src, a);
        else if (o instanceof String)
          searchText(text, (String)o, source, a.getDisplayName(), src, a);
      }
    }

    void searchText(String text, String content, String source, String context, Source src, Attribute a) {
      int n = text.length();
      int s = content.indexOf(text);
      while (s >= 0) {
        String c = context;
        int lineno = -1;
        if (c == null) { // use line number
          lineno = lineNumber(content, s);
          c = lineno >= 0 ? S.fmt("matchTextLine", lineno) : S.get("matchTextContent");
        }
        add(new Result(content, s, s+n, source, c, lineno, src, a));
        s = content.indexOf(text, s+1);
      }
    }

    void add(Result result) {
      data.add(result);
      fireIntervalAdded(this, data.size()-1, data.size());
    }

  }

  static int lineNumber(String t, int s) {
    int i = t.indexOf('\n');
    if (i < 0)
      return -1;
    int n = 1;
    while (i >= 0 && i < s) {
      n++;
      i = t.indexOf('\n', i+1);
    }
    return n;
  }

  static String escapeHtml(String s) {
    StringBuilder out = new StringBuilder(Math.max(16, s.length()));
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
        out.append("&#");
        out.append((int) c);
        out.append(';');
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  private interface Source { 
    public void reveal(int lineno, Attribute a);
  }

  private static class ToolSource implements Source {
    Project proj;
    ArrayList<Library> libPath;
    Tool tool;
    ToolSource(Project proj, ArrayList<Library> libPath, Tool tool) {
      this.proj = proj;
      this.libPath = libPath;
      this.tool = tool;
    }
    public void reveal(int lineno, Attribute a) {
      System.out.printf("ToolSource reveal: %s %s\n", lineno, a);
    }
  }

  private static class HdlSource implements Source {
    Project proj;
    ArrayList<Library> libPath;
    HdlModel hdl;
    HdlSource(Project proj, ArrayList<Library> libPath, HdlModel hdl) {
      this.proj = proj;
      this.libPath = libPath;
      this.hdl = hdl;
    }
    public void reveal(int lineno, Attribute a) {
      System.out.printf("HdlSource reveal: %s %s\n", lineno, a);
    }
  }

  private static class LibrarySource implements Source {
    Project proj;
    ArrayList<Library> libPath;
    ArrayList<Library> subPath;
    Library lib;
    LibrarySource(Project proj, ArrayList<Library> libPath, Library lib) {
      this.proj = proj;
      this.libPath = libPath;
      this.lib = lib;
      subPath = new ArrayList<Library>();
      subPath.addAll(libPath);
      subPath.add(lib);
    }
    CircuitSource forCircuit(Circuit circ) {
      return new CircuitSource(proj, subPath, circ);
    }
    HdlSource forHdl(HdlModel hdl) {
      return new HdlSource(proj, subPath, hdl);
    }
    ToolSource forTool(Tool tool) {
      return new ToolSource(proj, subPath, tool);
    }
    LibrarySource forLibrary(Library sublib) {
      return new LibrarySource(proj, subPath, sublib);
    }
    public void reveal(int lineno, Attribute a) {
      proj.getFrame().toFront();
      proj.getFrame().revealLibrary(libPath, lib);
    }
  }

  private static class CircuitSource implements Source {
    Project proj;
    ArrayList<Library> libPath;
    Circuit circ;
    CircuitSource(Project proj, ArrayList<Library> libPath, Circuit circ) {
      this.proj = proj;
      this.libPath = libPath;
      this.circ = circ;
    }

    CircuitSource findCircuit(Circuit subcirc) {
      ComponentFactory f = subcirc.getSubcircuitFactory();
      Library lib = libPath.get(libPath.size()-1);
      if (lib.contains(f))
        return new CircuitSource(proj, libPath, subcirc);
      ArrayList<Library> copy = new ArrayList<>();
      copy.addAll(libPath);
      findFactory(copy, f);
      return new CircuitSource(proj, copy, subcirc);
    }

    HdlSource findHdl(HdlModel hdl) {
      if (!(hdl instanceof VhdlContent))
        throw new IllegalArgumentException("hdl isn't vhdl? " + hdl);
      ComponentFactory f = ((VhdlContent)hdl).getEntityFactory();
      Library lib = libPath.get(libPath.size()-1);
      if (lib.contains(f))
        return new HdlSource(proj, libPath, hdl);
      ArrayList<Library> copy = new ArrayList<>();
      copy.addAll(libPath);
      findFactory(copy, f);
      return new HdlSource(proj, copy, hdl);
    }

    static boolean findFactory(ArrayList<Library> p, ComponentFactory f) {
      Library lib = p.get(p.size()-1);
      if (lib.contains(f))
        return true;
      for (Library sublib : lib.getLibraries()) {
        p.add(sublib);
        if (findFactory(p, f))
          return true;
        p.remove(sublib);
      }
      return false;
    }

    ComponentSource forComponent(Component comp) {
      return new ComponentSource(this, comp);
    }

    public void reveal(int lineno, Attribute a) {
      System.out.printf("CircuitSource reveal: %s %s\n", lineno, a);
    }

    void reveal(Component comp, Attribute a) {
      proj.getFrame().toFront();
      if (proj.getCurrentCircuit() != circ)
        proj.setCurrentCircuit(circ);
      Canvas canvas = proj.getFrame().getCanvas();
      canvas.setHaloedComponent(circ, comp);
      canvas.zoomScrollTo(comp.getBounds().toRectangle());
    }
  }

  private static class ComponentSource implements Source {
    CircuitSource circSource;
    Component comp;
    ComponentSource(CircuitSource cs, Component c) {
      circSource = cs;
      comp = c;
    }

    public void reveal(int lineno, Attribute a) {
      circSource.reveal(comp, a);
    }
  }

  private static class Result {
    String match;
    int s, e;
    String source, context;
    Attribute a; // null for Hdl content and library display name
    int lineno; // only for multi-line Text.ATTR_TEXT content
    Source src;

    Result() { }
    Result(String match, int s, int e, String source, String context, int lineno, Source src, Attribute a) {
      this.match = match;
      this.s = s;
      this.e = e;
      this.source = source;
      this.context = context;
      this.lineno = lineno;
      this.src = src;
      this.a = a;
    }

    static final String b = "<font color=\"#820a0a\"><b>";
    static final String d = "</b></font>";

    String toHtml() {
      int n = match.length();
      if (s == 0 && e == n)
        return b + escapeHtml(match) + d;
      else if (n < 40 || n < (e-s)+10)
        return escapeHtml(match.substring(0, s))
            + b + escapeHtml(match.substring(s, e)) + d
            + escapeHtml(match.substring(e, n));
      int c = Math.min(n+10, 40);
      if (s < c/2)
        return escapeHtml(match.substring(0, s))
            + b + escapeHtml(match.substring(s, e)) + d
            + escapeHtml(match.substring(e, e+c/2)) + "...";
      else if (e >= n-c/2)
        return "..." + escapeHtml(match.substring(s-c/2, s))
            + b + escapeHtml(match.substring(s, e)) + d
            + escapeHtml(match.substring(e, n));
      else
        return "..." + escapeHtml(match.substring(s-c/2, s))
            + b + escapeHtml(match.substring(s, e)) + d
            + escapeHtml(match.substring(e, e+c/2)) + "...";
    }
  }

  static final Color color = UIManager.getDefaults().getColor("Table.gridColor");
  static final Border matteBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, color);

  private class ResultRenderer extends DefaultListCellRenderer {
    static final String b = "<font color=\"#1a128e\">";
    static final String d = "</font>";

    @Override
    public java.awt.Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      if (value == null) {
        int n = model.data.size() - 1;
        if (n <= 0)
          value = S.get("zeroResultLabel");
        else
          value = S.fmt("numResultLabel", n);
      } else if (value instanceof Result) {
        Result r = (Result)value;
        String src = S.fmt("matchSource", b + escapeHtml(r.source) + d);
        value = String.format("<html><small>%s:</small><br><div style=\"padding-left: 15px;\">%s: %s</div></html>",
            src, escapeHtml(r.context), r.toHtml());
      }
      java.awt.Component c
          = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (c instanceof JComponent)
        ((JComponent)c).setBorder(matteBorder);
      return c;
    }
  }

  // Result revealed = null;
  private void reveal(Result r) {
    // if (revealed == r)
    //   return;
    // revealed = r;
    if (r != null) {
      // Canvas canvas = proj.getFrame().getCanvas();
      // canvas.setHaloedComponent(circ, comp);
      r.src.reveal(r.lineno, r.a);
    }
  }

  private void setLocationRelativeTo(Project proj) {
      // Try to place to right of circuit window, or at least near right of screen,
      // using same height as circuit window.
      Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
      Rectangle r = proj.getFrame().getBounds();
      pack();
      int w = getWidth();
      int h = Math.max(getHeight(), r.height);
      int x = r.x + r.width;
      int y = r.y;
      if (x + w > d.width) { // too small to right of circuit
        if (r.x >= w) // plenty of room to left of circuit
          x = r.x - w;
        else if (r.x > d.width - w) // circuit is near right of screen
          x = 0;
        else // circuit is near left of screen
          x = d.width - w;
      }
      setLocation(x, y);
      setMinimumSize(new Dimension(220, 280));
      setSize(new Dimension(w, h));
  }

  public static void showFindFrame(Project proj) {
    FindFrame finder = FindManager.getFindFrame(proj.getFrame());
    if (!finder.isVisible()) {
      finder.setLocationRelativeTo(proj);
      finder.setVisible(true);
    }
    finder.toFront();
  }

  // Search for matching text
  // - Find on current circuit / current vhdl, vs global search
  // - Replace?
  // Things to search:
  // - vhdl entity contents (entity.getContents...)
  // - circuit contents (circ.nonWires...)
  // - circuit name CircuitAttributes.NAME_ATTR
  // - vhdl name VhdlEntity.NAME_ATTR
  // - toolbar contents (layout toolbar getItems)
  // - standard label attributes on various components (except Pin, Tunnel) StdAttr.LABEL
  // - circuit shared label attributes CircuitAttributes.CIRCUIT_LABEL_ATTR
  // - text components Text.ATTR_TEXT
  // - tunnel names StdAttr.LABEL
  // - pin names StdAttr.LABEL
  // - later: memory data ? as bytes? as strings? as hex?

  // todo: search appearance elements, toolbars, mouse mappings
  
  // Search for matching components and attributes (e.g. find all 8-wide multiplexors)
  // - Find on current circuit vs global search
  // Things to search:
  // - circuits
  // - toolbar
  // - mouse mappings

  public static void main(String[] args) throws Exception {
    FindFrame frame = new FindFrame();
    frame.setPreferredSize(new Dimension(350, 600));
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
  }
}
