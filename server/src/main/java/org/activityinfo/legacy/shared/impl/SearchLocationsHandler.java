package org.activityinfo.legacy.shared.impl;

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

import com.bedatadriven.rebar.sql.client.SqlResultCallback;
import com.bedatadriven.rebar.sql.client.SqlResultSet;
import com.bedatadriven.rebar.sql.client.SqlResultSetRow;
import com.bedatadriven.rebar.sql.client.SqlTransaction;
import com.bedatadriven.rebar.sql.client.query.SqlDialect;
import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.activityinfo.core.shared.workflow.Workflow;
import org.activityinfo.legacy.shared.command.SearchLocations;
import org.activityinfo.legacy.shared.command.result.LocationResult;
import org.activityinfo.legacy.shared.model.LocationDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchLocationsHandler implements CommandHandlerAsync<SearchLocations, LocationResult> {

    private static final int MAX_LOCATIONS = 26;

    private final SqlDialect dialect;

    @Inject
    public SearchLocationsHandler(SqlDialect dialect) {
        super();
        this.dialect = dialect;
    }

    @Override
    public void execute(final SearchLocations command,
                        final ExecutionContext context,
                        final AsyncCallback<LocationResult> callback) {

        // first get a count of how many sites we're talking about
        baseQuery(command).appendColumn("count(*)", "count").execute(context.getTransaction(), new SqlResultCallback() {

            @Override
            public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                int count = results.getRow(0).getInt("count");
                if (count > MAX_LOCATIONS) {
                    LocationResult result = new LocationResult(new ArrayList<LocationDTO>());
                    result.setOffset(0);
                    result.setTotalLength(count);
                    callback.onSuccess(result);
                } else {
                    retrieveLocations(command, context, callback);
                }
            }
        });
    }

    private void retrieveLocations(final SearchLocations command,
                                   final ExecutionContext context,
                                   final AsyncCallback<LocationResult> callback) {
        SqlQuery query = baseQuery(command).appendColumns("LocationId", "Name", "Axe", "X", "Y", "LocationTypeId")
                                           .setLimitClause(dialect.limitClause(0, 26));

        query.execute(context.getTransaction(), new SqlResultCallback() {
            @Override
            public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                // Create a list of locations from query result
                List<LocationDTO> locations = Lists.newArrayList();
                Map<Integer, LocationDTO> locationsById = Maps.newHashMap();
                for (SqlResultSetRow row : results.getRows()) {
                    LocationDTO location = LocationDTO.fromSqlRow(row);
                    locations.add(location);
                    locationsById.put(location.getId(), location);
                }

                LocationResult result = new LocationResult(locations);
                result.setOffset(0);
                result.setTotalLength(results.getRows().size());

                callback.onSuccess(result);
            }

        });
    }

    private SqlQuery baseQuery(final SearchLocations command) {
        SqlQuery query = SqlQuery.select().from("location");
        query.where("workflowStatusId").equalTo(Workflow.VALIDATED);

        if (command.getAdminEntityIds() != null) {
            for (Integer adminEntityId : command.getAdminEntityIds()) {
                query.where("LocationId")
                     .in(SqlQuery.select("LocationId")
                                 .from("locationadminlink")
                                 .where("adminentityid")
                                 .equalTo(adminEntityId));
            }
        }
        query.orderBy("location.name");

        if (command.getLocationTypeId() != 0) {
            query.where("locationTypeID").equalTo(command.getLocationTypeId());
        }
        if (!Strings.isNullOrEmpty(command.getName())) {
            query.where("Name").startsWith(command.getName());
        }
        return query;
    }

}
