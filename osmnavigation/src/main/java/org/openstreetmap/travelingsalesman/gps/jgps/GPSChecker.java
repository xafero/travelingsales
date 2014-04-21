/**
 * GPSChecker.java
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
package org.openstreetmap.travelingsalesman.gps.jgps;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.travelingsalesman.nativetools.OsType;
import org.openstreetmap.travelingsalesman.nativetools.SystemInformation;
import org.openstreetmap.travelingsalesman.nativetools.SystemUtils;

/**
 * helper class for rxtx library, ports and gps connection checking.
 * Used by {@link org.openstreetmap.travelingsalesman.gps.JGPSProvider}.
 *
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov </a>
 */
public final class GPSChecker {

    /**
     * System information utility class.
     */
    private static SystemInformation sysinfo = SystemInformation.getInstance();

    /**
     * Was RXTX installed via a hack?
     */
    private static boolean rxtxHackInstalled = false;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(GPSChecker.class.getName());

    /**
     * Utility-classes have no default-constructor.
     *
     */
    private GPSChecker() {
    }

    /**
     * check for correct rxtx lib installation
     * it's platform dependents.
     * @return true if rxtx library installed
     */
    public static boolean isRxtxInstalled() {
        if (rxtxHackInstalled) {
            return true;
        }
        final String jrePath = SystemInformation.jrePath();
        if (sysinfo.os() == OsType.WINDOWS) {
            File rxtx = new File(jrePath + File.separator + "bin"
                    + File.separator + "rxtxSerial.dll");
            return rxtx.exists();
        } else if (sysinfo.os() == OsType.LINUX) {
            File rxtx = new File(jrePath + File.separator + "lib"
                    + File.separator + System.getProperty("os.arch") + File.separator
                    + "librxtxSerial.so");
            return rxtx.exists();
        } else {
            return false;
        }
    }

    /**
     * install rxtx library it's just copy necessary .dll/.so to java.library.path.
     *
     * @return true if the installation was successful.
     */
    public static boolean installRxtx() {
        final String jrePath = SystemInformation.jrePath();
        final String osArch = System.getProperty("os.arch");
        File outFile;
        File inFile;
        switch (sysinfo.os()) {
        case WINDOWS:
            if (!System.getProperty("java.vm.vendor").equals("Sun Microsystems Inc.")) {
                LOG.severe("The JavaComm-API is not installed and we can only do that for the Sun JDK.");
                return false;
            }
            outFile = new File(jrePath + File.separator + "bin" + File.separator
                    + "rxtxSerial.dll");
            inFile = new File(".." + File.separator + "osmnavigation" + File.separator + "lib"
                    + File.separator + "native" + File.separator + "Windows" + File.separator
                    + "rxtxSerial.dll");
            if (inFile.exists()) {
                if (!SystemUtils.copyFile(inFile, outFile)) {
                    // fallback if we have no write-access
                       try {
                           LOG.log(Level.WARNING, "Cannot install native serial-port library system-wide. Trying a local installation");
                           outFile = File.createTempFile("osmnavigation_GPXChecker", "");
                           if (outFile.exists()) {
                               outFile.delete();
                           }
                           outFile.mkdirs();
                           outFile = new File(outFile, "librxtxSerial.dll");
                           SystemUtils.copyFile(inFile, outFile);
                           loadLibraryHack(outFile);
                       } catch (IOException e) {
                           LOG.log(Level.SEVERE, "Cannot copy native library to temp-folder", e);
                       }
                   }
            } else {
                LOG.info("GPSChecker.installRxtx() - missing file  \"" + inFile.getAbsolutePath()
                        + "\" - probably we are running TravelingSalesman from jar");
                if (!SystemUtils.copyFileFromJar("org.openstreetmap.travelingsalesman.gps.jgps.GPSChecker", "/native/Windows/rxtxSerial.dll", outFile)) {
                    // fallback if we have no write-access
                    try {
                        LOG.log(Level.WARNING, "Cannot install native serial-port library system-wide. Trying a local installation");
                        outFile = File.createTempFile("osmnavigation_GPXChecker", null);
                        if (outFile.exists()) {
                            outFile.delete();
                        }
                        outFile.mkdirs();
                        outFile = new File(outFile, "rxtxSerial.dll");
                        SystemUtils.copyFileFromJar("org.openstreetmap.travelingsalesman.gps.jgps.GPSChecker", "/native/Windows/rxtxSerial.dll", outFile);
                        loadLibraryHack(outFile);
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Cannot copy native library to temp-folder", e);
                    }
                }
            }
            break;
        // probably we are using osmnavigation as library
        case LINUX:
            if (!System.getProperty("java.vm.vendor").equals("Sun Microsystems Inc.")) {
                LOG.severe("The JavaComm-API is not installed and we can only do that for the Sun JDK.");
                return false;
            }
            outFile = new File(jrePath + File.separator + "lib" + File.separator + osArch
                    + File.separator + "librxtxSerial.so");
            inFile = new File(".." + File.separator + "osmnavigation" + File.separator + "lib"
                    + File.separator + "native" + File.separator + "Linux" + File.separator
                    + convertLinuxArchToInstallationPath(osArch) + File.separator
                    + "librxtxSerial.so");
            if (inFile.exists()) {
                if (!SystemUtils.copyFile(inFile, outFile)) {
                 // fallback if we have no write-access
                    try {
                        LOG.log(Level.WARNING, "Cannot install native serial-port library system-wide. Trying a local installation");
                        outFile = File.createTempFile("osmnavigation_GPXChecker", "");
                        if (outFile.exists()) {
                            outFile.delete();
                        }
                        outFile.mkdirs();
                        outFile = new File(outFile, "librxtxSerial.so");
                        SystemUtils.copyFile(inFile, outFile);
                        loadLibraryHack(outFile);
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Cannot copy native library to temp-folder", e);
                    }
                }
            } else {
                LOG.info("GPSChecker.installRxtx() - missing file  \"" + inFile.getAbsolutePath()
                        + "\" - probably we are running TravelingSalesman from jar");
                if (!SystemUtils.copyFileFromJar("org.openstreetmap.travelingsalesman.gps.jgps.GPSChecker", "/native/Linux/" + convertLinuxArchToInstallationPath(osArch)
                        + "librxtxSerial.so", outFile)) {
                    // fallback if we have no write-access
                    try {
                        LOG.log(Level.WARNING, "Cannot install native serial-port library system-wide. Trying a local installation");
                        outFile = File.createTempFile("osmnavigation_GPXChecker", "");
                        if (outFile.exists()) {
                            outFile.delete();
                        }
                        outFile.mkdirs();
                        outFile = new File(outFile, "librxtxSerial.so");
                        SystemUtils.copyFileFromJar("org.openstreetmap.travelingsalesman.gps.jgps.GPSChecker",
                                "/native/Linux/" + convertLinuxArchToInstallationPath(osArch)
                                + "librxtxSerial.so", outFile);
                        loadLibraryHack(outFile);
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Cannot copy native library to temp-folder", e);
                    }
                }
            }
            break;
        case UNKNOW:
        default:
            LOG.info("GPSChecker.installRxtx() - unknown operating-system \""
                    + System.getProperty("os.name") + "\" - cannot install");
            break;
        }
        return isRxtxInstalled();
    }

    /**
     * If we canot install rxtxSerial in thr JDK-directory,
     * apply a dirty hack to use a local copy.
     * @param outFile the libRxtxSerial.so/rxtxSerial.dll -file
     */
    private static void loadLibraryHack(final File outFile) {
        try {
            System.load(outFile.getAbsolutePath());
            Class<ClassLoader> c = ClassLoader.class;
            Field field = c.getDeclaredField("sys_paths");
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            Object original = field.get(c);
            // reset to null, so we can change it
            try {
                field.set(c, null);
                System.setProperty("java.library.path", outFile.getParentFile().getAbsolutePath());
                System.loadLibrary("rxtxSerial");
                rxtxHackInstalled = true;
            } finally {
                field.set(c, original);
            }
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Cannot apply hack to load native serial-port"
                    + " library without having thee access-rights to install it"
                    + " system-wide"
                    + ". Temp-dir was "
                    + outFile.getParentFile().getAbsolutePath(), e);
        }
    }

    /**
     * Converts linux system architecture to librxtxSerial.so source installation path
     * e.g. "i386" (32 bit) to "i686-unknown-linux-gnu"
     *
     * @param osArch Linux architecture
     * @return part of installation path for librxtxSerial.so
     */
    private static String convertLinuxArchToInstallationPath(final String osArch) {
        if (osArch.equalsIgnoreCase("i386") || osArch.equalsIgnoreCase("i686")) {
            return "i686-unknown-linux-gnu";
        }
        if (osArch.equalsIgnoreCase("amd64")) {
            return "x86_64-unknown-linux-gnu";
        }
        if (osArch.equalsIgnoreCase("ia64")) {
            return "ia64-unkown-linux-gnu";
        }
        return "i686-unknown-linux-gnu";
    }

    /**
     * check serial port for existing and could be opened.
     * @param portString a string with only a port-name or
     *        a port-name followed by ","
     * @return true if the port is in the list of all ports
     */
    public static boolean portIsExist(final String portString) {
        StringTokenizer st = new StringTokenizer(portString, ",");
        String port = st.nextToken();

        String[] ports = RXTXConnection.getPortNames();
        for (String p : ports) {
            if (p.equalsIgnoreCase(port)) {
                return true;
            }
        }
        return false;
    }
}
