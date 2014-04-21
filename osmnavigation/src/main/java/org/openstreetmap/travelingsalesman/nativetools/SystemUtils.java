/**
 * 
 */
package org.openstreetmap.travelingsalesman.nativetools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class contains system dependent functions and file operations.
 *
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov </a>
 */
public final class SystemUtils {

    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(SystemUtils.class.getName());

    /**
     * Utility-classes have no default-constructor.
     *
     */
    private SystemUtils() {
    }

    /**
     * Copy inFile to outFile using FileChannels.
     * @param inFile input file
     * @param outFile output file
     * @return tue if the copy-operation was a success
     */
    public static boolean copyFile(final File inFile, final File outFile) {
        try {
            FileChannel ic = new FileInputStream(inFile).getChannel();
            FileChannel oc = new FileOutputStream(outFile).getChannel();
            ic.transferTo(0, ic.size(), oc);
            ic.close();
            oc.close();
            return true;
        } catch (IOException e) {
            SystemUtils.LOG.log(Level.INFO, "SystemUtils.copyFile() Exception while copy file "
                    + inFile.getAbsolutePath() + " to " + outFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Buffer-size used inside {@link #copyFileFromJar(String, String, File)}.
     */
    private static final int BUFFER_SIZE = 2048;

    /**
     * Copy file as stream for unpacking from jar.
     *
     * @param className
     *            class name for given resource
     * @param inFile
     *            input file, path in the jar
     * @param outFile
     *            output file
     * @return true if it worked
     */
    public static boolean copyFileFromJar(final String className, final String inFile,
            final File outFile) {
        try {
            Class<?> thisClass = Class.forName(className);
            InputStream is = thisClass.getResourceAsStream(inFile);
            FileOutputStream of = new FileOutputStream(outFile);
            int n = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((n = is.read(buffer)) != -1) {
                of.write(buffer, 0, n);
            }
            of.close();
            is.close();
            return true;
        } catch (ClassNotFoundException e) {
            SystemUtils.LOG.log(Level.WARNING,
                    "SystemUtils.copyFileFromJar() ClassNotFoundException while copy file " + inFile + " to "
                            + outFile.getAbsolutePath(), e);
            return false;
        } catch (IOException e) {
            SystemUtils.LOG.log(Level.WARNING,
                    "SystemUtils.copyFileFromJar() IOException while copy file " + inFile + " to "
                            + outFile.getAbsolutePath(), e);
            return false;
        } catch (Exception e) {
            SystemUtils.LOG.log(Level.WARNING,
                    "SystemUtils.copyFileFromJar() Exception while copy file " + inFile + " to "
                            + outFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Update system time.
     * @param date New System Date and Time
     */
    public static void updateSystemTime(final Date date) {
        Date oldDate = new Date(); // is saved only for LOG message output
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy");
        String actDate = fmt.format(date);
        fmt.applyPattern("HH:mm:ss");
        String actTime = fmt.format(date);
        try {
            switch (SystemInformation.getInstance().os()) {
            case WINDOWS:
                Runtime rt = Runtime.getRuntime();
                rt.exec("cmd /C date " + actDate);
                rt.exec("cmd /C time " + actTime);
                break;
            case LINUX:
                // with root permits only
                fmt.applyPattern("MM/dd/yyyy HH:mm:ss");
                actDate = fmt.format(date);
                // String cmdDate = "date -u -s'" + actDate + "' +'%D %T'";
                // execCommand(cmdDate);
                break;
            default:
                break;
            }
            fmt.applyPattern("dd-MM-yyyy HH:mm:ss");
            SystemUtils.LOG.log(Level.INFO,
                    "SystemUtils.updateSystemTime() System time was updated from "
                            + fmt.format(oldDate) + " to " + fmt.format(date));
        } catch (IOException e) {
            // cannot run commands
            SystemUtils.LOG.log(Level.SEVERE,
                    "SystemUtils.updateSystemTime() Exception while updating sys time.", e);
        }
    }

}
