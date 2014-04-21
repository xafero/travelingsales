/**
 * This file is part of Traveling-Salesman by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  Traveling-Salesman is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Traveling-Salesman is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Traveling-Salesman.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.gui.widgets;

import java.awt.BorderLayout;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Dialog to show all log-messages of level SEVERE and above.
 * 
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public final class ErrorDialog extends JFrame {

	/**
	 * generated.
	 */
	private static final long serialVersionUID = -3811627279256470913L;

	/**
	 * GUI-list to select an error-message from.
	 */
	private JList myErrorsList;

	/**
	 * GUI-area with the details of a single error-message.
	 */
	private JTextArea myErrorDetailsPanel;
	/**
	 * All messages we know of.
	 */
	private static List<Message> myMessages = new LinkedList<Message>();
	/**
	 * The singleton-instance.
	 */
	private static ErrorDialog myInstance = null;

	/**
	 * Helper-class that contains a single error-message to show.
	 * 
	 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
	 */
	private static class Message {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return getShortMessage();
		}

		/**
		 * @param aShortMessage
		 *            a short message of 1 line
		 * @param aLongMessage
		 *            a long version with all details
		 */
		public Message(final String aShortMessage, final String aLongMessage) {
			super();
			setShortMessage(aShortMessage);
			setLongMessage(aLongMessage);
		}

		/**
		 * A short message of 1 line.
		 */
		private String myShortMessage;

		/**
		 * @return the shortMessage
		 */
		private String getShortMessage() {
			return myShortMessage;
		}

		/**
		 * @param aShortMessage
		 *            a short message of 1 line
		 */
		private void setShortMessage(final String aShortMessage) {
			myShortMessage = aShortMessage;
		}

		/**
		 * @return the longMessage
		 */
		private String getLongMessage() {
			return myLongMessage;
		}

		/**
		 * @param aLongMessage
		 *            a long version with all details
		 */
		private void setLongMessage(final String aLongMessage) {
			myLongMessage = aLongMessage;
		}

		/**
		 * A long version with all details.
		 */
		private String myLongMessage;
	}

	/**
	 * Constructor. Creates the UI-elements but does not make this frame
	 * visible.
	 */
	private ErrorDialog() {
		setTitle("Error");
		getContentPane().setLayout(new BorderLayout());
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setLeftComponent(new JScrollPane(getErrorsList()));
		split.setRightComponent(new JScrollPane(getErrorDetailsPanel()));
		getContentPane().add(split, BorderLayout.CENTER);
		updateErrorList();
	}

	/**
	 * @return GUI-list to select an error-message from.
	 */
	private JList getErrorsList() {
		if (myErrorsList == null) {
			myErrorsList = new JList(new DefaultListModel());
			myErrorsList.addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(final ListSelectionEvent aE) {
					Message msg = (Message) myErrorsList.getSelectedValue();
					if (msg != null) {
						getErrorDetailsPanel().setText(msg.getLongMessage());
					}
				}
			});
		}
		return myErrorsList;
	}

	/**
	 * @return GUI-area with the details of a single error-message.
	 */
	private JTextArea getErrorDetailsPanel() {
		if (myErrorDetailsPanel == null) {
			myErrorDetailsPanel = new JTextArea("\n\n\n\n\n");
			myErrorDetailsPanel.setEditable(false);
		}
		return myErrorDetailsPanel;
	}

	/**
	 * Add an error to the list and show the frame if it is not yet visible.
	 * 
	 * @param aShortMessage
	 *            a short message of 1 line
	 * @param aLongMessage
	 *            a long version with all details
	 */
	public static synchronized void errorHappened(final String aShortMessage,
			final String aLongMessage) {
		System.err.println("ErrorDialog.errorHappened(" + aShortMessage + ", "
				+ aLongMessage + ")");
		String timestamp = DateFormat.getTimeInstance(DateFormat.SHORT).format(
				new Date());
		timestamp = "[" + timestamp + "] ";
		myMessages.add(new Message(timestamp + aShortMessage, aLongMessage));
		if (myInstance == null) {
			myInstance = new ErrorDialog();
			myInstance.pack();
			myInstance.setVisible(true);
		} else {
			myInstance.updateErrorList();
		}
	}

	/**
	 * Update the GUI with new messages.
	 */
	private void updateErrorList() {
		DefaultListModel model = (DefaultListModel) getErrorsList().getModel();
		for (Message msg : myMessages) {
			if (!model.contains(msg)) {
				model.addElement(msg);
			}
		}
	}
}
