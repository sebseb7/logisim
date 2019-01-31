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
import static com.cburch.logisim.gui.main.Strings.S;

import com.cburch.logisim.circuit.Simulator;

class TickCounter implements Simulator.Listener {
  private static final int N = 10;
  private static final long T = 500; // msec

  private final long[] bucketTime = new long[N];
  private final long[] bucketTick = new long[N];
  private int bucketIndex;
  private volatile int bucketCount;

  private long tick;
  private double tickFrequency;

  private volatile double rate = -1;

  public TickCounter() { }

  public void clear() {
    bucketCount = 0;
    rate = -1;
  }

  public String getTickRate() {
    double r = rate / 2; // report full-cycle rate instead of half-cycle rate
    if (r <= 0)
      return "";
    else if (r >= 1000.0)
      return S.fmt("tickRateKHz", roundString(r / 1000.0));
    else
      return S.fmt("tickRateHz", roundString(r));
  }

  private String roundString(double val) {
    // round so we have only three significant digits
    int i = 0; // invariant: a = 10^i
    double a = 1.0; // invariant: a * bv == val, a is power of 10
    double bv = val;
    if (bv >= 1000) {
      while (bv >= 1000) {
        i++;
        a *= 10;
        bv /= 10;
      }
    } else {
      while (bv < 100) {
        i--;
        a /= 10;
        bv *= 10;
      }
    }

    // Examples:
    // 2.34: i = -2, a = .2, b = 234
    // 20.1: i = -1, a = .1, b = 201

    if (i >= 0) // nothing after decimal point
      return "" + (int) Math.round(a * Math.round(bv));
    else // keep some after decimal point
      return String.format("%." + (-i) + "f", Double.valueOf(a * bv));
  }
  
  public void updateSimulator(Simulator.Event e) {
    Simulator sim = e.getSource();
    if (!sim.isAutoTicking())
      clear();

  }

  @Override
  public void simulatorStateChanged(Simulator.Event e) {
    updateSimulator(e);
  }

  @Override
  public void simulatorReset(Simulator.Event e) {
    updateSimulator(e);
  }

  @Override
  public void propagationCompleted(Simulator.Event evt) {
    if (!evt.didTick())
      return;

    Simulator sim = evt.getSource();
    if (!sim.isAutoTicking()) {
      clear();
      return;
    }

    double freq = sim.getTickFrequency();
    if (freq != tickFrequency) {
      clear();
      tickFrequency = freq;
    }

    int n = bucketCount;
    int s = bucketIndex;
    int e = (s + n - 1) % N;
    long t = System.currentTimeMillis();

    if (n > 0 && t < bucketTime[e] + T) {
      bucketTick[e] = ++tick;
      return;
    }

    if (n < N) {
      e = (e + 1) % N;
      bucketTime[e] = t;
      bucketTick[e] = ++tick;
      bucketCount++;
      n++;
    } else {
      e = (e + 1) % N;
      bucketTime[e] = t;
      bucketTick[e] = ++tick;
      s = bucketIndex = (bucketIndex + 1) % N;
    }

    if (n >= 3) {
      long ts = bucketTime[(s+1)%N];
      long te = t;
      double ticks = tick - (bucketTick[s] + 1);
      rate = 1000.0 * ticks / (te - ts);
    }
  }
}
