package org.activityinfo.legacy.shared.adapter;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.*;
import org.activityinfo.model.type.Cardinality;
import org.activityinfo.model.type.NarrativeType;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.model.type.TextType;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.enumerated.EnumValue;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.time.LocalDateType;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.activityinfo.legacy.shared.adapter.CuidAdapter.activityCategoryFolderId;
import static org.activityinfo.legacy.shared.adapter.CuidAdapter.attributeId;

/**
 * Adapts a Legacy "Activity" model to a FormClass
 */
public class ActivityUserFormBuilder {


    private final ActivityDTO activity;

    private FormClass siteForm;

    public ActivityUserFormBuilder(ActivityDTO activity) {
        assert activity != null;
        this.activity = activity;
    }

    public FormClass build() {
        ResourceId classId = CuidAdapter.activityFormClass(activity.getId());

        siteForm = new FormClass(classId);
        siteForm.setLabel(activity.getName());

        if (!Strings.isNullOrEmpty(activity.getCategory())) {
            siteForm.setParentId(activityCategoryFolderId(activity.getDatabaseId(), activity.getCategory()));
        } else {
            siteForm.setParentId(CuidAdapter.databaseId(activity.getDatabaseId()));
        }

        FormField partnerField = new FormField(CuidAdapter.field(classId, CuidAdapter.PARTNER_FIELD))
        .setLabel(I18N.CONSTANTS.partner())
        .setType(ReferenceType.single(CuidAdapter.partnerFormClass(activity.getDatabaseId())))
        .setRequired(true);
        siteForm.addElement(partnerField);

        FormField projectField = new FormField(CuidAdapter.field(classId, CuidAdapter.PROJECT_FIELD))
        .setLabel(I18N.CONSTANTS.project())
        .setType(ReferenceType.single(CuidAdapter.projectFormClass(activity.getDatabaseId())));
        siteForm.addElement(projectField);

        FormField endDateField = new FormField(CuidAdapter.field(classId, CuidAdapter.END_DATE_FIELD))
        .setLabel(I18N.CONSTANTS.endDate())
        .setType(LocalDateType.INSTANCE)
        .setRequired(true);
        siteForm.addElement(endDateField);

        FormField startDateField = new FormField(CuidAdapter.field(classId, CuidAdapter.START_DATE_FIELD))
        .setLabel(I18N.CONSTANTS.startDate())
        .setType(LocalDateType.INSTANCE)
        .setRequired(true);
        siteForm.addElement(startDateField);

        FormField locationField = new FormField(CuidAdapter.locationField(activity.getId()))
        .setLabel(activity.getLocationType().getName())
        .setType(ReferenceType.single(locationClass(activity.getLocationType())))
        .setRequired(true);
        siteForm.addElement(locationField);

        List<IsFormField> fields = Lists.newArrayList();
        fields.addAll(activity.getAttributeGroups());
        fields.addAll(activity.getIndicators());

        Collections.sort(fields, new Comparator<IsFormField>() {
            @Override
            public int compare(IsFormField o1, IsFormField o2) {
                return o1.getSortOrder() - o2.getSortOrder();
            }
        });

        for(IsFormField field : fields) {
            siteForm.addElement(field.asFormField());
        }

        FormField commentsField = new FormField(CuidAdapter.commentsField(activity.getId()));
        commentsField.setType(NarrativeType.INSTANCE);
        commentsField.setLabel(I18N.CONSTANTS.comments());
        siteForm.addElement(commentsField);

        return siteForm;
    }

    private static ResourceId locationClass(LocationTypeDTO locationType) {
        if (locationType.isAdminLevel()) {
            return CuidAdapter.adminLevelFormClass(locationType.getBoundAdminLevelId());
        } else {
            return CuidAdapter.locationFormClass(locationType.getId());
        }
    }
}
