package org.activityinfo.core.shared.application;

import org.activityinfo.model.resource.ResourceId;

/**
 * Defines Application-level properties.
 * <strong>Should not be used as a form field id, only as a super property</strong>
 */
public class ApplicationProperties {

    /**
     * Application-defined property that provides a human-readable name for
     * a given form instance.
     */
    public static final ResourceId LABEL_PROPERTY = ResourceId.create("_label");

    public static final ResourceId PARENT_PROPERTY = ResourceId.create("_parent");

    /**
     * Application-defined property that provides an extended human-readable description
     * for a given form instance.
     */
    public static final ResourceId DESCRIPTION_PROPERTY = ResourceId.create("_description");

    /**
     * Application-defined property that provides the class ids of an instance.
     */
    public static final ResourceId CLASS_PROPERTY = ResourceId.create("_classOf");


    public static final ResourceId HIERARCHIAL = ResourceId.create("_multiLevel");

    public static final ResourceId COUNTRY_CLASS = ResourceId.create("_country");

    public static final ResourceId COUNTRY_NAME_FIELD = ResourceId.create("_country_name");

    public static final ResourceId GEOREF_PROPERTY = ResourceId.create("_georef");
}
