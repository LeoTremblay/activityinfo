package org.activityinfo.ui.full.client.style;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.TextResource;

/**
 * Set of resources to support the transition from GXT to
 * a Bootstrapped-based site
 */
public interface TransitionBundle extends ClientBundle {

    public interface NewUiStyle extends CssResource {

        @ClassName("section-margin-left")
        String sectionMarginLeft();

    }

    @Source("newui-transition.css")
    NewUiStyle newUiStyle();
}
