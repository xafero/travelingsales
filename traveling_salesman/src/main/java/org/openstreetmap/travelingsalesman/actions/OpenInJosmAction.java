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
package org.openstreetmap.travelingsalesman.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

/**
 * This action allows opening the currently visible area
 * in JOSM for editing.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OpenInJosmAction extends AbstractAction {

    /**
     * Our OpenInJosmAction.java.
     */
    private static final long serialVersionUID = 1L;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(OpenInJosmAction.class.getName());

    /**
     * We need this to get the map and the visible area.
     */
    private INavigatableComponent myNavigatableComponent;

    /**
     * @param aNavigatableComponent We need this to get the map and the visible area.
     */
    public OpenInJosmAction(final INavigatableComponent aNavigatableComponent) {
        super(MainFrame.RESOURCE.getString("Actions.OpenInJOSM.Label"));
        this.myNavigatableComponent = aNavigatableComponent;
        putValue(Action.SHORT_DESCRIPTION, MainFrame.RESOURCE.getString("Actions.OpenInJOSM.Description"));
        /*
         * action.putValue(Action.SMALL_ICON, new ImageIcon(TaskPaneMain.class
                  .getResource(iconPath)));
         */
    }

    /**
     * ${@inheritDoc}.
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(final ActionEvent arg0) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("traveling_salesman_visibleExport_", ".osm");
            Bounds visibleArea = myNavigatableComponent.getMapBounds();
            IDataSet map = myNavigatableComponent.getDataSet();
            XmlWriter writer = new XmlWriter(tempFile, CompressionMethod.None);

            for (Iterator<Node> nodes = map.getNodes(visibleArea); nodes.hasNext();) {
                Node next = nodes.next();
                if (next.getUser() == null) {
                    next.setUser(OsmUser.NONE);
                }
                writer.process(new NodeContainer(next));
            }
            for (Iterator<Way> ways = map.getWays(visibleArea); ways.hasNext();) {
                Way next = ways.next();
                if (next.getUser() == null) {
                    next.setUser(OsmUser.NONE);
                }
                writer.process(new WayContainer(next));
            }
            for (Iterator<Relation> relations = map.getRelations(visibleArea); relations.hasNext();) {
                Relation next = relations.next();
                if (next.getUser() == null) {
                    next.setUser(OsmUser.NONE);
                }
                writer.process(new RelationContainer(next));
            }
            writer.complete();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot write visible area to temp-file for opening it in josm", e);
            return;
        }
        Class<?> mainClass = null;
        try {
            try {
                mainClass = Class.forName("org.openstreetmap.josm.gui.MainApplication");
            } catch (Exception e) {
                mainClass = AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
                    public Class<?> run() {
                        try {
                            URLClassLoader urlCL = new URLClassLoader(new URL[]{new URL("http://josm.openstreetmap.de/download/josm-tested.jar")});
                            return urlCL.loadClass("org.openstreetmap.josm.gui.MainApplication");
                        } catch (Exception e) {
                            LOG.log(Level.SEVERE, "cannot load josm-class via URLClassLoader", e);
                            return null;
                        }
                    } });
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot open JOSM-main-class", e);
            return;
        }

        if (mainClass == null)
            return;

        try {
            String[] args = new String[]{tempFile.getAbsolutePath()};
            Method mainMethod = mainClass.getMethod("main", new Class[]{args.getClass()});
            mainMethod.invoke(null, new Object[]{args});
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot invoke JOSM-main", e);
            return;
        }
    }

}
