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

package com.cburch.logisim.std.hdl;
import static com.cburch.logisim.std.Strings.S;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rtextarea.Gutter;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.Icons;

public class VhdlParser extends AbstractParser {

  private static class ScanLoc {
    String title;
    int charno, lineno, colno;
    ScanLoc(String t, int i, int n, int c) {
      title = t;
      charno = i;
      lineno = n;
      colno = c;
    }
    public String msg(String m) {
      if (title == null)
      return String.format("line %d, column %d: %s", lineno, colno, m);
      else
      return String.format("%s line %d, column %d: %s", title, lineno, colno, m);
    }
    @Override
    public String toString() {
      if (title == null)
        return String.format("line %d, column %d: ", lineno, colno);
      else
        return String.format("%s line %d, column %d:", title, lineno, colno);
    }
  }

  private static class Scanner extends ScanLoc {
    String input; // text appearing after this ScanLoc
    MatchResult m; // most recent match, ending at this ScanLoc
    ArrayList<ScanLoc> loc; // start of each match group, including group(0)

    Scanner(String title, String input) {
      super(title, 0, 1, 0);
      this.input = input;
      loc = new ArrayList<>();
    }

    boolean next(Pattern p) {
      m = null;

      // skip leading whitespace
      int w = 0;
      while (w < input.length() && Character.isWhitespace(input.charAt(w))) {
        if (input.charAt(w) == '\n') {
          lineno++;
          colno = 0;
        } else {
          colno++;
        }
        w++;
      }
      charno += w;
      if (w >= input.length()) {
        input = "";
        return false;
      }
      if (w > 0)
        input = input.substring(w);

      // try to match p
      Matcher match = p.matcher(input);
      if (!match.lookingAt())
        return false;
      m = match;

      int count = m.groupCount();
      loc.clear();
      loc.add(new ScanLoc(title, charno, lineno, colno));
      String txt = m.group(0);
      for (int g = 1; g <= m.groupCount(); g++) {
        int s = m.start(g);
        int n = (int)txt.substring(0, s).chars().filter(ch -> ch == '\n').count();
        int i = txt.substring(0, s).lastIndexOf('\n');
        loc.add(new ScanLoc(title, charno + s, lineno + n, n == 0 ? (colno + s) : (s - i)));
      }
      int s = txt.length();
      int n = (int)txt.chars().filter(ch -> ch == '\n').count();
      int i = txt.lastIndexOf('\n');
      charno += s;
      if (n == 0) {
        colno += s;
      } else {
        lineno += n;
        colno = s - i;
      }
      if (match.hitEnd())
        input = "";
      else
        input = input.substring(m.end());
      return true;
    }
    MatchResult match() {
      return m;
    }
    boolean isEmpty() {
      return input.isEmpty();
    }
    ScanLoc loc(int i) {
      return loc.get(i);
    }
  }

  public static class IllegalVhdlContentException extends Exception {

    private static final long serialVersionUID = 1L;
    public final ScanLoc loc;

    // public IllegalVhdlContentException() {
    //   super();
    // }

    public IllegalVhdlContentException(ScanLoc loc, String message) {
      super(loc.msg(message));
      this.loc = loc;
    }

    public IllegalVhdlContentException(ScanLoc loc, String message, Throwable cause) {
      super(loc.msg(message), cause);
      this.loc = loc;
    }

    public IllegalVhdlContentException(ScanLoc loc, Throwable cause) {
      this(loc, cause.getMessage(), cause);
    }

  }

  public static class PortDescription {

    private String name;
    private String type;
    private BitWidth width;

    public PortDescription(String name, String type, int width) {
      this.name = name;
      this.type = type;
      this.width = BitWidth.create(width);
    }

    public String getName() {
      return this.name;
    }

    public String getType() {
      return this.type;
    }

    public  String getVhdlType() {
      if (type == Port.INPUT)
        return "in";
      else if (type == Port.OUTPUT)
        return "out";
      else if (type == Port.INOUT)
        return "inout";
      else
        throw new IllegalArgumentException("Not recognized port type: " + type);
    }

    public BitWidth getWidth() {
      return this.width;
    }
  }

  public static class GenericDescription {

    protected String name;
    protected String type;
    protected int dval;

    public GenericDescription(String name, String type, int dval) {
      this.name = name;
      this.type = type;
      this.dval = dval;
    }

    public GenericDescription(String name, String type) {
      this.name = name;
      this.type = type;
      if (type.equals("positive"))
        dval = 1;
      else
        dval = 0;
    }

    public String getName() {
      return this.name;
    }

    public String getType() {
      return this.type;
    }

    public int getDefaultValue() {
      return this.dval;
    }
  }

  // All regex patterns used in this file follow the same conventions, so we use
  // shortcuts: space means any amount of whitespace, two spaces means any
  // non-empty amount of whitespace, dot matches anything including newlines,
  // and case is ignored. Any amount of whitespace is always allowed before and
  // after each pattern, and such whitespace is ignored by next(p), above.
  private static Pattern regex(String pattern) {
    pattern = pattern.trim();
    pattern = pattern.replaceAll("  ", "\\\\s+"); // Two spaces = required whitespace
    pattern = pattern.replaceAll(" ", "\\\\s*"); // One space = optional whitespace
    return Pattern.compile(pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  }

  private static final Pattern LIBRARY = regex("library  \\w+ ;");
  private static final Pattern USING = regex("use  \\S+ ;");
  private static final Pattern ENTITY = regex("entity  (\\w+)  is");
  private static final Pattern END = regex("end  (\\w+) ;");
  private static final Pattern ARCHITECTURE = regex("architecture .*"); // rest of input

  private static final Pattern SEMICOLON = regex(";");
  private static final Pattern OPENLIST = regex("[(]");
  private static final Pattern DONELIST = regex("[)] ;");

  private static final Pattern PORTS = regex("port");
  private static final Pattern PORT = regex("(\\w+(?: , \\w+)*) : (\\w+)  (\\w+)");
  private static final Pattern RANGE = regex("[(] (\\d+) downto (\\d+) [)]");

  private static final Pattern GENERICS = regex("generic");
  private static final Pattern GENERIC = regex("(\\w+(?: , \\w+)*) : (\\w+)");
  private static final Pattern DVALUE = regex(":= (\\w+)");

  private ArrayList<PortDescription> inputs = new ArrayList<>();
  private ArrayList<PortDescription> outputs = new ArrayList<>();
  private ArrayList<GenericDescription> generics = new ArrayList<>();
  private String title;
  private String source;
  private String name;
  private String libraries;
  private String architecture;

  public VhdlParser(String title, String source) {
    this.title = title;
    reset(source);
  }

  private Gutter gutter; // for displaying errors
  public VhdlParser(Gutter gutter) {
    this.gutter = gutter;
  }

  void reset(String src) {
    source = src;
    inputs.clear();
    outputs.clear();
    generics.clear();
    name = null;
    libraries = null;
    architecture = null;
  }

  public String getArchitecture() {
    return architecture;
  }

  public List<PortDescription> getInputs() {
    return inputs;
  }

  public List<PortDescription> getOutputs() {
    return outputs;
  }

  public List<GenericDescription> getGenerics() {
    return generics;
  }

  public String getLibraries() {
    return libraries;
  }

  public String getName() {
    return name;
  }

  private String getPortType(ScanLoc loc, String type) throws IllegalVhdlContentException {
    if (type.equalsIgnoreCase("in"))
      return Port.INPUT;
    if (type.equalsIgnoreCase("out"))
      return Port.OUTPUT;
    if (type.equalsIgnoreCase("inout"))
      return Port.INOUT;
    throw new IllegalVhdlContentException(loc, 
        S.get("invalidTypeException") + ": " + type);
  }

  public void parse() throws IllegalVhdlContentException {
    Scanner input = new Scanner(title, removeComments());
    parseLibraries(input);

    if (!input.next(ENTITY))
      throw new IllegalVhdlContentException(input, S.get("CannotFindEntityException"));
    name = input.match().group(1);

    while (parsePorts(input) || parseGenerics(input))
      ;

    if (!input.next(END))
      throw new IllegalVhdlContentException(input, S.fmt("vhdlExpectingEndEntity", name));
    if (!input.match().group(1).equals(name))
      throw new IllegalVhdlContentException(input.loc(1),
          S.fmt("vhdlWrongEndEntity", name, input.match().group(1)));

    parseArchitecture(input);

    if (!input.isEmpty())
      throw new IllegalVhdlContentException(input, S.get("vhdlTrailingContent"));
  }

  private void parseArchitecture(Scanner input) throws IllegalVhdlContentException {
    if (input.next(ARCHITECTURE))
      architecture = input.match().group().trim();
    else
      architecture = "";
  }

  private void parseLibraries(Scanner input) throws IllegalVhdlContentException {
    StringBuilder result = new StringBuilder();
    while (input.next(LIBRARY) || input.next(USING)) {
      result.append(input.match().group().trim().replaceAll("\\s+", " "));
      result.append(System.getProperty("line.separator"));
    }
    libraries = result.toString();
  }

  private void parsePort(Scanner input) throws IllegalVhdlContentException {
    // Example: "name : IN std_logic"
    // Example: "name : OUT std_logic_vector(expr downto expr)"
    // Example: "name1, name2, name3 : IN std_logic"
    // Example: "name1, name2, name3 : OUT std_logic_vector(expr downto expr)"

    if (!input.next(PORT))
      throw new IllegalVhdlContentException(input, S.get("portDeclarationException"));
    String names = input.match().group(1).trim();
    String ptype = getPortType(input, input.match().group(2).trim());
    String type = input.match().group(3).trim();

    int width;
    if (type.equalsIgnoreCase("std_logic")) {
      width = 1;
    } else {
      if (!input.next(RANGE))
        throw new IllegalVhdlContentException(input, S.get("portDeclarationException"));
      int upper = Integer.parseInt(input.match().group(1));
      int lower = Integer.parseInt(input.match().group(2));
      width = upper - lower + 1;
    }

    for (String name : names.split("\\s*,\\s*")) {
      if (ptype == Port.INPUT)
        inputs.add(new PortDescription(name, ptype, width));
      else
        outputs.add(new PortDescription(name, ptype, width));
    }
  }

  private boolean parsePorts(Scanner input) throws IllegalVhdlContentException {
    // Example: "port ( decl ) ;"
    // Example: "port ( decl ; decl ; decl ) ;"
    if (!input.next(PORTS))
      return false;
    if (!input.next(OPENLIST))
      throw new IllegalVhdlContentException(input, S.get("portDeclarationException"));
    parsePort(input);
    while (input.next(SEMICOLON))
      parsePort(input);
    if (!input.next(DONELIST))
      throw new IllegalVhdlContentException(input, S.get("portDeclarationException"));
    return true;
  }

  private void parseGeneric(Scanner input) throws IllegalVhdlContentException {
    // Example: "name : integer"
    // Example: "name : integer := constant"
    // Example: "name1, name2, name3 : integer"
    if (!input.next(GENERIC))
      throw new IllegalVhdlContentException(input,
          S.get("genericDeclarationException") + ": " + S.get("genericExpectedNames"));
    String names = input.match().group(1).trim();
    String type = input.match().group(2).trim();
    if (!type.equalsIgnoreCase("integer") && !type.equalsIgnoreCase("natural") && !type.equalsIgnoreCase("positive")) {
      throw new IllegalVhdlContentException(input.loc(2),
          S.get("genericTypeException") + ": " + type);
    }
    type = type.toLowerCase();
    int dval = 0;
    if (type.equals("positive")) {
      dval = 1;
    }
    if (input.next(DVALUE)) {
      String s = input.match().group(1);
      try {
        dval = Integer.decode(s);
      } catch (NumberFormatException e) {
        throw new IllegalVhdlContentException(input.loc(1),
            S.get("genericValueException") + ": " + s);
      }
      if (type.equals("natural") && dval < 0 || type.equals("positive") && dval < 1)
        throw new IllegalVhdlContentException(input.loc(2),
            S.get("genericValueException") + ": " + dval);
    }

    for (String name : names.split("\\s*,\\s*")) {
      generics.add(new GenericDescription(name, type, dval));
    }
  }

  private boolean parseGenerics(Scanner input) throws IllegalVhdlContentException {
    // Example: generic ( decl ) ;
    // Example: generic ( decl ; decl ; decl ) ;
    if (!input.next(GENERICS))
      return false;
    if (!input.next(OPENLIST))
      throw new IllegalVhdlContentException(input,
          S.get("genericDeclarationException") + ": " + S.get("genericExpectedOpenParen"));
    parseGeneric(input);
    while (input.next(SEMICOLON))
      parseGeneric(input);
    if (!input.next(DONELIST))
      throw new IllegalVhdlContentException(input,
          S.get("genericDeclarationException") + ": " + S.get("genericExpectedCloseParen"));
    return true;
  }

  private String removeComments() throws IllegalVhdlContentException {
    if (source == null)
      return "";

    char[] s = source.toCharArray();
    int n = s.length;
    boolean comment = false;
    for (int i = 0; i < n; i++) {
      if (comment && (s[i] == '\r' || s[i] == '\n'))
        comment = false;
      else if (!comment && (s[i] == '-' && i+1 < n && s[i+1] == '-'))
        comment = true;
      if (comment)
        s[i] = ' ';
    }

    return new String(s);
  }

  public boolean enabled = false;
  @Override
  public boolean isEnabled() { return enabled; }
  
  @Override
  public ParseResult parse(RSyntaxDocument doc, String style) {
    gutter.removeAllTrackingIcons();
    DefaultParseResult pr = new DefaultParseResult(this);
    if (!enabled) {
      // should never happen
      return pr;
    }
    try  {
      reset(doc.getText(0, doc.getLength()));
    } catch (Exception e) {
      pr.setError(e);
      return pr;
    }
    try {
      parse();
    } catch (IllegalVhdlContentException e) {
      int lineno = e.loc.lineno-1;
      pr.addNotice(new DefaultParserNotice(this, e.getMessage(), lineno, e.loc.charno, -1));
      pr.setError(e); // does this show in UI somehow?
      try {
        gutter.addLineTrackingIcon(lineno, Icons.getIcon("error.gif"), e.getMessage());
      } catch (Exception ex) {
      }
    }
    return pr;
  }

}
