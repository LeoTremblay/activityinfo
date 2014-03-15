package org.activityinfo.server.command;

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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.activityinfo.core.client.InstanceQuery;
import org.activityinfo.core.shared.Cuid;
import org.activityinfo.core.shared.Projection;
import org.activityinfo.core.shared.criteria.ClassCriteria;
import org.activityinfo.core.shared.form.tree.FieldPath;
import org.activityinfo.fp.client.Promise;
import org.activityinfo.legacy.shared.adapter.CuidAdapter;
import org.activityinfo.legacy.shared.adapter.LocationClassAdapter;
import org.activityinfo.legacy.shared.adapter.ResourceLocatorAdaptor;
import org.activityinfo.legacy.shared.command.GetLocations;
import org.activityinfo.legacy.shared.model.LocationDTO;
import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.server.database.OnDataSet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.activityinfo.core.client.PromiseMatchers.assertResolves;
import static org.activityinfo.legacy.shared.adapter.CuidAdapter.field;
import static org.activityinfo.legacy.shared.adapter.LocationClassAdapter.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(InjectionSupport.class)
@OnDataSet("/dbunit/sites-simple1.db.xml")
public class GetLocationsTest extends CommandTestCase2 {

    @Test
    public void testGetLocation() {
        setUser(1);

        LocationDTO location = execute(new GetLocations(1)).getData().get(0);

        assertThat(location, notNullValue());
        assertThat(location.getName(), equalTo("Penekusu Kivu"));
        assertThat(location.getAxe(), nullValue());
        assertThat(location.getAdminEntity(1).getName(), equalTo("Sud Kivu"));
        assertThat(location.getAdminEntity(2).getName(), equalTo("Shabunda"));
    }

    @Test
    public void testLocationQuery() {

        Cuid villageClassId = CuidAdapter.locationFormClass(1);
        Cuid provinceClassId = CuidAdapter.adminLevelFormClass(1);


        ResourceLocatorAdaptor adapter = new ResourceLocatorAdaptor(getDispatcher());
        FieldPath villageName = new FieldPath(getNameFieldId(villageClassId));
        FieldPath provinceName = new FieldPath(getAdminFieldId(villageClassId), field(provinceClassId, CuidAdapter.NAME_FIELD));

        List<Projection> projections = assertResolves(adapter.query(
                new InstanceQuery(
                        Arrays.asList(villageName, provinceName),
                        new ClassCriteria(villageClassId))));

        System.out.println(Joiner.on("\n").join(projections));

        assertThat(projections.size(), equalTo(4));
        assertThat(projections.get(0).getStringValue(provinceName), equalTo("Sud Kivu"));
    }

}
