package org.activityinfo.ui.client.page.config.mvp;

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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import org.activityinfo.legacy.shared.model.DTO;

@Deprecated
public interface CanFilter<M extends DTO> {
    public interface FilterHandler extends EventHandler {
        void onFilter(FilterEvent filterEvent);
    }

    // The user intends to filter the list of items
    public HandlerRegistration addFilterHandler(FilterHandler filter);

    public class FilterEvent extends GwtEvent<FilterHandler> {
        public static final Type TYPE = new Type<FilterHandler>();

        @Override
        public Type<FilterHandler> getAssociatedType() {
            return TYPE;
        }

        @Override
        protected void dispatch(FilterHandler handler) {
            handler.onFilter(this);
        }
    }
}
