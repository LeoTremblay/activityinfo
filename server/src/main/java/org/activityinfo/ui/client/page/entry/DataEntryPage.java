package org.activityinfo.ui.client.page.entry;

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

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.callback.SuccessCallback;
import org.activityinfo.legacy.client.monitor.MaskingAsyncMonitor;
import org.activityinfo.legacy.shared.adapter.ResourceLocatorAdaptor;
import org.activityinfo.legacy.shared.command.*;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.legacy.shared.model.SchemaDTO;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.ui.client.ClientContext;
import org.activityinfo.ui.client.EventBus;
import org.activityinfo.ui.client.component.importDialog.ImportPresenter;
import org.activityinfo.ui.client.page.NavigationCallback;
import org.activityinfo.ui.client.page.NavigationEvent;
import org.activityinfo.ui.client.page.NavigationHandler;
import org.activityinfo.ui.client.page.Page;
import org.activityinfo.ui.client.page.PageId;
import org.activityinfo.ui.client.page.PageState;
import org.activityinfo.ui.client.page.common.toolbar.ActionListener;
import org.activityinfo.ui.client.page.common.toolbar.ActionToolBar;
import org.activityinfo.ui.client.page.common.toolbar.UIActions;
import org.activityinfo.ui.client.page.entry.column.DefaultColumnModelProvider;
import org.activityinfo.ui.client.page.entry.form.PrintDataEntryForm;
import org.activityinfo.ui.client.page.entry.form.SiteDialogCallback;
import org.activityinfo.ui.client.page.entry.form.SiteDialogLauncher;
import org.activityinfo.ui.client.page.entry.grouping.GroupingComboBox;
import org.activityinfo.ui.client.page.entry.place.DataEntryPlace;
import org.activityinfo.ui.client.page.entry.sitehistory.SiteHistoryTab;
import org.activityinfo.ui.client.page.report.ExportDialog;
import org.activityinfo.ui.client.style.legacy.icon.IconImageBundle;

import java.util.Set;

/**
 * This is the container for the DataEntry page.
 */
public class DataEntryPage extends LayoutContainer implements Page, ActionListener {

    private static final boolean IMPORT_FUNCTION_ENABLED = true;

    public static final PageId PAGE_ID = new PageId("data-entry");

    private final Dispatcher dispatcher;
    private final EventBus eventBus;

    private GroupingComboBox groupingComboBox;

    private FilterPane filterPane;

    private SiteGridPanel gridPanel;
    private CollapsibleTabPanel tabPanel;

    private DetailTab detailTab;

    private MonthlyReportsPanel monthlyPanel;
    private TabItem monthlyTab;

    private DataEntryPlace currentPlace = new DataEntryPlace();

    private AttachmentsTab attachmentsTab;

    private SiteHistoryTab siteHistoryTab;

    private ActionToolBar toolBar;

    @Inject
    public DataEntryPage(final EventBus eventBus, Dispatcher dispatcher) {
        this.eventBus = eventBus;
        this.dispatcher = dispatcher;

        setLayout(new BorderLayout());

        addFilterPane();
        addCenter();
    }

    private void addFilterPane() {
        filterPane = new FilterPane(dispatcher);
        BorderLayoutData filterLayout = new BorderLayoutData(LayoutRegion.WEST);
        filterLayout.setCollapsible(true);
        filterLayout.setMargins(new Margins(0, 5, 0, 0));
        filterLayout.setSplit(true);
        add(filterPane, filterLayout);

        filterPane.getSet().addValueChangeHandler(new ValueChangeHandler<Filter>() {

            @Override
            public void onValueChange(ValueChangeEvent<Filter> event) {
                eventBus.fireEvent(new NavigationEvent(NavigationHandler.NAVIGATION_REQUESTED,
                        currentPlace.copy().setFilter(event.getValue())));
            }
        });
    }

    private void addCenter() {
        gridPanel = new SiteGridPanel(dispatcher, new DefaultColumnModelProvider(dispatcher));
        gridPanel.setTopComponent(createToolBar());

        LayoutContainer center = new LayoutContainer();
        center.setLayout(new BorderLayout());

        center.add(gridPanel, new BorderLayoutData(LayoutRegion.CENTER));

        gridPanel.addSelectionChangedListener(new SelectionChangedListener<SiteDTO>() {

            @Override
            public void selectionChanged(SelectionChangedEvent<SiteDTO> se) {
                onSiteSelected(se);
            }
        });

        detailTab = new DetailTab(dispatcher);

        monthlyPanel = new MonthlyReportsPanel(dispatcher);
        monthlyTab = new TabItem(I18N.CONSTANTS.monthlyReports());
        monthlyTab.setLayout(new FitLayout());
        monthlyTab.add(monthlyPanel);

        attachmentsTab = new AttachmentsTab(dispatcher, eventBus);

        siteHistoryTab = new SiteHistoryTab(dispatcher);

        tabPanel = new CollapsibleTabPanel();
        tabPanel.add(detailTab);
        tabPanel.add(monthlyTab);
        tabPanel.add(attachmentsTab);
        tabPanel.add(siteHistoryTab);
        tabPanel.setSelection(detailTab);
        center.add(tabPanel, tabPanel.getBorderLayoutData());
        onNoSelection();
        add(center, new BorderLayoutData(LayoutRegion.CENTER));
    }

    private ActionToolBar createToolBar() {
        toolBar = new ActionToolBar(this);

        groupingComboBox = new GroupingComboBox(dispatcher);
        groupingComboBox.withSelectionListener(new Listener<FieldEvent>() {

            @Override
            public void handleEvent(FieldEvent be) {
                eventBus.fireEvent(new NavigationEvent(NavigationHandler.NAVIGATION_REQUESTED,
                        currentPlace.copy().setGrouping(groupingComboBox.getGroupingModel())));
            }
        });

        toolBar.add(new Label(I18N.CONSTANTS.grouping()));
        toolBar.add(groupingComboBox);

        toolBar.addButton(UIActions.ADD, I18N.CONSTANTS.newSite(), IconImageBundle.ICONS.add());
        toolBar.addButton(UIActions.EDIT, I18N.CONSTANTS.edit(), IconImageBundle.ICONS.edit());
        toolBar.addDeleteButton(I18N.CONSTANTS.deleteSite());

        toolBar.add(new SeparatorToolItem());

        if (IMPORT_FUNCTION_ENABLED) {
            toolBar.addImportButton();
        }
        toolBar.addExcelExportButton();

        toolBar.addPrintButton();
        toolBar.addButton("EMBED", I18N.CONSTANTS.embed(), IconImageBundle.ICONS.embed());


        return toolBar;
    }

    private void onSiteSelected(final SelectionChangedEvent<SiteDTO> se) {
        if (se.getSelection().isEmpty()) {
            onNoSelection();
        } else {
            final SiteDTO site = se.getSelectedItem();
            int activityId = site.getActivityId();

            dispatcher.execute(new GetActivityForm(activityId), new AsyncCallback<ActivityFormDTO>() {

                @Override
                public void onFailure(Throwable caught) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onSuccess(ActivityFormDTO activity) {
                    updateSelection(activity, site);
                }
            });
        }
    }

    private void updateSelection(ActivityFormDTO activity, SiteDTO site) {

        boolean permissionToEdit = activity.isAllowedToEdit(site);
        toolBar.setActionEnabled(UIActions.EDIT, permissionToEdit && !site.isLinked());
        toolBar.setActionEnabled(UIActions.DELETE, permissionToEdit && !site.isLinked());

        detailTab.setSite(site);
        attachmentsTab.setSite(site);
        if (activity.getReportingFrequency() == ActivityFormDTO.REPORT_MONTHLY) {
            monthlyPanel.load(site);
            monthlyPanel.setReadOnly(!permissionToEdit);
            monthlyTab.setEnabled(true);
        } else {
            monthlyTab.setEnabled(false);
            if (tabPanel.getSelectedItem() == monthlyTab) {
                tabPanel.setSelection(detailTab);
            }
        }
        siteHistoryTab.setSite(site);
    }

    private void onNoSelection() {
        toolBar.setActionEnabled(UIActions.EDIT, false);
        toolBar.setActionEnabled(UIActions.DELETE, false);
        monthlyPanel.onNoSelection();
    }

    @Override
    public void shutdown() {
        // TODO Auto-generated method stub
    }

    @Override
    public PageId getPageId() {
        return PAGE_ID;
    }

    @Override
    public Object getWidget() {
        return this;
    }

    @Override
    public void requestToNavigateAway(PageState place, NavigationCallback callback) {
        callback.onDecided(true);
    }

    @Override
    public String beforeWindowCloses() {
        return null;
    }

    @Override
    public boolean navigate(PageState place) {
        currentPlace = (DataEntryPlace) place;
        if (!currentPlace.getFilter().isRestricted(DimensionType.Activity) &&
            !currentPlace.getFilter().isRestricted(DimensionType.Database)) {

            redirectToFirstActivity();
        } else {
            doNavigate();
        }
        return true;
    }

    private void redirectToFirstActivity() {
        dispatcher.execute(new GetSchema(), new AsyncCallback<SchemaDTO>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(SchemaDTO result) {
                for (UserDatabaseDTO db : result.getDatabases()) {
                    if (!db.getActivities().isEmpty()) {
                        currentPlace.getFilter()
                                    .addRestriction(DimensionType.Activity, db.getActivities().get(0).getId());
                        doNavigate();
                        return;
                    }
                }
            }
        });
    }

    private void doNavigate() {
        Filter filter = currentPlace.getFilter();

        gridPanel.load(currentPlace.getGrouping(), filter);
        groupingComboBox.setFilter(filter);
        filterPane.getSet().applyBaseFilter(filter);

        // currently the print form only does one activity
        Set<Integer> activities = filter.getRestrictions(DimensionType.Activity);
        toolBar.setActionEnabled(UIActions.PRINT, activities.size() == 1);

        // also embedding is only implemented for one activity
        toolBar.setActionEnabled("EMBED", activities.size() == 1);

        if (IMPORT_FUNCTION_ENABLED) {
            toolBar.setActionEnabled(UIActions.IMPORT, activities.size() == 1);
        }

        // adding is also only enabled for one activity, but we have to
        // lookup to see whether it possible for this activity
        toolBar.setActionEnabled(UIActions.ADD, false);
        if (activities.size() == 1) {
            enableToolbarButtons(activities.iterator().next());
        }
        onNoSelection();
    }

    private void enableToolbarButtons(final int activityId) {
        dispatcher.execute(new GetSchema(), new AsyncCallback<SchemaDTO>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(SchemaDTO result) {
                boolean isAllowed = result.getActivityById(activityId).isEditAllowed();
                toolBar.setActionEnabled(UIActions.ADD, isAllowed);
                if (IMPORT_FUNCTION_ENABLED) {
                    toolBar.setActionEnabled("IMPORT", isAllowed);
                }
            }
        });
    }


    @Override
    public void onUIAction(String actionId) {
        if (UIActions.ADD.equals(actionId)) {

            SiteDialogLauncher formHelper = new SiteDialogLauncher(dispatcher);
            formHelper.addSite(currentPlace.getFilter(), new SiteDialogCallback() {

                @Override
                public void onSaved() {
                    gridPanel.refresh();
                }
            });

        } else if (UIActions.EDIT.equals(actionId)) {
            final SiteDTO selection = gridPanel.getSelection();
            SiteDialogLauncher launcher = new SiteDialogLauncher(dispatcher);
            launcher.editSite(selection, new SiteDialogCallback() {

                @Override
                public void onSaved() {
                    gridPanel.refresh();
                }
            });
        } else if (UIActions.DELETE.equals(actionId)) {
            MessageBox.confirm(ClientContext.getAppTitle(),
                    I18N.MESSAGES.confirmDeleteSite(),
                    new Listener<MessageBoxEvent>() {
                        @Override
                        public void handleEvent(MessageBoxEvent be) {
                            if (be.getButtonClicked().getItemId().equals(Dialog.YES)) {
                                delete();
                            }
                        }
                    });

        } else if (UIActions.PRINT.equals(actionId)) {
            int activityId = currentPlace.getFilter().getRestrictedCategory(DimensionType.Activity);
            PrintDataEntryForm form = new PrintDataEntryForm(dispatcher);
            form.print(activityId);

        } else if (UIActions.EXPORT.equals(actionId)) {
            ExportDialog dialog = new ExportDialog(dispatcher);
            dialog.exportSites(currentPlace.getFilter());

        } else if ("EMBED".equals(actionId)) {
            EmbedDialog dialog = new EmbedDialog(dispatcher);
            dialog.show(currentPlace);

        } else if (IMPORT_FUNCTION_ENABLED && UIActions.IMPORT.equals(actionId)) {
            doImport();

        }

    }

    protected void doImport() {
        final int activityId = currentPlace.getFilter().getRestrictedCategory(DimensionType.Activity);
        final ResourceLocatorAdaptor resourceLocator = new ResourceLocatorAdaptor(dispatcher);
        ImportPresenter.showPresenter(CuidAdapter.activityFormClass(activityId), resourceLocator)
                       .then(new SuccessCallback<ImportPresenter>() {
                           @Override
                           public void onSuccess(ImportPresenter result) {
                               result.show();
                           }
                       });
    }

    private void delete() {
        dispatcher.execute(new DeleteSite(gridPanel.getSelection().getId()),
                new MaskingAsyncMonitor(this, I18N.CONSTANTS.deleting()),
                new AsyncCallback<VoidResult>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        // handled by monitor
                    }

                    @Override
                    public void onSuccess(VoidResult result) {
                        gridPanel.refresh();
                    }
                });
    }
}
