/**
 * 
 */
package org.openstreetmap.travelingsalesman.gui.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.travelingsalesman.painting.ImageResources;

/**
 * Third step of the wizard. Learning about the UI.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ConfigWizardStep3 extends JPanel implements IWizardStep {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The screenshot to display.
     */
    private BufferedImage myImage;

    /**
     * Label for the first translated step to take in the UI.
     */
    private JLabel labelStep1 = new JLabel(ConfigWizard.RESOURCE.getString("configwizard.step3.label1"));

    /**
     * Label for the second translated step to take in the UI.
     */
    private JLabel labelStep2 = new JLabel(ConfigWizard.RESOURCE.getString("configwizard.step3.label2"));

    /**
     * Label for the third translated step to take in the UI.
     */
    private JLabel labelStep3 = new JLabel(ConfigWizard.RESOURCE.getString("configwizard.step3.label3"));

    /**
     * Label for the last translated step to take in the UI.
     */
    private JLabel labelStep4 = new JLabel(ConfigWizard.RESOURCE.getString("configwizard.step3.label4"));
    /**
     * Initialize this panel.
     */
    public ConfigWizardStep3() {
        myImage = ImageResources.getImage("ConfigWizard/Panel3_en.jpg");
        if (myImage != null) {
            this.setPreferredSize(new Dimension(myImage.getWidth(), myImage.getHeight()));
            this.setMinimumSize(new Dimension(myImage.getWidth(), myImage.getHeight()));
            this.setSize(new Dimension(myImage.getWidth(), myImage.getHeight()));
        }
        setLayout(null);
        setOpaque(false);
        final int leftLabelX = 200;
        final int rightLabelX = 400;
        final int label1Y = 100;
        final int label2Y = 200;
        final int label3Y = 300;
        final int label4Y = 400;
        add(labelStep1);
        labelStep1.setOpaque(true);
        labelStep1.setBackground(Color.YELLOW);
        labelStep1.setBounds(leftLabelX, label1Y, labelStep1.getPreferredSize().width, labelStep1.getPreferredSize().height);
        add(labelStep2);
        labelStep2.setOpaque(true);
        labelStep2.setBounds(leftLabelX, label2Y, labelStep2.getPreferredSize().width, labelStep2.getPreferredSize().height);
        labelStep2.setBackground(Color.YELLOW);
        add(labelStep3);
        labelStep3.setOpaque(true);
        labelStep3.setBounds(leftLabelX, label3Y, labelStep3.getPreferredSize().width, labelStep3.getPreferredSize().height);
        labelStep3.setBackground(Color.YELLOW);
        add(labelStep4);
        labelStep4.setOpaque(true);
        labelStep4.setBounds(rightLabelX, label4Y, labelStep4.getPreferredSize().width, labelStep4.getPreferredSize().height);
        labelStep4.setBackground(Color.YELLOW);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReady() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public JComponent getComponent() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintComponent(final Graphics aG) {
        if (myImage != null) {
            aG.drawImage(myImage, 0, 0, null);
        }
        labelStep1.repaint();
        labelStep2.repaint();
        labelStep3.repaint();
        labelStep4.repaint();
        //super.paintComponent(aG);
    }
}
