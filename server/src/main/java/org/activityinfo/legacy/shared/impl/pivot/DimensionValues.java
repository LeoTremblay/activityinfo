package org.activityinfo.legacy.shared.impl.pivot;

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

import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import org.activityinfo.legacy.shared.command.DimensionType;
import org.activityinfo.legacy.shared.command.PivotSites;
import org.activityinfo.legacy.shared.command.PivotSites.ValueType;
import org.activityinfo.legacy.shared.impl.Tables;
import org.activityinfo.legacy.shared.model.IndicatorDTO;
import org.activityinfo.legacy.shared.reports.content.TargetCategory;

/**
 * Base table for retrieving pivotfilter-data based on other pivot criteria. Examples are attributegroups, partners and
 * daterange.
 */
public class DimensionValues extends BaseTable {

    private SqlQuery query;

    @Override
    public boolean accept(PivotSites command) {
        return command.getValueType() == ValueType.DIMENSION;
    }

    @Override
    public void setupQuery(PivotSites command, SqlQuery query) {
        this.query = query;

        if (command.getFilter().isRestricted(DimensionType.Indicator)) {
            // we only need to pull in indicator values if there is a filter on indicators
            query.from(Tables.INDICATOR_VALUE, "V");
            query.leftJoin(Tables.REPORTING_PERIOD, "RP").on("V.ReportingPeriodId = RP.ReportingPeriodId");
            query.leftJoin(Tables.SITE, "Site").on("RP.SiteId = Site.SiteId");

        } else {
            query.from(Tables.SITE, "Site");
            if( command.getFilter().isDateRestricted() ) {
                query.leftJoin(Tables.REPORTING_PERIOD, "RP").on("RP.SiteId = Site.SiteId");
            }
        }

        query.leftJoin(Tables.ACTIVITY, "Activity").on("Activity.ActivityId = Site.ActivityId");
        query.leftJoin(Tables.USER_DATABASE, "UserDatabase").on("Activity.DatabaseId = UserDatabase.DatabaseId");

        if(command.isPivotedBy(DimensionType.Attribute) ||
           command.isPivotedBy(DimensionType.AttributeGroup)) {
            query.leftJoin(Tables.ATTRIBUTE_VALUE, "AttributeValue").on("Site.SiteId = AttributeValue.SiteId");
            query.leftJoin(Tables.ATTRIBUTE, "Attribute").on("AttributeValue.AttributeId = Attribute.AttributeId");

            if(command.isPivotedBy(DimensionType.AttributeGroup)) {
                query.leftJoin(Tables.ATTRIBUTE_GROUP, "AttributeGroup")
                        .on("Attribute.AttributeGroupId = AttributeGroup.AttributeGroupId");
            }
        }
        // dummy data
        query.appendColumn("0", ValueFields.COUNT);
        query.appendColumn(Integer.toString(IndicatorDTO.AGGREGATE_SITE_COUNT), ValueFields.AGGREGATION);

        // QUICK FIX: limit result sets returned until we have a dedicated query for this
        query.setLimitClause("LIMIT 50");
    }

    @Override
    public String getDimensionIdColumn(DimensionType type) {
        switch (type) {
            case Partner:
                return "Site.PartnerId";
            case Activity:
                return "Site.ActivityId";
            case Database:
                return "Activity.DatabaseId";
            case Site:
                return "Site.SiteId";
            case Project:
                return "Site.ProjectId";
            case Location:
                return "Site.LocationId";
            case Indicator:
                return "V.IndicatorId";
            case Attribute:
                return "AttributeValue.AttributeId";
            case AttributeGroup:
                return "Attribute.AttributeGroupId";
        }
        throw new UnsupportedOperationException(type.name());
    }

    @Override
    public String getDateCompleteColumn() {
        return "RP.Date2";
    }

    @Override
    public TargetCategory getTargetCategory() {
        return TargetCategory.REALIZED;
    }

    @Override
    public SqlQuery createSqlQuery() {
        return SqlQuery.selectDistinct();
    }

    @Override
    public boolean groupDimColumns() {
        return false;
    }
}
