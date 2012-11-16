package org.activityinfo.server.report.generator.map;

import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.shared.command.Filter;
import org.activityinfo.shared.command.GetAdminEntities;
import org.activityinfo.shared.command.PivotSites;
import org.activityinfo.shared.command.PivotSites.PivotResult;
import org.activityinfo.shared.command.result.AdminEntityResult;
import org.activityinfo.shared.command.result.Bucket;
import org.activityinfo.shared.dto.AdminEntityDTO;
import org.activityinfo.shared.map.RgbColor;
import org.activityinfo.shared.report.content.AdminMarker;
import org.activityinfo.shared.report.content.AdminOverlay;
import org.activityinfo.shared.report.content.EntityCategory;
import org.activityinfo.shared.report.content.MapContent;
import org.activityinfo.shared.report.model.AdminDimension;
import org.activityinfo.shared.report.model.DimensionType;
import org.activityinfo.shared.report.model.layers.PolygonMapLayer;
import org.activityinfo.shared.util.mapping.Extents;

import com.google.common.base.Function;


public class PolygonLayerGenerator implements LayerGenerator {

	private PolygonMapLayer layer;
	private PivotResult buckets;
	private MagnitudeScaleBuilder.Scale colorScale;
	private AdminOverlay overlay;
	private Jenks breakBuilder = new Jenks();
	
	public PolygonLayerGenerator(PolygonMapLayer layer) {
		super();
		this.layer = layer;
		this.overlay = new AdminOverlay(layer.getAdminLevelId());
		this.overlay.setOutlineColor(layer.getMaxColor());
	}

	@Override
	public void query(DispatcherSync dispatcher, Filter effectiveFilter) {
		Filter layerFilter = new Filter(effectiveFilter, layer.getFilter());
		layerFilter.addRestriction(DimensionType.Indicator, layer.getIndicatorIds());

		queryBounds(dispatcher, layerFilter);
		queryBuckets(dispatcher, layerFilter);	
		color();
	}


	private void queryBounds(DispatcherSync dispatcher, Filter layerFilter) {
		GetAdminEntities query = new GetAdminEntities();
		query.setLevelId(layer.getAdminLevelId());
		
		AdminEntityResult entities = dispatcher.execute(query);
		for(AdminEntityDTO entity : entities.getData()) {
			if(entity.hasBounds()) {
				AdminMarker marker = new AdminMarker(entity);
				overlay.addPolygon(marker);
			}
		}
	}

	private void queryBuckets(DispatcherSync dispatcher, Filter layerFilter) {
		PivotSites query = new PivotSites();
		query.setFilter(layerFilter);
		AdminDimension adminDimension = new AdminDimension(layer.getAdminLevelId());
		query.setDimensions(adminDimension);
		
		MagnitudeScaleBuilder scaleBuilder = new MagnitudeScaleBuilder(layer);
		
		this.buckets = dispatcher.execute(query);
		for(Bucket bucket : buckets.getBuckets()) {
			EntityCategory category = (EntityCategory) bucket.getCategory(adminDimension);
			if(category != null) {
				int adminEntityId = category.getId(); 
				AdminMarker polygon = overlay.getPolygon(adminEntityId);
				if(polygon != null) {
					polygon.setValue(bucket.doubleValue());
					scaleBuilder.addValue(bucket.doubleValue());
				}
			}
		}
		colorScale = scaleBuilder.build();
	}
	

	private void color() {
		for(AdminMarker polygon : overlay.getPolygons()) {
			if(polygon.hasValue()) {
				polygon.setColor(colorScale.color(polygon.getValue()).toHexString());
			} else {
				polygon.setColor(colorScale.color(null).toHexString());
			}
		}
	}

	@Override
	public Extents calculateExtents() {
		Extents extents = Extents.emptyExtents();
		for(AdminMarker polygon : overlay.getPolygons()) {
			if(polygon.hasValue()) {
				extents.grow(polygon.getExtents());
			}
		}
		return extents;
	}

	@Override
	public Margins calculateMargins() {
		return new Margins(0);
	}

	@Override
	public void generate(TiledMap map, MapContent content) {
		content.getAdminOverlays().add(overlay);
		content.getLegends().add(colorScale.legend());
	}
}