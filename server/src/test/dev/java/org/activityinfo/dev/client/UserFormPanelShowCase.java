package org.activityinfo.dev.client;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import org.activityinfo.api2.shared.form.UserForm;
import org.activityinfo.api2.shared.form.UserFormInstance;
import org.activityinfo.ui.full.client.widget.form.UserFormPanel;

/**
 * @author yuriyz on 1/31/14.
 */
public class UserFormPanelShowCase extends FlowPanel {

    private final CheckBox readOnly = new CheckBox("Read-only");
    private final Button setTestData = new Button("setValue(<test data>)");
    private final CheckBox design = new CheckBox("Design Mode");

    private final UserForm userForm = DevUtils.createTestUserForm();
    private final UserFormPanel panel = new UserFormPanel(userForm, null);
    private final UserFormInstance userFormInstance = DevUtils.createTestUserFormInstance(userForm);

    public UserFormPanelShowCase() {
        init();
        add(readOnly);
        add(setTestData);
        add(design);
        add(panel);
    }

    private void init() {
        readOnly.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                panel.setReadOnly(readOnly.getValue());
            }
        });
        setTestData.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                panel.setValue(userFormInstance);
            }
        });
        design.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                panel.setDesignEnabled(design.getValue());
            }
        });
    }
}
