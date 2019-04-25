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

package com.cburch.logisim.util;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Scanner;

public class Debug {

  static final String LOG_FILE = System.getProperty("user.home") + "/logisim_debug.log";

  static DebugThread debugThread;
  static Scanner stdin;
  static PrintStream log;

  public static void enable() {
    if (debugThread != null)
      return;

    try {
      log = new PrintStream(new TeeOutputStream(
            new FileOutputStream(FileDescriptor.out),
            new FileOutputStream(LOG_FILE, true)));
      System.setOut(log);
      System.setErr(log);
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.printf("\n\n===== Logisim debug log: %s =====\n", new Date());

    stdin = new Scanner(System.in);

    debugThread = new DebugThread();
    debugThread.start();

    if (log != null)
      Runtime.getRuntime().addShutdownHook(new Thread(() -> log.flush()));
  }

  static void doCmd(String cmd) {
    System.out.printf("got %s\n", cmd);
  }

  private static class DebugThread extends UniquelyNamedThread {
    public DebugThread() {
      super("DebugThread");
    }
    @Override
    public void run() {
      while (stdin.hasNext()) {
        try {
          String cmd = stdin.next();
          doCmd(cmd);
          stdin.nextLine();
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
    }
  }

  static class TeeOutputStream extends OutputStream {
    private final OutputStream one;
    private final OutputStream two;

    public TeeOutputStream(OutputStream a, OutputStream b) {
      one = a;
      two = b;
    }

    @Override
    public void write(int b) throws IOException {
      one.write(b);
      two.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      one.write(b);
      two.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      one.write(b, off, len);
      two.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      one.flush();
      two.flush();
    }

    @Override
    public void close() throws IOException {
      try {
        one.close();
      } finally {
        two.close();
      }
    }
  }

}
