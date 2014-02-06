package org.activityinfo.ui.full.client.widget.form;
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

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.*;
import org.activityinfo.api2.shared.form.FormField;
import org.activityinfo.api2.shared.form.FormFieldCardinality;
import org.activityinfo.api2.shared.form.FormFieldEnumValue;
import org.activityinfo.api2.shared.form.FormFieldType;
import org.activityinfo.ui.full.client.Log;
import org.activityinfo.ui.full.client.widget.DateBoxWithReadOnly;

/**
 * @author yuriyz on 1/28/14.
 */
public class FormFieldWidgetFactory {

    /**
     * Based on this numbers FormField Widget generates different widgets and layouts:
     *
     * 1. Single :
     *    less SMALL_BALANCE_NUMBER -> Radio buttons
     *    less MEDIUM_BALANCE_NUMBER -> Dropdown list
     *    more MEDIUM_BALANCE_NUMBER -> Suggest box
     * 2. Multiple :
     *    less SMALL_BALANCE_NUMBER -> Check boxes
     *    less MEDIUM_BALANCE_NUMBER -> List of selected + add button
     *    more MEDIUM_BALANCE_NUMBER -> List of selected + add button
     */
    public static final int SMALL_BALANCE_NUMBER = 10;
    public static final int MEDIUM_BALANCE_NUMBER = 20;

    public static final DateTimeFormat DATE_TIME_FORMAT = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_FULL);

    private FormFieldWidgetFactory() {
    }

    public static IsWidget create(FormField field) {
        final FormFieldType fieldType = field.getType();
        if (fieldType != null) {
            switch (fieldType) {
                case QUANTITY:
                    return createDoubleBox();
                case NARRATIVE:
                    return createTextArea();
                case FREE_TEXT:
                    return createTextBox();
                case LOCAL_DATE:
                    return createDateTextBox();
                case GEOGRAPHIC_POINT:
                    return new GeographicTextBox();
                case ENUMERATED:
                    final int enumValuesSize = field.getEnumValues().size();
                    if (field.getCardinality() == FormFieldCardinality.SINGLE) {
                        if (enumValuesSize < SMALL_BALANCE_NUMBER) {
                            // Radio buttons
                            return null;
                        } else if (enumValuesSize < MEDIUM_BALANCE_NUMBER) {
                            // Dropdown list
                            final ListBox dropBox = new ListBox(false);
                            for (FormFieldEnumValue value : field.getEnumValues()) {
                                dropBox.addItem(value.getLabel().getValue(), value.getId().asString());
                            }

                            return dropBox;
                        } else {
                            // Suggest box
                            return null;
                        }
                    } else {
                        if (enumValuesSize < SMALL_BALANCE_NUMBER) {
                            // Check boxes
                            return null;
                        } else if (enumValuesSize < MEDIUM_BALANCE_NUMBER) {
                            // List of selected + add button
                            final ListBox dropBox = new ListBox(true);
                            return null;
                        } else {
                            // List of selected + add button
                            return null;
                        }
                    }
                case REFERENCE:
                    return createTextBox();
                default:
                    Log.error("Field type " + fieldType + " is not supported, created text box widget as fallback.");
                    return createTextBox();
            }
        }
        return null;
    }

    private static TextArea createTextArea() {
        final TextArea textBox = new TextArea();
        textBox.addStyleName("form-control");
        return textBox;
    }

    public static TextBox createTextBox() {
        final TextBox textBox = new TextBox();
        textBox.addStyleName("form-control");
        return textBox;
    }

    public static DoubleBox createDoubleBox() {
        final DoubleBox doubleBox = new DoubleBox();
        doubleBox.addStyleName("form-control");
        doubleBox.getElement().setPropertyString("type", "number");
        return doubleBox;
    }

    public static DateBoxWithReadOnly createDateTextBox() {
        final DateBoxWithReadOnly dateBox = new DateBoxWithReadOnly();
        dateBox.getTextBox().addStyleName("form-control");
        dateBox.setFormat(new DateBoxWithReadOnly.DefaultFormat(DATE_TIME_FORMAT));
        return dateBox;
    }
}