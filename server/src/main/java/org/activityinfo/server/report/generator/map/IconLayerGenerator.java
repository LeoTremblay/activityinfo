package org.activityinfo.server.report.generator.map;

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

import org.activityinfo.core.shared.model.AiLatLng;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.legacy.shared.reports.content.IconLayerLegend;
import org.activityinfo.legacy.shared.reports.content.IconMapMarker;
import org.activityinfo.legacy.shared.reports.content.MapContent;
import org.activityinfo.legacy.shared.reports.content.Point;
import org.activityinfo.legacy.shared.reports.model.MapIcon;
import org.activityinfo.legacy.shared.reports.model.PointValue;
import org.activityinfo.legacy.shared.reports.model.layers.IconMapLayer;
import org.activityinfo.legacy.shared.reports.util.mapping.Extents;
import org.activityinfo.server.database.hibernate.entity.Indicator;
import org.activityinfo.server.report.generator.map.cluster.Cluster;
import org.activityinfo.server.report.generator.map.cluster.Clusterer;
import org.activityinfo.server.report.generator.map.cluster.ClustererFactory;
import org.activityinfo.server.report.generator.map.cluster.genetic.MarkerGraph.IntersectionCalculator;
import org.activityinfo.server.report.generator.map.cluster.genetic.MarkerGraph.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IconLayerGenerator extends PointLayerGenerator<IconMapLayer> {

    private MapIcon icon;

    public IconLayerGenerator(IconMapLayer layer, Map<Integer, Indicator> indicators) {
        super(layer, indicators);
        this.icon = new MapIcon(layer.getIcon(), 32, 37, 16, 35);
    }

    public boolean meetsCriteria(SiteDTO site) {
        if (layer.getIndicatorIds().size() != 0) {
            for (Integer indicatorId : layer.getIndicatorIds()) {
                Double indicatorValue = site.getIndicatorDoubleValue(indicatorId);
                if (indicatorValue != null && indicatorValue > 0) {
                    return true;
                }
            }
            return false;
        } else {
            return layer.getActivityIds().contains(site.getActivityId());
        }
    }

    @Override
    public Extents calculateExtents() {
        Extents extents = Extents.emptyExtents();
        for (SiteDTO site : sites) {
            if (meetsCriteria(site)) {
                if (site.hasLatLong()) {
                    extents.grow(site.getLatitude(), site.getLongitude());
                } else {
                    Extents siteExtents = getBounds(site);
                    if (siteExtents != null) {
                        extents.grow(siteExtents);
                    }
                }
            }
        }
        return extents;
    }

    @Override
    public Margins calculateMargins() {
        return new Margins(icon.getAnchorX(),
                icon.getAnchorY(),
                icon.getHeight() - icon.getAnchorY(),
                icon.getWidth() - icon.getAnchorX());
    }

    @Override
    public void generate(TiledMap map, MapContent content) {
        List<PointValue> points = new ArrayList<PointValue>();
        IconRectCalculator rectCalculator = new IconRectCalculator(icon);

        IntersectionCalculator intersectionCalculator = new IntersectionCalculator() {
            @Override
            public boolean intersects(Node a, Node b) {
                return a.getPointValue().getIconRect().intersects(b.getPointValue().getIconRect());
            }
        };

        Clusterer clusterer = ClustererFactory.fromClustering(layer.getClustering(),
                rectCalculator,
                intersectionCalculator);

        for (SiteDTO site : sites) {
            if (meetsCriteria(site)) {
                AiLatLng geoPoint = getPoint(site);
                if (geoPoint != null || clusterer.isMapped(site)) {
                    Point point = null;
                    if (geoPoint != null) {
                        point = map.fromLatLngToPixel(geoPoint);
                    }
                    points.add(new PointValue(site,
                            point,
                            point == null ? null : rectCalculator.iconRect(point),
                            getValue(site, layer.getIndicatorIds())));
                } else {
                    content.getUnmappedSites().add(site.getId());
                }
            }
        }

        List<Cluster> clusters = clusterer.cluster(map, points);
        createMarkersFrom(clusters, map, content);

        IconLayerLegend legend = new IconLayerLegend();
        legend.setDefinition(layer);

        content.addLegend(legend);
    }

    private void createMarkersFrom(List<Cluster> clusters, TiledMap map, MapContent content) {
        for (Cluster cluster : clusters) {
            IconMapMarker marker = new IconMapMarker();
            marker.setX(cluster.getPoint().getX());
            marker.setY(cluster.getPoint().getY());
            AiLatLng latlng = map.fromPixelToLatLng(cluster.getPoint());
            marker.setLat(latlng.getLat());
            marker.setLng(latlng.getLng());
            marker.setTitle(formatTitle(cluster));
            marker.setIcon(icon);
            marker.setIndicatorId(layer.getIndicatorIds().get(0));

            for (PointValue pv : cluster.getPointValues()) {
                marker.getSiteIds().add(pv.getSite().getId());
            }
            content.getMarkers().add(marker);
        }
    }

    protected String formatTitle(Cluster cluster) {
        if (cluster.getPointValues() != null) {
            return Double.toString(cluster.sumValues());
        }
        return "";
    }

}
