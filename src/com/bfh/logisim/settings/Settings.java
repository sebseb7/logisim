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

	private static String WorkSpace = "WorkSpace";
	private static String WorkPath = "WorkPath";
	private static String WorkPathName = "logisim_fpga_workspace" + File.separator;
	private static String XilinxName = "XilinxToolsPath";
	private static String AlteraName = "AlteraToolsPath";
	private static String HdlName = "GenerateHDLOnly";
	private static String Altera64Bit = "Altera64Bit";
	private static String HdlTypeName = "HDLTypeToGenerate";
	public static String Unknown = "Unknown";
	public static String VHDL = "VHDL";
	public static String VERILOG = "Verilog";
	private static String Boards = "FPGABoards";
	private static String SelectedBoard = "SelectedBoard";
	private static String ExternalBoard = "ExternalBoardFile";
	private String HomePath;
	private String SharedPath;
	private String SettingsElement = "LogisimFPGASettings";
	private String UserSettingsFileName = ".LogisimFPGASettings.xml";
	private String SharedSettingsFileName = "LogisimFPGASettings.xml";
        private String LoadedSettingsFileName = "";
	private Document SettingsDocument;
	boolean modified = false;
	private BoardList KnownBoards = new BoardList();

	public static final String[] AlteraPrograms = loadAltera();

	public static final String[] XilinxPrograms = loadXilinx();

	/* big TODO: add language support */
	public Settings() {
		HomePath = System.getProperty("user.home");
                SharedPath = "";
                try {
                    String path = Settings.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                    String decodedPath = URLDecoder.decode(path, "UTF-8");
                    SharedPath = new File(decodedPath).getParent();
                } catch (UnsupportedEncodingException e) {
                }

                if (!readFrom(HomePath, UserSettingsFileName)) {
                    readFrom(SharedPath, SharedSettingsFileName);
                }

		if (!SettingsComplete()) {
			if (!WriteXml()) {
				JOptionPane.showMessageDialog(null, "Fatal Error: Cannot write FPGA settings file");
			}
		}
	}

        private boolean readFrom(String dir, String name) {
		File SettingsFile = new File(Join(dir, name));
		if (!SettingsFile.exists())
                    return false;
                LoadedSettingsFileName = SettingsFile.getPath();
                try {
                        // Create instance of DocumentBuilderFactory
                        DocumentBuilderFactory factory = DocumentBuilderFactory
                                        .newInstance();
                        // Get the DocumentBuilder
                        DocumentBuilder parser = factory.newDocumentBuilder();
                        // Create blank DOM Document
                        SettingsDocument = parser.parse(SettingsFile);
                } catch (Exception e) {
                        JOptionPane.showMessageDialog(null,
                                        "Fatal Error: Cannot read FPGA settings file: "
                                                        + SettingsFile.getPath());
                        return false;
                }
                NodeList SettingsList = SettingsDocument
                                .getElementsByTagName(Boards);
                if (SettingsList.getLength() != 1) {
                        JOptionPane.showMessageDialog(null,
                                        "Fatal Error: Cannot parse FPGA settings file: "
                                                        + SettingsFile.getPath());
                        return false;
                }
                Node ThisWorkspace = SettingsList.item(0);
                NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
                for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
                        if (WorkspaceParameters.item(i).getNodeName().contains(ExternalBoard)) {
                                File TestFile = new File(WorkspaceParameters.item(i).getNodeValue());
                                if (TestFile.exists())
                                   KnownBoards.AddExternalBoard(WorkspaceParameters.item(i).getNodeValue());
                        }
                }
                return true;
        }

	private boolean AlteraToolsFound(String path) {
		for (int i = 0; i < AlteraPrograms.length; i++) {
			File test = new File(Join(path, AlteraPrograms[i]));
			if (!test.exists())
				return false;
		}
		return true;
	}

	private String Join(String path, String name) {
		if (path.endsWith(File.separator))
			return path + name;
		else
			return path + File.separator + name;
	}

	public String GetAlteraToolPath() {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return Unknown;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(AlteraName)) {
				if (AlteraToolsFound(WorkspaceParameters.item(i).getNodeValue()))
					return WorkspaceParameters.item(i).getNodeValue();
				else {
					WorkspaceParameters.item(i).setNodeValue("Unknown");
					modified = true;
					return Unknown;
				}
			}
		}
		/* The attribute does not exists so add it */
		Attr altera = SettingsDocument.createAttribute(AlteraName);
		altera.setNodeValue(Unknown);
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(altera);
		modified = true;
		return Unknown;
	}

	public Collection<String> GetBoardNames() {
		return KnownBoards.GetBoardNames();
	}

	public boolean GetHDLOnly() {
		return GetBoolean(HdlName, true);
	}

	public boolean GetAltera64Bit() {
		return GetBoolean(Altera64Bit, true);
	}

	private boolean GetBoolean(String name, boolean defVal) {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return defVal;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(name))
				return WorkspaceParameters.item(i).getNodeValue()
						.equals(Boolean.TRUE.toString());
		}
		/* The attribute does not exists so add it */
		Attr hdl = SettingsDocument.createAttribute(name);
		hdl.setNodeValue((defVal ? Boolean.TRUE : Boolean.FALSE).toString());
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(hdl);
		modified = true;
		return defVal;
	}

	public String GetHDLType() {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return VHDL;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(HdlTypeName)) {
				if (!WorkspaceParameters.item(i).getNodeValue().equals(VHDL)
						&& !WorkspaceParameters.item(i).getNodeValue()
								.equals(VERILOG)) {
					WorkspaceParameters.item(i).setNodeValue(VHDL);
					modified = true;
				}
				return WorkspaceParameters.item(i).getNodeValue();
			}
		}
		/* The attribute does not exists so add it */
		Attr hdl = SettingsDocument.createAttribute(HdlTypeName);
		hdl.setNodeValue(VHDL);
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(hdl);
		modified = true;
		return VHDL;
	}

	public String GetSelectedBoard() {
		NodeList SettingsList = SettingsDocument.getElementsByTagName(Boards);
		if (SettingsList.getLength() != 1) {
			return null;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(SelectedBoard)) {
				if (!KnownBoards.BoardInCollection(WorkspaceParameters.item(i)
						.getNodeValue())) {
					WorkspaceParameters.item(i)
							.setNodeValue(
									KnownBoards.GetBoardNames().toArray()[0]
											.toString());
					modified = true;
				}
				return WorkspaceParameters.item(i).getNodeValue();
			}
		}
		/* The attribute does not exists so add it */
		Attr selboard = SettingsDocument.createAttribute(SelectedBoard);
		selboard.setNodeValue(KnownBoards.GetBoardNames().toArray()[0]
				.toString());
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(selboard);
		modified = true;
		return KnownBoards.GetBoardNames().toArray()[0].toString();
	}

	public String GetSelectedBoardFileName() {
		String SelectedBoardName = GetSelectedBoard();
		return KnownBoards.GetBoardFilePath(SelectedBoardName);
	}

	public String GetStaticWorkspacePath() {
		NodeList SettingsList = SettingsDocument.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() == 1) {
			Node ThisWorkspace = SettingsList.item(0);
			NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
			for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
				if (WorkspaceParameters.item(i).getNodeName().equals(WorkPath)) {
					String p = WorkspaceParameters.item(i).getNodeValue();
					if (p !=  null && p.length() > 0) {
						return p;
					}
				}
			}
		}
		return null;
	}

	public String GetWorkspacePath(File projectFile) {
		String p = GetStaticWorkspacePath();
		if (p != null)
			return p;
		if (projectFile != null) {
			String dir = projectFile.getAbsoluteFile().getParentFile().getAbsolutePath();
			String name = projectFile.getName();
			name = name.replaceAll(".circ.xml$", "").replaceAll(".circ$", "") + "_fpga_workspace";
			return Join(dir, name);
		}
		return Join(HomePath, WorkPathName);
	}

	public String GetXilinxToolPath() {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return Unknown;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(XilinxName)) {
				if (XilinxToolsFound(WorkspaceParameters.item(i).getNodeValue()))
					return WorkspaceParameters.item(i).getNodeValue();
				else {
					WorkspaceParameters.item(i).setNodeValue("Unknown");
					modified = true;
					return Unknown;
				}
			}
		}
		/* The attribute does not exists so add it */
		Attr xilinx = SettingsDocument.createAttribute(XilinxName);
		xilinx.setNodeValue(Unknown);
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(xilinx);
		modified = true;
		return Unknown;
	}

	public boolean SetAlteraToolPath(String path) {
		if (path.length() > 1 && path.endsWith(File.separator))
			path = path.substring(0, path.length() - 1);
		if (!AlteraToolsFound(path))
			return false;
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(AlteraName)) {
				WorkspaceParameters.item(i).setNodeValue(path);
				modified = true;
				return true;
			}
		}
		return false;
	}

	public boolean SetHdlOnly(boolean only) {
		return SetBoolean(HdlName, only);
	}

	public boolean SetAltera64Bit(boolean enable) {
		return SetBoolean(Altera64Bit, enable);
	}

	private boolean SetBoolean(String name, boolean enable) {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(name)) {
				WorkspaceParameters.item(i)
						.setNodeValue(Boolean.toString(enable));
				modified = true;
				return true;
			}
		}
		return false;
	}

	public boolean SetHDLType(String hdl) {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		if (!hdl.equals(VHDL) && !hdl.equals(VERILOG))
			return false;
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(HdlTypeName)) {
				WorkspaceParameters.item(i).setNodeValue(hdl);
				modified = true;
				return true;
			}
		}
		return false;
	}

	public boolean SetSelectedBoard(String BoardName) {
		NodeList SettingsList = SettingsDocument.getElementsByTagName(Boards);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		if (!KnownBoards.BoardInCollection(BoardName))
			return false;
		if (GetSelectedBoard().equals(BoardName))
			return true;
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(SelectedBoard)) {
				WorkspaceParameters.item(i).setNodeValue(BoardName);
				modified = true;
				return true;
			}
		}
		return false;
	}

	private boolean SettingsComplete() {
		boolean result = true;
		if (SettingsDocument == null) {
			result = false;
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
						"Fatal Error: Cannot create settings Document!");
				System.exit(-4);
			}
			Element root = SettingsDocument.createElement(SettingsElement);
			SettingsDocument.appendChild(root);
		}

		NodeList RootList = SettingsDocument.getChildNodes();

		if (RootList.getLength() != 1) {
			JOptionPane.showMessageDialog(null,
					"Fatal Error: Settings file corrupted; please delete the file:"
							+ LoadedSettingsFileName);
			System.exit(-5);
		}

		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() > 1) {
			JOptionPane.showMessageDialog(null,
					"Fatal Error: Settings file corrupted; please delete the file:"
							+ LoadedSettingsFileName + ".xml");
			System.exit(-5);
		}
		if (SettingsList.getLength() == 0) {
			Element workspace = SettingsDocument.createElement(WorkSpace);
			// workspace.setAttribute(WorkPath, Join(HomePath, WorkPathName));
			workspace.setAttribute(WorkPath, "");
			RootList.item(0).appendChild(workspace);
			SettingsList = SettingsDocument.getElementsByTagName(WorkSpace);
			result = false;
		}
		GetXilinxToolPath();
		GetAlteraToolPath();
		GetHDLOnly();
		GetHDLType();
		GetAltera64Bit();

		SettingsList = SettingsDocument.getElementsByTagName(Boards);
		if (SettingsList.getLength() > 1) {
			JOptionPane.showMessageDialog(null,
					"Fatal Error: Settings file corrupted; please delete the file:"
							+ LoadedSettingsFileName + ".xml");
			System.exit(-5);
		}
		if (SettingsList.getLength() == 0) {
			Element workspace = SettingsDocument.createElement(Boards);
			workspace.setAttribute(SelectedBoard, KnownBoards.GetBoardNames()
					.toArray()[0].toString());
			RootList.item(0).appendChild(workspace);
			SettingsList = SettingsDocument.getElementsByTagName(Boards);
			result = false;
		}
		GetSelectedBoard();

		result &= !modified;
		return result;
	}
	
	public boolean AddExternalBoard(String CompleteFileName) {
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(Boards);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		Node ThisWorkspace = SettingsList.item(0);
		int NrOfBoards = 0;
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int j = 0; j < WorkspaceParameters.getLength();j++) {
			if (WorkspaceParameters.item(j).getNodeName().contains(ExternalBoard)) {
				String[] Items = WorkspaceParameters.item(j).getNodeName().split("_");
				if (Items.length == 2) {
					if (Integer.parseInt(Items[1])>NrOfBoards)
						NrOfBoards = Integer.parseInt(Items[1]);
				}
			}
		}
		NrOfBoards += 1;
		/* The attribute does not exists so add it */
		Attr extBoard = SettingsDocument.createAttribute(ExternalBoard+"_"+Integer.toString(NrOfBoards));
		extBoard.setNodeValue(CompleteFileName);
		Element workspace = (Element) SettingsList.item(0);
		workspace.setAttributeNode(extBoard);
		KnownBoards.AddExternalBoard(CompleteFileName);
		modified = true;
		return true;
	}

	public boolean SetStaticWorkspacePath(String path) {
		if (path.length() > 1 && path.endsWith(File.separator))
			path = path.substring(0, path.length() - 1);
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(WorkPath)) {
				WorkspaceParameters.item(i).setNodeValue(path);
				modified = true;
				return true;
			}
		}
		((Element)ThisWorkspace).setAttribute(WorkPath, path);
		modified = true;
		return true;
	}

	public boolean SetXilinxToolPath(String path) {
		if (path.length() > 1 && path.endsWith(File.separator))
			path = path.substring(0, path.length() - 1);
		if (!XilinxToolsFound(path))
			return false;
		NodeList SettingsList = SettingsDocument
				.getElementsByTagName(WorkSpace);
		if (SettingsList.getLength() != 1) {
			return false;
		}
		Node ThisWorkspace = SettingsList.item(0);
		NamedNodeMap WorkspaceParameters = ThisWorkspace.getAttributes();
		for (int i = 0; i < WorkspaceParameters.getLength(); i++) {
			if (WorkspaceParameters.item(i).getNodeName().equals(XilinxName)) {
				WorkspaceParameters.item(i).setNodeValue(path);
				modified = true;
				return true;
			}
		}
		return false;
	}

	public boolean UpdateSettingsFile() {
		if (!modified)
			return true;
		return WriteXml();
	}
	
        private boolean WriteXml() {
            try {
                writeTo(SharedPath, SharedSettingsFileName);
                return true;
            } catch (Exception e) { }
            try {
                writeTo(HomePath, UserSettingsFileName);
                return true;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
                return false;
            }
        }

        private void writeTo(String dir, String name) throws Exception {
                File SettingsFile = new File(Join(dir, name));
                TransformerFactory tranFactory = TransformerFactory.newInstance();
                tranFactory.setAttribute("indent-number", 3);
                Transformer aTransformer = tranFactory.newTransformer();
                aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
                Source src = new DOMSource(SettingsDocument);
                Result dest = new StreamResult(SettingsFile);
                aTransformer.transform(src, dest);
                modified = false;
	}

	private boolean XilinxToolsFound(String path) {
		for (int i = 0; i < XilinxPrograms.length; i++) {
			File test = new File(Join(path, XilinxPrograms[i]));
			if (!test.exists())
				return false;
		}
		return true;
	}
}
