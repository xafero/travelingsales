/**
 * StdoutConsoleHandler.java
 * created: 01.02.2009 13:18:58
 * (c) 2009 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of traveling_salesman by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  traveling_salesman is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  traveling_salesman is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with traveling_salesman.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * This <tt>Handler</tt> publishes log records to <tt>System.out</tt>. By
 * default the <tt>SimpleFormatter</tt> is used to generate brief summaries.
 * <p>
 * <b>Configuration:</b> By default each <tt>ConsoleHandler</tt> is initialized
 * using the following <tt>LogManager</tt> configuration properties. If
 * properties are not defined (or have invalid values) then the specified
 * default values are used.
 * <ul>
 * <li>java.util.logging.ConsoleHandler.level specifies the default level for
 * the <tt>Handler</tt> (defaults to <tt>Level.INFO</tt>).
 * <li>java.util.logging.ConsoleHandler.filter specifies the name of a
 * <tt>Filter</tt> class to use (defaults to no <tt>Filter</tt>).
 * <li>java.util.logging.ConsoleHandler.formatter specifies the name of a
 * <tt>Formatter</tt> class to use (defaults to
 * <tt>java.util.logging.SimpleFormatter</tt>).
 * <li>java.util.logging.ConsoleHandler.encoding the name of the character set
 * encoding to use (defaults to the default platform encoding).
 * </ul>
 * <p>
 * 
 * @version 1.13, 11/17/05
 * @since 1.4
 */

public class StdoutConsoleHandler extends StreamHandler {
	/**
	 * Private method to configure a ConsoleHandler.
	 */
	private void configure() {
		setLevel(Level.INFO);
		setFilter(null);
		setFormatter(new SimpleFormatter());
	}

	/**
	 * Create a <tt>ConsoleHandler</tt> for <tt>System.err</tt>.
	 * <p>
	 * The <tt>ConsoleHandler</tt> is configured based on <tt>LogManager</tt>
	 * properties (or their default values).
	 */
	public StdoutConsoleHandler() {
		configure();
		setOutputStream(System.out);
	}

	/**
	 * Publish a <tt>LogRecord</tt>.
	 * <p>
	 * The logging request was made initially to a <tt>Logger</tt> object, which
	 * initialized the <tt>LogRecord</tt> and forwarded it here.
	 * <p>
	 * 
	 * @param record
	 *            description of the log event. A null record is silently
	 *            ignored and is not published
	 */
	public void publish(final LogRecord record) {
		super.publish(record);
		flush();
	}

	/**
	 * Override <tt>StreamHandler.close</tt> to do a flush but not to close the
	 * output stream. That is, we do <b>not</b> close <tt>System.err</tt>.
	 */
	public void close() {
		flush();
	}
}
