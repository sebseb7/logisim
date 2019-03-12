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

package com.bfh.logisim.settings;

import java.io.File;
import java.util.Collection;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cburch.logisim.util.Errors;

public class Settings {

  private static String[] loadAltera() {
    String[] alteraProgs = { "quartus_sh", "quartus_pgm", "quartus_map" };
    String osname = System.getProperty("os.name");
    if (osname == null)
      throw new IllegalArgumentException("no os.name");
    else {
      if (osname.toLowerCase().indexOf("windows") != -1)
        for (int i = 0; i < alteraProgs.length; i++)
          alteraProgs[i] += ".exe";
    }
    return alteraProgs;
  }

  private static String[] loadXilinx() {
    String[] xilinxProgs = { "xst", "ngdbuild", "map", "par", "bitgen",
      "impact", "cpldfit", "hprep6" };
    String osname = System.getProperty("os.name");
    if (osname == null)
      throw new IllegalArgumentException("no os.name");
    else {
      if (osname.toLowerCase().indexOf("windows") != -1)
        for (int i = 0; i < xilinxProgs.length; i++)
          xilinxProgs[i] += ".exe";
    }
    return xilinxProgs;
  }

  public static final String VHDL = "VHDL";
  public static final String VERILOG = "Verilog";

  private static final String WorkSpace = "WorkSpace";
  private static final String WorkPath = "WorkPath";
  private static final String WorkPathName = "logisim_fpga_workspace" + File.separator;
  private static final String XilinxToolsPath = "XilinxToolsPath";
  private static final String AlteraToolsPath = "AlteraToolsPath";
  private static final String Altera64Bit = "Altera64Bit";
  private static final String HDLTypeToGenerate = "HDLTypeToGenerate";
  private static final String FPGABoards = "FPGABoards";
  private static final String SelectedBoard = "SelectedBoard";
  private static final String ExternalBoard = "ExternalBoardFile_";

  private String HomePath;
  private String SharedPath;
  private String SettingsElement = "LogisimFPGASettings";
  private String UserSettingsFileName = ".LogisimFPGASettings.xml";
  private String SharedSettingsFileName = "LogisimFPGASettings.xml";
  private String LoadedSettingsFileName = "";
  private Document SettingsDocument;
  private boolean modified = false;
  private BoardList KnownBoards = new BoardList();

  public static final String[] AlteraPrograms = loadAltera();

  public static final String[] XilinxPrograms = loadXilinx();

  public Settings() {
    HomePath = System.getProperty("user.home");
    SharedPath = "";
    try {
      String path = Settings.class.getProtectionDomain().getCodeSource().getLocation().getPath();
      String decodedPath = URLDecoder.decode(path, "UTF-8");
      SharedPath = new File(decodedPath).getParent();
    } catch (UnsupportedEncodingException e) {
    }

    if (!readFrom(HomePath, UserSettingsFileName))
      readFrom(SharedPath, SharedSettingsFileName);

    if (!settingsComplete())
      writeXml();
  }

  private boolean readFrom(String dir, String name) {
    File SettingsFile = new File(join(dir, name));
    if (!SettingsFile.exists())
      return false;
    LoadedSettingsFileName = SettingsFile.getPath();
    try {
      // Create instance of DocumentBuilderFactory
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // Get the DocumentBuilder
      DocumentBuilder parser = factory.newDocumentBuilder();
      // Create blank DOM Document
      SettingsDocument = parser.parse(SettingsFile);
    } catch (Exception e) {
      Errors.title("FPGA Settings").show("Can't open FPGA settings file: "
          + SettingsFile.getPath(), e);
      return false;
    }
    return true;
  }

  private String join(String path, String name) {
    if (path.endsWith(File.separator))
      return path + name;
    else
      return path + File.separator + name;
  }

  private String getAttribute(String nodeName, String attrName, String defValue) {
    Element e = (Element)SettingsDocument.getElementsByTagName(nodeName).item(0);
    Attr a = e.getAttributeNode(attrName);
    if (a != null)
      return a.getNodeValue();
    a = SettingsDocument.createAttribute(attrName);
    a.setNodeValue(defValue);
    e.setAttributeNode(a);
    modified = true;
    return defValue;
  }

  private void setAttribute(String nodeName, String attrName, String value) {
    if (value == null)
      value = "";
    Element e = (Element)SettingsDocument.getElementsByTagName(nodeName).item(0);
    Attr a = e.getAttributeNode(attrName);
    if (a != null && a.getNodeValue().equals(value))
      return;
    if (a == null) {
      a = SettingsDocument.createAttribute(attrName);
      e.setAttributeNode(a);
    }
    a.setNodeValue(value);
    modified = true;
  }

  private String normalizePath(String path) {
    if (path == null || path.isEmpty())
      return null;
    if (path.length() > 1 && path.endsWith(File.separator))
      path = path.substring(0, path.length() - 1);
    return path;
  }

  public String GetAlteraToolPath() {
    String s = getAttribute(WorkSpace, AlteraToolsPath, "");
    return normalizePath(s);
  }

  public String GetXilinxToolPath() {
    String s = getAttribute(WorkSpace, XilinxToolsPath, "");
    return normalizePath(s);
  }

  public boolean SetAlteraToolPath(String path) {
    path = normalizePath(path);
    if (path != null && !allToolsPresent(path, AlteraPrograms))
      return false;
    setAttribute(WorkSpace, AlteraToolsPath, path);
    return true;
  }

  public boolean SetXilinxToolPath(String path) {
    path = normalizePath(path);
    if (path != null && !allToolsPresent(path, XilinxPrograms))
      return false;
    setAttribute(WorkSpace, XilinxToolsPath, path);
    return true;
  }

  public Collection<String> GetBoardNames() {
    return KnownBoards.GetBoardNames();
  }

  public boolean GetAltera64Bit() {
    String s = getAttribute(WorkSpace, Altera64Bit, "true");
    return "true".equalsIgnoreCase(s);
  }

  public void SetAltera64Bit(boolean enable) {
    setAttribute(WorkSpace, Altera64Bit, ""+enable);
  }

  public String GetHDLType() {
    String s = getAttribute(WorkSpace, HDLTypeToGenerate, VHDL);
    if (VHDL.equalsIgnoreCase(s))
      return VHDL;
    if (VERILOG.equalsIgnoreCase(s))
      return VERILOG;
    setAttribute(WorkSpace, HDLTypeToGenerate, VHDL); // correct broken XML value
    return VHDL;
  }

  public void SetHDLType(String lang) {
    if (VHDL.equalsIgnoreCase(lang))
      setAttribute(WorkSpace, HDLTypeToGenerate, VHDL);
    else if (VERILOG.equalsIgnoreCase(lang))
      setAttribute(WorkSpace, HDLTypeToGenerate, VERILOG);
  }

  public String GetSelectedBoard() {
    String defBoard = KnownBoards.GetBoardNames().get(0);
    String s = getAttribute(FPGABoards, SelectedBoard, defBoard);
    if (KnownBoards.BoardInCollection(s))
      return s;
    setAttribute(FPGABoards, SelectedBoard, defBoard); // correct broken XML value
    return defBoard;
  }

  public boolean SetSelectedBoard(String boardName) {
    if (!KnownBoards.BoardInCollection(boardName))
      return false;
    setAttribute(FPGABoards, SelectedBoard, boardName);
    return true;
  }

  public String GetSelectedBoardFileName() {
    String SelectedBoardName = GetSelectedBoard();
    return KnownBoards.GetBoardFilePath(SelectedBoardName);
  }

  public String GetStaticWorkspacePath() {
    String s = getAttribute(WorkSpace, WorkPath, "");
    return normalizePath(s);
  }

  public void SetStaticWorkspacePath(String path) {
    path = normalizePath(path);
    setAttribute(WorkSpace, WorkPath, path);
  }

  public String GetWorkspacePath(File projectFile) {
    String p = GetStaticWorkspacePath();
    if (p != null)
      return p;
    if (projectFile != null) {
      String dir = projectFile.getAbsoluteFile().getParentFile().getAbsolutePath();
      String name = projectFile.getName();
      name = name.replaceAll(".circ.xml$", "").replaceAll(".circ$", "") + "_fpga_workspace";
      return join(dir, name);
    }
    return join(HomePath, WorkPathName);
  }

  private void ensureExactlyOneNode(String nodeName) {
    NodeList nodes = SettingsDocument.getElementsByTagName(nodeName);
    int n = nodes.getLength();
    if (n == 0) {
      Element e = SettingsDocument.createElement(nodeName);
      SettingsDocument.getDocumentElement().appendChild(e);
      modified = true;
    } else if (n > 1) {
      JOptionPane.showMessageDialog(null,
          "FPGA settings file is corrupted, some settings may be lost: " 
          + LoadedSettingsFileName);
      while (n > 1) {
        nodes.item(1).getParentNode().removeChild(nodes.item(1));
        nodes = SettingsDocument.getElementsByTagName(nodeName);
        n = nodes.getLength();
      }
      modified = true;
    }
  }

  private boolean settingsComplete() {
    boolean missingXML = (SettingsDocument == null);
    if (missingXML) { 
      try {
        // Create instance of DocumentBuilderFactory
        DocumentBuilderFactory factory = DocumentBuilderFactory
            .newInstance();
        // Get the DocumentBuilder
        DocumentBuilder parser;
        parser = factory.newDocumentBuilder();
        // Create blank DOM Document
        SettingsDocument = parser.newDocument();
      } catch (ParserConfigurationException e) {
        JOptionPane.showMessageDialog(null,
            "Fatal Error: Cannot create settings XML Document");
        System.exit(-4);
      }
      Element root = SettingsDocument.createElement(SettingsElement);
      SettingsDocument.appendChild(root);
    }

    ensureExactlyOneNode(WorkSpace);
    ensureExactlyOneNode(FPGABoards);

    Element e = (Element)SettingsDocument.getElementsByTagName(FPGABoards).item(0);
    NamedNodeMap attrs = e.getAttributes();
    for (int i = 0; i < attrs.getLength(); i++) {
      String k = attrs.item(i).getNodeName();
      String v = attrs.item(i).getNodeValue();
      if (k.startsWith(ExternalBoard) && new File(v).exists())
        KnownBoards.AddExternalBoard(v);
    }

    GetStaticWorkspacePath();
    GetXilinxToolPath();
    GetAlteraToolPath();
    GetHDLType();
    GetAltera64Bit();

    GetSelectedBoard();

    return !missingXML && !modified;
  }

  public void AddExternalBoard(String filename) {
    Element e = (Element)SettingsDocument.getElementsByTagName(FPGABoards).item(0);
    int id = 1;
    while (e.getAttributeNode(ExternalBoard+id) != null)
      id++;
    setAttribute(FPGABoards, ExternalBoard+id, filename);
    KnownBoards.AddExternalBoard(filename);
  }

  public boolean UpdateSettingsFile() {
    if (!modified)
      return true;
    return writeXml();
  }

  private boolean writeXml() {
    try {
      writeTo(SharedPath, SharedSettingsFileName);
      return true;
    } catch (Exception e) { }
    try {
      writeTo(HomePath, UserSettingsFileName);
      return true;
    } catch (Exception e) {
      Errors.title("FPGA Settings").show("Can't write FPGA settings file: "
          + UserSettingsFileName, e);
      return false;
    }
  }
  
  private void removeWhitespace(Element e) {
    NodeList nodes = e.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      if (n.getNodeName().equalsIgnoreCase("#text"))
          e.removeChild(n);
      else if (n instanceof Element)
        removeWhitespace((Element)n);
    }
  }

  private void writeTo(String dir, String name) throws Exception {
    removeWhitespace(SettingsDocument.getDocumentElement());
    File SettingsFile = new File(join(dir, name));
    TransformerFactory tranFactory = TransformerFactory.newInstance();
    tranFactory.setAttribute("indent-number", 3);
    Transformer aTransformer = tranFactory.newTransformer();
    aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
    Source src = new DOMSource(SettingsDocument);
    Result dest = new StreamResult(SettingsFile);
    aTransformer.transform(src, dest);
    modified = false;
  }

  private boolean allToolsPresent(String path, String[] progNames) {
    for (String prog: progNames) {
      File test = new File(join(path, prog));
      if (!test.exists())
        return false;
    }
    return true;
  }
}
