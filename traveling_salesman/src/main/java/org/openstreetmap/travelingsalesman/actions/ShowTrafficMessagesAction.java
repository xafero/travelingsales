/**
 * ShowTrafficMessagesAction.java
 * created: 8.4.2009
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of Traveling Salesman by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  jgnucashLib-GPL is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  jgnucashLib-GPL is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with jgnucashLib-V1.  If not, see <http://www.gnu.org/licenses/>.
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

package org.openstreetmap.travelingsalesman.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.gui.MainFrame;
import org.openstreetmap.travelingsalesman.routing.NameHelper;
import org.openstreetmap.travelingsalesman.trafficblocks.ITrafficMessageSource;
import org.openstreetmap.travelingsalesman.trafficblocks.ITrafficMessageSource.ITunableTrafficMessageSource;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessage;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessageStore;

import com.l2fprod.common.swing.JTaskPane;
import com.l2fprod.common.swing.JTaskPaneGroup;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und
 * Beratung</a>.<br/>
 * Project: Traveling Salesman<br/>
 * ShowTrafficMessagesAction<br/>
 * created: 8.4.2009 <br/>
 * <br/>
 * <br/>
 * <b>Open the system-web-browser to file a bug-report.</b>
 * 
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ShowTrafficMessagesAction implements Action {

	/**
	 * Our logger for debug- and error-output.
	 */
	private static final Logger LOG = Logger
			.getLogger(ShowTrafficMessagesAction.class.getName());

	/**
	 * Used by {@link TrafficMessagesFrame}.
	 */
	private INavigatableComponent myNavigation;

	/**
	 * Initialize this action.
	 * 
	 * @param aNavigation
	 *            used to get the current map
	 */
	public ShowTrafficMessagesAction(final INavigatableComponent aNavigation) {
		this.putValue(Action.NAME, MainFrame.RESOURCE
				.getString("Actions.ShowTrafficMessages.Label"));
		this.myNavigation = aNavigation;
		if (aNavigation == null) {
			throw new IllegalArgumentException("null NavigationManager given");
		}
	}

	/**
	 * The frame we are showing. To avoid showing 2 frames.
	 */
	private TrafficMessagesFrame myFrame = null;

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(final ActionEvent aE) {
		try {
			if (myFrame == null) {
				myFrame = new TrafficMessagesFrame(this.myNavigation);
				myFrame.pack();
				myFrame.setVisible(true);
			} else {
				myFrame.pack();
				myFrame.setVisible(true);
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Error, cannot show traffic messages", e);
			JOptionPane.showMessageDialog(
					null,
					"Error, cannot launch web browser:\n"
							+ e.getLocalizedMessage());
		}
	}

	/**
	 * Panel for an {@link ITrafficMessageSource} e.g. to provide radio-tuning
	 * widgets.
	 */
	private class TrafficMessageSourcePanel extends JTaskPaneGroup implements
			PropertyChangeListener {

		/**
		 * {@link JTextField} for the frequency we are tuned to/shall tune to.
		 */
		private JTextField myFrequencyText;

		/**
		 * The ITrafficMessageSource we represent.
		 */
		private ITrafficMessageSource mySource;

		/**
		 * @return the frequencyText
		 */
		protected JTextField getFrequencyText() {
			if (myFrequencyText == null
					&& this.mySource instanceof ITunableTrafficMessageSource) {
				ITunableTrafficMessageSource tuner = (ITunableTrafficMessageSource) this.mySource;
				myFrequencyText = new JTextField(tuner.getFrequency()
						.toPlainString());
				tuner.addPropertyChangeListener("frequency", this);
				myFrequencyText.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(final ActionEvent aEvt) {
						BigDecimal freq;
						try {
							freq = new BigDecimal(myFrequencyText.getText());
						} catch (NumberFormatException e) {
							throw new IllegalArgumentException("not a float");
						}

						ITunableTrafficMessageSource tuner = (ITunableTrafficMessageSource) mySource;
						final BigDecimal oldFreq = tuner.getFrequency();
						if (!freq.equals(oldFreq)) {
							try {
								tuner.setFrequency(freq);
							} catch (Exception e) {
								LOG.log(Level.SEVERE,
										"Cannot set frequency to " + freq, e);
							}
						}
					}
				});
			}
			return myFrequencyText;
		}

		public TrafficMessageSourcePanel(final ITrafficMessageSource aSource) {
			setTitle(aSource.getPortName());
			this.mySource = aSource;
			if (aSource instanceof ITunableTrafficMessageSource) {
				add(getFrequencyText());
				add(new JLabel("MHz"));
			}
		}

		/**
		 * Update {@link #myFrequencyText}.
		 * 
		 * @param aEvt
		 *            ignored
		 */
		@Override
		public void propertyChange(final PropertyChangeEvent aEvt) {
			ITunableTrafficMessageSource tuner = (ITunableTrafficMessageSource) this.mySource;
			myFrequencyText.setText(tuner.getFrequency().toPlainString());
		}

		/**
		 * @see #mySource.
		 * @return the source we represent.
		 */
		public ITrafficMessageSource getSource() {
			return mySource;
		}
	}

	/**
	 * Frame to show traffic-info in.
	 */
	private class TrafficMessagesFrame extends JFrame implements
			TrafficMessageStore.TrafficMessagesListener {
		/**
		 * generated.
		 */
		private static final long serialVersionUID = 2045001641605309357L;
		/**
		 * Table to show the messages in.
		 */
		private JTable myMessagesTable;
		/**
		 * Where we get our map from.
		 */
		private INavigatableComponent myNavigation;
		/**
		 * Where we get our messages from.
		 */
		private TrafficMessageStore myMessageStore;
		/**
		 * Button to close this windows.
		 */
		private JButton myCloseButton;

		/**
		 * The panel with the {@link TrafficMessageSourcePanel}s.
		 */
		private JTaskPane myMessageSourceList;

		/**
		 * @param aNavigation
		 *            used to get the map.
		 */
		public TrafficMessagesFrame(final INavigatableComponent aNavigation) {
			super(MainFrame.RESOURCE
					.getString("Actions.ShowTrafficMessages.WindowTitle"));
			this.myNavigation = aNavigation;
			myMessageStore = TrafficMessageStore.getInstance();
			myMessageStore.addTrafficMessagesListener(this);

			this.myMessagesTable = new JTable();

			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(new JScrollPane(getMessageSourcesList()),
					BorderLayout.NORTH);
			this.getContentPane().add(new JScrollPane(this.myMessagesTable),
					BorderLayout.CENTER);
			this.setDefaultCloseOperation(HIDE_ON_CLOSE);
			this.myCloseButton = new JButton(
					MainFrame.RESOURCE
							.getString("Actions.ShowTrafficMessages.Buttons.Close"));
			this.myCloseButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent aE) {
					TrafficMessagesFrame.this.setVisible(false);
				}
			});
			this.getContentPane().add(this.myCloseButton, BorderLayout.SOUTH);

			Thread thread = new Thread("updating trafficmessage-frame") {
				public void run() {
					updateMessages();
				}
			};
			thread.start();
		}

		/**
		 * @return the panel with the {@link TrafficMessageSourcePanel}s.
		 */
		private JTaskPane getMessageSourcesList() {
			if (myMessageSourceList == null) {
				myMessageSourceList = new JTaskPane();
				Set<ITrafficMessageSource> sources = myMessageStore
						.getTrafficMessageSources();
				for (ITrafficMessageSource source : sources) {
					sourceAdded(source);
				}
			}
			return myMessageSourceList;
		}

		/**
		 * Update the table with new messages.
		 */
		private void updateMessages() {
			String[] columnNames = new String[] {
					MainFrame.RESOURCE
							.getString("Actions.ShowTrafficMessages.Colum.Place"),
					MainFrame.RESOURCE
							.getString("Actions.ShowTrafficMessages.Colum.Message"),
					MainFrame.RESOURCE
							.getString("Actions.ShowTrafficMessages.Colum.Until"),
					MainFrame.RESOURCE
							.getString("Actions.ShowTrafficMessages.Colum.Known") };
			Collection<TrafficMessage> allMessages = myMessageStore
					.getAllMessages(myNavigation.getDataSet());
			DefaultTableModel model = new DefaultTableModel(columnNames, 0);
			DateFormat timeFormat = DateFormat.getDateTimeInstance();
			for (TrafficMessage trafficMessage : allMessages) {
				Vector<String> rowData = new Vector<String>(columnNames.length);
				String name = NameHelper.getNameForNode(
						this.myNavigation.getDataSet(),
						trafficMessage.getEntity());
				rowData.add(name);
				rowData.add(trafficMessage.getMessage());
				rowData.add(timeFormat.format(trafficMessage.getValidUntil()));
				if (trafficMessage.getType().equals(
						TrafficMessage.TYPES.IGNORED)) {
					rowData.add("");
				} else {
					rowData.add("X");
				}
				model.addRow(rowData);
			}
			this.myMessagesTable.setModel(model);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void trafficMessageAdded(final TrafficMessage aMessage) {
			updateMessages();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void trafficMessageRemoved(final TrafficMessage aMessage) {
			updateMessages();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void trafficMessageUpdated(final TrafficMessage aMessage) {
			updateMessages();
		}

		/**
		 * {@inheritDoc}.
		 */
		// @Override
		public void sourceAdded(final ITrafficMessageSource aSource) {
			JTaskPaneGroup panel = new TrafficMessageSourcePanel(aSource);
			JTaskPane messageSourcesList = getMessageSourcesList();
			messageSourcesList.add(panel);
			this.pack();
		}

		/**
		 * {@inheritDoc}.
		 */
		// @Override
		public void sourceRemoved(final ITrafficMessageSource aSource) {
			JTaskPane messageSourcesList = getMessageSourcesList();
			Component[] components = messageSourcesList.getComponents();
			for (Component component : components) {
				if (component instanceof TrafficMessageSourcePanel) {
					TrafficMessageSourcePanel panel = (TrafficMessageSourcePanel) component;
					if (panel.getSource() == aSource) {
						messageSourcesList.remove(panel);
					}
				}
			}
			this.pack();
		}
	}

	// //////////////////////////
	/**
	 * Used for {@link PropertyChangeListener}s.
	 */
	private PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(
			this);

	/**
	 * Used for {@link #putValue(String, Object)}.
	 */
	private Map<String, Object> myValues = new HashMap<String, Object>();

	/**
	 * ${@inheritDoc}.
	 */
	public void addPropertyChangeListener(final PropertyChangeListener aListener) {
		this.myPropertyChangeSupport.addPropertyChangeListener(aListener);
	}

	/**
	 * ${@inheritDoc}.
	 */
	public Object getValue(final String aKey) {
		return myValues.get(aKey);
	}

	/**
	 * ${@inheritDoc}.
	 */
	public boolean isEnabled() {
		return true;
	}

	/**
	 * ${@inheritDoc}.
	 */
	public void putValue(final String aKey, final Object aValue) {
		Object old = myValues.put(aKey, aValue);
		myPropertyChangeSupport.firePropertyChange(aKey, old, aValue);
	}

	/**
	 * ${@inheritDoc}.
	 */
	public void removePropertyChangeListener(
			final PropertyChangeListener aListener) {
		this.myPropertyChangeSupport.removePropertyChangeListener(aListener);
	}

	/**
	 * ${@inheritDoc}.
	 */
	public void setEnabled(final boolean aB) {
		// ignored
	}

}
