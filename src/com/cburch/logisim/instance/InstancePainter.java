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

package com.cburch.logisim.instance;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Palette;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;

// This is more like ComponentPainter maybe?
// It combines a ComponentDrawContext together with data about a specific
// component that is being drawn. That component can be defined
// as either:
//  - a Component object (which has attributes, location, and state)
//  - a ComponentFactory and AttributeSet together (no location, no state)
public class InstancePainter extends GraphicsUtil implements InstanceState {
  private ComponentDrawContext context;
  private Component comp;
  private ComponentFactory factory;
  private AttributeSet attrs;

  public InstancePainter(ComponentDrawContext context, Component comp) {
    this.context = context;
    this.comp = comp;
    this.factory = null;
    this.attrs = null;
  }

  //
  // helper methods for drawing common elements in components
  //
  public void drawBounds() {
    context.drawBounds(getBounds());
  }

  public void drawClock(int i, Direction dir) {
    if (comp != null)
      context.drawClock(comp, i, dir);
  }

  public void drawClockSymbol(int xpos, int ypos) {
    if (comp != null)
      context.drawClockSymbol(comp, xpos, ypos);
  }

  public void drawDongle(int x, int y) {
    context.drawDongle(x, y);
  }

  public void drawHandle(int x, int y) {
    context.drawHandle(x, y);
  }

  public void drawHandle(Location loc) {
    context.drawHandle(loc);
  }

  public void drawHandles() {
    if (comp != null)
      context.drawHandles(comp);
  }

  public void drawLabel() {
    if (comp != null)
      comp.drawLabel(context);
  }

  public void drawPort(int i) {
    if (comp != null)
      context.drawPin(comp, i);
  }

  public void drawPort(int i, String label, Direction dir) {
    if (comp != null)
      context.drawPin(comp, i, label, dir);
  }

  public void drawPorts() {
    if (comp != null)
      context.drawPins(comp);
  }

  public void drawRectangle(Bounds bds, String label) {
    context.drawRectangle(bds.getX(), bds.getY(), bds.getWidth(),
        bds.getHeight(), label);
  }

  public void drawRectangle(int x, int y, int width, int height, String label) {
    context.drawRectangle(x, y, width, height, label);
  }

  public void drawAndFill(Shape shape) {
    switchToWidth(2);
    Graphics2D g = getGraphics();
    g.setColor(getPalette().SOLID);
    g.fill(shape);
    g.setColor(getPalette().LINE);
    g.draw(shape);
  }

  @Override
  public void fireInvalidated() {
    if (comp != null)
      comp.fireInvalidated();
  }

  public AttributeSet getAttributeSet() {
    return comp == null ? attrs : comp.getAttributeSet();
  }

  public <E> E getAttributeValue(Attribute<E> attr) {
    return getAttributeSet().getValue(attr);
  }
  
  public Bounds getBounds() {
    return comp == null
        ? factory.getOffsetBounds(attrs)
        : comp.getBounds();
  }

  public Bounds getBoundsWithText() {
    return comp == null
        ? factory.getOffsetBounds(attrs, context.getDestination().getGraphics())
        : comp.getBounds(context.getDestination().getGraphics());
  }

  public Circuit getCircuit() {
    return context.getCircuit();
  }

  public InstanceData getData() {
    CircuitState circState = context.getCircuitState();
    if (circState == null || comp == null)
      throw new UnsupportedOperationException("InstancePainter.getData without state");
    return (InstanceData) circState.getData(comp);
  }

  public java.awt.Component getDestination() {
    return context.getDestination();
  }

  public InstanceFactory getFactory() {
    if (comp instanceof InstanceComponent)
      return (InstanceFactory)comp.getFactory();
    else if (factory instanceof InstanceFactory)
      return (InstanceFactory)factory;
    else
      return null;
  }

  public Object getGateShape() {
    return context.getGateShape();
  }

  public Graphics2D getGraphics() {
    return context.getGraphics();
  }

  //
  // methods related to the context of the canvas
  //
  public WireSet getHighlightedWires() {
    return context.getHighlightedWires();
  }

  //
  // methods related to the instance
  //
  public Instance getInstance() {
    return comp instanceof InstanceComponent
        ? ((InstanceComponent)comp).getInstance() :  null;
  }

  public Component getComponent() {
    return comp;
  }

  public Location getLocation() {
    return comp == null ? Location.create(0, 0) : comp.getLocation();
  }

  public Bounds getOffsetBounds() {
    if (comp == null) {
      return factory.getOffsetBounds(attrs);
    } else {
      Location loc = comp.getLocation();
      return comp.getBounds().translate(-loc.getX(), -loc.getY());
    }
  }

  public Value getPortValue(int portIndex) {
    CircuitState s = context.getCircuitState();
    if (comp != null && s != null) {
      return s.getValue(comp.getEnd(portIndex).getLocation());
    } else {
      return Value.UNKNOWN;
    }
  }

  public Color getPortColor(int portIndex) {
    // if (getShowState())
      return getPortValue(portIndex).getColor(getPalette());
    // else
    //  return getPalette().LINE;
  }

  //
  // methods related to the circuit state
  //
  public Project getProject() {
    return context.getCircuitState().getProject();
  }
  
  public boolean isPrintView() {
    return context.isPrintView();
  }

  // deprecated, but maybe keep just for Jar libraries?
  public boolean getShowState() { return context.getShowState(); }
  public boolean shouldDrawColor() { return context.shouldDrawColor(); }

  public int getTickCount() {
    return context.getCircuitState().getPropagator().getTickCount();
  }

  public boolean isCircuitRoot() {
    return !context.getCircuitState().isSubstate();
  }

  public boolean isPortConnected(int index) {
    Circuit circ = context.getCircuit();
    Location loc = comp.getEnd(index).getLocation();
    return circ.isConnected(loc, comp);
  }

  public void setData(InstanceData value) {
    CircuitState circState = context.getCircuitState();
    if (circState == null || comp == null)
      throw new UnsupportedOperationException("setData on InstancePainter");
    circState.setData(comp, value);
  }

  @Override
  public CircuitState createCircuitSubstateFor(Circuit circ) {
    CircuitState circState = context.getCircuitState();
    if (circState == null || comp == null)
      throw new UnsupportedOperationException("createCircuitSubstateFor on InstancePainter");
    return circState.createCircuitSubstateFor(comp, circ);
  }

  void setFactory(ComponentFactory factory, AttributeSet attrs) {
    this.comp = null;
    this.factory = factory;
    this.attrs = attrs;
  }

  public void setComponent(Component comp) {
    this.comp = comp;
    this.factory = null;
    this.attrs = null;
  }

  public void setPort(int portIndex, Value value, int delay) {
    throw new UnsupportedOperationException("setValue on InstancePainter");
  }

  public Palette getPalette() {
    return context.getPalette();
  }

  // Convenience method for translating, rotating, then painting,
  // and also drawing ports and label.
  public interface PainterInterface {
    public void paint(InstancePainter painter);
  }
  public void paintWithLocRotPortLabel(PainterInterface f) {
    Graphics2D g = getGraphics();
    Direction facing = getAttributeValue(StdAttr.FACING);
    Location loc = getLocation();
    g.translate(loc.x, loc.y);
    double rotate = 0.0;
    if (facing != null && facing != Direction.EAST) {
      rotate = -facing.toRadians();
      g.rotate(rotate);
    }
    g.setColor(getPalette().LINE);
    f.paint(this);
    if (rotate != 0.0)
      g.rotate(-rotate);
    g.translate(-loc.x, -loc.y);
    drawPorts();
    drawLabel();
  }


  // non-static versions of everyting in GraphicsUtil
  
  public void drawArrow(int x0, int y0, int x1, int y1,
      int stemWidth, int headLength, int headAngle) {
    drawArrow(getGraphics(), x0, y0, x1, y1, stemWidth, headLength, headAngle);
  }

  public void drawCenteredArc(int x, int y, int r,
      int start, int dist) {
    drawCenteredArc(getGraphics(), x, y, r, start, dist);
  }

  public void drawCenteredText(String text, int x, int y) {
    drawCenteredText(getGraphics(), text, x, y);
  }

  public void drawCenteredText(Font font, String text, int x, int y) {
    drawCenteredText(getGraphics(), font, text, x, y);
  }

  public Rectangle getTextCursor(Font font, String text,
      int x, int y, int pos, int halign, int valign) {
    return getTextCursor(getGraphics(), font, text, x, y, pos, halign, valign);
  }

  public int getTextPosition(Font font, String text,
      int x, int y, int halign, int valign) {
    return getTextPosition(getGraphics(), font, text, x, y, halign, valign);
  }

  public void drawText(Font font, String text, int x,
      int y, int halign, int valign) {
    drawText(getGraphics(), font, text, x, y, halign, valign);
  }

  public void drawText(String text, int x, int y,
      int halign, int valign) {
    drawText(getGraphics(), text, x, y, halign, valign);
  }

  public Rectangle getTextBounds(String text, int x,
      int y, int halign, int valign) {
    return getTextBounds(getGraphics(), text, x, y, halign, valign);
  }

  public Rectangle getTextBounds(Font font, String text,
      int x, int y, int halign, int valign) {
    return getTextBounds(getGraphics(), font, text, x, y, halign, valign);
  }

  public void outlineText(String text, int x, int y, Color fg, Color bg) {
    outlineText(getGraphics(), text, x, y, fg, bg);
  }

  public Rectangle getTextCursor(Font font, String text[],
      int x, int y, int pos, int halign, int valign) {
    return getTextCursor(getGraphics(), font, text, x, y, pos, halign, valign);
  }

  public int getTextPosition(Font font, String text[],
      int x, int y, int halign, int valign) {
    return getTextPosition(getGraphics(), font, text, x, y, halign, valign);
  }

  public void drawText(Font font, String text[], int x,
      int y, int halign, int valign) {
    drawText(getGraphics(), font, text, x, y, halign, valign);
  }

  public void drawText(String text[], int x, int y,
      int halign, int valign) {
    drawText(getGraphics(), text, x, y, halign, valign);
  }

  public Rectangle getTextBounds(String text[], int x,
      int y, int halign, int valign) {
    return getTextBounds(getGraphics(), text, x, y, halign, valign);
  }

  public Rectangle getTextBounds(Font font, String text[],
      int x, int y, int halign, int valign) {
    return getTextBounds(getGraphics(), font, text, x, y, halign, valign);
  }

  public void switchToWidth(int width) {
    switchToWidth(getGraphics(), width);
  }
}
