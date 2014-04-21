package org.openstreetmap.travelingsalesman.gui.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

import com.l2fprod.common.util.ResourceManager;

/**
 * Wizard to import a first map, configure the GPS-device
 * and learn about the user-interface.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ConfigWizard extends JDialog {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Out resource-manager for i18n.
     */
    public static final ResourceManager RESOURCE = ResourceManager.get(MainFrame.class);

    /**
     * Label that shows the current step.
     */
    private JLabel myCurrentStepLabel = new JLabel(RESOURCE.getString("configwizard.step1.title"));

    /**
     * The panel we are currently showing.
     */
    private IWizardStep myCurrentPanel = new ConfigWizardStep1();

    /**
     * The button to go to the next step/finish the wizard.
     */
    private JButton myNextStepButton = new JButton(RESOURCE.getString("configwizard.buttons.next"));

    /**
     * Our map.
     */
    private IDataSet myDataSet;

    /**
     * Start the wizard.
     */
    public ConfigWizard() {
        // allow skipping the wizard by closing the dialog.
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout());
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BorderLayout());
        buttonsPanel.add(this.myNextStepButton, BorderLayout.EAST);

        final float fontSize = 20f;
        this.myCurrentStepLabel.setBackground(Color.BLUE.brighter().brighter());
        this.myCurrentStepLabel.setForeground(Color.WHITE);
        this.myCurrentStepLabel.setOpaque(true);
        this.myCurrentStepLabel.setFont(this.myCurrentStepLabel.getFont().deriveFont(fontSize));

        JPanel spacer = new JPanel();
        spacer.setBackground(Color.BLUE.brighter().brighter());
        final int spacerSize = 100;
        spacer.setPreferredSize(new Dimension(spacerSize, spacerSize));

        getContentPane().add(spacer, BorderLayout.WEST);
        getContentPane().add(this.myCurrentStepLabel, BorderLayout.NORTH);
        getContentPane().add(this.myCurrentPanel.getComponent(), BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

        this.myCurrentPanel.getComponent().setBorder(BorderFactory.createEtchedBorder());
        setTitle("Configuration Wizard");

        this.myNextStepButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent aE) {
                nextStep();
            }
        });
    }

    /**
     * Go to the next step or close this dialog.
     */
    private void nextStep() {
        if (this.myCurrentPanel.isReady()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                if (this.myCurrentPanel instanceof ConfigWizardStep1) {
                    this.myDataSet = ((ConfigWizardStep1) this.myCurrentPanel).getOsmData();
                    getContentPane().remove(this.myCurrentPanel.getComponent());
                    this.myCurrentPanel = new ConfigWizardStep2();
                    getContentPane().add(this.myCurrentPanel.getComponent());
                    this.myCurrentPanel.getComponent().setBorder(BorderFactory.createEtchedBorder());
                    this.myCurrentStepLabel.setText(RESOURCE.getString("configwizard.step2.title"));
                    this.pack();
                } else if (this.myCurrentPanel instanceof ConfigWizardStep2) {
                    getContentPane().remove(this.myCurrentPanel.getComponent());
                    this.myCurrentPanel = new ConfigWizardStep3();
                    getContentPane().add(this.myCurrentPanel.getComponent());
                    this.myCurrentPanel.getComponent().setBorder(BorderFactory.createEtchedBorder());
                    this.myCurrentStepLabel.setText(RESOURCE.getString("configwizard.step3.title"));
                    this.pack();
                } else {
                    this.setVisible(false);
                }
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    /**
     * @return the dataSet
     */
    public IDataSet getDataSet() {
        return myDataSet;
    }
}
