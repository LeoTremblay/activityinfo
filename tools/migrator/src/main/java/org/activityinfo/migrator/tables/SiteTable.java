package org.activityinfo.migrator.tables;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.activityinfo.migrator.ResourceMigrator;
import org.activityinfo.migrator.ResourceWriter;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.ReferenceValue;
import org.activityinfo.model.type.enumerated.EnumFieldValue;
import org.activityinfo.model.type.number.Quantity;
import org.activityinfo.model.type.primitive.TextValue;
import org.activityinfo.model.type.time.LocalDate;
import org.activityinfo.model.type.time.LocalDateInterval;

import java.sql.*;
import java.util.Map;
import java.util.Set;

import static org.activityinfo.model.legacy.CuidAdapter.*;

public class SiteTable extends ResourceMigrator {

    @Override
    public void getResources(Connection connection, ResourceWriter writer) throws Exception {

        String sql = "SELECT S.*, " +
                        "LT.boundAdminLevelId, " +
                        "A.ReportingFrequency, " +
                        "A.DatabaseId  " +
                     "FROM site S " +
                     "LEFT JOIN activity A ON (A.activityId=S.activityId) " +
                     "LEFT JOIN locationtype LT ON (A.locationTypeId=LT.locationtypeid) " +
                     "LEFT JOIN userdatabase db ON (A.databaseid=db.DatabaseId) " +
                     "WHERE S.dateDeleted is null and " +
                        " A.dateDeleted is null and " +
                        " db.dateDeleted is null and" +
                        " date1 is not null and date2 is not null";


        Map<Integer, FormInstance> sites = Maps.newHashMap();


        try(Statement statement = connection.createStatement()) {
            try(ResultSet rs = statement.executeQuery(sql)) {

                while(rs.next()) {

                    int siteId = rs.getInt("siteId");
                    ResourceId siteResourceId = resourceId(SITE_DOMAIN, siteId);
                    ResourceId classId = resourceId(ACTIVITY_DOMAIN, rs.getInt("activityId"));

                    FormInstance resource = new FormInstance(siteResourceId, classId);
                    resource.set(field(classId, PARTNER_FIELD), new ReferenceValue(
                            partnerInstanceId(rs.getInt("databaseId"), rs.getInt("partnerId"))));

                    int reportingFrequency = rs.getInt("ReportingFrequency");
                    if(reportingFrequency == ActivityTable.ONCE) {
                       // resource.set(CuidAdapter.DATE_FIELD, dateInterval(rs));

                        resource.set(field(classId, START_DATE_FIELD), new LocalDate(rs.getDate("date1")));
                        resource.set(field(classId, END_DATE_FIELD), new LocalDate(rs.getDate("date2")));
                    }

                    if(!isAdminBound(rs)) {
                        resource.set(field(classId, LOCATION_FIELD),
                                new ReferenceValue(resourceId(LOCATION_DOMAIN, rs.getInt("LocationId"))));
                    }
                    sites.put(siteId, resource);
                }
            }
        }

        populateAttributes(connection, sites);

        populateIndicators(connection, sites);
        populateBoundAdminLevels(connection, sites);

        for(FormInstance site : sites.values()) {
            writer.writeResource(site.asResource());
        }

    }

    private LocalDateInterval dateInterval(ResultSet rs) throws SQLException {
        Date startDate = rs.getDate("date1");
        Date endDate = rs.getDate("date2");
        return new LocalDateInterval(new LocalDate(startDate), new LocalDate(endDate));
    }

    private void populateAttributes(Connection connection, Map<Integer, FormInstance> sites) throws SQLException {

        String sql =
                "SELECT V.siteId, V.attributeid, V.Value, A.attributeGroupId " +
                "FROM attributevalue V " +
                "INNER JOIN attribute A ON (A.attributeId=V.attributeID) " +
                "INNER JOIN attributegroup G on (A.attributeGroupId=G.attributeGroupId) " +
                "WHERE V.value IS NOT NULL and A.dateDeleted is null and G.dateDeleted is null " +
                "ORDER BY V.siteId, A.AttributeGroupId";

        int currentSiteId = -1;
        int currentGroupId = -1;
        Set<ResourceId> currentValue = Sets.newHashSet();

        try(Statement statement = connection.createStatement()) {
            try(ResultSet rs = statement.executeQuery(sql)) {

                while(rs.next()) {

                    int siteId = rs.getInt("siteId");
                    int groupId = rs.getInt("attributeGroupId");

                    if(currentSiteId != siteId || currentGroupId != groupId) {
                        if(!currentValue.isEmpty()) {
                            FormInstance site = sites.get(currentSiteId);
                            if(site != null) {
                                site.set(attributeGroupField(currentGroupId), new EnumFieldValue(currentValue));
                            }
                        }
                        currentSiteId = siteId;
                        currentGroupId = groupId;
                        currentValue = Sets.newHashSet();
                    }

                    int attributeId = rs.getInt("attributeId");
                    currentValue.add(resourceId(ATTRIBUTE_DOMAIN, attributeId));
                }

                if(!currentValue.isEmpty()) {
                    FormInstance site = sites.get(currentSiteId);
                    if(site != null) {
                        site.set(attributeGroupField(currentGroupId), new ReferenceValue(currentValue));
                    }
                }
            }
        }
    }

    private void populateIndicators(Connection connection, Map<Integer, FormInstance> sites) throws SQLException {

        String sql = "SELECT V.*, I.Type, I.Units, RP.SiteId " +
                     "FROM indicatorvalue V " +
                     "LEFT JOIN reportingperiod RP ON (V.ReportingPeriodId=RP.ReportingPeriodId) " +
                     "LEFT JOIN site S ON (RP.SiteId = S.SiteId) " +
                     "LEFT JOIN indicator I ON (V.IndicatorId = I.IndicatorId) " +
                     "LEFT JOIN activity A ON (S.ActivityId = A.ActivityId) " +
                     "WHERE A.ReportingFrequency = 0";

        try(Statement statement = connection.createStatement()) {
            try(ResultSet rs = statement.executeQuery(sql)) {

                while(rs.next()) {

                    int siteId = rs.getInt("siteId");
                    FormInstance site = sites.get(siteId);
                    if(site != null) {

                        int indicatorId = rs.getInt("indicatorId");
                        ResourceId fieldId = indicatorField(indicatorId);
                        String type = rs.getString("Type");

                        switch (type) {
                            case "QUANTITY":
                                double quantity = rs.getDouble("Value");
                                if(!rs.wasNull()) {
                                    site.set(fieldId, new Quantity(quantity, rs.getString("units")));
                                }
                                break;
                            case "NARRATIVE":
                            case "FREE_TEXT":
                                site.set(fieldId, TextValue.valueOf(rs.getString("TextValue")));
                                break;
                        }
                    }
                }
            }
        }
    }


    private void populateBoundAdminLevels(Connection connection, Map<Integer, FormInstance> sites) throws SQLException {

        String sql = "SELECT S.SiteId, E.AdminEntityId " +
                     "FROM site S " +
                     "LEFT JOIN location L ON (S.LocationId = L.LocationID) " +
                     "LEFT JOIN locationtype LT ON (L.LocationTypeID = LT.LocationTypeId) " +
                     "LEFT JOIN locationadminlink LK ON (L.LocationId=LK.LocationId) " +
                     "LEFT JOIN adminentity E ON (LK.AdminEntityId=E.AdminEntityId) " +
                     "WHERE E.AdminLevelId = LT.BoundAdminLevelId";

        try(Statement statement = connection.createStatement()) {
            try(ResultSet rs = statement.executeQuery(sql)) {

                while(rs.next()) {

                    int siteId = rs.getInt("siteId");
                    FormInstance resource = sites.get(siteId);
                    if(resource != null) {
                        ResourceId entityId = resourceId(ADMIN_ENTITY_DOMAIN, rs.getInt("adminEntityId"));
                        assert entityId != null;
                        resource.set(field(resource.getClassId(), LOCATION_FIELD), new ReferenceValue(entityId));
                    }
                }
            }
        }
    }

    private boolean isAdminBound(ResultSet rs) throws SQLException {
        rs.getInt("boundAdminLevelId");
        return !rs.wasNull();
    }
}
