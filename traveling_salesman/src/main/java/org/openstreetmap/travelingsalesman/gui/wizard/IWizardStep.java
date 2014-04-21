package org.openstreetmap.travelingsalesman.gui.wizard;

import javax.swing.JComponent;

/**
 * A step of the {@link ConfigWizard}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IWizardStep {

    /**
     * Return true if this step was a success.
     * @return true if the user is allows to proceed.
     */
    boolean isReady();

    /**
     * @return the component to be displayed
     */
    JComponent getComponent();
}
