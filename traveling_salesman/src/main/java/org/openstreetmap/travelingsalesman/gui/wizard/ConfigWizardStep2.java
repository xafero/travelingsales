/**
 * 
 */
package org.openstreetmap.travelingsalesman.gui.wizard;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.travelingsalesman.gps.DummyGPSProvider;
import org.openstreetmap.travelingsalesman.gps.GpsDProvider;
import org.openstreetmap.travelingsalesman.gps.JGPSProvider;
import org.openstreetmap.travelingsalesman.gps.gpsdemulation.MiniGPSD;
import org.openstreetmap.travelingsalesman.gps.jgps.GPSChecker;
import org.openstreetmap.travelingsalesman.gps.jgps.GpsFinder;
import org.openstreetmap.travelingsalesman.gps.jgps.RXTXConnection;

/**
 * Second step of the wizard. Configure the GPS.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ConfigWizardStep2 extends JPanel implements IWizardStep {


    /**
     * for serialization.
     */
    private static final long serialVersionUID = 2075180475249726967L;

    /**
     *  logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(ConfigWizardStep2.class.getName());

    /**
     * Radio-button to choose for "no GPS".
     */
    private JRadioButton myRadioNoGPS = new JRadioButton(ConfigWizard.RESOURCE.getString("configwizard.step2.nogps"));

    /**
     * Radio-button to choose for "GPSD".
     */
    private JRadioButton myRadioGPSD = new JRadioButton(ConfigWizard.RESOURCE.getString("configwizard.step2.gpsd"));

    /**
     * The port to use when connecting to GPSD.
     */
    private JTextField myGPSDPort = new JTextField("" + GpsDProvider.DEFAULTPORT);

    /**
     * Radio-button to choose for "JGPS".
     */
    private JRadioButton myRadioJGPS = new JRadioButton(ConfigWizard.RESOURCE.getString("configwizard.step2.serial"));
    /**
     * Button to auto-detect the serial-port for JGPS.
     */
    private JButton myAutoDetectSerialPort = new JButton(ConfigWizard.RESOURCE.getString("configwizard.step2.autodetect"));

    /**
     * ComboBox to select the serial-port for JGPS.
     */
    private JComboBox mySerialPort = new JComboBox();
    /**
     * ComboBox to select the serial-port-speed for JGPS.
     */
    private JComboBox mySerialSpeed = new JComboBox();

    /**
     * Checkbox to enable gpsd-emulation.
     */
    private JCheckBox myGPSDemulation = new JCheckBox(ConfigWizard.RESOURCE.getString("configwizard.step2.emulategpsd"));

    /**
     * The ButtonGroup our RadioButtons are in.
     */
    private ButtonGroup myButtonGroup;

    /**
     * Create this wizard-panel.
     */
    public ConfigWizardStep2() {
        this.add(this.myRadioGPSD);
        final int gridCols = 3;

        JPanel gpsdPortPanel = new JPanel();
        gpsdPortPanel.setLayout(new GridLayout(1, gridCols));
        gpsdPortPanel.add(new JPanel());
        gpsdPortPanel.add(new JLabel("GPSD port"));
        gpsdPortPanel.add(this.myGPSDPort);
        this.add(gpsdPortPanel);

        this.add(this.myRadioJGPS);
        JPanel serialPortPanel = new JPanel();
        serialPortPanel.setLayout(new GridLayout(0, gridCols));
        serialPortPanel.add(new JPanel());
        serialPortPanel.add(new JPanel());
        serialPortPanel.add(myAutoDetectSerialPort);
        serialPortPanel.add(new JPanel());
        serialPortPanel.add(new JLabel(ConfigWizard.RESOURCE.getString("configwizard.step2.serial.port")));
        serialPortPanel.add(this.mySerialPort);
        serialPortPanel.add(new JPanel());
        serialPortPanel.add(new JLabel(ConfigWizard.RESOURCE.getString("configwizard.step2.serial.speed")));
        serialPortPanel.add(this.mySerialSpeed);
        this.add(serialPortPanel);

        this.add(this.myRadioNoGPS);

        this.add(this.myGPSDemulation);

        this.setLayout(new GridLayout(this.getComponentCount(), 1));
        myButtonGroup = new ButtonGroup();
        myButtonGroup.add(this.myRadioGPSD);
        myButtonGroup.add(this.myRadioJGPS);
        myButtonGroup.add(this.myRadioNoGPS);

        this.myAutoDetectSerialPort.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent aE) {
                autoDetectPort();
            }
        });
        this.mySerialSpeed.setModel(new DefaultComboBoxModel(
                new String[] {"115200", "57600", "9600", "4800", "2400"}));
        try {
            if (!GPSChecker.isRxtxInstalled()) {
//              LOG.log(Level.INFO,
//                      "RxTx serial port library is not installed."
//                      + " Installation started...");
              GPSChecker.installRxtx();
          }
            String[] portNames = RXTXConnection.getPortNames();
            this.mySerialPort.setModel(new DefaultComboBoxModel(portNames));
        } catch (Throwable x) {
            LOG.log(Level.WARNING, "Cannot enumerate the serial-ports", x);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReady() {
        // JGPS-port
        if (this.mySerialPort.getSelectedItem() != null) {
            Settings.getInstance().put("JGPSProvider.port",
                    this.mySerialPort.getSelectedItem().toString()
                    + ","
                    + this.mySerialSpeed.getSelectedItem().toString()
            );
        }

        // GPSD-port
        int gpsdPort = GpsDProvider.DEFAULTPORT;
        try {
            gpsdPort = Integer.parseInt(myGPSDPort.getText());
        } catch (NumberFormatException e) {
            LOG.info("Ignoring illegal user-supplied GPSD-port " + myGPSDPort.getText());
        }
        Settings.getInstance().put("gpsd.port", "" + gpsdPort);

        // GPS-emulation-port
        if (this.myGPSDemulation.isSelected()) {
            int emulationPort = GpsDProvider.DEFAULTPORT;
            if (this.myButtonGroup.isSelected(myRadioGPSD.getModel()) && gpsdPort == emulationPort) {
                emulationPort++;
            }
            Settings.getInstance().put(MiniGPSD.SETTINGS_GPSDEMULATION_PORT, "" + emulationPort);
        } else {
            Settings.getInstance().put(MiniGPSD.SETTINGS_GPSDEMULATION_PORT, "-1");
        }

        // GPS-plugin
        if (this.myButtonGroup.isSelected(myRadioNoGPS.getModel())) {
            Settings.getInstance().put("plugin.useImpl.IGPSProvider", DummyGPSProvider.class.getName());
            return true;
        } else if (this.myButtonGroup.isSelected(myRadioGPSD.getModel())) {
            Settings.getInstance().put("plugin.useImpl.IGPSProvider", GpsDProvider.class.getName());
            return true;
        } else if (this.myButtonGroup.isSelected(myRadioJGPS.getModel())) {
            Settings.getInstance().put("plugin.useImpl.IGPSProvider", JGPSProvider.class.getName());
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public JComponent getComponent() {
        return this;
    }

    /**
     * Auto-detect the serial-port and speed.
     */
    private void autoDetectPort() {
        try {
            String port = GpsFinder.findGpsPort();
            if (port != null) {
                String[] split = port.split(",");

                // select the port found
                boolean foundPort = false;
                ComboBoxModel model = mySerialPort.getModel();
                for (int i = 0; i < model.getSize(); i++) {
                    if (split[0].equalsIgnoreCase(model.getElementAt(i).toString())) {
                        foundPort = true;
                        mySerialPort.setSelectedIndex(i);
                        break;
                    }
                }
                if (!foundPort) {
                    ((DefaultComboBoxModel) model).addElement(split[0]);
                    mySerialPort.setSelectedItem(split[0]);
                }
                if (split.length > 1) {
                    boolean foundSpeed = false;
                    model = mySerialSpeed.getModel();
                    for (int i = 0; i < model.getSize(); i++) {
                        if (split[1].equalsIgnoreCase(model.getElementAt(i).toString())) {
                            foundSpeed = true;
                            mySerialSpeed.setSelectedIndex(i);
                            break;
                        }
                    }
                    if (!foundSpeed) {
                        ((DefaultComboBoxModel) model).addElement(split[1]);
                        mySerialSpeed.setSelectedItem(split[1]);
                    }
                }
            }
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Error detecting GPS-port", e);
        }
    }

}
