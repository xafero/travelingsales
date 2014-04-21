/**
 * This file is part of LibOSM by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  LibOSM is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibOSM is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibOSM.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.osm;

import java.util.LinkedList;
import java.util.List;

//import java.util.logging.Logger;

/**
 * A configuration-section groups a number of ConfigurationSettings and
 * subsections.
 * 
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ConfigurationSection {

	/**
	 * my logger for debug and error-output.
	 */
	// private static final Logger LOG =
	// Logger.getLogger(ConfigurationSection.class.getName());

	/**
	 * All Sections below this one.
	 */
	private List<ConfigurationSection> mySubSections = new LinkedList<ConfigurationSection>();

	/**
	 * All the settings in this section.
	 */
	private List<ConfigurationSetting> mySettings = new LinkedList<ConfigurationSetting>();

	/**
	 * The name of this section.
	 */
	private String myName;

	/**
	 * @param aName
	 *            The name of this section.
	 */
	public ConfigurationSection(final String aName) {
		if (aName == null)
			throw new IllegalArgumentException("null name given");
		setName(aName);
	}

	/**
	 * @return All the settings in this section.
	 */
	public List<ConfigurationSetting> getSettings() {
		return mySettings;
	}

	/**
	 * @param aSettings
	 *            All the settings in this section.
	 */
	public void setSettings(final List<ConfigurationSetting> aSettings) {
		if (aSettings == null)
			throw new IllegalArgumentException("null settings-list given");
		mySettings = aSettings;
	}

	/**
	 * @param aSetting
	 *            All the settings in this section.
	 */
	public void addSetting(final ConfigurationSetting aSetting) {
		if (aSetting == null)
			throw new IllegalArgumentException("null setting given");
		mySettings.add(aSetting);
	}

	/**
	 * @return All Sections below this one.
	 */
	public List<ConfigurationSection> getSubSections() {
		return mySubSections;
	}

	/**
	 * @param aSubSections
	 *            All Sections below this one.
	 */
	public void setSubSections(final List<ConfigurationSection> aSubSections) {
		if (aSubSections == null)
			throw new IllegalArgumentException("null subsection-list given");
		mySubSections = aSubSections;
	}

	/**
	 * @param aSubSection
	 *            a Section below this one.
	 */
	public void addSubSections(final ConfigurationSection aSubSection) {
		if (aSubSection == null)
			throw new IllegalArgumentException("null subsection given");
		mySubSections.add(aSubSection);
	}

	/**
	 * @return The name of this section.
	 */
	public String getName() {
		return myName;
	}

	/**
	 * @param aName
	 *            The name of this section.
	 */
	public void setName(final String aName) {
		if (aName == null)
			throw new IllegalArgumentException("null name given");
		myName = aName;
	}
}
