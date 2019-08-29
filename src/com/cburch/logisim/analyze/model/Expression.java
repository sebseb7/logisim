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

package com.cburch.logisim.analyze.model;
import static com.cburch.logisim.analyze.model.Strings.S;

import java.util.HashSet;

public abstract class Expression {

  public static interface Visitor<T> {
    public default T visitVariable(Expression e, String name) { return null; }
    public default T visitConstant(Expression e, int value) { return null; }
    public default T visitNot(Expression e, Expression a) { return null; }
    public default T visitBinary(Expression e, Expression a, Expression b, Op op) {
      a.visit(this);
      b.visit(this);
      return null;
    }

    public default T visitAnd(Expression e, Expression a, Expression b) { return visitBinary(e, a, b, Op.AND); }
    public default T visitOr(Expression e, Expression a, Expression b) { return visitBinary(e, a, b, Op.OR); }
    public default T visitXor(Expression e, Expression a, Expression b) { return visitBinary(e, a, b, Op.XOR); }
    public default T visitXnor(Expression e, Expression a, Expression b) { return visitBinary(e, a, b, Op.XNOR); }
    public default T visitEq(Expression e, Expression a, Expression b) { return visitBinary(e, a, b, Op.EQ); }
  }

  // internal, just for fast eval
  protected static interface IntVisitor {
    public int visitVariable(String name);
    public int visitConstant(int value);
    public int visitNot(Expression a);

    public int visitAnd(Expression a, Expression b);
    public int visitOr(Expression a, Expression b);
    public int visitXor(Expression a, Expression b);
    public int visitXnor(Expression a, Expression b);
    public int visitEq(Expression a, Expression b);
  }

  public boolean contains(Op o) {
    return o == visit(new Visitor<Op>() {
      @Override
      public Op visitBinary(Expression e, Expression a, Expression b, Op op) {
        return (op == o || a.visit(this) == o || b.visit(this) == o) ? o : null;
      }
      @Override
      public Op visitNot(Expression e, Expression a) { return a.visit(this); }
    });
  }

  public boolean evaluate(final Assignments assignments) {
    int ret = visit(new IntVisitor() {
      @Override
      public int visitAnd(Expression a, Expression b) {
        return a.visit(this) & b.visit(this);
      }

      @Override
      public int visitConstant(int value) {
        return value;
      }

      @Override
      public int visitNot(Expression a) {
        return ~a.visit(this);
      }

      @Override
      public int visitOr(Expression a, Expression b) {
        return a.visit(this) | b.visit(this);
      }

      @Override
      public int visitVariable(String name) {
        return assignments.get(name) ? 1 : 0;
      }

      @Override
      public int visitXor(Expression a, Expression b) {
        return a.visit(this) ^ b.visit(this);
      }

      @Override
      public int visitXnor(Expression a, Expression b) {
        return ~(a.visit(this) ^ b.visit(this));
      }

      @Override
      public int visitEq(Expression a, Expression b) {
        return ~(a.visit(this) ^ b.visit(this)&1);
      }
    });
    return (ret & 1) != 0;
  }

  public abstract int getPrecedence(Notation notation);
  public abstract Op getOp();

  public boolean isCircular() {
    final HashSet<Expression> visited = new HashSet<Expression>();
    visited.add(this);
    Object loop = new Object();
    return loop == visit(new Visitor<Object>() {
      @Override
      public Object visitBinary(Expression e, Expression a, Expression b, Op op) {
        if (!visited.add(a))
          return loop;
        if (a.visit(this) == loop)
          return loop;
        visited.remove(a);

        if (!visited.add(b))
          return loop;
        if (b.visit(this) == loop)
          return loop;
        visited.remove(b);

        return null;
      }

      @Override
      public Object visitNot(Expression e, Expression a) {
        if (!visited.add(a))
          return loop;
        if (a.visit(this) == loop)
          return loop;
        visited.remove(a);
        return null;
      }
    });
  }

  public boolean isCnf() {
    Object cnf = new Object();
    return cnf == visit(new Visitor<Object>() {
      int level = 0;

      @Override
      public Object visitAnd(Expression e, Expression a, Expression b) {
        if (level > 1)
          return null;
        int oldLevel = level;
        level = 1;
        Object ret = a.visit(this) == cnf && b.visit(this) == cnf ? cnf : null;
        level = oldLevel;
        return ret;
      }

      @Override
      public Object visitConstant(Expression e, int value) {
        return cnf;
      }

      @Override
      public Object visitNot(Expression e, Expression a) {
        if (level == 2)
          return null;
        int oldLevel = level;
        level = 2;
        Object ret = a.visit(this);
        level = oldLevel;
        return ret;
      }

      @Override
      public Object visitOr(Expression e, Expression a, Expression b) {
        if (level > 0)
          return null;
        return a.visit(this) == cnf && b.visit(this) == cnf ? cnf : null;
      }

      @Override
      public Object visitVariable(Expression e, String name) {
        return cnf;
      }

      @Override
      public Object visitXor(Expression e, Expression a, Expression b) {
        return null;
      }

      @Override
      public Object visitXnor(Expression e, Expression a, Expression b) {
        return null;
      }

      @Override
      public Object visitEq(Expression e, Expression a, Expression b) {
        return null;
      }
    });
  }

  Expression removeVariable(final String input) {
    return visit(new Visitor<Expression>() {
      @Override
      public Expression visitAnd(Expression e, Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        if (l == null)
          return r;
        if (r == null)
          return l;
        return Expressions.and(l, r);
      }

      @Override
      public Expression visitConstant(Expression e, int value) {
        return Expressions.constant(value);
      }

      @Override
      public Expression visitNot(Expression e, Expression a) {
        Expression l = a.visit(this);
        if (l == null)
          return null;
        return Expressions.not(l);
      }

      @Override
      public Expression visitOr(Expression e, Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        if (l == null)
          return r;
        if (r == null)
          return l;
        return Expressions.or(l, r);
      }

      @Override
      public Expression visitVariable(Expression e, String name) {
        return name.equals(input) ? null : Expressions.variable(name);
      }

      @Override
      public Expression visitXor(Expression e, Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        if (l == null)
          return r;
        if (r == null)
          return l;
        return Expressions.xor(l, r);
      }

      @Override
      public Expression visitXnor(Expression e, Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        if (l == null)
          return r;
        if (r == null)
          return l;
        return Expressions.xnor(l, r);
      }

      @Override
      public Expression visitEq(Expression e, Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        if (l == null)
          return r;
        if (r == null)
          return l;
        return Expressions.eq(l, r);
      }
    });
  }

  Expression replaceVariable(final String oldName, final String newName) {
    return visit(new Visitor<Expression>() {
      @Override
      public Expression visitAnd(Expression e, Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        return Expressions.and(l, r);
      }

      @Override
      public Expression visitConstant(Expression e, int value) {
        return Expressions.constant(value);
      }

      @Override
      public Expression visitNot(Expression e, Expression a) {
        Expression l = a.visit(this);
        return Expressions.not(l);
      }

      @Override
      public Expression visitOr(Expression e, Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        return Expressions.or(l, r);
      }

      @Override
      public Expression visitVariable(Expression e, String name) {
        return Expressions.variable(name.equals(oldName) ? newName : name);
      }

      @Override
      public Expression visitXor(Expression e, Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        return Expressions.xor(l, r);
      }

      @Override
      public Expression visitXnor(Expression e, Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        return Expressions.xnor(l, r);
      }

      @Override
      public Expression visitEq(Expression e, Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        return Expressions.eq(l, r);
      }
    });
  }

  public static enum Notation {
    ENGINEERING(0), LOGIC(1), ALTLOGIC(2), PROGBOOLS(3), PROGBITS(4);

    public final int Id;
    public final int[] opLvl;
    public final String[] opSym;

    // Notes on precedence:
    // all forms of NOT are the highest precedence level
    public static final int NOT_PRECEDENCE = 14;
    // times and implicit and are next
    public static final int IMPLICIT_AND_PRECEDENCE = 13;
    public static final int TIMES_PRECEDENCE = 13;
    // oplus is next
    public static final int OPLUS_PRECEDENCE = 12;
    // plus is next
    public static final int PLUS_PRECEDENCE = 11;
    // otimes is next
    public static final int OTIMES_PRECEDENCE = 10;
    // not-equals, not-equiv, equiv, vee, vee-underbar, and cap are next
    public static final int LOGIC_PRECEDENCE = 9;
    // & is next
    public static final int BITAND_PRECEDENCE = 8;
    // ^ is next
    public static final int BITXOR_PRECEDENCE = 7;
    // | is next
    public static final int BITOR_PRECEDENCE = 6;
    // && is next
    public static final int AND_PRECEDENCE = 5;
    // || is next
    public static final int OR_PRECEDENCE = 4;
    // "and" is next
    public static final int PYTHON_AND_PRECEDENCE = 3;
    // "xor" is next
    public static final int PYTHON_XOR_PRECEDENCE = 2;
    // "or" is next
    public static final int PYTHON_OR_PRECEDENCE = 1;
    // all forms of equals are level 0
    public static final int EQ_PRECEDENCE = 0;

    private Notation(int id) {
      Id = id;
      // Precendence level and symbol for each of { EQ, XNOR, OR, XOR, AND, NOT }
      switch(id) {
      case 0: // Engineering notation: otimes, plus, oplus, times, and overbar (slash)
        opLvl = new int[] { 0, 10, 11, 12, 13, 14, };
        opSym = new String[] { " := ", " \u2299 ", " + ", " \u2295 ", " \u22C5 ", "/", };
        break;
      case 1:
        // Logic notation: equiv, vee, vee-underbar, cap, tilde
        opLvl = new int[] { 0, 9, 9, 9, 9, 14, };
        opSym = new String[] { ": ", " \u2261", " \u2228 ", " \u22BB ", " \u2227 ", "~", };
        break;
      case 2:
        // Alternative Logic notation: equiv, vee, not-equiv, cap, ell
        opLvl = new int[] { 0, 9, 9, 9, 9, 14, };
        opSym = new String[] { ": ", " \u2261 ", " \u2228 ", " \u2262 ", " \u2227 ", "\u00AC", };
        break;
      case 3:
        // Programming with booleans notation: ==, ||, !=, &&, !
        opLvl = new int[] { 0, 9, 4, 9, 5, 14, };
        opSym = new String[] { " = ", " == ", " || ", " != ", " && ", "!", };
        break;
      case 4:
      default:
        // Programming with bits notation: ^ ~, |, ^, &, ~
        opLvl = new int[] { 0, 9, 6, 7, 8, 14, };
        opSym = new String[] { " = ", " ^ ~ ", " | ", " ^ ", " & ", "~", };
        break;
      }
    }

    public String toString() {
      String key = name().toLowerCase() + "Notation";
      return S.get(key);
    }

  }

  public static enum Op {
    EQ(0,2), XNOR(1,2), OR(2,2), XOR(3,2), AND(4,2), NOT(5,1);

    public final int Id, Arity;

    private Op(int id, int arity) {
      Id = id;
      Arity = arity;
    }
  }

  @Override
  public String toString() {
    return toString(Notation.ENGINEERING);
  }

  public String toString(Notation notation) {
    final StringBuilder text = new StringBuilder();
    visit(new Visitor<Void>() {
      @Override
      public Void visitBinary(Expression e, Expression a, Expression b, Op op) {
        int opLvl = notation.opLvl[op.Id];
        int aLvl = a.getPrecedence(notation);
        int bLvl = b.getPrecedence(notation);
        if (aLvl < opLvl || (aLvl == opLvl && a.getOp() != op)) {
          text.append("(");
          a.visit(this);
          text.append(")");
        } else {
          a.visit(this);
        }
        text.append(notation.opSym[op.Id]);
        if (bLvl < opLvl || (bLvl == opLvl && b.getOp() != op)) {
          text.append("(");
          b.visit(this);
          text.append(")");
        } else {
          b.visit(this);
        }
        return null;
      }

      @Override
      public Void visitConstant(Expression e, int value) {
        text.append(Integer.toString(value, 16));
        return null;
      }

      @Override
      public Void visitNot(Expression e, Expression a) {
        int opLvl = notation.opLvl[Op.NOT.Id];
        int aLvl = a.getPrecedence(notation);
        text.append(notation.opSym[Op.NOT.Id]);
        if (aLvl < opLvl || (aLvl == opLvl && a.getOp() != Op.NOT)) {
          text.append("(");
          a.visit(this);
          text.append(")");
        } else {
          a.visit(this);
        }
        return null;
      }

      @Override
      public Void visitVariable(Expression e, String name) {
        text.append(name);
        return null;
      }

    });
    return text.toString();
  }

  public static boolean isAssignment(Expression expr) {
    if (expr == null || !(expr instanceof Expressions.Eq))
      return false;
    Expressions.Eq eq = (Expressions.Eq)expr;
    return (eq.a != null && (eq.a instanceof Expressions.Variable));
  }

  public static String getAssignmentVariable(Expression expr) {
    if (expr == null || !(expr instanceof Expressions.Eq))
      return null;
    Expressions.Eq eq = (Expressions.Eq)expr;
    return (eq.a != null && (eq.a instanceof Expressions.Variable)) ? eq.a.toString() : null;
  }

  public static Expression getAssignmentExpression(Expression expr) {
    if (expr == null || !(expr instanceof Expressions.Eq))
      return null;
    Expressions.Eq eq = (Expressions.Eq)expr;
    return (eq.a != null && (eq.a instanceof Expressions.Variable)) ? eq.b : null;
  }

  public abstract <T> T visit(Visitor<T> visitor);

  abstract int visit(IntVisitor visitor);

}
