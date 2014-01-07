package org.activityinfo.ui.desktop.client.widget.resources;

import com.google.gwt.user.client.ui.IsWidget;

public interface HasResource<T> extends IsWidget {

    void showResource(T resource);
}