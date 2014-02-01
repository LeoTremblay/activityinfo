package org.activityinfo.ui.full.client.report.editor.map.layerOptions;

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

import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.form.Radio;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasValue;
import org.activityinfo.reports.shared.model.clustering.AdministrativeLevelClustering;
import org.activityinfo.reports.shared.model.clustering.AutomaticClustering;
import org.activityinfo.reports.shared.model.clustering.Clustering;
import org.activityinfo.reports.shared.model.clustering.NoClustering;
import org.activityinfo.reports.shared.model.layers.MapLayer;
import org.activityinfo.api.shared.command.GetSchema;
import org.activityinfo.api.shared.model.*;
import org.activityinfo.api.client.Dispatcher;
import org.activityinfo.ui.full.client.i18n.I18N;

import java.util.List;
import java.util.Set;

/*
 * Shows a list of options to aggregate markers on the map
 */
public class ClusteringOptionsWidget extends LayoutContainer implements
        HasValue<Clustering> {

    private Clustering value = new NoClustering();

    private class ClusteringRadio extends Radio {
        private Clustering clustering;

        ClusteringRadio(String label, Clustering clustering) {
            this.clustering = clustering;
            this.setBoxLabel(label);
        }

        public Clustering getClustering() {
            return clustering;
        }
    }

    private List<ClusteringRadio> radios;
    private RadioGroup radioGroup;
    private Dispatcher service;

    public ClusteringOptionsWidget(Dispatcher service) {
        super();

        this.service = service;
    }

    private void destroyForm() {
        if (radioGroup != null) {
            radioGroup.removeAllListeners();
        }
        radioGroup = null;
        removeAll();
    }

    public void loadForm(final MapLayer layer) {
        // mask();
        destroyForm();

        service.execute(new GetSchema(), new AsyncCallback<SchemaDTO>() {

            @Override
            public void onFailure(Throwable caught) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onSuccess(SchemaDTO schema) {
                buildForm(countriesForLayer(schema, layer));
            }
        });
    }

    private void buildForm(Set<CountryDTO> countries) {

        radios = Lists.newArrayList();
        radios.add(new ClusteringRadio(I18N.CONSTANTS.none(),
                new NoClustering()));
        radios.add(new ClusteringRadio(I18N.CONSTANTS.automatic(),
                new AutomaticClustering()));

        if (countries.size() == 1) {
            CountryDTO country = countries.iterator().next();
            for (AdminLevelDTO level : country.getAdminLevels()) {

                AdministrativeLevelClustering clustering = new AdministrativeLevelClustering();
                clustering.getAdminLevels().add(level.getId());

                radios.add(new ClusteringRadio(level.getName(), clustering));
            }
        }
        radioGroup = new RadioGroup();
        radioGroup.setOrientation(Orientation.VERTICAL);
        radioGroup.setStyleAttribute("padding", "5px");
        for (ClusteringRadio radio : radios) {
            radioGroup.add(radio);
            if (radio.getClustering().equals(value)) {
                radioGroup.setValue(radio);
            }
        }
        add(radioGroup);

        radioGroup.addListener(Events.Change, new Listener<FieldEvent>() {
            @Override
            public void handleEvent(FieldEvent be) {
                ClusteringRadio radio = (ClusteringRadio) radioGroup.getValue();
                setValue(radio.getClustering(), true);
            }
        });
        layout();
        // unmask();
    }

    private Set<CountryDTO> countriesForLayer(SchemaDTO schema, MapLayer layer) {
        Set<Integer> indicatorIds = Sets.newHashSet(layer.getIndicatorIds());
        Set<CountryDTO> countries = Sets.newHashSet();
        for (UserDatabaseDTO database : schema.getDatabases()) {
            if (databaseContainsIndicatorId(database, indicatorIds)) {
                countries.add(database.getCountry());
            }
        }
        return countries;
    }

    private boolean databaseContainsIndicatorId(UserDatabaseDTO database,
                                                Set<Integer> indicatorIds) {

        for (ActivityDTO activity : database.getActivities()) {
            for (IndicatorDTO indicator : activity.getIndicators()) {
                if (indicatorIds.contains(indicator.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<Clustering> handler) {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    @Override
    public Clustering getValue() {
        return ((ClusteringRadio) radioGroup.getValue()).getClustering();
    }

    @Override
    public void setValue(Clustering value) {
        setValue(value, true);
    }

    @Override
    public void setValue(Clustering value, boolean fireEvents) {
        this.value = value;
        if (radioGroup != null) {
            updateSelectedRadio();
        }
        if (fireEvents) {
            ValueChangeEvent.fire(this, value);
        }
    }

    private void updateSelectedRadio() {
        for (ClusteringRadio radio : radios) {
            if (radio.getClustering().equals(value)) {
                radioGroup.setValue(radio);
                return;
            }
        }
    }
}