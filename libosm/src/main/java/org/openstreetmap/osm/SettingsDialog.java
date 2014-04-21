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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.Plugins.IPlugin;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.ComboBoxPropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorFactory;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertyRendererFactory;
import com.l2fprod.common.propertysheet.PropertyRendererRegistry;
import com.l2fprod.common.propertysheet.PropertySheetPanel;


/**
 * This dialog takes all the settings in
 * {@link Settings} and shows a nice dialog to edit them.
 * It is fully self-contained and needs only to be shown.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class SettingsDialog extends JFrame {

    /**
     * For serializing.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The button to cancel.
     */
    private JButton closeButton = new JButton(Settings.RESOURCE.getString("libosm.settingsdialog.button.close"));

    /**
     * Default-size of the dialog.
     */
    private static final int DEFAULTSETTINGSDIALOGWIDTH = 800;
    /**
     * Default-size of the dialog.
     */
    private static final int DEFAULTSETTINGSDIALOGHEIGHT = 600;

    /**
     * Create the dialog.
     * It is automaticall populated from {@link Settings#getRootConfigurationSection()}
     */
    public SettingsDialog() {
       this(Settings.getRootConfigurationSection());
    }
    /**
     * Create the dialog.
     * It is automaticall populated from {@link Settings#getRootConfigurationSection()}
     * @param config the configuration to show
     */
    public SettingsDialog(final ConfigurationSection config) {
        JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.add(closeButton, BorderLayout.EAST);

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                SettingsDialog.this.setVisible(false);
                Settings settings = Settings.getInstance();
                if (settings.getBoolean("traveling-salesman.restoreLastState", true)) {
                    settings.put("state.SettingsDialog.width",   String.valueOf(SettingsDialog.this.getWidth()));
                    settings.put("state.SettingsDialog.height",  String.valueOf(SettingsDialog.this.getHeight()));
                    settings.put("state.SettingsDialog.x",       String.valueOf(SettingsDialog.this.getX()));
                    settings.put("state.SettingsDialog.y",       String.valueOf(SettingsDialog.this.getY()));
                }
                SettingsDialog.this.dispose();
            }
        });

        this.setTitle(Settings.RESOURCE.getString("libosm.settingsdialog.title"));
        this.setLayout(new BorderLayout());
        this.add(buttonsPanel, BorderLayout.SOUTH);
        this.add(new PreferencesPanel(config), BorderLayout.CENTER);

        Settings settings   = Settings.getInstance();
        int width     = DEFAULTSETTINGSDIALOGWIDTH;
        int height    = DEFAULTSETTINGSDIALOGHEIGHT;
        int x         = 0;
        int y         = 0;
        if (settings.getBoolean("traveling-salesman.restoreLastState", true)) {
            width     = settings.getInteger("state.SettingsDialog.width", DEFAULTSETTINGSDIALOGWIDTH);
            height    = settings.getInteger("state.SettingsDialog.height", DEFAULTSETTINGSDIALOGHEIGHT);
            x         = settings.getInteger("state.SettingsDialog.x", 0);
            y         = settings.getInteger("state.SettingsDialog.y", 0);
        }
        this.setBounds(x, y, width, height);

        //this.pack();
    }


    /**
     * A panel to show a config-section and it's
     * sub-sections.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private static class PreferencesPanel extends JPanel {

        /**
         * For serializing.
         */
        private static final long serialVersionUID = 1L;

        /**
         * This PropertyChangeListener checks if the property is a Setting
         * and saves it.
         * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
         */
        private static final class SavingPropertyChangeListener implements PropertyChangeListener {

            /**
             * ${@inheritDoc}.
             * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
             */
            public void propertyChange(final PropertyChangeEvent evt) {
                Object property = evt.getSource();
                if (property instanceof MyProperty) {
                    MyProperty prop = (MyProperty) property;
                    if (evt.getNewValue() != null) {
                        Settings.getInstance().put(prop.getSetting().getKey(), evt.getNewValue().toString());
                    }
                    if (prop.getSetting().isRequiredRestart()) {
                        javax.swing.JOptionPane.showMessageDialog(null, Settings.RESOURCE.getString("libosm.settingsdialog.message.settingRequiresRestart"));
                    }
                }
            }
        }

        /**
         * The config-section we show.
         */
        private ConfigurationSection myConfigSection;

        /**
         * @param aConfigSection The config-section we show.
         */
        public PreferencesPanel(final ConfigurationSection aConfigSection) {
            if (aConfigSection == null)
                throw new IllegalArgumentException("null configSection given");
            myConfigSection = aConfigSection;

            this.setLayout(new BorderLayout());

            // add my settings
            if (getConfigSection().getSettings().size() > 0) {
                JPanel mySettingsPanel = buildMySettingsPanel();
                JScrollPane container = new JScrollPane(mySettingsPanel);
                this.add(container, BorderLayout.CENTER);
            }


            // add sub-sections;
            if (getConfigSection().getSubSections().size() > 0) {
                JTabbedPane subSections = new JTabbedPane();
                for (ConfigurationSection subSection : getConfigSection().getSubSections()) {
                    if (subSection.getSettings().size() > 0 || subSection.getSubSections().size() > 0)
                        subSections.add(new PreferencesPanel(subSection), subSection.getName());
                }

                if (getConfigSection().getSettings().size() > 0) {
                    this.add(subSections, BorderLayout.SOUTH);
                } else {
                    this.add(subSections, BorderLayout.CENTER);
                }
            }
        }

        /**
         * @return a panel to edit the settings of this section
         */
        private JPanel buildMySettingsPanel() {
            JPanel mySettingsPanel = new JPanel();

            mySettingsPanel.setLayout(new BorderLayout());
            PropertySheetPanel propertySheet = new PropertySheetPanel();
            propertySheet.setToolBarVisible(true);
            propertySheet.setSorting(false);
            propertySheet.setMode(PropertySheetPanel.VIEW_AS_CATEGORIES);
            propertySheet.setDescriptionVisible(true);
            SavingPropertyChangeListener savingPropertyChangeListener = new SavingPropertyChangeListener();

            for (ConfigurationSetting setting : getConfigSection().getSettings()) {
                MyProperty myProperty = new MyProperty(setting);
                myProperty.addPropertyChangeListener(savingPropertyChangeListener);
                propertySheet.addProperty(myProperty);
            }
            mySettingsPanel.add(propertySheet);

            MyPropertyEditorFactory propertyEditorFactory = new MyPropertyEditorFactory();
            propertySheet.setEditorFactory(propertyEditorFactory);
            propertySheet.setRendererFactory(propertyEditorFactory);

            return mySettingsPanel;
        }

        /**
         * @return the configSection
         */
        public ConfigurationSection getConfigSection() {
            return myConfigSection;
        }
    }

    /**
     * Helper-class for {@link PluginPropertyEditor}.
     */
    private static class PluginValue {
        /**
         * @param aValue The value to set the setting to.
         * @param aLabel The human readable label for the value.
         */
        protected PluginValue(final String aValue, final String aLabel) {
            super();
            value = aValue;
            label = aLabel;
        }
        /**
         * The value to set the setting to.
         */
        private String value;
        /**
         * The human readable label for the value.
         */
        private String label;
        /**
         * @see #getLabel()
         * @see java.lang.Object#toString()
         * @return the label
         */
        @Override
        public String toString() {
            return label;
        }
        /**
         * @return The value to set the setting to.
         */
        public String getValue() {
            return value;
        }
        /**
         * @return The human readable label for the value.
         */
        public String getLabel() {
            return label;
        }
    }
    /**
     * This is a special PropertyEditor for the Settings of type "Plugin".
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private static class PluginPropertyEditor extends AbstractPropertyEditor implements PropertyEditor, PropertyChangeListener, TableCellRenderer  {

        /**
         * The setting we are an editor for.
         */
        private ConfigurationSetting myConfigurationSetting;
        /**
         * Does the currently selected plugin have settings of its own?
         */
        private ConfigurationSection myPluginSettings = null;
        /**
         * The settings of the currently selected plugin.
         */
        private JButton mySelectedPluginSettings;
        /**
         * The actual editor.
         */
        private JComboBox myComboBox = null;
        /**
         * Label in the TableCellRenderer showing the currently selected plugin.
         */
        private JLabel mySelectPluginLabel;
          /**
           * @param aConfigurationSetting the plugin-interface we shall be an editor for.
           */
          public PluginPropertyEditor(final ConfigurationSetting aConfigurationSetting) {
            this.myConfigurationSetting = aConfigurationSetting;
            Map<String, String> registredPlugins = Settings.getRegistredPlugins(aConfigurationSetting.getPluginInterface());
            Settings.getInstance().addPropertyChangeListener(aConfigurationSetting.getKey(), this);
            this.myComboBox = new JComboBox();
            this.propertyChange(null);

            Vector<PluginValue> pluginValues = new Vector<PluginValue>();
            for (Entry<String, String> plugin : registredPlugins.entrySet()) {
                PluginValue pluginValue = new PluginValue(plugin.getValue(), plugin.getKey());
                pluginValues.add(pluginValue);
            }
            this.myComboBox.setModel(new DefaultComboBoxModel(pluginValues));
            // update the combo-box
            String value = Settings.getInstance().get(myConfigurationSetting.getKey(), "");
            for (int i = 0; i < myComboBox.getItemCount(); i++) {
                PluginValue comboValue = (PluginValue) myComboBox.getItemAt(i);
                if (comboValue.getValue().equals(value)) {
                    myComboBox.setSelectedIndex(i);
                }
            }
            this.myComboBox.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(final ItemEvent aE) {
                    if (aE.getStateChange() == ItemEvent.SELECTED) {
                        PluginValue value = (PluginValue) aE.getItem();
                        if (!Settings.getInstance().get(myConfigurationSetting.getKey(), "").equals(value.getValue())) {
                            firePropertyChange(Settings.getInstance().get(myConfigurationSetting.getKey(), null),
                                    value.getValue());
                            Settings.getInstance().put(myConfigurationSetting.getKey(), value.getValue());
                        }
                    }
                }
            });

          }

        /**
         * {@inheritDoc}
         * @see com.l2fprod.common.beans.editor.AbstractPropertyEditor#getCustomEditor()
         */
        @Override
        public Component getCustomEditor() {

            JPanel customEditor = new JPanel(new BorderLayout());
            customEditor.add(this.myComboBox, BorderLayout.CENTER);
            String val = Settings.getInstance().get(myConfigurationSetting.getKey(), null);
            int index = 0;
            Map<String, String> registredPlugins = Settings.getRegistredPlugins(myConfigurationSetting.getPluginInterface());
            for (Entry<String, String> entry : registredPlugins.entrySet()) {
                if (entry.getValue().equals(val)) {
                    break;
                }
                index++;
            }
            if (index < this.myComboBox.getItemCount()) {
                this.myComboBox.setSelectedIndex(index);
            } else {
                if (this.myComboBox.getItemCount() > 0) {
                    this.myComboBox.setSelectedIndex(0);
                    Settings.getInstance().put(myConfigurationSetting.getKey(), ((PluginValue) this.myComboBox.getSelectedItem()).getValue());
                }
            }
            customEditor.add(getSelectedPluginSettings(), BorderLayout.EAST);
            if (myPluginSettings == null) {
                getSelectedPluginSettings().setEnabled(false);
            }
            return customEditor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void propertyChange(final PropertyChangeEvent aEvt) {
            IPlugin plugin = Settings.getInstance().getPlugin(this.myConfigurationSetting.getPluginInterface(), null);
            this.myPluginSettings = null;
            try {
                if (plugin != null) {
                    this.myPluginSettings = plugin.getSettings();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            getSelectedPluginSettings().setEnabled(this.myPluginSettings != null);

            String value = Settings.getInstance().get(myConfigurationSetting.getKey(), "");
         // update the combo-box
            for (int i = 0; i < myComboBox.getItemCount(); i++) {
                PluginValue comboValue = (PluginValue) myComboBox.getItemAt(i);
                if (comboValue.getValue().equals(value)) {
                    myComboBox.setSelectedIndex(i);
                }
            }
         // update the label
            if (mySelectPluginLabel != null) {
                if (value.startsWith("JPF:")) {
                    value = value.substring("JPF:".length());
                } else {
                    int packageLength = value.lastIndexOf('.');
                    if (packageLength > 0) {
                        value = value.substring(packageLength + 1);
                    }
                }
                mySelectPluginLabel.setText(value);
            }
        }

//        /* (non-Javadoc)
//         * @see com.l2fprod.common.beans.editor.AbstractPropertyEditor#getAsText()
//         */
//        @Override
//        public String getAsText() {
//            if (this.myConfigurationSetting != null) {
//                return Settings.RESOURCE.getString("libosm.settingsdialog.pluginPropertyEditor.hasSettingsPrefix") + super.getAsText();
//            }
//            return super.getAsText();
//        }

        /**
         * @return the selectedPluginSettings
         */
        private JButton getSelectedPluginSettings() {
            if (mySelectedPluginSettings == null) {
                mySelectedPluginSettings = new JButton("...");
                mySelectedPluginSettings.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(final ActionEvent aE) {
                        String value = Settings.getInstance().get(myConfigurationSetting.getKey(), "");
                        if (value.startsWith("JPF:")) {
                            value = value.substring("JPF:".length());
                        } else {
                            int packageLength = value.lastIndexOf('.');
                            if (packageLength > 0) {
                                value = value.substring(packageLength + 1);
                            }
                        }
                        @SuppressWarnings("unused")
                        IPlugin plugin = Settings.getInstance().getPlugin(myConfigurationSetting.getPluginInterface(), null);
                        SettingsDialog pluginSetting = new SettingsDialog(myPluginSettings);
                        pluginSetting.setTitle(pluginSetting.getTitle() + " - " + value);
                        pluginSetting.setVisible(true);
                    }
                }
                );
            }
            return mySelectedPluginSettings;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(final JTable aTable,
                final Object aValue, final boolean aIsSelected, final boolean aHasFocus,
                final int aRow, final int aColumn) {
            String value = Settings.getInstance().get(myConfigurationSetting.getKey(), "");
            if (value.startsWith("JPF:")) {
                value = value.substring("JPF:".length());
            } else {
                int packageLength = value.lastIndexOf('.');
                if (packageLength > 0) {
                    value = value.substring(packageLength + 1);
                }
            }
            mySelectPluginLabel = new JLabel(value);

            JPanel customEditor = new JPanel(new BorderLayout());
                        customEditor.add(mySelectPluginLabel, BorderLayout.CENTER);
            customEditor.add(getSelectedPluginSettings(), BorderLayout.EAST);
            if (myPluginSettings == null) {
                getSelectedPluginSettings().setEnabled(false);
            }
            return customEditor;
        }
    }

    /**
     * This is a special PropertyEditor for the Settings that have a limited number of choices to select from.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private static class SelectPropertyEditor extends ComboBoxPropertyEditor implements PropertyEditor {

//        /**
//         * The setting we are an editor for.
//         */
//        private ConfigurationSetting myConfigurationSetting;

          /**
           * @param aConfigurationSetting the plugin-interface we shall be an editor for.
           */
          public SelectPropertyEditor(final ConfigurationSetting aConfigurationSetting) {
            super();
//            this.myConfigurationSetting = aConfigurationSetting;
            Map<String, String> selectValues = aConfigurationSetting.getValues();

            List<Value> values = new ArrayList<Value>(selectValues.size());
            for (Entry<String, String> plugin : selectValues.entrySet()) {
                values.add(new Value(plugin.getValue(), plugin.getKey()));
            }

            setAvailableValues(values.toArray(new Value[values.size()]));

            Icon[] icons = new Icon[selectValues.size()];
            Arrays.fill(icons, UIManager.getIcon("Tree.openIcon"));
            setAvailableIcons(icons);
          }

    }

    /**
     * We extend the PropertyEditorRegistry to create a
     * {@link PluginPropertyEditor} if needed.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private static class MyPropertyEditorFactory extends PropertyEditorRegistry implements PropertyEditorFactory, PropertyRendererFactory {

        /**
         * Create a PropertyEditor and take special care of our Plugin-Typed properties.
         * ${@inheritDoc}
         * @see com.l2fprod.common.propertysheet.PropertyEditorRegistry#createPropertyEditor(com.l2fprod.common.propertysheet.Property)
         */
        public PropertyEditor createPropertyEditor(final Property property) {
            if (property instanceof MyProperty) {
                MyProperty prop = (MyProperty) property;
                if (prop.getSetting().getType() == TYPES.PLUGIN) {
                    return new PluginPropertyEditor(prop.getSetting());
                }
                if (prop.getSetting().getValues() != null) {
                    return new SelectPropertyEditor(prop.getSetting());
                }
            }
            return super.createPropertyEditor(property);
        }

        /**
         * Default for #createTableCellRenderer.
         */
        private PropertyRendererRegistry myPropertyRendererRegistry = new PropertyRendererRegistry();
        /**
         * {@inheritDoc}
         */
        @Override
        public TableCellRenderer createTableCellRenderer(final Property aProperty) {
            if (aProperty instanceof MyProperty) {
                MyProperty prop = (MyProperty) aProperty;
                if (prop.getSetting().getType() == TYPES.PLUGIN) {
                    return new PluginPropertyEditor(prop.getSetting());
                }
            }
            return myPropertyRendererRegistry.createTableCellRenderer(aProperty);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public TableCellRenderer createTableCellRenderer(final Class aClass) {
            return myPropertyRendererRegistry.createTableCellRenderer(aClass);
        }
    }

    /**
     * This is a special Property-class that adapts a
     * {@link ConfigurationSetting} into a Property.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private static class MyProperty extends DefaultProperty {
        /**
         * For serializing.
         */
        private static final long serialVersionUID = 1L;

        /**
         * setting the setting we represent.
         */
        private ConfigurationSetting mySetting;

        /**
         * @param setting the setting we represent.
         */
        public MyProperty(final ConfigurationSetting setting) {
            this.mySetting = setting;
            setName(setting.getKey());
            setDisplayName(setting.getDisplayName());
            setType(setting.getType().toClass());
            Settings settings = Settings.getInstance();
            switch(setting.getType()) {
            case BOOLEAN : setValue(settings.getBoolean(setting.getKey(), false));
                      break;
            case DOUBLE : setValue(settings.getDouble(setting.getKey(), 0.0));
                      break;
            case INTEGER : setValue(settings.getInteger(setting.getKey(), 0));
                      break;
            default : setValue(settings.get(setting.getKey(), null));
            }
            setCategory(setting.getCategory());
            String shortDescription2 = setting.getShortDescription();
            if (shortDescription2 != null && shortDescription2.trim().length() > 0)
                setShortDescription(shortDescription2);
        }
        /**
         * @return the setting we represent.
         */
        public ConfigurationSetting getSetting() {
            return mySetting;
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public int hashCode() {
            return getSetting().hashCode();
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public boolean equals(final Object o) {
           if (!(o instanceof MyProperty))
               return false;
           MyProperty other = (MyProperty) o;

           return getSetting().equals(other.getSetting());
        }
    }
}
