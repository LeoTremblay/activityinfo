package org.activityinfo.core.shared.application;

import com.google.common.collect.Maps;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;

import java.util.Map;

/**
 * Provides application-level form classes
 */
public class ApplicationClassProvider {

    private Map<ResourceId, FormClass> classMap = Maps.newHashMap();

    public ApplicationClassProvider() {

        classMap.put(FormClass.CLASS_ID, createFormClassClass());
        classMap.put(FolderClass.CLASS_ID, FolderClass.get());
    }

    private FormClass createFormClassClass() {
        FormField labelField = new FormField(FormClass.LABEL_FIELD_ID);
        labelField.setSuperProperty(ApplicationProperties.LABEL_PROPERTY);

        FormClass formClass = new FormClass(FormClass.CLASS_ID);
        formClass.addElement(labelField);

        return formClass;
    }

    public FormClass get(ResourceId classId) {
        FormClass formClass = classMap.get(classId);
        if(formClass == null) {
            throw new IllegalArgumentException("No such system class: " + classId);
        }
        return formClass;
    }
}
