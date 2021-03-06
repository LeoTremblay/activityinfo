package org.activityinfo.legacy.shared.model;

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

import com.extjs.gxt.ui.client.data.BaseModelData;
import org.activityinfo.legacy.shared.model.LockedPeriodDTO.HasLockedPeriod;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonView;

import java.util.HashSet;
import java.util.Set;

@JsonAutoDetect(JsonMethod.NONE)
public final class ProjectDTO extends BaseModelData implements EntityDTO, HasLockedPeriod {

    private Set<LockedPeriodDTO> lockedPeriods = new HashSet<LockedPeriodDTO>(0);
    private UserDatabaseDTO userDatabase;

    public final static String ENTITY_NAME = "Project";

    public ProjectDTO() {
        super();
    }

    public ProjectDTO(int id, String name) {
        super();

        setId(id);
        setName(name);
    }

    @Override @JsonProperty @JsonView(DTOViews.Schema.class)
    public int getId() {
        return (Integer) get("id");
    }

    public void setId(int id) {
        set("id", id);
    }

    @Override @JsonProperty @JsonView(DTOViews.Schema.class)
    public String getName() {
        return (String) get("name");
    }

    public void setName(String name) {
        set("name", name);
    }

    public void setDescription(String description) {
        set("description", description);
    }

    @JsonProperty @JsonView(DTOViews.Schema.class)
    public String getDescription() {
        return (String) get("description");
    }

    @Override
    public Set<LockedPeriodDTO> getLockedPeriods() {
        return lockedPeriods;
    }

    public void setLockedPeriods(Set<LockedPeriodDTO> lockedPeriods) {
        this.lockedPeriods = lockedPeriods;
    }

    public void setUserDatabase(UserDatabaseDTO database) {
        this.userDatabase = database;
    }

    public UserDatabaseDTO getUserDatabase() {
        return userDatabase;
    }

    @Override
    public Set<LockedPeriodDTO> getEnabledLockedPeriods() {
        Set<LockedPeriodDTO> enabled = new HashSet<LockedPeriodDTO>(0);
        for (LockedPeriodDTO lockedPeriod : getLockedPeriods()) {
            if (lockedPeriod.isEnabled()) {
                enabled.add(lockedPeriod);
            }
        }
        return enabled;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }
}