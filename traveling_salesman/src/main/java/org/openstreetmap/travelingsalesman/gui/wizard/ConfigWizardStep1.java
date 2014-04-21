package org.openstreetmap.travelingsalesman.gui.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.LODDataSet;
import org.openstreetmap.travelingsalesman.actions.DownloadMenu;
import org.openstreetmap.travelingsalesman.actions.LoadMapFileActionListener;


/**
 * First step of the wizard. Import a first map.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ConfigWizardStep1 extends JPanel implements IWizardStep {

    /**
     * Spacer-size.
     */
    private static final int SPACER = 10;

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     *  logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(ConfigWizardStep1.class.getName());

    /**
     * Button to load a map from a local file.
     */
    private JButton myLoadFromFileButton = new JButton(ConfigWizard.RESOURCE.getString("configwizard.buttons.importfile"));

    /**
     * Button to load a map from a remote location.
     */
    private JButton myDownloadButton = new JButton(ConfigWizard.RESOURCE.getString("configwizard.buttons.downloadfile"));

    /**
     * File-path to load a map from a local file.
     */
    private JTextField myLoadFromFileText = new JTextField(ConfigWizard.RESOURCE.getString("configwizard.step1.defaultfile"));

    /**
     * File-path to store a map.
     */
    private JTextField myMapLocationDir = new JTextField(ConfigWizard.RESOURCE.getString("configwizard.step1.defaultlocation")
            + Settings.getInstance().get("tiledMapCache.dir", System.getProperty("user.home")
                    + File.separator + ".openstreetmap" + File.separator + "map" + File.separator));
    /**
     * Button to select a file to load.
     */
    private JButton myMapLocationDirButton;

    /**
     * Button to select a file to load.
     */
    private JButton mySelectFileButton;

    /**
     * Panel for {@link #myLoadFromFileText} and {@link #mySelectFileButton}.
     */
    private JPanel myLoadFromFilePanel = new JPanel();

    /**
     * Panel for {@link #myMapLocationDirText} and {@link #mymyMapLocationDirButton}.
     */
    private JPanel myMapLocationPanel = new JPanel();

    /**
     * Tree with all possible locations to download.
     */
    private JTree myDownloadList;

    /**
     * The label to display next to {@link #myStatusBarLabel} in {@link #myStatusBar}.
     */
    private JProgressBar myProgressBar = null;

    /**
     * The status-bar with the {@link #myStatusBarLabel} and {@link #myProgressBar}
     * to display at the bottom of the screen.
     */
    private JPanel myStatusBar = null;

    /**
     * The label to display next to {@link #myProgressBar} in {@link #myStatusBar}.
     */
    private JLabel myStatusBarLabel = null;

    /**
     * Panel for the center area.
     */
    private JPanel myCenterPanel = new JPanel();

    /**
     * Our map.
     */
    private IDataSet myDataSet;

    /**
     * Create the Panel.
     */
    public ConfigWizardStep1() {
        this.myMapLocationPanel.setBackground(Color.LIGHT_GRAY);
        this.myMapLocationPanel.setLayout(new BorderLayout());
        this.myMapLocationPanel.add(this.myMapLocationDir, BorderLayout.CENTER);
        this.myMapLocationPanel.add(getMapLocationDirButton(), BorderLayout.EAST);
        this.myMapLocationPanel.setBorder(BorderFactory.createEmptyBorder(SPACER, 0, SPACER, 0));

        JPanel myLoadFromFileButtonPanel = new JPanel();
        myLoadFromFileButtonPanel.setBackground(Color.WHITE);
        myLoadFromFileButtonPanel.setLayout(new BorderLayout());
        myLoadFromFileButtonPanel.add(this.myLoadFromFileButton, BorderLayout.WEST);
        this.myLoadFromFilePanel.setLayout(new BorderLayout());
        this.myLoadFromFilePanel.setBackground(Color.WHITE);
        this.myLoadFromFilePanel.add(this.myLoadFromFileText, BorderLayout.CENTER);
        this.myLoadFromFilePanel.add(getSelectFileButton(), BorderLayout.EAST);
        this.myLoadFromFilePanel.add(myLoadFromFileButtonPanel, BorderLayout.SOUTH);
        this.myLoadFromFilePanel.setBorder(BorderFactory.createEmptyBorder(SPACER, 0, SPACER, 0));

        JPanel tempPanel2 = new JPanel();
        tempPanel2.setBackground(Color.WHITE);
        tempPanel2.setLayout(new BorderLayout());
        tempPanel2.add(this.myDownloadButton, BorderLayout.WEST);

        JTextArea label = new JTextArea(ConfigWizard.RESOURCE.getString("configwizard.buttons.step1expl"));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setEditable(false);
        label.setBorder(BorderFactory.createEmptyBorder(SPACER, 0, SPACER, 0));

        JPanel upperPanel = new JPanel();
        upperPanel.setBackground(Color.WHITE);
        upperPanel.setLayout(new GridLayout(2, 1));
        upperPanel.add(this.myMapLocationPanel);
        upperPanel.add(this.myLoadFromFilePanel);

        this.myCenterPanel.setBackground(Color.WHITE);
        this.myCenterPanel.setLayout(new BorderLayout());
        this.myCenterPanel.add(upperPanel, BorderLayout.NORTH);
        this.myCenterPanel.add(new JScrollPane(getDownloadList()), BorderLayout.CENTER);
        this.myCenterPanel.add(tempPanel2, BorderLayout.SOUTH);
        this.myCenterPanel.setBackground(Color.WHITE);

        this.setLayout(new BorderLayout());
        this.add(label, BorderLayout.NORTH);
        this.add(this.myCenterPanel, BorderLayout.CENTER);
        this.add(getStatusBar(), BorderLayout.SOUTH);

        this.myLoadFromFileButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent aE) {
                LoadMapFileActionListener actionListener = new LoadMapFileActionListener(null,
                        getStatusBarLabel(),
                        getJProgressBar(),
                        getOsmData(),
                        new File(myLoadFromFileText.getText()));
                actionListener.actionPerformed(aE);
            }
        });
        this.myMapLocationDir.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent aE) {
                String text = myMapLocationDir.getText();
                if (text.startsWith(ConfigWizard.RESOURCE.getString("configwizard.step1.defaultlocation"))) {
                    text = text.substring(ConfigWizard.RESOURCE.getString("configwizard.step1.defaultlocation").length());
                }
                Settings.getInstance().put("tiledMapCache.dir", text);
            }
        });
        this.myDownloadButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent aArg0) {
            (new Thread() {

                public void run() {
                    TreePath selectionPath = getDownloadList().getSelectionPath();
                    TreeNode leaf = (TreeNode) selectionPath.getLastPathComponent();
                    if (!(leaf instanceof DownloadMenu.LeafTreeNode)) {
                        return;
                    }
                    DownloadMenu.LeafTreeNode item = (DownloadMenu.LeafTreeNode) leaf;
                    if (item == null || item.getURL() == null) {
                        return;
                    }
                    try {
                        LoadMapFileActionListener.loadMapURL(null, item.getURL(), getStatusBarLabel(), getJProgressBar(), getOsmData());
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Cannot download " + item.toString() + "=" + item.getURL(), e);
                    }
                }
            }).start();
        }
        }
    );


        getDownloadList();
    }


    /**
     * @return the list with all possible maps to download.
     */
    private JTree getDownloadList() {
        if (this.myDownloadList == null) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("download");
            DefaultTreeModel treeModel = new DefaultTreeModel(root);
            root.add((new DownloadMenu("http://download.geofabrik.de/osm/europe/", "Europe")).getTreeNode(root, treeModel));
            root.add((new DownloadMenu("http://downloads.cloudmade.com/", "World")).getTreeNode(root, treeModel));
            myDownloadList = new JTree(treeModel);
        }
        return this.myDownloadList;
    }

    /**
     * @return the map with the loaded data.
     */
    public IDataSet getOsmData() {
        if (myDataSet == null) {
            myDataSet = Settings.getInstance().getPlugin(IDataSet.class, LODDataSet.class.getName());
            getMapLocationDirButton().setEnabled(false);
        }
        return myDataSet;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isReady() {
        String defaultDir = Settings.getInstance().get("tiledMapCache.dir", System.getProperty("user.home")
                + File.separator + ".openstreetmap" + File.separator + "map" + File.separator);
        File dir = new File(defaultDir);
        return dir.exists();
    }

    /**
     * {@inheritDoc}
     */
    public JComponent getComponent() {
        return this;
    }


    /**
     * This method initializes jProgressBar.
     *
     * @return javax.swing.JProgressBar
     */
    private JProgressBar getJProgressBar() {
        if (myProgressBar == null) {
            myProgressBar = new JProgressBar();
            myProgressBar.setBackground(Color.WHITE);
            myProgressBar.setBorderPainted(false);
//          use the same background-color that the task-pane uses at the bottom
            if (UIManager.getBoolean("TaskPane.useGradient")) {
                myProgressBar.setBackground(UIManager.getColor("TaskPane.backgroundGradientEnd"));
            } else {
                myProgressBar.setBackground(UIManager.getColor("TaskPane.background"));
            }
        }
        return myProgressBar;
    }


    /**
     * This method initializes jProgressBar.
     *
     * @return javax.swing.JProgressBar
     */
    private JLabel getStatusBarLabel() {
        if (myStatusBarLabel == null) {
            myStatusBarLabel = new JLabel();
            myStatusBarLabel.setBackground(Color.WHITE);
        }
        return myStatusBarLabel;
    }

    /**
     * This method initializes jProgressBar.
     *
     * @return javax.swing.JProgressBar
     */
    private JPanel getStatusBar() {
        if (myStatusBar == null) {
            myStatusBar = new JPanel();
            myStatusBar.setBackground(Color.LIGHT_GRAY);
            myStatusBar.setLayout(new GridLayout(1, 2));
            myStatusBar.add(getStatusBarLabel());
            myStatusBar.add(getJProgressBar());
        }
        return myStatusBar;
    }


    /**
     * @return the selectFileButton
     */
    private JButton getSelectFileButton() {
        if (this.mySelectFileButton == null) {
            this.mySelectFileButton = new JButton(" ... ");
            this.mySelectFileButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent aArg0) {
                    JFileChooser fileChooser = new JFileChooser(new File(Settings.getInstance().get("traveling-salesman.loadFile.lastPath", ".")));
                    fileChooser.setAcceptAllFileFilterUsed(true);
                    fileChooser.setMultiSelectionEnabled(false);
                    fileChooser.addChoosableFileFilter(new FileFilter() {

                        @Override
                        public boolean accept(final File file) {
                            if (file.isDirectory())
                                return true;
                            String fileName = file.getName().toLowerCase();
                            return fileName.endsWith(".osm")
                            || fileName.endsWith(".osm.gz")
                            || fileName.endsWith(".osm.bz2")
                            || fileName.endsWith(".xml")
                            || fileName.endsWith(".xml.gz")
                            || fileName.endsWith(".xml.bz2");
                        }

                        @Override
                        public String getDescription() {
                            return "OSM-File (.osm/.xml)";
                        }
                    });
                    int result = fileChooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        final File selectedFile = fileChooser.getSelectedFile();
                        myLoadFromFileText.setText(selectedFile.getAbsolutePath());
                    }
                }
            });
        }
        return mySelectFileButton;
    }

    /**
     * @return the selectFileButton
     */
    private JButton getMapLocationDirButton() {
        if (this.myMapLocationDirButton == null) {
            this.myMapLocationDirButton = new JButton(" ... ");
            this.myMapLocationDirButton.setEnabled(this.myDataSet == null);
            this.myMapLocationDirButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent aArg0) {
                    JFileChooser fileChooser = new JFileChooser(new File(Settings.getInstance().get("traveling-salesman.loadFile.lastPath", ".")));
                    fileChooser.setAcceptAllFileFilterUsed(true);
                    fileChooser.setMultiSelectionEnabled(false);
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    fileChooser.addChoosableFileFilter(new FileFilter() {

                        @Override
                        public boolean accept(final File file) {
                            return file.isDirectory();
                        }

                        @Override
                        public String getDescription() {
                            return "Directory";
                        }
                    });
                    int result = fileChooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        final File selectedFile = fileChooser.getSelectedFile();
                        String path = selectedFile.getAbsolutePath();
                        if (!path.endsWith(File.separator)) {
                            path = path + File.separator;
                        }
                        Settings.getInstance().put("map.dir", path);
                        Settings.getInstance().put("tiledMapCache.dir", path);
                        myMapLocationDir.setText(ConfigWizard.RESOURCE.getString("configwizard.step1.defaultlocation") + selectedFile.getPath());
                    }
                }
            });
        }
        return myMapLocationDirButton;
    }

}
