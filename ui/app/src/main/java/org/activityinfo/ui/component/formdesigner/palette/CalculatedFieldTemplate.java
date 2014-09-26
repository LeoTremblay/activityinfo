package org.activityinfo.ui.component.formdesigner.palette;

import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.resource.Resources;
import org.activityinfo.model.type.expr.CalculatedFieldType;

public class CalculatedFieldTemplate implements FieldTemplate {
    @Override
    public String getLabel() {
        return I18N.CONSTANTS.calculation();
    }

    @Override
    public FormField createField() {
        return new FormField(Resources.generateId()).setType(new CalculatedFieldType());
    }
}