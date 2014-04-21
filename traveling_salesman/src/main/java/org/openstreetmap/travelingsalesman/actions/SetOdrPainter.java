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

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

/**
 *
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 */
public class SetOdrPainter extends AbstractAction {

    /**
     * generated.
     */
    private static final long serialVersionUID = -8136640455463445179L;

    /**
     * Constructor for this action.
     */
    public SetOdrPainter() {
        super(MainFrame.RESOURCE.getString("Actions.SetOdrPainter.Label"));
        putValue(Action.SHORT_DESCRIPTION, MainFrame.RESOURCE.getString("Actions.SetOdrPainter.Description"));
        /*
         * action.putValue(Action.SMALL_ICON, new ImageIcon(TaskPaneMain.class
         * .getResource(iconPath)));
         */
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        Settings.getInstance().put("plugin.useImpl.IPaintVisitor", "org.openstreetmap.travelingsalesman.painting.ODRPaintVisitor");
    }
}
