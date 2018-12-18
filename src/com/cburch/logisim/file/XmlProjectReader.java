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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.std.hdl.VhdlContent;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;

class XmlProjectReader extends XmlReader {

  class ReadProjectContext extends ReadContext {

    HashMap<String, Library> libs = new HashMap<>();

    ReadProjectContext(LogisimFile f, String path) { super(f, path); }

    Library findLibrary(String libName) throws XmlReaderException {
      if (libName == null || libName.equals(""))
        return file;
      Library lib = libs.get(libName);
      if (lib != null)
        return lib;
      throw new XmlReaderException(S.fmt("libMissingError", libName));
    }

    private void initMouseMappings(Element elt) {
      MouseMappings map = file.getOptions().getMouseMappings();
      for (Element sub_elt : XmlIterator.forChildElements(elt, "tool")) {
        Tool tool;
        try {
          tool = toTool(sub_elt);
        } catch (XmlReaderException e) {
          addErrors(e, "mapping");
          continue;
        }

        String mods_str = sub_elt.getAttribute("map");
        if (mods_str == null || mods_str.equals("")) {
          addError(S.get("mappingMissingError"), "mouse mapping");
          continue;
        }
        int mods;
        try {
          mods = InputEventUtil.fromString(mods_str);
        } catch (NumberFormatException e) {
          addError(S.fmt("mappingBadError", mods_str), "mouse mapping");
          continue;
        }

        tool = tool.cloneTool();
        try {
          initAttributeSet(sub_elt, tool.getAttributeSet(), tool);
        } catch (XmlReaderException e) {
          addErrors(e, "mapping." + tool.getName());
        }

        map.setToolFor(mods, tool);
      }
    }

    private void initToolbarData(Element elt) {
      ToolbarData toolbar = file.getOptions().getToolbarData();
      for (Element sub_elt : XmlIterator.forChildElements(elt)) {
        if (sub_elt.getTagName().equals("sep")) {
          toolbar.addSeparator();
        } else if (sub_elt.getTagName().equals("tool")) {
          Tool tool;
          try {
            tool = toTool(sub_elt);
          } catch (XmlReaderException e) {
            addErrors(e, "toolbar");
            continue;
          }
          if (tool != null) {
            tool = tool.cloneTool();
            try {
              initAttributeSet(sub_elt, tool.getAttributeSet(), tool);
            } catch (XmlReaderException e) {
              addErrors(e, "toolbar." + tool.getName());
            }
            toolbar.addTool(tool);
          }
        }
      }
    }

    private Library toLibrary(Element elt) throws LoadCanceledByUser {
      if (!elt.hasAttribute("name")) {
        addError(S.get("libNameMissingError"), "loading library");
        return null;
      }
      if (!elt.hasAttribute("desc")) {
        addError(S.get("libDescMissingError"), "loading library");
        return null;
      }
      String name = elt.getAttribute("name");
      String desc = elt.getAttribute("desc");
      Library ret = loader.loadLibrary(desc);
      if (ret == null)
        return null;
      libs.put(name, ret);
      for (Element sub_elt : XmlIterator.forChildElements(elt, "tool")) {
        if (!sub_elt.hasAttribute("name")) {
          addError(S.get("toolNameMissingError"), "loading library tool");
        } else {
          String tool_str = sub_elt.getAttribute("name");
          Tool tool = ret.getTool(tool_str);
          if (tool != null) {
            try {
              initAttributeSet(sub_elt, tool.getAttributeSet(), tool);
            } catch (XmlReaderException e) {
              addErrors(e, "lib." + name + "." + tool_str);
            }
          }
        }
      }
      return ret;
    }

    private void parseProject(Element elt) throws LoadCanceledByUser {
      // determine the version producing this file
      String versionString = elt.getAttribute("source");
      if (versionString.equals("")) {
        sourceVersion = Main.VERSION;
      } else {
        sourceVersion = LogisimVersion.parse(versionString);
      }

      // If we are opening a pre-logisim-evolution file, there might be
      // some components
      // (such as the RAM or the counters), that have changed their shape
      // and other details.
      // We have therefore to warn the user that things might be a little
      // strange in their
      // circuits...
      if (sourceVersion.compareTo(LogisimVersion.get(2, 7, 2)) < 0) {
        String msg = 
            "You are opening a file created with original Logisim code.\n"
            + "You might encounter some problems in the execution, since many components\n"
            + "have evolved since then. Some components and labels will be adjusted.";
        if (Main.headless)
          System.err.println("WARNING:\n" + msg);
        else
          JOptionPane.showMessageDialog(null, msg, "Warning: Legacy Circuit", JOptionPane.WARNING_MESSAGE);
      }

      // first, load the sublibraries
      for (Element o : XmlIterator.forChildElements(elt, "lib")) {
        Library lib = toLibrary(o);
        if (lib != null)
          file.addLibrary(lib);
      }

      // second, create the circuits - empty for now - and the vhdl entities
      List<CircuitData> circuitsData = new ArrayList<>();
      for (Element subElt : XmlIterator.forChildElements(elt)) {
        switch (subElt.getTagName()) {
        case "vhdl":
          VhdlContent contents = parseVhdl(subElt);
          if (contents != null) {
            file.addVhdlContent(contents);
          }
          break;
        case "circuit":
          CircuitData circData = parseCircuit(subElt);
          if (circData != null) {
            file.addCircuit(circData.circuit);
            circuitsData.add(circData);
          }
        default:
          // do nothing
        }
      }

      // third, process the other child elements
      for (Element sub_elt : XmlIterator.forChildElements(elt)) {
        String name = sub_elt.getTagName();

        switch (name) {
        case "circuit":
        case "vhdl":
        case "lib":
          // Nothing to do: Done earlier.
          break;
        case "options":
          try {
            initAttributeSet(sub_elt, file.getOptions().getAttributeSet(), null);
          } catch (XmlReaderException e) {
            addErrors(e, "options");
          }
          break;
        case "mappings":
          initMouseMappings(sub_elt);
          break;
        case "toolbar":
          initToolbarData(sub_elt);
          break;
        case "main":
          String main = sub_elt.getAttribute("name");
          Circuit circ = file.getCircuit(main);
          if (circ != null) {
            file.setMainCircuit(circ);
          }
          break;
        case "message":
          file.addMessage(sub_elt.getAttribute("value"));
          break;
        default:
          throw new IllegalArgumentException(
              "Invalid node in logisim file: " + name);
        }
      }

      // fourth, execute a transaction that initializes all the circuits
      XmlCircuitReader builder = new XmlCircuitReader(this, circuitsData);
      builder.execute();
    }

    Tool toTool(Element elt) throws XmlReaderException {
      Library lib = findLibrary(elt.getAttribute("lib"));
      String name = elt.getAttribute("name");
      if (name == null || name.equals("")) {
        throw new XmlReaderException(S.get("toolNameMissing"));
      }
      Tool tool = lib.getTool(name);
      if (tool == null) {
        throw new XmlReaderException(S.fmt("xmlToolNotFound", name));
      }
      return tool;
    }
  }

  LogisimFile parseProject(InputStream is)
      throws IOException, SAXException, LoadCanceledByUser {
    Document doc = loadXmlFrom(is);
    Element elt = doc.getDocumentElement();
    elt = ensureLogisimCompatibility(elt);

    considerRepairs(doc, elt);
    LogisimFile file = new LogisimFile(loader);
    ReadProjectContext context = new ReadProjectContext(file,
        srcFile == null ? null : srcFile.getAbsolutePath());

    context.parseProject(elt);

    if (file.getCircuitCount() == 0) {
      file.addCircuit(new Circuit("main", file));
    }
    if (context.messages.size() > 0) {
      StringBuilder all = new StringBuilder();
      all.append("Error were encountered loading the project:\n");
      for (String msg : context.messages) {
        all.append(msg);
        all.append("\n");
      }
      loader.showError(all.substring(0, all.length() - 1));
    }
    return file;
  }

  private Loader loader;
  private File srcFile; // used for de-relativizing paths in xml

  XmlProjectReader(Loader loader, File srcFile) {
    this.loader = loader;
    this.srcFile = srcFile;
  }

  // --------------------------------------------------------------------------------
  // Everything below this point is for repairing and validating old and potentially
  // misformed xml data, e.g. from old or broken versions of Logisim.
  // --------------------------------------------------------------------------------

  /**
   * Change label names in an XML tree according to a list of suggested labels
   *
   * @param root
   *            root element of the XML tree
   * @param nodeType
   *            type of nodes to consider
   * @param attrType
   *            type of attributes to consider
   * @param validLabels
   *            label set of correct label names
   */
  public static void applyValidLabels(Element root, String nodeType,
      String attrType, Map<String, String> validLabels)
      throws IllegalArgumentException {
    assert (root != null);
    assert (nodeType != null);
    assert (attrType != null);
    assert (nodeType.length() > 0);
    assert (attrType.length() > 0);
    assert (validLabels != null);

    switch (nodeType) {
    case "circuit":
      replaceCircuitNodes(root, attrType, validLabels);
      break;
    case "comp":
      replaceCompNodes(root, validLabels);
      break;
    default:
      throw new IllegalArgumentException("Invalid node type requested: "
          + nodeType);
    }
  }

  /**
   * Sets to the empty string any label attribute in tool nodes derived from
   * elt.
   *
   * @param root
   *            root node
   */
  private static void cleanupToolsLabel(Element root) {
    assert (root != null);

    // Iterate on tools
    for (Element toolElt : XmlIterator.forChildElements(root, "tool")) {
      // Iterate on attribute nodes
      for (Element attrElt : XmlIterator.forChildElements(toolElt, "a")) {
        // Each attribute node should have a name field
        if (attrElt.hasAttribute("name")) {
          String aName = attrElt.getAttribute("name");
          if (aName.equals("label")) {
            // Found a label node in a tool, clean it up!
            attrElt.setAttribute("val", "");
          }
        }
      }
    }

  }

  public static Element ensureLogisimCompatibility(Element elt) {
    Map<String, String> validLabels;
    validLabels = findValidLabels(elt, "circuit", "name");
    applyValidLabels(elt, "circuit", "name", validLabels);
    validLabels = findValidLabels(elt, "circuit", "label");
    applyValidLabels(elt, "circuit", "label", validLabels);
    validLabels = findValidLabels(elt, "comp", "label");
    applyValidLabels(elt, "comp", "label", validLabels);
    // In old, buggy Logisim versions, labels where incorrectly
    // stored also in toolbar and lib components. If this is the
    // case, clean them up.
    fixInvalidToolbarLib(elt);
    return (elt);
  }

  private static void findLibraryUses(ArrayList<Element> dest, String label,
      Iterable<Element> candidates) {
    for (Element elt : candidates) {
      String lib = elt.getAttribute("lib");
      if (lib.equals(label)) {
        dest.add(elt);
      }
    }
  }

  /**
   * Check an XML tree for VHDL-incompatible labels, then propose a list of
   * valid ones. Here valid means: [a-zA-Z][a-zA-Z0-9_]* This applies, in our
   * context, to circuit's names and labels (and their corresponding
   * component's names, of course), and to comp's labels.
   *
   * @param root
   *            root element of the XML tree
   * @param nodeType
   *            type of nodes to consider
   * @param attrType
   *            type of attributes to consider
   * @return map containing the original attribute values as keys, and the
   *         corresponding valid attribute values as the values
   */
  public static Map<String, String> findValidLabels(Element root,
      String nodeType, String attrType) {
    assert (root != null);
    assert (nodeType != null);
    assert (attrType != null);
    assert (nodeType.length() > 0);
    assert (attrType.length() > 0);

    Map<String, String> validLabels = new HashMap<>();

    List<String> initialLabels = getXMLLabels(root, nodeType, attrType);

    Iterator<String> iterator = initialLabels.iterator();
    while (iterator.hasNext()) {
      String label = iterator.next();
      if (!validLabels.containsKey(label)) {
        // Check if the name is invalid, in which case create
        // a valid version and put it in the map
        if (VhdlContent.labelVHDLInvalid(label)) {
          String initialLabel = label;
          label = generateValidVHDLLabel(label);
          validLabels.put(initialLabel, label);
        }
      }
    }

    return validLabels;
  }

  /**
   * In some old version of Logisim, buggy Logisim versions, labels where
   * incorrectly stored also in toolbar and lib components. If this is the
   * case, clean them up..
   *
   * @param root
   *            root element of the XML tree
   */
  private static void fixInvalidToolbarLib(Element root) {
    assert (root != null);

    // Iterate on toolbars -- though there should be only one!
    for (Element toolbarElt : XmlIterator.forChildElements(root, "toolbar")) {
      cleanupToolsLabel(toolbarElt);
    }

    // Iterate on libs
    for (Element libsElt : XmlIterator.forChildElements(root, "lib")) {
      cleanupToolsLabel(libsElt);
    }
  }

  /**
   * Given a label, generates a valid VHDL label by removing invalid
   * characters, putting a letter at the beginning, and putting a shortened (8
   * characters) UUID at the end if the name has been altered. Whitespaces at
   * the beginning and at the end of the string are trimmed by default (if
   * this is the only change, then no suffix is appended).
   *
   * @param initialLabel
   *            initial (possibly invalid) label
   * @return a valid VHDL label
   */
  public static String generateValidVHDLLabel(String initialLabel) {
    return (generateValidVHDLLabel(initialLabel, UUID.randomUUID()
          .toString().substring(0, 8)));
  }

  /**
   * Given a label, generates a valid VHDL label by removing invalid
   * characters, putting a letter at the beginning, and putting the requested
   * suffix at the end if the name has been altered. Whitespaces at the
   * beginning and at the end of the string are trimmed by default (if this is
   * the only change, then no suffix is appended).
   *
   * @param initialLabel
   *            initial (possibly invalid) label
   * @param suffix
   *            string that has to be appended to a modified label
   * @return a valid VHDL label
   */
  public static String generateValidVHDLLabel(String initialLabel,
      String suffix) {
    assert (initialLabel != null);

    // As a default, trim whitespaces at the beginning and at the end
    // of a label (no risks with that potentially, therefore avoid
    // to append the suffix if that was the only change)
    initialLabel = initialLabel.trim();

    String label = initialLabel;

    if (label.isEmpty()) {
      logger.warn("Empty label is not a valid VHDL label");
      label = "L_";
    }

    // If the string has a ! or ~ symbol, then replace it with "NOT"
    label = label.replaceAll("[\\!~]", "NOT_");

    // Force string to start with a letter
    if (!label.matches("^[A-Za-z].*$"))
      label = "L_" + label;

    // Force the rest to be either letters, or numbers, or underscores
    label = label.replaceAll("[^A-Za-z0-9_]", "_");
    // Suppress multiple successive underscores and an underscore at the end
    label = label.replaceAll("_+", "_");
    if (label.endsWith("_"))
      label = label.substring(0, label.length() - 1);

    if (!label.equals(initialLabel)) {
      // Concatenate a unique ID if the string has been altered
      label = label + "_" + suffix;
      // Replace the "-" characters in the UUID with underscores
      label = label.replaceAll("-", "_");
    }

    return (label);
  }

  /**
   * Traverses an XML tree and gets a list of attribute values for the given
   * attribute and node types.
   *
   * @param root
   *            root element of the XML tree
   * @param nodeType
   *            type of nodes to consider
   * @param attrType
   *            type of attributes to consider
   * @return list of names for the considered node/attribute pairs
   */
  public static List<String> getXMLLabels(Element root, String nodeType,
      String attrType) throws IllegalArgumentException {
    assert (root != null);
    assert (nodeType != null);
    assert (attrType != null);
    assert (nodeType.length() > 0);
    assert (attrType.length() > 0);

    ArrayList<String> attrValuesList = new ArrayList<>();

    switch (nodeType) {
    case "circuit":
      inspectCircuitNodes(root, attrType, attrValuesList);
      break;
    case "comp":
      inspectCompNodes(root, attrValuesList);
      break;
    default:
      throw new IllegalArgumentException("Invalid node type requested: "
          + nodeType);
    }

    return attrValuesList;
  }

  /**
   * Check XML's circuit nodes, and return a list of values corresponding to
   * the desired attribute.
   *
   * @param root
   *            XML's root
   * @param attrType
   *            attribute type (either name or label)
   * @param attrValuesList
   *            empty list that will contain the values found
   */
  private static void inspectCircuitNodes(Element root, String attrType,
      List<String> attrValuesList) throws IllegalArgumentException {
    assert (root != null);
    assert (attrType != null);
    assert (attrValuesList != null);
    assert (attrValuesList.isEmpty());

    // Circuits are top-level in the XML file
    switch (attrType) {
    case "name":
      for (Element circElt : XmlIterator
          .forChildElements(root, "circuit")) {
        // Circuit's name is directly available as an attribute
        String name = circElt.getAttribute("name");
        attrValuesList.add(name);
      }
      break;
    case "label":
      for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
        // label is available through its a child node
        for (Element attrElt : XmlIterator.forChildElements(circElt, "a")) {
          if (attrElt.hasAttribute("name")) {
            String aName = attrElt.getAttribute("name");
            if (aName.equals("label")) {
              String label = attrElt.getAttribute("val");
              if (label.length() > 0) {
                attrValuesList.add(label);
              }
            }
          }
        }
      }
      break;
    default:
      throw new IllegalArgumentException(
          "Invalid attribute type requested: " + attrType
          + " for node type: circuit");
    }
  }

  /**
   * Check XML's comp nodes, and return a list of values corresponding to the
   * desired attribute. The checked comp nodes are NOT those referring to
   * circuits -- we can see if this is the case by checking whether the lib
   * attribute is present or not.
   *
   * @param root
   *            XML's root
   * @param attrValuesList
   *            empty list that will contain the values found
   */
  private static void inspectCompNodes(Element root,
      List<String> attrValuesList) {
    assert (root != null);
    assert (attrValuesList != null);
    assert (attrValuesList.isEmpty());

    for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
      // In circuits, we have to look for components, then take
      // just those components that do have a lib attribute and look at
      // their
      // a child nodes
      for (Element compElt : XmlIterator
          .forChildElements(circElt, "comp")) {
        if (compElt.hasAttribute("lib")) {
          for (Element attrElt : XmlIterator.forChildElements(compElt, "a")) {
            if (attrElt.hasAttribute("name")) {
              String aName = attrElt.getAttribute("name");
              if (aName.equals("label")) {
                String label = attrElt.getAttribute("val");
                if (label.length() > 0) {
                  attrValuesList.add(label);
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Replace invalid labels in circuit nodes.
   *
   * @param root
   *            XML's root
   * @param attrType
   *            attribute type (either name or label)
   * @param validLabels
   *            map containing valid label values
   */
  private static void replaceCircuitNodes(Element root, String attrType,
      Map<String, String> validLabels) throws IllegalArgumentException {
    assert (root != null);
    assert (attrType != null);
    assert (validLabels != null);

    if (validLabels.isEmpty()) {
      // Particular case, all the labels were good!
      return;
    }

    // Circuits are top-level in the XML file
    switch (attrType) {
    case "name":
      // We have not only to replace the circuit names in each circuit,
      // but in the corresponding comps too!
      for (Element circElt : XmlIterator
          .forChildElements(root, "circuit")) {
        // Circuit's name is directly available as an attribute
        String name = circElt.getAttribute("name");
        if (validLabels.containsKey(name)) {
          circElt.setAttribute("name", validLabels.get(name));
          // Also, it is present as value for the "circuit" attribute
          for (Element attrElt : XmlIterator.forChildElements(circElt, "a")) {
            if (attrElt.hasAttribute("name")) {
              String aName = attrElt.getAttribute("name");
              if (aName.equals("circuit")) {
                attrElt.setAttribute("val",
                    validLabels.get(name));
              }
            }
          }
        }
        // Now do the comp part
        for (Element compElt : XmlIterator.forChildElements(circElt, "comp")) {
          // Circuits are components without lib
          if (!compElt.hasAttribute("lib")) {
            if (compElt.hasAttribute("name")) {
              String cName = compElt.getAttribute("name");
              if (validLabels.containsKey(cName)) {
                compElt.setAttribute("name",
                    validLabels.get(cName));
              }
            }
          }
        }
      }
      break;
    case "label":
      for (Element circElt : XmlIterator
          .forChildElements(root, "circuit")) {
        // label is available through its a child node
        for (Element attrElt : XmlIterator.forChildElements(circElt, "a")) {
          if (attrElt.hasAttribute("name")) {
            String aName = attrElt.getAttribute("name");
            if (aName.equals("label")) {
              String label = attrElt.getAttribute("val");
              if (validLabels.containsKey(label)) {
                attrElt.setAttribute("val",
                    validLabels.get(label));
              }
            }
          }
        }
      }
      break;
    default:
      throw new IllegalArgumentException(
          "Invalid attribute type requested: " + attrType
          + " for node type: circuit");
    }
  }

  /**
   * Replace invalid labels in comp nodes.
   *
   * @param root
   *            XML's root
   * @param validLabels
   *            map containing valid label values
   */
  private static void replaceCompNodes(Element root,
      Map<String, String> validLabels) {
    assert (root != null);
    assert (validLabels != null);

    if (validLabels.isEmpty()) {
      // Particular case, all the labels were good!
      return;
    }

    for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
      // In circuits, we have to look for components, then take
      // just those components that do have a lib attribute and look at
      // their
      // a child nodes
      for (Element compElt : XmlIterator.forChildElements(circElt, "comp")) {
        if (compElt.hasAttribute("lib")) {
          for (Element attrElt : XmlIterator.forChildElements(compElt, "a")) {
            if (attrElt.hasAttribute("name")) {
              String aName = attrElt.getAttribute("name");
              if (aName.equals("label")) {
                String label = attrElt.getAttribute("val");
                if (validLabels.containsKey(label)) {
                  attrElt.setAttribute("val",
                      validLabels.get(label));
                }
              }
            }
          }
        }
      }
    }
  }

  private void addToLabelMap(HashMap<String, String> labelMap,
      String srcLabel, String dstLabel, String toolNames) {
    if (srcLabel != null && dstLabel != null) {
      for (String tool : toolNames.split(";")) {
        labelMap.put(srcLabel + ":" + tool, dstLabel);
      }
    }
  }

  private void considerRepairs(Document doc, Element root) {
    LogisimVersion version = LogisimVersion.parse(root
        .getAttribute("source"));
    if (version.compareTo(LogisimVersion.get(2, 3, 0)) < 0) {
      // This file was saved before an Edit tool existed. Most likely
      // we should replace the Select and Wiring tools in the toolbar
      // with the Edit tool instead.
      for (Element toolbar : XmlIterator
          .forChildElements(root, "toolbar")) {
        Element wiring = null;
        Element select = null;
        Element edit = null;
        for (Element elt : XmlIterator
            .forChildElements(toolbar, "tool")) {
          String eltName = elt.getAttribute("name");
          if (eltName != null && !eltName.equals("")) {
            if (eltName.equals("Select Tool"))
              select = elt;
            if (eltName.equals("Wiring Tool"))
              wiring = elt;
            if (eltName.equals("Edit Tool"))
              edit = elt;
          }
        }
        if (select != null && wiring != null && edit == null) {
          select.setAttribute("name", "Edit Tool");
          toolbar.removeChild(wiring);
        }
      }
    }
    if (version.compareTo(LogisimVersion.get(2, 6, 3)) < 0) {
      for (Element circElt : XmlIterator
          .forChildElements(root, "circuit")) {
        for (Element attrElt : XmlIterator.forChildElements(circElt, "a")) {
          String name = attrElt.getAttribute("name");
          if (name != null && name.startsWith("label")) {
            attrElt.setAttribute("name", "c" + name);
          }
        }
      }

      repairForWiringLibrary(doc, root);
      repairForLegacyLibrary(doc, root);
    }
    if (version.compareTo(LogisimVersion.get(2, 7, 2)) < 0) {
      addBuiltinLibrariesIfMissing(doc, root);
      // pre logisim-evolution, we didn't have "Appearance" labels
      // on many components. Add StdAttr.APPEAR_CLASSIC on each subcircuit
      // and instances of FlipFlops, Registers, Counters, RAM, ROM, and
      // Shift Registers.
      String memLibName = null;
      for (Element libElt : XmlIterator.forChildElements(root, "lib")) {
        String desc = libElt.getAttribute("desc");
        String name = libElt.getAttribute("name");
        if (name != null && desc != null && desc.equals("#Memory")) {
          memLibName = name;
          break;
        }
      }
      for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
        setDefaultAttribute(doc, circElt, "appearance", "classic");
        if (memLibName != null) {
          for (Element compElt : XmlIterator.forChildElements(circElt, "comp")) {
            String lib = compElt.getAttribute("lib");
            String name = compElt.getAttribute("name");
            if (lib == null || name == null || !lib.equals(memLibName))
              continue;
            if (name.equals("J-K Flip-Flop") || name.equals("S-R Flip-Flop")
                || name.equals("T Flip-Flop") || name.equals("D Flip-Flop")
                || name.equals("RAM") || name.equals("ROM")
                || name.equals("Register") || name.equals("Shift Register")) {
              setDefaultAttribute(doc, compElt, "appearance", "classic");
            }
            if (name.equals("J-K Flip-Flop") || name.equals("S-R Flip-Flop")
                || name.equals("T Flip-Flop") || name.equals("D Flip-Flop")) {
              setDefaultAttribute(doc, compElt, "enable", "true");
            }
          }
        }
      }
    }
    // if (version.compareTo(LogisimVersion.get(4, 0, 0)) < 0) {
    //   // Pre 4.0.0, the #Base library had some useless tools. These are gone.
    //   removeDeprecatedBaseTools(doc, root);
    // }
  }

  // private void removeDeprecatedTools(Base baseLib, String baseName, Element section) {
  //   ArrayList<Element> toRemove = new ArrayList<>();
  //   for (Element elt : XmlIterator.forChildElements(section, "tool")) {
  //     String toolName = elt.getAttribute("name");
  //     String toolLib = elt.getAttribute("lib");
  //     if ((baseName == null || baseName.equals(toolLib))
  //         && baseLib.isDeprecatedTool(toolName))
  //       toRemove.add(elt);
  //   }
  //   for (Element elt : toRemove)
  //     section.removeChild(elt);
  // }

  // private void removeDeprecatedBaseTools(Document doc, Element root) {
  //   Base baseLib = null;
  //   for (Library lib : loader.getBuiltin().getLibraries()) {
  //     if (lib.getName().equals("Base")) {
  //       baseLib = (Base)lib;
  //       break;
  //     }
  //   }
  //   // // remove #Base lib references to deprecated tools
  //   // Element baseElt = null;
  //   // String baseName = null;
  //   // for (Element libElt : XmlIterator.forChildElements(root, "lib")) {
  //   //   String desc = libElt.getAttribute("desc");
  //   //   String name = libElt.getAttribute("name");
  //   //   if (desc != null && desc.equals("#Base")) {
  //   //     baseElt = libElt;
  //   //     baseName = name;
  //   //     // remove all references to deprecated tools within #Base lib element
  //   //     removeDeprecatedTools(baseLib, null, libElt);
  //   //   }
  //   // }

  //   // // remove mapping references to deprecated tools
  //   // for (Element mapElt : XmlIterator.forChildElements(root, "mappings"))
  //   //   removeDeprecatedTools(baseLib, baseName, mapElt);

  //   // // remove toolbar references to deprecated tools
  //   // for (Element mapElt : XmlIterator.forChildElements(root, "toolbar"))
  //   //   removeDeprecatedTools(baseLib, baseName, mapElt);
  // }

  private void addBuiltinLibrariesIfMissing(Document doc, Element root) {
    HashSet<String> found = new HashSet<>();
    Node end = root.getFirstChild();
    int maxLib = 0;
    for (Element libElt : XmlIterator.forChildElements(root, "lib")) {
      String desc = libElt.getAttribute("desc");
      String name = libElt.getAttribute("name");
      if (desc != null)
        found.add(desc);
      if (name != null) {
        int thisLabel = Integer.parseInt(name);
        if (thisLabel > maxLib)
          maxLib = thisLabel;
      }
      end = libElt.getNextSibling();
    }
    for (Library lib : loader.getBuiltin().getLibraries()) {
      String desc = loader.getDescriptor(lib);
      if (found.contains(desc))
        continue;
      Element libElt = doc.createElement("lib");
      libElt.setAttribute("name", "" + (maxLib + 1));
      libElt.setAttribute("desc", desc);
      for (Tool t : lib.getTools()) {
        AttributeSet attrs = t.getAttributeSet();
        if (attrs != null) {
          Element toAdd = doc.createElement("tool");
          toAdd.setAttribute("name", t.getName());
          addAttributeSetContent(doc, toAdd, attrs, t);
          if (toAdd.getChildNodes().getLength() > 0) {
            libElt.appendChild(toAdd);
          }
        }
      }
      root.insertBefore(libElt, end);
      found.add(desc);
      maxLib++;
    }
  }

  void addAttributeSetContent(Document doc, Element elt, AttributeSet attrs,
      AttributeDefaultProvider source) {
    String outFilepath = null;
    if (attrs == null)
      return;
    LogisimVersion ver = Main.VERSION;
    if (source != null && source.isAllDefaultValues(attrs, ver))
      return;
    for (Attribute<?> attrBase : attrs.getAttributes()) {
      @SuppressWarnings("unchecked")
      Attribute<Object> attr = (Attribute<Object>) attrBase;
      Object val = attrs.getValue(attr);
      if (attrs.isToSave(attr) && val != null) {
        Object dflt = source == null
            ? null : source.getDefaultAttributeValue(attr, ver);
        if (dflt == null || !dflt.equals(val)) {
          Element a = doc.createElement("a");
          a.setAttribute("name", attr.getName());
          String value = attr.toStandardString(val);
          if (attr.getName().equals("filePath")
              && outFilepath != null) {
            Path outFP = Paths.get(outFilepath);
            Path attrValP = Paths.get(value);
            value = (outFP.relativize(attrValP)).toString();
            a.setAttribute("val", value);
          } else {
            if (value.indexOf("\n") >= 0) {
              a.appendChild(doc.createTextNode(value));
            } else {
              a.setAttribute("val", attr.toStandardString(val));
            }
          }
          elt.appendChild(a);
        }
      }
    }
  }

  private void setDefaultAttribute(Document doc, Element elt, String attrib, String val) {
    Node end = elt.getFirstChild();
    for (Element attrElt : XmlIterator.forChildElements(elt, "a")) {
      String name = attrElt.getAttribute("name");
      if (name != null && name.equals(attrib)) {
        return;
      }
      end = attrElt.getNextSibling();
    }
    Element a = doc.createElement("a");
    a.setAttribute("name", attrib);
    a.setAttribute("val", val);
    elt.insertBefore(a, end);
  }

  private void relocateTools(Element src, Element dest,
      HashMap<String, String> labelMap) {
    if (src == null || src == dest)
      return;
    String srcLabel = src.getAttribute("name");
    if (srcLabel == null)
      return;

    ArrayList<Element> toRemove = new ArrayList<>();
    for (Element elt : XmlIterator.forChildElements(src, "tool")) {
      String name = elt.getAttribute("name");
      if (name != null && labelMap.containsKey(srcLabel + ":" + name)) {
        toRemove.add(elt);
      }
    }
    for (Element elt : toRemove) {
      src.removeChild(elt);
      if (dest != null) {
        dest.appendChild(elt);
      }
    }
  }

  private void repairForLegacyLibrary(Document doc, Element root) {
    Element legacyElt = null;
    String legacyLabel = null;
    for (Element libElt : XmlIterator.forChildElements(root, "lib")) {
      String desc = libElt.getAttribute("desc");
      String label = libElt.getAttribute("name");
      if (desc != null && desc.equals("#Legacy")) {
        legacyElt = libElt;
        legacyLabel = label;
      }
    }

    if (legacyElt != null) {
      root.removeChild(legacyElt);

      ArrayList<Element> toRemove = new ArrayList<>();
      findLibraryUses(toRemove, legacyLabel,
          XmlIterator.forDescendantElements(root, "comp"));
      boolean componentsRemoved = !toRemove.isEmpty();
      findLibraryUses(toRemove, legacyLabel,
          XmlIterator.forDescendantElements(root, "tool"));
      for (Element elt : toRemove) {
        elt.getParentNode().removeChild(elt);
      }
      if (componentsRemoved) {
        String error = "Some components have been deleted;"
            + " the Legacy library is no longer supported.";
        Element elt = doc.createElement("message");
        elt.setAttribute("value", error);
        root.appendChild(elt);
      }
    }
  }

  // Before version 2.6.3, #Base contained a lot of stuff, including a mix of
  // component-like things (Pin, splitters, transistors, constants, etc.), and
  // the standard tools (edit, poke, wiring-tool text, menu-tool, etc.). At
  // version 2.6.3, this was split into two libraries, one called #Wiring for
  // all the component-like things, and another called #Base with the standard
  // tools. After 3.1.1-HC (maybe 4.0.0-HC?), #Base is hidden, except in the
  // toolbar and the toolbar options explorer. It does not appear in the project
  // explorer at in the left panel. The file format remains the same.
  private void repairForWiringLibrary(Document doc, Element root) {
    Element oldBaseElt = null;
    String oldBaseLabel = null;
    Element gatesElt = null;
    String gatesLabel = null;
    int maxLabel = -1;
    Element firstLibElt = null;
    Element lastLibElt = null;
    for (Element libElt : XmlIterator.forChildElements(root, "lib")) {
      String desc = libElt.getAttribute("desc");
      String label = libElt.getAttribute("name");
      if (desc == null) {
        // skip these tests
      } else if (desc.equals("#Base")) {
        oldBaseElt = libElt;
        oldBaseLabel = label;
      } else if (desc.equals("#Wiring")) {
        // Wiring library already in file. This shouldn't happen, but if
        // somehow it does, we don't want to add it again.
        return;
      } else if (desc.equals("#Gates")) {
        gatesElt = libElt;
        gatesLabel = label;
      }

      if (firstLibElt == null)
        firstLibElt = libElt;
      lastLibElt = libElt;
      try {
        if (label != null) {
          int thisLabel = Integer.parseInt(label);
          if (thisLabel > maxLabel)
            maxLabel = thisLabel;
        }
      } catch (NumberFormatException e) {
        // ignore, will likely fail later anyway
      }
    }

    Element wiringElt;
    String wiringLabel;
    Element newBaseElt;
    String newBaseLabel;
    if (oldBaseElt != null) {
      wiringLabel = oldBaseLabel;
      wiringElt = oldBaseElt;
      wiringElt.setAttribute("desc", "#Wiring");

      newBaseLabel = "" + (maxLabel + 1);
      newBaseElt = doc.createElement("lib");
      newBaseElt.setAttribute("desc", "#Base");
      newBaseElt.setAttribute("name", newBaseLabel);
      root.insertBefore(newBaseElt, lastLibElt.getNextSibling());
    } else {
      wiringLabel = "" + (maxLabel + 1);
      wiringElt = doc.createElement("lib");
      wiringElt.setAttribute("desc", "#Wiring");
      wiringElt.setAttribute("name", wiringLabel);
      root.insertBefore(wiringElt, lastLibElt.getNextSibling());

      newBaseLabel = null;
      newBaseElt = null;
    }

    HashMap<String, String> labelMap = new HashMap<>();
    addToLabelMap(labelMap, oldBaseLabel, newBaseLabel, "Poke Tool;"
        + "Edit Tool;Select Tool;Wiring Tool;Text Tool;Menu Tool;Text");
    addToLabelMap(labelMap, oldBaseLabel, wiringLabel, "Splitter;Pin;"
        + "Probe;Tunnel;Clock;Pull Resistor;Bit Extender");
    addToLabelMap(labelMap, gatesLabel, wiringLabel, "Constant");
    relocateTools(oldBaseElt, newBaseElt, labelMap);
    relocateTools(oldBaseElt, wiringElt, labelMap);
    relocateTools(gatesElt, wiringElt, labelMap);
    updateFromLabelMap(XmlIterator.forDescendantElements(root, "comp"),
        labelMap);
    updateFromLabelMap(XmlIterator.forDescendantElements(root, "tool"),
        labelMap);
  }

  private void updateFromLabelMap(Iterable<Element> elts,
      HashMap<String, String> labelMap) {
    for (Element elt : elts) {
      String oldLib = elt.getAttribute("lib");
      String name = elt.getAttribute("name");
      if (oldLib != null && name != null) {
        String newLib = labelMap.get(oldLib + ":" + name);
        if (newLib != null) {
          elt.setAttribute("lib", newLib);
        }
      }
    }
  }

  public static final Logger logger = LoggerFactory.getLogger(XmlProjectReader.class);
}
