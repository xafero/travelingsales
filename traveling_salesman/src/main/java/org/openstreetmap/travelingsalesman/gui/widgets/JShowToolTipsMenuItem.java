/**
 * 
 */
package org.openstreetmap.travelingsalesman.gui.widgets;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.travelingsalesman.TSNavigationConfigSection;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und
 * Beratung</a>.<br/>
 * Project: Traveling Salesman<br/>
 * JShowToolTipsMenuItem.java<br/>
 * created: 19.03.2009 <br/>
 * <br/>
 * <br/>
 * <b>Open the system-web-browser to file a bug-report.</b>
 * 
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */

public class JShowToolTipsMenuItem extends JCheckBoxMenuItem implements
		ChangeListener {

	/**
	 * generated.
	 */
	private static final long serialVersionUID = -6776790904898134524L;

	/**
	 * Initialize the Menu-item.
	 */
	public JShowToolTipsMenuItem() {
		super(
				TSNavigationConfigSection.RESOURCE
						.getString("travelingsalesman.configsection.showAllTags.title"),
				Settings.getInstance().getBoolean("showAllTags", false));
		getModel().addChangeListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stateChanged(final ChangeEvent aE) {
		Settings.getInstance().put("showAllTags",
				Boolean.toString(isSelected()));
	}

}
