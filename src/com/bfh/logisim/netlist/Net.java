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

package com.bfh.logisim.netlist;

import java.util.ArrayList;
import java.util.HashSet;

import com.cburch.logisim.data.Location;

public class Net {

  static class ConnectionPoints extends ArrayList<NetlistComponent.ConnectionPoint> { }

	public int width = 0; // number of bits in this network, initially unknown
	private HashSet<Location> points = new HashSet<>(); // points this net touches
	private boolean forcedRoot  = false;

	private Net parent = null;

	private ArrayList<Byte> inheritedBits = new ArrayList<>();

  // Data for each bit of this network
	private ArrayList<ConnectionPoints> sources = new ArrayList<>();
	private ArrayList<ConnectionPoints> sinks = new ArrayList<>();
	private ArrayList<ConnectionPoints> sourceNets = new ArrayList<>();
	private ArrayList<ConnectionPoints> sinkNets = new ArrayList<>();

	public Net(HashSet<Location> locs) {
    points.addAll(locs);
  }

	public void add(HashSet<Location> locs) {
    points.addAll(locs); }
	}

	public boolean AddParentBit(byte BitID) {
		if (BitID < 0)
			return false;
		inheritedBits.add(BitID);
		return true;
	}

	public boolean addSink(int bitIndex, ConnectionPoint sink) {
		if ((bitIndex < 0) || (bitIndex >= sinks.size()))
			return false;
		sinks.get(bitIndex).add(sink);
		return true;
	}

	public boolean addSinkNet(int bitIndex, ConnectionPoint net) {
		if ((bitIndex < 0) || (bitIndex >= sinkNets.size()))
			return false;
		sinkNets.get(bitIndex).add(net);
		return true;
	}

	public boolean addSource(int bitIndex, ConnectionPoint source) {
		if ((bitIndex < 0) || (bitIndex >= sources.size()))
			return false;
		sources.get(bitIndex).add(source);
		return true;
	}

	public boolean addSourceNet(int bitIndex, ConnectionPoint net) {
		if ((bitIndex < 0) || (bitIndex >= sourceNets.size()))
			return false;
		sourceNets.get(bitIndex).add(net);
		return true;
	}

	public void FinalCleanup() {
		points.clear();
		inheritedBits.clear();
	}

	public void ForceRootNet() {
		parent = null;
		forcedRoot = true;
		inheritedBits.clear();
	}

	public byte getBit(byte bit) {
		if ((bit < 0) || (bit >= inheritedBits.size()) || isRootNet())
			return -1;
		return inheritedBits.get(bit);
	}

	public Net getParent() {
		return parent;
	}

	public ArrayList<ConnectionPoint> GetSinkNets(int bitIndex) {
		if ((bitIndex < 0) || (bitIndex >= sinkNets.size()))
			return new ArrayList<ConnectionPoint>();
		return sinkNets.get(bitIndex);
	}

	public ArrayList<ConnectionPoint> GetSourceNets(int bitIndex) {
		if ((bitIndex < 0) || (bitIndex >= sourceNets.size()))
			return new ArrayList<ConnectionPoint>();
		return sourceNets.get(bitIndex);
	}

	public boolean hasBitSinks(int bitid) {
		if (bitid < 0 || bitid >= sinks.size())
			return false;
		return sinks.get(bitid).size() > 0;
	}

	public boolean hasBitSource(int bitid) {
		if (bitid < 0 || bitid >= sources.size())
			return false;
		return sources.get(bitid).size() > 0;
	}

	public boolean hasShortCircuit() {
		for (int i = 0; i < width; i++)
			if (sources.get(i).size() > 1)
        return true;
		return false;
	}

	public boolean hasSinks() {
		for (int i = 0; i < width; i++)
			if (sinks.get(i).size() > 0)
        return true;
		return false;
	}

	public boolean hasSource() {
		for (int i = 0; i < width; i++)
			if (sources.get(i).size() > 0)
        return true;
		return false;
	}

	public void InitializeSourceSinks() {
		sources.clear();
		sinks.clear();
		sourceNets.clear();
		sinkNets.clear();
		for (int i = 0; i < width; i++) {
			sources.add(new ConnectionPoints());
			sinks.add(new ConnectionPoints());
			sourceNets.add(new ConnectionPoints());
			sinkNets.add(new ConnectionPoints());
		}
	}

  public int bitWidth() { return width; }
	public boolean isBus() { return width > 1; }
  public void setBitWidth(int w) { width = w; }; // clear?

	public boolean isEmpty() { return points.isEmpty(); }
	public HashSet<Location> getPoints() { return points; }
	public boolean contains(Location point) { return points.contains(point); }

	public boolean IsForcedRootNet() {
		return forcedRoot;
	}

	public boolean isRootNet() {
		return (parent == null) || forcedRoot;
	}

	public boolean setParent(Net newParent) {
		if (forcedRoot || newParent == null  || parent != null)
			return false;
		parent = newParent;
		return true;
	}

}
