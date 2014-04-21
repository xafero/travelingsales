/**
 * SystemInformation.java
 * (c) 2008 by <a href="mailto:oleg.chubaryov@mail.ru">Oleg Chubaryov</a>
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 *
 *  osmnavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  osmnavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with osmnavigation.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***********************************
 * Editing this file:
 *  -For consistent code-quality this file should be checked with the
 *   checkstyle-ruleset enclosed in this project.
 *  -After the design of this file has settled it should get it's own
 *   JUnit-Test that shall be executed regularly. It is best to write
 *   the test-case BEFORE writing this class and to run it on every build
 *   as a regression-test.
 */
package org.openstreetmap.travelingsalesman.nativetools;

import java.io.File;

/**
 * Class for gets system information.
 * It needs by system dependent code.
 * Create to reduce System... getProperties calls.
 * Singleton.
 *
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov </a>
 */
public final class SystemInformation {

    /**
     * Operation system type.
     */
    private static OsType os = null;

    /**
     * The singleton-instance.
     */
    private static SystemInformation instance;

    /**
     * @return The singleton-instance.
     */
    public static SystemInformation getInstance() {
        if (instance == null) {
            instance = new SystemInformation();
        }
        return instance;
    }

    /**
     * utility-classes have no public constructor.
     */
    private SystemInformation() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            os = OsType.WINDOWS;
        } else if (System.getProperty("os.name").startsWith("Linux")) {
            os = OsType.LINUX;
        } else {
            os = OsType.UNKNOW;
        }
    }

    /**
     * @return Operation system type at this computer.
     */
    public OsType os() {
        return os;
    }

    /**
     * It don't depends from JAVA_HOME variable.
     * JAVA_HOME environment variable may be point to JDK or JRE directory.
     * @return JRE directory always
     */
    public static String jrePath() {
      SystemInformation sysinfo = SystemInformation.getInstance();
      //String javaHome = System.getenv("JAVA_HOME");
      String javaHome = System.getProperty("java.home");
      if (javaHome == null) {
          javaHome = System.getenv("java.home");
      }
      //File javaHomeDir = new File(JAVA_HOME);
      // Find compiler file to see JDK or JRE is using
      String ext = null;
      if (sysinfo.os() == OsType.WINDOWS) {
          ext = ".exe";
      } else {
          ext = "";
      }
      String javacPath = File.separator + "bin" + File.separator + "javac" + ext;
      File javacFile = new File(javaHome + javacPath);
      if (javacFile.exists()) {
          // JAVA_HOME is set to JDK
          return javaHome + File.separator + "jre";
      } else {
          return javaHome;
      }
    }

}
