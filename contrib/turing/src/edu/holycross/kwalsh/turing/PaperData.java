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

package edu.holycross.kwalsh.turing;

import java.util.ArrayList;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.start.TtyInterface;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;

/** Represents the state of a paper tape. */
class PaperData implements InstanceData, Cloneable, TtyInterface.PaperTape {

	public static PaperData get(InstanceState s) {
		PaperData ret = (PaperData)s.getData();
    String ab = s.getAttributeValue(PaperTape.ATTR_ALPHABET);
    String init = s.getAttributeValue(PaperTape.ATTR_INIT);
		if (ret == null) {
			ret = new PaperData(ab, s.getAttributeValue(PaperTape.ATTR_INIT));
			s.setData(ret);
		} else if (ret.alphabet != ab) {
      ret.setAlphabet(ab, init);
    }
		return ret;
	}

	private Value lastClock;
	private Value cur, blank;
  private ArrayList<Value> left = new ArrayList<>();
  private ArrayList<Value> right = new ArrayList<>();
  private int pos;
  private String alphabet, initial;
  private boolean movingLeft, movingRight, disableAnimation;


	public PaperData(String ab, String init) {
    setAlphabet(ab, init);
	}

	@Override
	public Object clone() {
		try {
			PaperData copy = (PaperData)super.clone();
      copy.left = (ArrayList<Value>)left.clone();
      copy.right = (ArrayList<Value>)right.clone();
      return copy;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

  public synchronized int getPosition() { return pos; }
	public synchronized Value getValue() { return cur == null ? blank : cur; }
  public synchronized boolean isMovingLeft() { return movingLeft && !movingRight && !disableAnimation; }
  public synchronized boolean isMovingRight() { return movingRight && !movingLeft && !disableAnimation; }
  public synchronized boolean isMoving() { return movingRight != movingLeft && !disableAnimation; }
  public synchronized void setAnimated(boolean b) { disableAnimation = !b; }

  // public synchronized String getAlphabet() { return alphabet; }

  private void setAlphabet(String ab, String init) {
    if (alphabet == ab && initial.equals(init))
      return;
    alphabet = ab;
    initial = init;
    int w = PaperTape.widthFor(alphabet);
    if (alphabet == PaperTape.BCD)
      blank = Value.createKnown(BitWidth.create(w), -1);
    else
      blank = Value.createKnown(BitWidth.create(w), 0);

    left.clear();
    cur = null;
    right.clear();
    for (int i = 0; i < initial.length(); ) {
      int utf32 = initial.codePointAt(i);
      if (printUtf32Symbol(utf32))
        moveRight();
      i += Character.charCount(utf32);
    }
    while (pos > 0)
      moveLeft();
  }

  public synchronized Value getValue(int i) {
    Value v;
    if (i > pos) {
      int r = i - pos;
      v = r > right.size() ? null : right.get(right.size() - r);
    } else if (i < pos) {
      int l = pos - i;
      v = l > left.size() ? null : left.get(left.size() - l);
    } else {
      v = cur;
    }
    return v == null ? blank : v;
  }

  public synchronized boolean printUtf32Symbol(int utf32) {
    BitWidth w = BitWidth.create(PaperTape.widthFor(alphabet));
    if (alphabet == PaperTape.BCD) {
      if ('0' <= utf32 && utf32 <= '9')
        print(Value.createKnown(w, utf32));
      else if (utf32 == ' ')
        print(blank);
      else return false;
    } else if (alphabet == PaperTape.ABC) {
      if ('a' <= utf32 && utf32 <= 'c')
        print(Value.createKnown(w, utf32-'a'+1));
      else if ('A' <= utf32 && utf32 <= 'C')
        print(Value.createKnown(w, utf32-'A'+1));
      else if (utf32 == ' ')
        print(blank);
      else return false;
    } else if (alphabet == PaperTape.DOTS) {
      if (utf32 == 0x00b7 || utf32 == 0x22c5 || 
          utf32 == 0x2022 || utf32 == '.' || utf32 == '*' || utf32 == 'x')
        print(Value.TRUE);
      else if (utf32 == ' ')
        print(blank);
      else return false;
    } else if (alphabet == PaperTape.BINARY) {
      if (utf32 == 'e' || utf32 == '@' || utf32 == 0x0259)
        print(Value.createKnown(w, 1));
      else if (utf32 == '0')
        print(Value.createKnown(w, 2));
      else if (utf32 == '1')
        print(Value.createKnown(w, 3));
      else if (utf32 == ' ')
        print(blank);
      else return false;
    } else if (alphabet == PaperTape.ALAN) {
      if (utf32 == ' ')
        print(blank);
      else if (utf32 == '0')
        print(Value.createKnown(w, 1));
      else if (utf32 == '1')
        print(Value.createKnown(w, 2));
      else if (utf32 == '@' || utf32 == '.' || utf32 == 0x0259)
        print(Value.createKnown(w, 3));
      else if ('a' <= utf32 && utf32 <= 'z')
        print(Value.createKnown(w, 4 + (utf32 - 'a')));
      else if ('A' <= utf32 && utf32 <= 'Z')
        print(Value.createKnown(w, 4 + (utf32 - 'A')));
      else if (utf32 == '|')
        print(Value.createKnown(w, 0x1e));
      else if (utf32 == '#')
        print(Value.createKnown(w, 0x1f));
      else return false;
    } else if (alphabet == PaperTape.PRINTABLE_ASCII) {
      if (0x20 <= utf32 && utf32 <= 0x7E)
        print(Value.createKnown(w, utf32));
      else return false;
    } else if (alphabet == PaperTape.FULL_ASCII) {
      if (0x1 <= utf32 && utf32 <= 0x7F)
        print(Value.createKnown(w, utf32));
      else return false;
    } else if (alphabet == PaperTape.UNICODE) {
      print(Value.createKnown(w, utf32));
    } else {
      return false;
    }
    return true;
  }

  public synchronized int getUtf32Symbol(int i) {
    Value v;
    if (i > pos) {
      int r = i - pos;
      v = r > right.size() ? null : right.get(right.size() - r);
    } else if (i < pos) {
      int l = pos - i;
      v = l > left.size() ? null : left.get(left.size() - l);
    } else {
      v = cur;
    }
    if (v == null)
      return 0;
    int s = v.toIntValue();
    if (alphabet == PaperTape.BCD) {
      if (0 <= s && s <= 9)
        return '0' + s; // digits
      else
        return 0;
    } else if (alphabet == PaperTape.PRINTABLE_ASCII) {
      if (s == 0x20) // space
        return 0x23B5; // bottom square bracket
      else if (0x20 < s && s <= 0x7e) // other printable
        return s;
      else
        return 0;
    } else if (alphabet == PaperTape.FULL_ASCII) {
      if (s == 0)
        return 0;
      else if (s < 0x20)
        return 0x2400 + s;
      else if (s == 0x20)
        return 0x23B5;
      else if (s == 0x7f)
        return 0x2421;
      else if (0x20 < s && s < 0x7e) // other printable
        return s;
      else
        return 0; // unreachable
    } else if (alphabet == PaperTape.DOTS) {
      if (s == 1)
        return 0x2022;
      else
        return 0; // unreachable
    } else if (alphabet == PaperTape.ABC) {
      if (s == 1)
        return (int)'A';
      else if (s == 2)
        return (int)'B';
      else if (s == 3)
        return (int)'C';
      else
        return 0; // unreachable
    } else if (alphabet == PaperTape.BINARY) {
      if (s == 1)
        return 0x0259;
      else if (s == 2)
        return (int)'0';
      else if (s == 3)
        return (int)'1';
      else
        return 0; // unreachable
    } else if (alphabet == PaperTape.ALAN) {
      if (s == 0)
        return 0;
      else if (s == 1)
        return (int)'0';
      else if (s == 2)
        return (int)'1';
      else if (s == 3)
        return 0x0259;
      else if (s == 0x1e)
        return 0x1D736 + 0x1d;
      else if (s == 0x1f)
        return 0x1D736 + 0x1f;
      else
        return 0x1D736 + (s - 4); // alpha, beta, etc.
    } else if (alphabet == PaperTape.UNICODE) {
      return s;
    } else {
      return (int)'?';
    }
  }

  public boolean isBlank(Value v) {
    if (v == null || v.equals(blank))
      return true;
    int s = v.toIntValue();
    if (alphabet == PaperTape.BCD)
      return s > 9;
    else if (alphabet == PaperTape.PRINTABLE_ASCII)
      return s < 0x20 || s > 0x7e;
    else
      return false;
  }

	public synchronized void print(Value v) {
		cur = isBlank(v) ? null : v;
	}

  public synchronized void erase() { print(blank); }

  public synchronized void moveRight() {
    if (cur != null || !left.isEmpty())
      left.add(cur);
    if (right.isEmpty())
      cur = null;
    else
      cur = right.remove(right.size()-1);
    pos++;
    movingRight = !disableAnimation;
  }

  public synchronized void moveLeft() {
    if (cur != null || !right.isEmpty())
      right.add(cur);
    if (left.isEmpty())
      cur = null;
    else
      cur = left.remove(left.size()-1);
    pos--;
    movingLeft = !disableAnimation;
  }

	public boolean updateClock(Value value) {
		Value old = lastClock;
		lastClock = value;
    if (value != Value.TRUE)
      movingLeft = movingRight = false;
		return old == Value.FALSE && value == Value.TRUE;
	}

  // hack: TtyInterface.PaperTape
  public void set(String s) { setAlphabet(alphabet, s); }

  Object lrpv[] = new Object[] { Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE };
  void setLRPV(Value v[]) { lrpv = v; }

  public String[] getHeaders() {
    return new String[] { "S", "L", "R", "P", "V", "pos", "tape" };
  }
  public Object[] getValues() {
    return new Object[] {
      getValue(), lrpv[0], lrpv[1], lrpv[2], lrpv[3],
      String.format("%4d", getPosition()),
      getTapeString()
    };
  }

  /*
  Object[] prevVals = null;
  Value prevState =  null;
  int dupcount = 0;
  public boolean isHalted(Value state, Object vals[]) {
    System.out.println("dupcount " + dupcount);
    if (prevVals != null && prevState != null) {
      boolean same = prevState.equals(state);
      System.out.println("prev? " + same);
      for (int i = 0; same && i < vals.length; i++)
        same = same && prevVals[i].equals(vals[i]);
      if (same && dupcount > 3) {
        return true;
      } else if (same) {
        dupcount++;
        return false;
      }
    }
    dupcount = 0;
    prevVals = vals;
    prevState = state;
    return false;
  }
  */

  int lbound = 0, rbound = -1;
  String getTapeString() {
    if (lbound > rbound) {
      lbound = pos - left.size();
      rbound = pos + right.size();
      while (rbound - lbound < 15)
        rbound ++;
    }
    while (pos < lbound-3) { lbound--; rbound--; }
    while (pos > rbound+3) { lbound++; rbound++; }
    String s = "";
    for (int i = lbound - 4; i <= rbound + 4; i++) {
      int utf32 = getUtf32Symbol(i);
      String c = utf32 == 0 ? "_" : new String(new int[]{ utf32 }, 0, 1);
      if (i == pos)
        s += c + "]";
      else if (i == pos-1)
        s += c + "[";
      else
        s += c + " ";
    }
    return s.substring(0, s.length()-1);
  }

}
